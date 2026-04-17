package com.cookiecraftmods.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface ICommand {
    String getName();
    String getDescription();
    default String getUsage() { return getName(); }
    void execute(MessageReceivedEvent event, String[] args);
}
