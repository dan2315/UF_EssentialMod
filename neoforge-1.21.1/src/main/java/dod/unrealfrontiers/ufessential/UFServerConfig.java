package dod.unrealfrontiers.ufessential;

import dod.unrealfrontiers.ufessential.Auth.AuthService;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Dedicated-server settings: the backend URL joining players are verified against, and server-side skins.
 * Stored in config/ufessential-common.toml; ignored on clients, which use {@link UFConfig}.
 */
public class UFServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> BASE_URL = BUILDER
            .comment("Backend URL a dedicated server verifies players against. Leave empty to use UF_BACKEND_URL from the environment, or the default.")
            .define("serverBaseUrl", "", v -> v instanceof String s && (s.isBlank() || s.startsWith("http")));

    static {
        BUILDER.comment("Server-side skins from the Unreal Frontiers site (dedicated server only, no client mod needed)").push("skins");
    }

    public static final ModConfigSpec.BooleanValue SKINS_ENABLED = BUILDER
            .comment("Apply players' active skins from the backend")
            .define("enabled", true);

    public static final ModConfigSpec.ConfigValue<String> SKINS_API_KEY = BUILDER
            .comment("Backend API key (sent as x-api-key). Skins stay off while this is empty.")
            .define("apiKey", "");

    public static final ModConfigSpec.IntValue SKINS_JOIN_TIMEOUT_MS = BUILDER
            .comment("How long a joining player waits for their skin; a later answer is applied live")
            .defineInRange("joinTimeoutMs", 1500, 100, 10_000);

    public static final ModConfigSpec.IntValue SKINS_REQUEST_TIMEOUT_MS = BUILDER
            .comment("Timeout of a single backend request")
            .defineInRange("requestTimeoutMs", 5000, 500, 60_000);

    public static final ModConfigSpec.IntValue SKINS_POLL_INTERVAL_SECONDS = BUILDER
            .comment("How often online players' skins are re-checked for changes made on the site")
            .defineInRange("pollIntervalSeconds", 45, 10, 3600);

    public static final ModConfigSpec.BooleanValue SKINS_LOG_FAILURES = BUILDER
            .comment("Log failed or timed-out skin requests")
            .define("logFailures", true);

    static {
        BUILDER.pop();
        BUILDER.comment("The network of servers behind the front server (dedicated server only)").push("network");
    }

    public static final ModConfigSpec.ConfigValue<String> NETWORK_SERVER_ID = BUILDER
            .comment("This server's id in the backend's server catalog. Empty: UF_SERVER_ID from the environment; neither: no heartbeats")
            .define("serverId", "");

    public static final ModConfigSpec.ConfigValue<String> NETWORK_API_KEY = BUILDER
            .comment("Backend API key for the heartbeats (x-api-key). Empty: UF_API_KEY from the environment, then skins.apiKey")
            .define("apiKey", "");

    public static final ModConfigSpec.BooleanValue NETWORK_REQUIRE_PASS = BUILDER
            .comment("Let players in only with an entry pass from the backend (sent here by the front server or another server of the network); operators always get in")
            .define("requirePass", false);

    public static final ModConfigSpec.ConfigValue<String> NETWORK_ENTRY_ADDRESS = BUILDER
            .comment("The network's address (the front server) told to players turned away for coming in directly, e.g. unrealfrontiers.com:25580")
            .define("entryAddress", "");

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec.ConfigValue<String> MAINTENANCE_MOTD = BUILDER
            .comment("Server list text (MOTD) while maintenance mode is on, replacing the one from server.properties.",
                    "§ colour codes; \\n starts the second line. Empty = keep the normal MOTD.")
            .define("maintenanceMotd",
                    // A server-list line fits 271 px; a bold "/" is 7 px, the bold title about 127 px
                    "§e§l//§8§l//§e§l//§8§l// §6§lТЕХНИЧЕСКИЕ РАБОТЫ §8§l//§e§l//§8§l//§e§l//"
                            // Second line: the title swapped for 18 slashes (126 px), so all three blocks line up;
                            // its colours run one slash ahead of the first line's
                            + "\n§e§l/§8§l//§e§l//§8§l//§e§l/ §e§l/§8§l//§e§l//§8§l//§e§l//§8§l//§e§l//§8§l//§e§l//§8§l/"
                            + " §8§l/§e§l//§8§l//§e§l//§8§l/");

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC && FMLEnvironment.dist.isDedicatedServer()) {
            AuthService.setBaseUrl(BASE_URL.get());
            UFEssential.LOGGER.info("[UF] Backend: {}", AuthService.getBaseUrl());
        }
    }
}
