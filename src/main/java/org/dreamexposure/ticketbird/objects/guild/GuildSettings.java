package org.dreamexposure.ticketbird.objects.guild;

import java.util.ArrayList;
import java.util.List;

public class GuildSettings {
    private final long guildID;

    private String lang;
    private String prefix;

    private boolean patronGuild;
    private boolean devGuild;

    private long awaitingCategory;
    private long respondedCategory;
    private long holdCategory;
    private long closeCategory;

    private long supportChannel;
    private long staticMessage;

    private int nextId;

    private int totalClosed;

    private List<Long> staff = new ArrayList<>();

    public GuildSettings(long _guildId) {
        guildID = _guildId;

        lang = "ENGLISH";
        prefix = "=";

        patronGuild = false;
        devGuild = false;

        nextId = 1;
        totalClosed = 0;
    }

    //Getters
    public long getGuildID() {
        return guildID;
    }

    public String getLang() {
        return lang;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isPatronGuild() {
        return patronGuild;
    }

    public boolean isDevGuild() {
        return devGuild;
    }

    public long getAwaitingCategory() {
        return awaitingCategory;
    }

    public long getRespondedCategory() {
        return respondedCategory;
    }

    public long getHoldCategory() {
        return holdCategory;
    }

    public long getCloseCategory() {
        return closeCategory;
    }

    public long getSupportChannel() {
        return supportChannel;
    }

    public long getStaticMessage() {
        return staticMessage;
    }

    public int getNextId() {
        return nextId;
    }

    public int getTotalClosed() {
        return totalClosed;
    }

    public List<Long> getStaff() {
        return staff;
    }

    public String getStaffString() {
        StringBuilder list = new StringBuilder();
        for (long l : staff) {
            list.append(l).append(",");
        }

        return list.toString();
    }

    //Setters
    public void setLang(String _lang) {
        lang = _lang;
    }

    public void setPrefix(String _prefix) {
        prefix = _prefix;
    }

    public void setPatronGuild(boolean _patronGuild) {
        patronGuild = _patronGuild;
    }

    public void setDevGuild(boolean _devGuild) {
        devGuild = _devGuild;
    }

    public void setAwaitingCategory(long _awaiting) {
        awaitingCategory = _awaiting;
    }

    public void setRespondedCategory(long _responded) {
        respondedCategory = _responded;
    }

    public void setHoldCategory(long _hold) {
        holdCategory = _hold;
    }

    public void setCloseCategory(long _close) {
        closeCategory = _close;
    }

    public void setSupportChannel(long _support) {
        supportChannel = _support;
    }

    public void setStaticMessage(long _static) {
        staticMessage = _static;
    }

    public void setNextId(int _next) {
        nextId = _next;
    }

    public void setTotalClosed(int _total) {
        totalClosed = _total;
    }

    public void setStaffFromString(String _staff) {
        for (String s : _staff.split(",")) {
            try {
                staff.add(Long.valueOf(s));
            } catch (NumberFormatException ignore) {
            }
        }
    }
}