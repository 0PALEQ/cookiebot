package com.cookiecraftmods;

import com.cookiecraftmods.bot.Bot;
import com.cookiecraftmods.config.Config;

public class Main {
    public static void main(String[] args) {

        String token = Config.getDiscordToken();
        if (token == null || token.isBlank()) {
            System.err.println("[CookieBot] DISCORD_TOKEN is not set. Create a .env file with DISCORD_TOKEN=your_token or set env var.");
            return;
        }

        new Bot(token).start();
    }
}
