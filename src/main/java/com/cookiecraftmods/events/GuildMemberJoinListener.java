package com.cookiecraftmods.events;

import com.cookiecraftmods.database.UserRepository;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildMemberJoinListener extends ListenerAdapter {
    private final UserRepository repo = UserRepository.getInstance();

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        var user = event.getUser();
        repo.ensureUser(user.getIdLong(), user.getName());
    }
}
