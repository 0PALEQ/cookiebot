package com.cookiecraftmods.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Locale;
import java.util.Map;

public final class Config {
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    private Config() {}

    public static String getDiscordToken() {
        return get("DISCORD_TOKEN", null);
    }

    public static String getPrefix() {
        return get("PREFIX", "!");
    }

    public static String getGuildId() {
        return get("GUILD_ID", null);
    }

    public static String getUpdatesChannelId() {
        return get("UPDATES_CHANNEL_ID", "1099589405192228926");
    }

    public static String getAllUpdatesRoleId() {
        return get("ALL_UPDATES_ROLE_ID", "1277223801205297306");
    }

    public static String getUpdateFeedUrl() {
        return get("DISCORD_UPDATE_FEED_URL", "https://cookiecraftmods.com/api/discord/update-announcements");
    }

    public static String getUpdateFeedSecret() {
        return get("DISCORD_UPDATE_FEED_SECRET", null);
    }

    public static long getUpdatePollSeconds() {
        String value = get("DISCORD_UPDATE_POLL_SECONDS", "20");
        try {
            return Math.max(10L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return 20L;
        }
    }

    public static String getUpdateRoleId(String slug) {
        if (slug == null) return null;

        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        String configured = get("UPDATE_ROLE_" + normalized.toUpperCase(Locale.ROOT), null);
        if (configured != null) return configured;

        return DEFAULT_UPDATE_ROLES.get(normalized);
    }

    private static final Map<String, String> DEFAULT_UPDATE_ROLES = Map.of(
            "mdm", "1277223877151555595",
            "edm", "1278069022881087558",
            "fdm", "1339651552742346823",
            "bp", "1278069183543906346",
            "mta", "1507474083342843945"
    );

    private static String get(String key, String fallback) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        String fromDotenv = DOTENV.get(key);
        return fromDotenv != null && !fromDotenv.isBlank() ? fromDotenv.trim() : fallback;
    }
}
