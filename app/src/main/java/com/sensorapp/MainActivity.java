package com.sensorapp;

import static com.sensorapp.util.NetworkUtil.getGenerateUrl;
import static com.sensorapp.util.NetworkUtil.getResponseFromURL;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.sensorapp.exception.NullResponseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    TextView status;
    TextView temp;
    TextView gas;
    TextView damp;

    private final long time = 20000;

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
                status.setText("Статус: сервер не работает");
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(response);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                System.out.println(jsonObject);
                status.setText("Статус: устройство работает");
                gas.setText("Газ: " + jsonObject.getInt("gas"));
                damp.setText("Влажность: " + jsonObject.getInt("damp"));
                temp.setText("Температура: " + jsonObject.getInt("temperature"));
            } catch (JSONException e) {
                e.printStackTrace();
                status.setText(status.getText() + "Статус: cервер не работает");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton reportButton = (ImageButton) findViewById(R.id.report_button);
        status = findViewById(R.id.status);
        temp = findViewById(R.id.temp);
        gas = findViewById(R.id.gas);
        damp = findViewById(R.id.damp);

        reportButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TrackActivity.class));
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(getApplication(), new Intent(getApplication(), MainService.class));
        } else {
            startService(new Intent(getApplication(), MainService.class));
        }

        new Thread(() -> {
            while (true) {
                try {
                    new SensorQueryTask().execute(getGenerateUrl());
                    sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    status.setText(status.getText() + "Статус: cервер не работает");
                }
            }
        }).start();
    }
}