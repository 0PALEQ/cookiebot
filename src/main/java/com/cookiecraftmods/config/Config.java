package com.cookiecraftmods.config;

import io.github.cdimascio.dotenv.Dotenv;

public final class Config {
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    private Config() {}

    public static String getDiscordToken() {
        String fromEnv = System.getenv("DISCORD_TOKEN");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        return DOTENV.get("DISCORD_TOKEN");
    }

    public static String getPrefix() {
        String fromEnv = System.getenv("PREFIX");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        String v = DOTENV.get("PREFIX");
        return v != null && !v.isBlank() ? v : "!";
    }
}
