package dod.unrealfrontiers.ufessential;

import dod.unrealfrontiers.ufessential.Auth.AuthService;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Backend URL for a dedicated server, which verifies joining players against it.
 * Stored in config/ufessential-common.toml; ignored on clients, which use {@link UFConfig}.
 */
public class UFServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> BASE_URL = BUILDER
            .comment("Backend URL a dedicated server verifies players against. Leave empty to use UF_BACKEND_URL from the environment, or the default.")
            .define("serverBaseUrl", "", v -> v instanceof String s && (s.isBlank() || s.startsWith("http")));

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC && FMLEnvironment.dist.isDedicatedServer()) {
            AuthService.setBaseUrl(BASE_URL.get());
            UFEssential.LOGGER.info("[UF] Backend: {}", AuthService.getBaseUrl());
        }
    }
}
