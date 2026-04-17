package com.cookiecraftmods.events;

import com.cookiecraftmods.database.UserRepository;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceListener extends ListenerAdapter {
    private static final int XP_PER_MINUTE = 2;
    private final Map<Long, Long> joinTimestamps = new ConcurrentHashMap<>();
    private final UserRepository repo = UserRepository.getInstance();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        var member = event.getMember();
        long uid = member.getIdLong();

        if (event.getChannelJoined() != null) {
            joinTimestamps.put(uid, System.currentTimeMillis());
            repo.ensureUser(uid, member.getUser().getName());
        }

        if (event.getChannelLeft() != null) {
            Long start = joinTimestamps.remove(uid);
            if (start != null) {
                long minutes = Math.max(0, (System.currentTimeMillis() - start) / 60000L);
                if (minutes > 0) {
                    int xp = (int) (minutes * XP_PER_MINUTE);
                    repo.addXp(uid, xp);
                }
            }
        }
    }
}
