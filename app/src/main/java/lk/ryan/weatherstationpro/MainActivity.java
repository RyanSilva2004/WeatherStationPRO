package lk.ryan.weatherstationpro;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private SensorDataAdapter adapter;
    private List<SensorData> sensorDataList = new ArrayList<>();
    private TextView weatherStatusTextView;
    private ImageView weatherStatusImageView;
    private TextView greetingTextView;
    private Button btnTemperatureChart;
    private Button btnRainChart;
    private Button btnWindSpeedHistory;
    private Button btnDamControl;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Force light mode
        Intent serviceIntent = new Intent(this, WeatherAlertService.class);
        startService(serviceIntent);
        setContentView(R.layout.home_layout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference("sensor");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SensorDataAdapter(sensorDataList);
        recyclerView.setAdapter(adapter);

        weatherStatusTextView = findViewById(R.id.weatherStatusTextView);
        weatherStatusImageView = findViewById(R.id.weatherStatusImageView);
        greetingTextView = findViewById(R.id.greetingTextView);
        btnTemperatureChart = findViewById(R.id.btnTemperatureHistory);
        btnRainChart = findViewById(R.id.btnRainHistory);
        btnWindSpeedHistory = findViewById(R.id.btnWindSpeedHistory);
        btnDamControl = findViewById(R.id.btnDamControl);

        // Initialize and show the ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        fetchSensorData();

        btnTemperatureChart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TemperatureChartActivity.class);
            startActivity(intent);
        });

        btnRainChart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RainChartActivity.class);
            startActivity(intent);
        });

        btnWindSpeedHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WindSpeedHistoryActivity.class);
            startActivity(intent);
        });

        btnDamControl.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DamControlActivity.class);
            startActivity(intent);
        });
    }

    private void fetchSensorData() {
        Query latestSensorDataQuery = mDatabase.orderByKey().limitToLast(1);
        latestSensorDataQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sensorDataList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    SensorData sensorData = snapshot.getValue(SensorData.class);
                    if (sensorData != null) {
                        sensorDataList.add(sensorData);
                        updateWeatherStatus(sensorData);
                        Log.d("MainActivity", "Fetched data: " + sensorData.toString());
                    }
                }
                adapter.notifyDataSetChanged();
                Log.d("MainActivity", "Sensor data list size: " + sensorDataList.size());

                // Dismiss the ProgressDialog once data is fetched
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Database error: " + databaseError.getMessage());

                // Dismiss the ProgressDialog in case of error
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });

        new Handler().postDelayed(this::fetchSensorData, 3000);
    }

    private void updateWeatherStatus(SensorData sensorData) {
        String weatherStatus;
        int weatherImageResId;

        if (sensorData.rain_status != null && sensorData.rain_status.equals("Rain")) {
            weatherStatus = "It's a rainy day";
            weatherImageResId = R.drawable.ic_rainy;
        } else if (sensorData.wind_speed > 20) {
            weatherStatus = "It's a windy day";
            weatherImageResId = R.drawable.ic_windy;
        } else if (sensorData.humidity > 80) {
            weatherStatus = "It's a humid day";
            weatherImageResId = R.drawable.ic_humid;
        } else {
            weatherStatus = "It's a sunny day";
            weatherImageResId = R.drawable.ic_sunny;
        }

        greetingTextView.setText("Hi Welcome...");
        weatherStatusTextView.setText(weatherStatus);
        weatherStatusImageView.setImageResource(weatherImageResId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "WeatherAlerts",
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