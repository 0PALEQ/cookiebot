package com.cookiecraftmods.bot;

import com.cookiecraftmods.commands.CommandManager;
import com.cookiecraftmods.config.Config;
import com.cookiecraftmods.events.MessageListener;
import com.cookiecraftmods.events.ReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.EnumSet;

public class Bot {
    private final String token;
    private JDA jda;
    private ListenerManager listenerManager;
    private CommandManager commandManager;

    public Bot(String token) {
        this.token = token;
    }

    public void start() {
        try {
            EnumSet<GatewayIntent> intents = EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT
            );

            this.commandManager = new CommandManager();
            this.listenerManager = new ListenerManager();

            this.jda = JDABuilder.createLight(token, intents)
                    .setActivity(Activity.playing(Config.getPrefix() + "help"))
                    .setStatus(OnlineStatus.ONLINE)
                    .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                    .addEventListeners(
                            new ReadyListener(),
                            new MessageListener(commandManager)
                    )
                    .build();

        } catch (Exception e) {
            System.err.println("[CookieBot] Failed to start bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JDA getJda() {
        return jda;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
