package com.sensorapp;

import static com.sensorapp.util.NetworkUtil.getGenerateUrl;
import static com.sensorapp.util.NetworkUtil.getResponseFromURL;
import static java.lang.Thread.sleep;

import androidx.annotation.InspectableProperty;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sensorapp.exception.NullResponseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TrackActivity extends AppCompatActivity {

    private final long time = 20000;
    LinearLayout linearLayout;

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
                TextView text = new TextView(TrackActivity.this);
                text.setText("Сервер не рабоает");
                linearLayout.addView(text);
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(response);
                System.out.println(jsonArray.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    generationReport(jsonArray.getJSONObject(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        linearLayout = (LinearLayout) findViewById(R.id.reports_view);
        ImageButton back = findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TrackActivity.this, MainActivity.class));
            }
        });

        new Thread(() -> {
            try {
                new TrackActivity.SensorQueryTask().execute(getGenerateUrl());
                sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void generationReport(JSONObject json) {
        try {
            LinearLayout report = cloneReport();
            LinearLayout GasLinearLayout = (LinearLayout) report.getChildAt(0);
            TextView GasTextView = (TextView) GasLinearLayout.getChildAt(1);
            GasTextView.setText("Газ: " + json.getString("gas"));
            LinearLayout TemperatureLinearLayout = (LinearLayout) report.getChildAt(1);
            TextView TemperatureTextView = (TextView) TemperatureLinearLayout.getChildAt(1);
            TemperatureTextView.setText("Температура: " + json.getString("temperature"));
            LinearLayout DampLinearLayout = (LinearLayout) report.getChildAt(2);
            TextView DampTextView = (TextView) DampLinearLayout.getChildAt(1);
            DampTextView.setText("Влажность: " + json.getString("damp"));
            LinearLayout DateLinearLayout = (LinearLayout) report.getChildAt(3);
            TextView DateTextView = (TextView) DateLinearLayout.getChildAt(1);
            DateTextView.setText("Дата и время: " + generateDateTime(json.getString("date")));
            linearLayout.addView(report);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected LinearLayout cloneReport() {
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        return (LinearLayout) inflater.inflate(R.layout.template_report, null);
    }

    protected String generateDateTime(String date) {
        System.out.println(date);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
            return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        }
        return date;
    }
}