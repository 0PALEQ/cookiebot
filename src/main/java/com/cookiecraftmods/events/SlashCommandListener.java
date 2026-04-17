package com.cookiecraftmods.events;

import com.cookiecraftmods.database.UserRepository;
import com.cookiecraftmods.database.UserRepository.UserProfile;
import com.cookiecraftmods.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SlashCommandListener extends ListenerAdapter {
    private static final int DAILY_COOKIES = 10;
    private final UserRepository repo = UserRepository.getInstance();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                event.reply("Pong!").setEphemeral(true).queue();
                break;
            case "profile":
                var optUser = event.getOption("user");
                var user = optUser != null ? optUser.getAsUser() : event.getUser();
                long uid = user.getIdLong();
                String uname = user.getName();
                repo.ensureUser(uid, uname);
                UserProfile p = repo.getProfile(uid);
                if (p == null) {
                    event.reply("Profile not found.").setEphemeral(true).queue();
                    return;
                }
                EmbedBuilder eb = EmbedUtil.defaultEmbed()
                        .setTitle(user.getName() + "'s Profile")
                        .addField("XP", String.valueOf(p.xp), true)
                        .addField("Cookies", String.valueOf(p.cookies), true)
                        .addField("Reputation", String.valueOf(p.reputation), true);
                event.replyEmbeds(eb.build()).queue();
                break;
            case "rep":
                var target = event.getOption("user") != null ? event.getOption("user").getAsUser() : null;
                if (target == null) {
                    event.reply("You must specify a user.").setEphemeral(true).queue();
                    return;
                }
                long giver = event.getUser().getIdLong();
                long receiver = target.getIdLong();
                repo.ensureUser(giver, event.getUser().getName());
                repo.ensureUser(receiver, target.getName());
                boolean ok = repo.tryGiveRep(giver, receiver);
                if (!ok) {
                    event.reply("You have already given a reputation point today, or invalid target.").setEphemeral(true).queue();
                } else {
                    event.reply("You gave a reputation point to " + target.getAsTag() + "!").queue();
                }
                break;
            case "daily":
                long uid2 = event.getUser().getIdLong();
                repo.ensureUser(uid2, event.getUser().getName());
                if (!repo.canClaimDaily(uid2)) {
                    event.reply("You already claimed your daily reward. Try again later.").setEphemeral(true).queue();
                    return;
                }
                boolean claimed = repo.claimDaily(uid2, DAILY_COOKIES);
                if (claimed) {
                    event.reply("You claimed your daily reward: " + DAILY_COOKIES + " cookies!").queue();
                } else {
                    event.reply("Failed to claim reward. Please try again later.").setEphemeral(true).queue();
                }
                break;
        }
    }
}
