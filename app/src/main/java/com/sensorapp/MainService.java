package com.sensorapp;

import static com.sensorapp.util.NetworkUtil.getGenerateUrl;
import static com.sensorapp.util.NetworkUtil.getResponseFromURL;

import static java.lang.Thread.sleep;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.sensorapp.exception.NullResponseException;
import com.sensorapp.util.NotificationInfo;
import com.sensorapp.util.NotificationUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class MainService extends Service {

    private final long time = 20000;
    private final long[] pattern = new long[] {time, 1000};

    private final String FOREGROUND_NOTIFICATION_ID = "foreground_id";
    private final String FOREGROUND_NAME = "foregroun";

    private MediaPlayer ringtone;
    private AudioManager audio;
    private Vibrator vibrator;
    private NotificationUtil notificationUtil;
    private NotificationManager notificationManager;

    class SensorQueryTask extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... urls) {
            try {
                return getResponseFromURL(urls[0]);
            } catch (IOException | NullResponseException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if(response == null) {
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(response);
                JSONObject jsonObject  = jsonArray.getJSONObject(0);

                if(ringtone != null && vibrator != null) {
                    ringtone.stop();
                    ringtone.release();
                    vibrator.cancel();
                    ringtone = null;
                    vibrator = null;
                }

                if (jsonObject.getInt("gas") >= 400) {
                    getNotification(new NotificationInfo(1, "Опасаность", "Превышен уровень газа", "sensorGas", R.drawable.ic_baseline_cloud_queue_24));
                }

                if (jsonObject.getInt("temperature") >= 50) {
                    getNotification(new NotificationInfo(2, "Опасаность", "Превышена температура", "sensorTemp", R.drawable.ic_baseline_cloud_queue_24));
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationUtil = new NotificationUtil();
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            while (true) {
                try {
                    new MainService.SensorQueryTask().execute(getGenerateUrl());
                    sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void startMyOwnForeground() {
        Intent notificationIntent = new Intent(this, MainService.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel channel = new NotificationChannel(FOREGROUND_NOTIFICATION_ID, FOREGROUND_NAME, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, FOREGROUND_NOTIFICATION_ID);
        Notification notification = notificationBuilder
                .setOngoing(true)
                .setContentTitle("Приложение работает")
                .setSmallIcon(R.drawable.ic_baseline_cloud_queue_24)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(105, notification);
    }

    protected void getNotification(NotificationInfo info) {
        Notification notification = notificationUtil.getNotification(info, new NotificationCompat.Builder(MainService.this, info.getChannelId())
                .setContentIntent(getInfoPendingIntent())
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), info.getIcon())));
        notificationUtil.commonNotification(notificationManager, notification, info);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        playRingtone();
    }

    protected PendingIntent getInfoPendingIntent() {
        Intent intent = new Intent(this, MainService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(MainService.this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
    }

    protected void playRingtone() {
        if(ringtone == null || vibrator == null) {
            ringtone = MediaPlayer.create(this, R.raw.sound_warning);
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
                vibrator.vibrate(VibrationEffect.createOneShot(60000, VibrationEffect.EFFECT_DOUBLE_CLICK));
            }
            else if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(60000, VibrationEffect.DEFAULT_AMPLITUDE));
            else
                vibrator.vibrate(pattern, 1);
        }

        if(!ringtone.isPlaying()) {
            ringtone.start();
        }
    }
}
