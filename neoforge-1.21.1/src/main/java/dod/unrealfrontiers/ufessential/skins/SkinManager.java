package dod.unrealfrontiers.ufessential.skins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.UFServerConfig;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-side skins: puts the backend's Mojang-signed "textures" property into players' game profiles, so every
 * vanilla client shows the skin without a client mod.
 * <ul>
 *     <li>On join (configuration phase) the skin is fetched with a short timeout and written into the profile
 *     before the player is announced to anyone. A late answer is applied live.</li>
 *     <li>Live changes re-send the player's tab-list entry to everyone and respawn the player entity for the
 *     players watching it. The player's own client caches its entry, so they see their own new skin after rejoining.</li>
 *     <li>Online players are polled in batches, picking up skin changes made on the site.</li>
 * </ul>
 * All profile changes and packets happen on the server thread.
 */
public final class SkinManager {
    private static final String TEXTURES = "textures";
    private static final int LOOKUP_BATCH = 200;

    /** Last skin the backend reported per player; absent = no skin applied by us. */
    private static final Map<UUID, SkinService.Skin> CACHE = new ConcurrentHashMap<>();
    /** Players an admin cleared with /ufskin clear; the poll leaves them alone until a refresh or rejoin. */
    private static final Set<UUID> CLEARED = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean POLL_IN_FLIGHT = new AtomicBoolean();
    private static final AtomicBoolean WARNED_NO_KEY = new AtomicBoolean();
    private static int ticksSincePoll;

    private SkinManager() {}

    /** Skins work on a dedicated server with the feature on and an API key set. */
    public static boolean isActive() {
        if (!FMLEnvironment.dist.isDedicatedServer() || !UFServerConfig.SKINS_ENABLED.get()) {
            return false;
        }
        if (apiKey().isBlank()) {
            if (WARNED_NO_KEY.compareAndSet(false, true)) {
                UFEssential.LOGGER.warn("[UF] Skins are enabled but skins.apiKey is empty in ufessential-common.toml; skins are off");
            }
            return false;
        }
        return true;
    }

    /** Configuration phase: fetch the skin, write it into the profile, then let the player continue. */
    static void onConfigure(ServerConfigurationPacketListener listener, Runnable finishTask) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        GameProfile profile = ((ServerCommonPacketListenerImpl) listener).getOwner();
        UUID id = profile.getId();
        CLEARED.remove(id);

        CompletableFuture<Optional<SkinService.Skin>> fetch =
                SkinService.fetch(profile.getName(), apiKey(), Duration.ofMillis(UFServerConfig.SKINS_REQUEST_TIMEOUT_MS.get()));

        fetch.copy()
                .orTimeout(UFServerConfig.SKINS_JOIN_TIMEOUT_MS.get(), TimeUnit.MILLISECONDS)
                .whenComplete((skin, error) -> server.execute(() -> {
                    if (error == null) {
                        remember(id, skin.orElse(null));
                    } else {
                        logFailure("Skin of " + profile.getName() + " not ready at join, using cache", error);
                    }
                    SkinService.Skin known = CACHE.get(id);
                    if (known != null) {
                        setTextures(profile, known);
                    }
                    if (listener.getConnection().isConnected()) {
                        finishTask.run();
                    }
                }));

        // Answer after the join timeout: apply it live once the player is in the game
        fetch.whenComplete((skin, error) -> {
            if (error == null) {
                server.execute(() -> {
                    remember(id, skin.orElse(null));
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player != null && !CLEARED.contains(id)) {
                        applyLive(player, skin.orElse(null));
                    }
                });
            }
        });
    }

    /** Re-fetches one player's skin and applies it live. Completes on the server thread with the applied skin. */
    public static CompletableFuture<Optional<SkinService.Skin>> refresh(ServerPlayer player) {
        UUID id = player.getUUID();
        CLEARED.remove(id);
        return SkinService.fetch(player.getGameProfile().getName(), apiKey(), Duration.ofMillis(UFServerConfig.SKINS_REQUEST_TIMEOUT_MS.get()))
                .thenApplyAsync(skin -> {
                    remember(id, skin.orElse(null));
                    if (!player.hasDisconnected()) {
                        applyLive(player, skin.orElse(null));
                    }
                    return skin;
                }, player.server);
    }

    /** Removes the skin until the next refresh or rejoin. Server thread. */
    public static void clear(ServerPlayer player) {
        CLEARED.add(player.getUUID());
        applyLive(player, null);
    }

    public static void onPlayerLeft(UUID id) {
        CLEARED.remove(id);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticksSincePoll < UFServerConfig.SKINS_POLL_INTERVAL_SECONDS.get() * 20) {
            return;
        }
        ticksSincePoll = 0;
        MinecraftServer server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty() || !isActive() || !POLL_IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }

        List<CompletableFuture<Void>> batches = new ArrayList<>();
        for (int start = 0; start < players.size(); start += LOOKUP_BATCH) {
            List<ServerPlayer> batch = List.copyOf(players.subList(start, Math.min(start + LOOKUP_BATCH, players.size())));
            List<String> names = batch.stream().map(p -> p.getGameProfile().getName()).toList();
            batches.add(SkinService.lookup(names, apiKey(), Duration.ofMillis(UFServerConfig.SKINS_REQUEST_TIMEOUT_MS.get()))
                    .thenAcceptAsync(skins -> applyPolled(batch, skins), server));
        }
        CompletableFuture.allOf(batches.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
            POLL_IN_FLIGHT.set(false);
            if (error != null) {
                logFailure("Skin poll failed", error);
            }
        });
    }

    private static void applyPolled(List<ServerPlayer> batch, Map<String, SkinService.Skin> skins) {
        for (ServerPlayer player : batch) {
            UUID id = player.getUUID();
            if (player.hasDisconnected() || CLEARED.contains(id)) {
                continue;
            }
            SkinService.Skin skin = skins.get(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
            SkinService.Skin previous = CACHE.get(id);
            if (skin == null && previous == null) {
                continue; // never had one of ours: leave whatever the profile has
            }
            remember(id, skin);
            applyLive(player, skin);
        }
    }

    private static void remember(UUID id, SkinService.Skin skin) {
        if (skin == null) {
            CACHE.remove(id);
        } else {
            CACHE.put(id, skin);
        }
    }

    /**
     * Writes the skin into the player's profile and makes clients pick it up: everyone gets the tab-list entry again,
     * and players watching the entity see it despawn and respawn. Does nothing if the profile already matches.
     */
    private static void applyLive(ServerPlayer player, SkinService.Skin skin) {
        if (!setTextures(player.getGameProfile(), skin)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Set<ServerPlayer> watchers = new HashSet<>(level.getChunkSource().chunkMap.getPlayersWatching(player));
        // Only used to build the spawn packets; the real tracker entry stays untouched
        ServerEntity pairing = new ServerEntity(level, player, 1, true, packet -> {});
        var infoRemove = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        var infoAdd = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));

        for (ServerPlayer receiver : player.server.getPlayerList().getPlayers()) {
            boolean watching = watchers.contains(receiver);
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            if (watching) {
                packets.add(new ClientboundRemoveEntitiesPacket(player.getId()));
            }
            packets.add(infoRemove);
            packets.add(infoAdd);
            if (watching) {
                pairing.sendPairingData(receiver, new PacketAndPayloadAcceptor<>(packets::add));
            }
            receiver.connection.send(new ClientboundBundlePacket(packets));
        }
        UFEssential.LOGGER.info("[UF] {} skin of {}", skin == null ? "Removed" : "Applied", player.getGameProfile().getName());
    }

    /** Replaces the profile's textures property. Returns false if nothing changed. */
    private static boolean setTextures(GameProfile profile, SkinService.Skin skin) {
        PropertyMap properties = profile.getProperties();
        Property current = properties.get(TEXTURES).stream().findFirst().orElse(null);
        if (skin == null) {
            if (current == null) {
                return false;
            }
            properties.removeAll(TEXTURES);
            return true;
        }
        if (current != null && skin.value().equals(current.value()) && skin.signature().equals(current.signature())) {
            return false;
        }
        properties.removeAll(TEXTURES);
        properties.put(TEXTURES, new Property(TEXTURES, skin.value(), skin.signature()));
        return true;
    }

    private static String apiKey() {
        return UFServerConfig.SKINS_API_KEY.get().trim();
    }

    private static void logFailure(String message, Throwable error) {
        if (UFServerConfig.SKINS_LOG_FAILURES.get()) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            UFEssential.LOGGER.warn("[UF] {}: {}", message, cause.toString());
        }
    }
}
