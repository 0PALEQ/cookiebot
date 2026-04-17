package com.cookiecraftmods.events;

import com.cookiecraftmods.commands.CommandManager;
import com.cookiecraftmods.config.Config;
import com.cookiecraftmods.database.UserRepository;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageListener extends ListenerAdapter {
    private final CommandManager commandManager;
    private final UserRepository repo = UserRepository.getInstance();
    private final java.util.Map<Long, Long> messageCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MESSAGE_XP = 5;
    private static final long MESSAGE_COOLDOWN_MS = 60_000L;

    public MessageListener(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.isFromType(ChannelType.PRIVATE)) return;

        // Ensure user exists in DB
        repo.ensureUser(event.getAuthor().getIdLong(), event.getAuthor().getName());

        // Award XP for messages with simple cooldown to avoid spam
        String content = event.getMessage().getContentRaw();
        if (content != null && content.trim().length() >= 5) {
            long uid = event.getAuthor().getIdLong();
            long now = System.currentTimeMillis();
            Long last = messageCooldowns.get(uid);
            if (last == null || (now - last) >= MESSAGE_COOLDOWN_MS) {
                repo.addXp(uid, MESSAGE_XP);
                messageCooldowns.put(uid, now);
            }
        }

        String prefix = Config.getPrefix();
        if (!content.startsWith(prefix)) return;

        commandManager.handle(event, content);
    }
}
