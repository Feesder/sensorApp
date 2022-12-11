package com.sensorapp.util;

import android.net.Uri;

import com.sensorapp.exception.NullResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class NetworkUtil {

    static final String url = "https://sensor-367415.du.r.appspot.com/device";
    static final String id = "id";
    static final String deviceId = "1";

    public static URL getGenerateUrl() {
        try {
            Uri builtUri = Uri.parse(url)
                    .buildUpon()
                    .appendQueryParameter(id, deviceId)
                    .build();
            return new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getResponseFromURL(URL url) throws IOException, NullResponseException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream in = urlConnection.getInputStream();
        Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\\A");

        if(scanner.hasNext()) {
            return scanner.next();
        }

        urlConnection.disconnect();
        throw new NullResponseException("Сервер не работает");
    }
}
