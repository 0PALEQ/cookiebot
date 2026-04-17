package com.cookiecraftmods.events;

import com.cookiecraftmods.commands.CommandManager;
import com.cookiecraftmods.config.Config;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageListener extends ListenerAdapter {
    private final CommandManager commandManager;

    public MessageListener(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // ignore bots
        if (event.isFromType(ChannelType.PRIVATE)) return; // ignore DMs for now

        String content = event.getMessage().getContentRaw();
        String prefix = Config.getPrefix();
        if (!content.startsWith(prefix)) return;

        commandManager.handle(event, content);
    }
}
