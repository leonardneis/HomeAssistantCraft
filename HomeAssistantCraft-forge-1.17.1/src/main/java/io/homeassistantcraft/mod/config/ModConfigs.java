package io.homeassistantcraft.mod.config;

import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import java.net.URI;
import java.util.Optional;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ModConfigs {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> HA_BASE_URL = BUILDER
        .comment("Home Assistant base URL, for example http://192.168.1.10:8123")
        .define("haBaseUrl", "http://127.0.0.1:8123");

    private static final ForgeConfigSpec.ConfigValue<String> HA_ACCESS_TOKEN = BUILDER
        .comment("Long-lived Home Assistant token. Prefer environment variable override for servers.")
        .define("haAccessToken", "");

    private static final ForgeConfigSpec.ConfigValue<String> HA_ACCESS_TOKEN_ENV_VAR = BUILDER
        .comment("Environment variable name used to override haAccessToken when present.")
        .define("haAccessTokenEnvVar", "HOMEASSISTANTCRAFT_TOKEN");

    private static final ForgeConfigSpec.BooleanValue ALLOW_INSECURE_HTTP = BUILDER
        .comment("Allow plain HTTP URLs. Disable in production environments.")
        .define("allowInsecureHttp", true);

    private static final ForgeConfigSpec SPEC = BUILDER.build();

    private ModConfigs() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static Optional<HomeAssistantConnectionSettings> resolveSettings() {
        String rawUrl = HA_BASE_URL.get().trim();
        if (rawUrl.isEmpty()) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return Optional.empty();
        }
        if ("http".equalsIgnoreCase(scheme) && !ALLOW_INSECURE_HTTP.get()) {
            return Optional.empty();
        }

        String token = resolveToken();
        if (token.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new HomeAssistantConnectionSettings(uri, token));
    }

    private static String resolveToken() {
        String envName = HA_ACCESS_TOKEN_ENV_VAR.get().trim();
        if (!envName.isEmpty()) {
            String envToken = System.getenv(envName);
            if (envToken != null && !envToken.isBlank()) {
                return envToken.trim();
            }
        }

        String configToken = HA_ACCESS_TOKEN.get();
        if (configToken == null || configToken.isBlank()) {
            return "";
        }
        return configToken.trim();
    }
}
