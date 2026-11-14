package dod.unrealfrontiers.ufessential.Network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dod.unrealfrontiers.ufessential.Auth.AuthService;
import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Sends a player to another server of the network (/redirect, portals) with the vanilla transfer packet (1.20.5+):
 * the backend gives the target's address if it is open, up and not full ({@code GET /network/servers/{id}/target}),
 * then the client leaves and connects there by itself. The target needs {@code accepts-transfers=true}.
 */
public final class ServerTransfer {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    /** A player is sent at most once in this time: a portal fires every tick while they crouch on it. */
    private static final long COOLDOWN_MS = 3000;
    private static final long SUGGESTIONS_TTL_MS = 30_000;

    private static final Map<UUID, Long> RECENT = new ConcurrentHashMap<>();
    private static volatile List<String> knownServers = List.of();
    private static volatile long knownServersAt;

    private ServerTransfer() {}

    /**
     * Sends the player to {@code serverId}. What went wrong (unknown server, down, full, no backend) goes to
     * {@code onFailure} on the server thread; nothing is called on success, the player just leaves.
     */
    public static void send(ServerPlayer player, String serverId, Consumer<Component> onFailure) {
        MinecraftServer server = player.server;
        String apiKey = NetworkPresence.apiKey();
        if (apiKey.isEmpty()) {
            onFailure.accept(Component.literal("Переходы между серверами не настроены"));
            return;
        }
        if (serverId.equalsIgnoreCase(NetworkPresence.serverId())) {
            onFailure.accept(Component.literal("Вы уже на сервере " + serverId));
            return;
        }
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = RECENT.get(uuid);
        if (last != null && now - last < COOLDOWN_MS) return;
        RECENT.put(uuid, now);

        // With the player's name: the backend gives them an entry pass to the target
        String path = "/network/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/target?player="
                + URLEncoder.encode(player.getGameProfile().getName(), StandardCharsets.UTF_8);
        CLIENT.sendAsync(get(path, apiKey), HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> server.execute(() -> {
                    ServerPlayer target = server.getPlayerList().getPlayer(uuid);
                    if (target == null) return;
                    if (error != null) {
                        UFEssential.LOGGER.warn("[UF] Transfer of {} to {}: backend unavailable: {}", target.getGameProfile().getName(), serverId, error.toString());
                        onFailure.accept(Component.literal("Не удалось связаться с сетью, попробуйте позже"));
                        return;
                    }
                    Component refusal = refusal(serverId, response.statusCode(), response.body());
                    if (refusal != null) {
                        onFailure.accept(refusal);
                        return;
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String host = json.get("host").getAsString();
                    int port = json.get("port").getAsInt();
                    UFEssential.LOGGER.info("[UF] Transferring {} to {} ({}:{})", target.getGameProfile().getName(), serverId, host, port);
                    target.connection.send(new ClientboundTransferPacket(host, port));
                }));
    }

    /** Null when the backend gave an address, else what to tell the player. */
    private static Component refusal(String serverId, int status, String body) {
        if (status == 200) return null;
        if (status == 404) return Component.literal("Сервера «" + serverId + "» нет");
        if (status == 503) {
            String reason = "";
            try {
                reason = JsonParser.parseString(body).getAsJsonObject().get("reason").getAsString();
            } catch (Exception ignored) {
            }
            return Component.literal(switch (reason) {
                case "full" -> "Сервер " + serverId + " заполнен";
                case "locked" -> "Сервер " + serverId + " закрыт";
                default -> "Сервер " + serverId + " сейчас выключен";
            });
        }
        UFEssential.LOGGER.warn("[UF] Transfer to {}: backend answered HTTP {}", serverId, status);
        return Component.literal("Не удалось связаться с сетью, попробуйте позже");
    }

    /** Ids of the servers that are up, from the backend (refreshed at most every 30 s), for command suggestions. */
    public static CompletableFuture<Suggestions> suggestServers(SuggestionsBuilder builder) {
        String apiKey = NetworkPresence.apiKey();
        if (apiKey.isEmpty()) return builder.buildFuture();
        if (System.currentTimeMillis() - knownServersAt < SUGGESTIONS_TTL_MS) return suggest(builder, knownServers);
        return CLIENT.sendAsync(get("/network/presence", apiKey), HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error == null && response.statusCode() == 200) {
                        List<String> ids = new ArrayList<>();
                        JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("servers")
                                .forEach(s -> ids.add(s.getAsJsonObject().get("id").getAsString()));
                        ids.remove(NetworkPresence.serverId());
                        knownServers = List.copyOf(ids);
                        knownServersAt = System.currentTimeMillis();
                    }
                    return knownServers;
                })
                .thenCompose(ids -> suggest(builder, ids));
    }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, List<String> ids) {
        String typed = builder.getRemaining().toLowerCase();
        for (String id : ids) {
            if (id.startsWith(typed)) builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private static HttpRequest get(String path, String apiKey) {
        return HttpRequest.newBuilder(URI.create(AuthService.getBaseUrl() + path))
                .timeout(TIMEOUT)
                .header("x-api-key", apiKey)
                .GET()
                .build();
    }
}
