package com.cookiecraftmods.commands.impl;

import com.cookiecraftmods.commands.ICommand;
import com.cookiecraftmods.database.UserRepository;
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

        String userId = target.getId();
        int cookies = repo.getCookies(userId);
        String bio = repo.getBio(userId);

        event.getChannel().sendMessageEmbeds(
                EmbedUtil.defaultEmbed()
                        .setTitle(target.getEffectiveName() + "'s Profile")
                        .addField("Cookies", String.valueOf(cookies), true)
                        .addField("Bio", bio == null ? "No bio set" : bio, false)
                        .build()
        ).queue();
    }
}
