package org.dreamexposure.ticketbird.listeners;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Project;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.utils.GeneralUtils;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

import java.util.EnumSet;

@SuppressWarnings("Duplicates")
public class MessageReceiveListener {

    public static void onMessageReceive(MessageReceivedEvent event, GuildSettings settings) {
        //Make sure a bot (including us) didn't send the message.
        if (!event.getAuthor().isBot() && event.getAuthor().getLongID() != Main.getClient().getOurUser().getLongID()) {
            //Check if in support request channel
            if (event.getChannel().getLongID() == settings.getSupportChannel()) {
                //Create a new ticket!
                try {
                    String content = event.getMessage().getContent();

                    int ticketNumber = settings.getNextId();
                    settings.setNextId(ticketNumber + 1);
                    DatabaseManager.getManager().updateSettings(settings);

                    IChannel channel = ChannelManager.createChannel("ticket-" + ticketNumber, event.getGuild());
                    channel.changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                    //Set channel permissions...
                    EnumSet<Permissions> toAdd = EnumSet.noneOf(Permissions.class);
                    toAdd.add(Permissions.MENTION_EVERYONE);
                    toAdd.add(Permissions.ATTACH_FILES);
                    toAdd.add(Permissions.EMBED_LINKS);
                    toAdd.add(Permissions.SEND_MESSAGES);
                    toAdd.add(Permissions.READ_MESSAGES);
                    toAdd.add(Permissions.READ_MESSAGE_HISTORY);

                    EnumSet<Permissions> toRemove = EnumSet.allOf(Permissions.class);

                    channel.overrideRolePermissions(event.getGuild().getEveryoneRole(), EnumSet.noneOf(Permissions.class), toRemove);
                    channel.overrideUserPermissions(event.getAuthor(), toAdd, EnumSet.noneOf(Permissions.class));

                    for (long uid : settings.getStaff()) {
                        if (event.getGuild().getUserByID(uid) != null)
                            channel.overrideUserPermissions(event.getGuild().getUserByID(uid), toAdd, EnumSet.noneOf(Permissions.class));
                    }

                    //Register ticket in database.
                    Ticket ticket = new Ticket(event.getGuild().getLongID(), ticketNumber);
                    ticket.setChannel(channel.getLongID());
                    ticket.setCategory(settings.getAwaitingCategory());
                    ticket.setCreator(event.getAuthor().getLongID());
                    ticket.setLastActivity(System.currentTimeMillis());
                    DatabaseManager.getManager().updateTicket(ticket);

                    //Send message
                    String msgOr = MessageManager.getMessage("Ticket.Open", settings);
                    String msg = msgOr.replace("%creator%", event.getAuthor().mention(true)).replace("%content%", content);

                    EmbedBuilder em = new EmbedBuilder();
                    em.withAuthorIcon(Main.getClient().getGuildByID(GlobalVars.serverId).getIconURL());
                    em.withAuthorName("TicketBird");
                    em.withTitle("Select a Project/Service!");
                    em.withDesc("Send a message with **ONLY** the project/service's name so we can better help you!");
                    for (Project p: DatabaseManager.getManager().getAllProjects(settings.getGuildID())) {
                        em.appendField(p.getName(), "\u200B", false);
                    }
                    em.withColor(GlobalVars.embedColor);

                    MessageManager.sendMessage(em.build(), msg, channel);

                    //Delete message in support channel.
                    event.getMessage().delete();

                    //Lets update the static message!
                    GeneralUtils.updateStaticMessage(event.getGuild(), settings);
                } catch (Exception e) {
                    Logger.getLogger().exception(event.getAuthor(), "Failed to handle new ticket creation!", e, MessageReceiveListener.class);
                }
            } else {
                //Check if in ticket channel...
                try {
                    //Brand new ticket needing project set format ticket-[number]
                    if (event.getChannel().getName().split("-").length == 2) {
                        //New ticket needs project set!!!
                        int ticketNumber = Integer.valueOf(event.getChannel().getName().split("-")[1]);
                        Ticket ticket = DatabaseManager.getManager().getTicket(event.getGuild().getLongID(), ticketNumber);

                        //Check if ticket, if not, fail silently.
                        if (ticket != null) {
                            //Check if message was valid project or not...
                            Project project = DatabaseManager.getManager().getProject(event.getGuild().getLongID(), event.getMessage().getContent());

                            if (project != null) {
                                //Valid project! Lets assign the prefix!

                                //Parse prefix to remove disallowed characters...
                                String prefix = project.getPrefix();

                                for (String s : GlobalVars.disallowed) {
                                    prefix = prefix.replace(s, "");
                                }

                                event.getChannel().changeName(prefix.toLowerCase() + "-ticket-" + ticket.getNumber());

                                //Update database!
                                ticket.setProject(project.getName());
                                ticket.setLastActivity(System.currentTimeMillis());
                                DatabaseManager.getManager().updateTicket(ticket);

                                //Send message...
                                MessageManager.sendMessage(MessageManager.getMessage("Ticket.Project.Success", "%project%", project.getName(), settings), event);
                            } else {
                                //Invalid project.... cannot assign prefix to ticket.
                                MessageManager.sendMessage(MessageManager.getMessage("Ticket.Project.Invalid", settings), event);
                            }
                        }
                    } else {
                        //Existing Ticket channel format: [prefix]-ticket-[number]
                        int ticketNumber = Integer.valueOf(event.getChannel().getName().split("-")[2]);
                        Ticket ticket = DatabaseManager.getManager().getTicket(event.getGuild().getLongID(), ticketNumber);

                        //Check if ticket, if not, fail silently.
                        if (ticket != null) {
                            //It be a ticket, let's handle it!
                            if (event.getChannel().getCategory().getLongID() == settings.getCloseCategory()) {
                                //Ticket was closed, reopen ticket...
                                if (settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Staff member responded...

                                    //Move ticket
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getRespondedCategory()));

                                    //Let everyone know it was reopened...
                                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Everyone", settings), event);

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild(), settings);
                                } else {
                                    //Move ticket...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                                    //Let everyone know it was reopened...
                                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Everyone", settings), event);

                                    //Update database....
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild(), settings);
                                }
                            } else if (event.getChannel().getCategory().getLongID() == settings.getHoldCategory()) {
                                //Ticket was on hold, reopen ticket...
                                if (settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Staff member responded...

                                    //Move ticket...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getRespondedCategory()));

                                    //Let creator know it was reopened...
                                    if (ticket.getCreator() == 0) {
                                        MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", "NO CREATOR", settings), event);
                                    } else {
                                        if (event.getGuild().getUserByID(ticket.getCreator()) != null) {
                                            MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", event.getGuild().getUserByID(ticket.getCreator()).mention(), settings), event);
                                        } else {
                                            MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", Main.getClient().fetchUser(ticket.getCreator()).mention(), settings), event);
                                        }
                                    }

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild(), settings);
                                } else {
                                    //Move ticket...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                                    //Let creator know it was reopened...
                                    if (ticket.getCreator() == 0) {
                                        MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", "NO CREATOR", settings), event);
                                    } else {
                                        if (event.getGuild().getUserByID(ticket.getCreator()) != null) {
                                            MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", event.getGuild().getUserByID(ticket.getCreator()).mention(), settings), event);
                                        } else {
                                            MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", Main.getClient().fetchUser(ticket.getCreator()).mention(), settings), event);
                                        }
                                    }

                                    //Update database...
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild(), settings);
                                }
                            } else if (event.getChannel().getCategory().getLongID() == settings.getAwaitingCategory()) {
                                //Ticket awaiting response from staff, check user response...
                                if (settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Staff member responded...

                                    //Move to responded...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getRespondedCategory()));

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            } else if (event.getChannel().getCategory().getLongID() == settings.getRespondedCategory()) {
                                //Ticket responded to by staff, check user response...
                                if (!settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Move to awaiting...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                                    //Update database...
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException ignore) {
                    //Not in a ticket channel. Fail silently.
                }
            }
        }
    }
}