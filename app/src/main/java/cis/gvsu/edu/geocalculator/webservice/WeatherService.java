package cis.gvsu.edu.geocalculator.webservice;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class WeatherService extends IntentService {
    private static final String GET_WEATHER_DARK_SKY = "cis.gvsu.edu.geocalculator.webservice.action.WEATHER_AT";
    private static final String DARK_SKY_URL = "https://api.darksky.net/forecast/548fd146b32b14caefa09f0bf43626da";
    public static final String SHOW_WEATHER = "cis.gvsu.edu.geocalculator.webservice.action.BROADCAST";
    private static final String DARK_SKY_KEY = "cis.gvsu.edu.geocalculator.webservice.extra.KEY";
    private static final String DARK_SKY_LAT = "cis.gvsu.edu.geocalculator.webservice.extra.LAT";
    private static final String DARK_SKY_LNG = "cis.gvsu.edu.geocalculator.webservice.extra.LNG";
    private static final String DARK_SKY_TIME = "cis.gvsu.edu.geocalculator.webservice.extra.TIME";

    public WeatherService() {
        super("WeatherService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startGetWeather(Context context, String lat, String lng, String key, long time) {
        Intent intent = new Intent(context, WeatherService.class);
        intent.setAction(GET_WEATHER_DARK_SKY);
        intent.putExtra(DARK_SKY_LAT, lat);
        intent.putExtra(DARK_SKY_LNG, lng);
        intent.putExtra(DARK_SKY_KEY, key);
        intent.putExtra(DARK_SKY_TIME, String.valueOf(time));
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (GET_WEATHER_DARK_SKY.equals(action)) {
                final String key = intent.getStringExtra(DARK_SKY_KEY);
                final String lat = intent.getStringExtra(DARK_SKY_LAT);
                final String lng = intent.getStringExtra(DARK_SKY_LNG);
                final String when = intent.getStringExtra(DARK_SKY_TIME);
                fetchWeatherData(key, lat, lng, when);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void fetchWeatherData(String key, String lat, String lon, String time) {
        try {
            URL url = new URL(DARK_SKY_URL + "/" + lat + "," + lon + "," + time);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000 /* milliseconds */);
            conn.setConnectTimeout(10000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = bis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                JSONObject data = new JSONObject(new String(baos.toByteArray()));
                JSONObject current = data.getJSONObject("currently");

                String icon = current.getString("icon");
                double temp = current.getDouble("temperature");
                String summary = current.getString("summary");

                Intent result = new Intent(SHOW_WEATHER);

                result.putExtra("ICON", icon);
                result.putExtra("TEMPERATURE", temp);
                result.putExtra("SUMMARY", summary);

                result.putExtra("KEY", key);
                LocalBroadcastManager.getInstance(this).sendBroadcast(result);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}