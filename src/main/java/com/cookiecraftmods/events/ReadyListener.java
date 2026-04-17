package com.cookiecraftmods.events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.Command;

public class ReadyListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(System.getenv("GUILD_ID"));

        if (guild != null) {
            guild.updateCommands().addCommands(
                    Commands.slash("ping", "Sends a ping/announcement or a mod update message")
                            .addOptions(
                                    new OptionData(OptionType.STRING, "type", "Type of ping", true)
                                            .addChoices(
                                                    new Command.Choice("Announcement", "announcement"),
                                                    new Command.Choice("Update", "update")
                                            ),
                                    new OptionData(OptionType.STRING, "content", "Text content", false),
                                    new OptionData(OptionType.STRING, "mod", "Mod for update ping (use short name, e.g. mdm)", false),
                                    new OptionData(OptionType.STRING, "version", "Mod version", false),
                                    new OptionData(OptionType.STRING, "date", "Date of update", false),
                                    new OptionData(OptionType.STRING, "pings", "Users/roles to ping", false)
                            ),
                    Commands.slash("profile", "Show a user's profile")
                            .addOption(OptionType.USER, "user", "Target user (default: you)", false),
                    Commands.slash("leaderboard", "Show leaderboard for XP/Cookies/Reputation")
                            .addOptions(
                                    new OptionData(OptionType.STRING, "type", "Leaderboard type", false)
                                            .addChoices(
                                                    new Command.Choice("XP", "xp"),
                                                    new Command.Choice("Cookies", "cookies"),
                                                    new Command.Choice("Reputation", "reputation")
                                            )
                            ),
                    Commands.slash("help", "Show help entries from database"),
                    Commands.slash("rep", "Grant one reputation point to someone (1x per day)")
                            .addOption(OptionType.USER, "user", "User to give reputation to", true),
                    Commands.slash("daily", "Claim your daily cookies reward")
            ).queue();
        }
    }
}
