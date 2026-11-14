package dod.unrealfrontiers.ufessential.Network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dod.unrealfrontiers.ufessential.Auth.AuthService;
import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.UFServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tells the backend this server is up and who is on it, every 10 s ({@code POST /network/servers/{id}/heartbeat}), and
 * that it is going away when it stops. The front server routes joining players and shows the combined player count
 * from these. Off until the server has an id ({@code network.serverId} or {@code UF_SERVER_ID}) and a key.
 */
public final class NetworkPresence {
    private static final int INTERVAL_TICKS = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean();
    /** The last send failed: the next failure isn't logged again, a success is. */
    private static volatile boolean failing;
    private static int ticks = INTERVAL_TICKS; // the first heartbeat goes out on the first tick

    private NetworkPresence() {}

    /**
     * A join or a leave is reported within a second instead of at the next 10 s heartbeat: the server a player was just
     * transferred to becomes their last server at once, and the network's count is current.
     */
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        reportSoon();
    }

    public static void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        reportSoon();
    }

    private static void reportSoon() {
        ticks = Math.max(ticks, INTERVAL_TICKS - 20);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer() || ++ticks < INTERVAL_TICKS) return;
        ticks = 0;
        String serverId = serverId(), apiKey = apiKey();
        if (serverId.isEmpty() || apiKey.isEmpty() || !IN_FLIGHT.compareAndSet(false, true)) return;

        JsonObject body = new JsonObject();
        body.addProperty("maxPlayers", server.getMaxPlayers());
        JsonArray players = new JsonArray();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", player.getGameProfile().getName());
            entry.addProperty("uuid", player.getUUID().toString());
            players.add(entry);
        }
        body.add("players", players);

        CLIENT.sendAsync(request(serverId, "heartbeat", apiKey, body.toString()), HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    IN_FLIGHT.set(false);
                    if (error == null && response.statusCode() / 100 == 2) {
                        if (failing) UFEssential.LOGGER.info("[UF] Network heartbeats reach the backend again");
                        failing = false;
                    } else if (!failing) {
                        failing = true;
                        UFEssential.LOGGER.warn("[UF] Network heartbeat failed: {}", error != null ? error.toString() : "HTTP " + response.statusCode());
                    }
                });
    }

    /** The backend stops sending players here at once instead of after the heartbeats run out. */
    public static void onServerStopping(ServerStoppingEvent event) {
        String serverId = serverId(), apiKey = apiKey();
        if (!event.getServer().isDedicatedServer() || serverId.isEmpty() || apiKey.isEmpty()) return;
        try {
            CLIENT.send(request(serverId, "stopping", apiKey, "{}"), HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            UFEssential.LOGGER.warn("[UF] Could not tell the backend the server is stopping: {}", e.toString());
        }
    }

    private static HttpRequest request(String serverId, String action, String apiKey, String json) {
        String path = "/network/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/" + action;
        return HttpRequest.newBuilder(URI.create(AuthService.getBaseUrl() + path))
                .timeout(TIMEOUT)
                .header("x-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    /** This server's id in the catalog, or "" when it takes no part in the network. */
    static String serverId() {
        String configured = UFServerConfig.NETWORK_SERVER_ID.get().trim();
        if (!configured.isEmpty()) return configured;
        String env = System.getenv("UF_SERVER_ID");
        return env == null ? "" : env.trim();
    }

    /** The backend's server key, or "". */
    static String apiKey() {
        String configured = UFServerConfig.NETWORK_API_KEY.get().trim();
        if (!configured.isEmpty()) return configured;
        String env = System.getenv("UF_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return UFServerConfig.SKINS_API_KEY.get().trim();
    }
}
