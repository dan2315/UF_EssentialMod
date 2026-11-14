package dod.unrealfrontiers.ufessential.Network;

import com.mojang.authlib.GameProfile;
import dod.unrealfrontiers.ufessential.Auth.AuthService;
import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.UFServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * A server of the network lets a player in only with an entry pass: the backend issues one when the front server sends
 * the player here or another server of the network transfers them (/redirect, portals, the Depths lift), for this
 * player and this server, for a minute. So nobody joins a server of the network directly, around the front server.
 * Operators get in without one (tests, moderation). When the backend can't be asked, players are let in (the front
 * server then sends everyone to its fallback address without passes). Only with {@code network.requirePass}.
 */
public record EntryPassTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final Type TYPE = new Type(ResourceLocation.fromNamespaceAndPath(UFEssential.MODID, "entry_pass"));
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public static boolean active(MinecraftServer server) {
        return server != null && server.isDedicatedServer() && UFServerConfig.NETWORK_REQUIRE_PASS.get()
                && !NetworkPresence.serverId().isEmpty() && !NetworkPresence.apiKey().isEmpty();
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerConfigurationPacketListenerImpl impl = (ServerConfigurationPacketListenerImpl) listener;
        GameProfile profile = impl.getOwner();
        if (server == null || server.getPlayerList().isOp(profile)) {
            listener.finishCurrentTask(TYPE);
            return;
        }
        String path = "/network/tickets/consume?server=" + URLEncoder.encode(NetworkPresence.serverId(), StandardCharsets.UTF_8)
                + "&nickname=" + URLEncoder.encode(profile.getName(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(AuthService.getBaseUrl() + path))
                .timeout(TIMEOUT)
                .header("x-api-key", NetworkPresence.apiKey())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> server.execute(() -> {
            if (!impl.getConnection().isConnected()) return;
            if (error != null) {
                UFEssential.LOGGER.warn("[UF] Entry pass of {} not checked, letting in: {}", profile.getName(), error.toString());
            } else if (response.statusCode() == 403) {
                UFEssential.LOGGER.info("[UF] {} came in directly, without an entry pass: turned away", profile.getName());
                String entry = UFServerConfig.NETWORK_ENTRY_ADDRESS.get().trim();
                impl.disconnect(Component.literal(entry.isEmpty()
                        ? "Этот сервер входит в сеть: заходите через её общий адрес"
                        : "Этот сервер входит в сеть: заходите через " + entry));
                return;
            } else if (response.statusCode() != 200) {
                UFEssential.LOGGER.warn("[UF] Entry pass of {}: backend answered HTTP {}, letting in", profile.getName(), response.statusCode());
            }
            listener.finishCurrentTask(TYPE);
        }));
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
