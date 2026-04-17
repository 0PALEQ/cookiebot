package com.cookiecraftmods.commands.impl;

import com.cookiecraftmods.commands.ICommand;
import com.cookiecraftmods.database.UserRepository;
import com.cookiecraftmods.database.UserRepository.UserProfile;
import com.cookiecraftmods.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ProfileCommand implements ICommand {
    private final UserRepository repo = UserRepository.getInstance();

    @Override
    public String getName() { return "profile"; }

    @Override
    public String getDescription() { return "Show a simple user profile (demo)."; }

    @Override
    public String getUsage() { return "profile [@user]"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        Member target = event.getMember();
        if (args.length > 0 && !event.getMessage().getMentions().getMembers().isEmpty()) {
            target = event.getMessage().getMentions().getMembers().get(0);
        }
        if (target == null) {
            event.getChannel().sendMessage("Cannot determine target member.").queue();
            return;
        }

        long userId = target.getIdLong();
        repo.ensureUser(userId, target.getUser().getName());
        UserProfile p = repo.getProfile(userId);

        event.getChannel().sendMessageEmbeds(
                EmbedUtil.defaultEmbed()
                        .setTitle(target.getEffectiveName() + "'s Profile")
                        .addField("XP", p == null ? "0" : String.valueOf(p.xp), true)
                        .addField("Cookies", p == null ? "0" : String.valueOf(p.cookies), true)
                        .addField("Reputation", p == null ? "0" : String.valueOf(p.reputation), true)
                        .build()
        ).queue();
    }
}
