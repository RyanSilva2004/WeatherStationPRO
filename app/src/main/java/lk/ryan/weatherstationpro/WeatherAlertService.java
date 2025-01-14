package lk.ryan.weatherstationpro;

import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WeatherAlertService extends Service {

    private static final String CHANNEL_ID = "WeatherAlerts";
    private DatabaseReference mDatabase;

    private static final double HIGH_TEMPERATURE_THRESHOLD = 35.0;
    private static final double HIGH_WIND_SPEED_THRESHOLD = 20.0;
    private static final double HIGH_RAIN_LEVEL_THRESHOLD = 100.0;
    private static final double HIGH_HUMIDITY_THRESHOLD = 80.0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d("WeatherAlertService", "Service created");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Weather Alerts Active")
                .setContentText("Monitoring weather data in the background.")
                .setSmallIcon(R.drawable.ic_weather_alert) // Replace with your own icon
                .build();
        startForeground(1, notification);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference("realtime_update");

        listenForWeatherData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WeatherAlertService", "Service started");
        return START_STICKY;  // Keep the service running even if it gets killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("WeatherAlertService", "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void listenForWeatherData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RealtimeUpdate realtimeUpdate = dataSnapshot.getValue(RealtimeUpdate.class);
                if (realtimeUpdate != null) {
                    Log.d("WeatherAlertService", "Fetched realtime data: " + realtimeUpdate.toString());
                    checkForAlerts(realtimeUpdate);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("WeatherAlertService", "Error fetching data: " + databaseError.getMessage());
            }
        });
    }

    private void checkForAlerts(RealtimeUpdate realtimeUpdate) {

        if ("Rain".equals(realtimeUpdate.rain_status)) {
            sendNotification("Rain Alert", "It's raining: " + realtimeUpdate.rain_level + " mm");
        }

        if (realtimeUpdate.temp > HIGH_TEMPERATURE_THRESHOLD) {
            sendNotification("High temperature warning", "Temperature is high: " + realtimeUpdate.temp + " °C");
        }

        if (realtimeUpdate.rain_level > HIGH_RAIN_LEVEL_THRESHOLD) {
            sendNotification("Heavy rain warning", "Rain level is high: " + realtimeUpdate.rain_level + " mm");
        }

        if (realtimeUpdate.wind_speed > HIGH_WIND_SPEED_THRESHOLD) {
            sendNotification("High wind speed warning", "Wind speed is high: " + realtimeUpdate.wind_speed + " KM/H");
        }

        if (realtimeUpdate.humidity > HIGH_HUMIDITY_THRESHOLD) {
            sendNotification("High humidity warning", "Humidity is high: " + realtimeUpdate.humidity + "%");
        }

    }

    private void sendNotification(String title, String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_weather_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }

        Log.d("WeatherAlertService", "Notification sent: " + title);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Weather Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for weather alerts");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
