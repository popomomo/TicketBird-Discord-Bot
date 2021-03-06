package org.dreamexposure.ticketbird.service;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

import java.util.TimerTask;

public class ActivityMonitor extends TimerTask {


    @Override
    public void run() {
        Logger.getLogger().debug("Running ticket inactivity close task.");
        for (IGuild g : Main.getClient().getGuilds()) {
            GuildSettings settings = DatabaseManager.getManager().getSettings(g.getLongID());

            for (Ticket t : DatabaseManager.getManager().getAllTickets(g.getLongID())) {
                //Make sure ticket channel exists.
                IChannel channel = g.getChannelByID(t.getChannel());
                if (channel != null) {
                    try {
                        if (t.getCategory() == settings.getRespondedCategory() || t.getCategory() == settings.getAwaitingCategory()) {
                            //Ticket not already closed or on hold.
                            if (System.currentTimeMillis() - t.getLastActivity() > GlobalVars.oneWeekMs) {
                                //Inactive...
                                channel.changeCategory(g.getCategoryByID(settings.getCloseCategory()));
                                t.setCategory(settings.getCloseCategory());

                                DatabaseManager.getManager().updateTicket(t);

                                MessageManager.sendMessage(MessageManager.getMessage("Tickets.Close.Inactive", "%creator%", g.getUserByID(t.getCreator()).mention(), settings), g.getChannelByID(t.getChannel()));
                            }
                        } else if (t.getCategory() == settings.getCloseCategory()) {
                            //Ticket closed. Check time to purge.
                            if (System.currentTimeMillis() - t.getLastActivity() > GlobalVars.oneDayMs) {
                                //Purge ticket...
                                ChannelManager.deleteChannelAsync(t.getChannel(), g);

                                settings.setTotalClosed(settings.getTotalClosed() + 1);
                            }
                        }
                    } catch (Exception e) {
                        Logger.getLogger().exception(null, "Failed to handle ticket inactivity!", e, this.getClass());
                    }
                }
            }
        }
        Logger.getLogger().debug("Finished ticket inactivity close/purge task.");
    }
}