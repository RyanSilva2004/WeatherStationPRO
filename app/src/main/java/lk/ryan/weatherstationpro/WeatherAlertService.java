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

    // State variables to track alert statuses
    private boolean temperatureAlertSent = false;
    private boolean windSpeedAlertSent = false;
    private boolean rainLevelAlertSent = false;
    private boolean humidityAlertSent = false;
    private boolean damAlertSent = false;

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
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                RealtimeUpdate realtimeUpdate = dataSnapshot.getValue(RealtimeUpdate.class);
                if (realtimeUpdate != null)
                {
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
        // High temperature alert
        if (realtimeUpdate.temp > HIGH_TEMPERATURE_THRESHOLD) {
            if (!temperatureAlertSent) {
                sendNotification("High Temperature Warning", "Temperature is high: " + realtimeUpdate.temp + " °C");
                temperatureAlertSent = true;
            }
        }
        else
        {
            temperatureAlertSent = false;
        }

        // High wind speed alert
        if (realtimeUpdate.wind_speed > HIGH_WIND_SPEED_THRESHOLD) {
            if (!windSpeedAlertSent) {
                sendNotification("High Wind Speed Warning", "Wind speed is high: " + realtimeUpdate.wind_speed + " KM/H");
                windSpeedAlertSent = true;
            }
        } else {
            windSpeedAlertSent = false;
        }

        // High rain level alert
        if (realtimeUpdate.rain_level > HIGH_RAIN_LEVEL_THRESHOLD) {
            if (!rainLevelAlertSent) {
                sendNotification("Heavy Rain Warning", "Rain level is high: " + realtimeUpdate.rain_level + " mm");
                rainLevelAlertSent = true;
            }
        } else {
            rainLevelAlertSent = false;
        }

        // High humidity alert
        if (realtimeUpdate.humidity > HIGH_HUMIDITY_THRESHOLD) {
            if (!humidityAlertSent) {
                sendNotification("High Humidity Warning", "Humidity is high: " + realtimeUpdate.humidity + "%");
                humidityAlertSent = true;
            }
        }
        else
        {
            humidityAlertSent = false;
        }

        // Dam status alert with water level conditions
        if (realtimeUpdate.dam_status && realtimeUpdate.rain_level > HIGH_RAIN_LEVEL_THRESHOLD && !damAlertSent)
        {
            sendNotification("Dam Gate 1 Opened", "Dam is open. Water level: " + realtimeUpdate.dam_water_level + " is high.");
            damAlertSent = true;
        }
        else
        {
            // Dam is closed, notify about high or low water levels
            if (realtimeUpdate.rain_level > HIGH_RAIN_LEVEL_THRESHOLD && !damAlertSent)
            {
                sendNotification("High Water Level Warning", "Dam Water level is high: " + realtimeUpdate.dam_water_level+"Rain Water Level:"+realtimeUpdate.rain_level+"mm");
                damAlertSent = true;
            }
            else if (realtimeUpdate.rain_level <= HIGH_RAIN_LEVEL_THRESHOLD && damAlertSent) {
                sendNotification("Low Water Level Update", "Dam Water level is now normal: " + realtimeUpdate.dam_water_level);
                damAlertSent = false; // Reset the state
            }
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
            notificationManager.notify((int) System.currentTimeMillis(), notification); // Unique ID for each notification
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
