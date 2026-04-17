package com.cookiecraftmods.commands.impl;

import com.cookiecraftmods.commands.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class PingCommand implements ICommand {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Check bot latency.";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        long start = System.currentTimeMillis();
        event.getChannel().sendMessage("Pinging...").queue(msg -> {
            long latency = System.currentTimeMillis() - start;
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Pong!")
                    .setColor(new Color(0x2ecc71))
                    .addField("Message latency", latency + " ms", true)
                    .addField("WS ping", event.getJDA().getGatewayPing() + " ms", true);
            msg.editMessageEmbeds(eb.build()).queue();
        });
    }
}
