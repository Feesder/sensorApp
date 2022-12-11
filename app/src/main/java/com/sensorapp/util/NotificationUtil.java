package com.sensorapp.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.core.app.NotificationCompat;

public class NotificationUtil {
    public Notification getNotification(NotificationInfo info, NotificationCompat.Builder builder) {
        return builder
                .setSmallIcon(info.getIcon())
                .setContentTitle(info.getTitle())
                .setContentText(info.getText())
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    public void commonNotification(NotificationManager manager, Notification notification, NotificationInfo info) {
        notification.defaults = Notification.DEFAULT_ALL;
        manager.notify(info.getId(), notification);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(info.getChannelId(), info.getTitle(), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(info.getText());
            manager.createNotificationChannel(channel);
        }
    }
}
