package com.sensorapp;

import static com.sensorapp.util.NetworkUtil.getGenerateUrl;
import static com.sensorapp.util.NetworkUtil.getResponseFromURL;

import static java.lang.Thread.sleep;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sensorapp.exception.NullResponseException;
import com.sensorapp.util.NotificationInfo;
import com.sensorapp.util.NotificationUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class MainService extends IntentService {

    private final long time = 20000;
    private final long[] pattern = new long[] {time, 1000};

    private MediaPlayer ringtone;
    private AudioManager audio;
    private Vibrator vibrator;
    private NotificationUtil notificationUtil;
    private NotificationManager notificationManager;

    public MainService() {
        super("MainService");
    }

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
                JSONObject jsonObject = new JSONObject(response);

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
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
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
