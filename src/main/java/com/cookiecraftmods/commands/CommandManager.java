package com.cookiecraftmods.commands;

import com.cookiecraftmods.commands.impl.PingCommand;
import com.cookiecraftmods.commands.impl.ProfileCommand;
import com.cookiecraftmods.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CommandManager {
    private final Map<String, ICommand> commands = new HashMap<>();

    public CommandManager() {
        register(new PingCommand());
        register(new ProfileCommand());
    }

    public void register(ICommand cmd) {
        commands.put(cmd.getName().toLowerCase(Locale.ROOT), cmd);
    }

    public boolean handle(MessageReceivedEvent event, String raw) {
        String prefix = Config.getPrefix();
        if (!raw.startsWith(prefix)) return false;
        String content = raw.substring(prefix.length()).trim();
        if (content.isEmpty()) return false;

        String[] parts = content.split("\\s+");
        String name = parts[0].toLowerCase(Locale.ROOT);
        String[] args = new String[Math.max(0, parts.length - 1)];
        if (parts.length > 1) System.arraycopy(parts, 1, args, 0, parts.length - 1);

        ICommand cmd = commands.get(name);
        if (cmd == null) {
            sendUnknownCommand(event, name, prefix);
            return false;
        }

        try {
            cmd.execute(event, args);
            return true;
        } catch (Exception ex) {
            sendError(event, ex);
            return false;
        }
    }

    private void sendUnknownCommand(MessageReceivedEvent event, String name, String prefix) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Unknown command")
                .setColor(Color.RED)
                .setDescription("`" + name + "` is not a valid command. Try `" + prefix + "ping`.");
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    private void sendError(MessageReceivedEvent event, Exception ex) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Command error")
                .setColor(Color.RED)
                .setDescription("An error occurred while executing the command.\n```")
                .setFooter(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
        ex.printStackTrace();
    }
}
