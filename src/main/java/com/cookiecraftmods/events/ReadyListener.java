package com.cookiecraftmods.events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class ReadyListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(System.getenv("GUILD_ID"));

        if (guild != null) {
            guild.updateCommands().addCommands(
                    Commands.slash("ping", "Check if the bot is alive"),
                    Commands.slash("profile", "Show a user's profile")
                            .addOption(OptionType.USER, "user", "Target user (default: you)", false),
                    Commands.slash("rep", "Grant one reputation point to someone (1x per day)")
                            .addOption(OptionType.USER, "user", "User to give reputation to", true),
                    Commands.slash("daily", "Claim your daily cookies reward")
            ).queue();
        }
    }
}
