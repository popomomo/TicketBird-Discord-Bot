package org.dreamexposure.ticketbird.module.command;

import org.dreamexposure.ticketbird.objects.command.CommandInfo;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.ArrayList;

interface ICommand {
    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    String getCommand();

    /**
     * Gets the short aliases of the command this object is responsible for.
     * </br>
     * This will return an empty ArrayList if none are present
     *
     * @return The aliases of the command.
     */
    ArrayList<String> getAliases();

    /**
     * Gets the info on the command (not sub command) to be used in help menus.
     *
     * @return The command info.
     */
    CommandInfo getCommandInfo();

    /**
     * Issues the command this Object is responsible for.
     *
     * @param args  The command arguments.
     * @param event The event received.
     * @return <code>true</code> if successful, else <code>false</code>.
     */
    Boolean issueCommand(String[] args, MessageReceivedEvent event, GuildSettings settings);
}
