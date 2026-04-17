package com.cookiecraftmods;

import com.cookiecraftmods.bot.Bot;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("[CookieBot] DISCORD_TOKEN is not set. Create a .env file with DISCORD_TOKEN=your_token or set env var.");
            return;
        }

        new Bot(token).start();
    }
}