package dod.unrealfrontiers.ufessential;

import dod.unrealfrontiers.ufessential.Auth.AuthService;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class UFConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> BASE_URL = BUILDER
            .comment("Base URL of the Unreal Frontiers backend, without a trailing slash")
            .define("baseUrl", AuthService.DEFAULT_BASE_URL, v -> v instanceof String s && s.startsWith("http"));

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            AuthService.setBaseUrl(BASE_URL.get());
        }
    }
}
