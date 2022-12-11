package com.sensorapp.util;

public class NotificationInfo {
    int id;
    String title;
    String text;
    String channelId;
    int icon;

    public NotificationInfo(int id, String title, String text, String channelId, int icon) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
