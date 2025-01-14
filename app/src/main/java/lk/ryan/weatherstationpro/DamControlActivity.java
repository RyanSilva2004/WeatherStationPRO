package lk.ryan.weatherstationpro;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class DamControlActivity extends AppCompatActivity {

    private DatabaseReference damDatabase;
    private DatabaseReference sensorDatabase;
    private DatabaseReference waterLevelDatabase;
    private SeekBar seekBarGateAngle;
    private Button btnEnableControl;
    private Button btnBack;
    private TextView tvStatus;
    private TextView tvGateAngle;
    private TextView tvRainStatus;
    private TextView tvRainLevel;
    private TextView tvWaterLevel;
    private ImageView rainStatusIcon;
    private ImageView rainLevelIcon;
    private boolean isEnabled = false;
    private Handler handler = new Handler();
    private Runnable fetchWaterLevelTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dam_control);

        seekBarGateAngle = findViewById(R.id.seekBarGateAngle);
        btnEnableControl = findViewById(R.id.btnEnableControl);
        btnBack = findViewById(R.id.btnBack);
        tvStatus = findViewById(R.id.tvStatus);
        tvGateAngle = findViewById(R.id.tvGateAngle);
        tvRainStatus = findViewById(R.id.tvRainStatus);
        tvRainLevel = findViewById(R.id.tvRainLevel);
        tvWaterLevel = findViewById(R.id.tvWaterLevel);
        rainStatusIcon = findViewById(R.id.rainStatusIcon);
        rainLevelIcon = findViewById(R.id.rainLevelIcon);

        damDatabase = FirebaseDatabase.getInstance().getReference("dam_controller");
        sensorDatabase = FirebaseDatabase.getInstance().getReference("sensor");
        waterLevelDatabase = FirebaseDatabase.getInstance().getReference("realtime_update/dam_water_level");

        btnEnableControl.setOnClickListener(v -> toggleControl());

        seekBarGateAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isEnabled) {
                    int angle = (int) (progress * 0.9); // Map 0-100% to 0-90 degrees
                    tvGateAngle.setText(angle + "°");
                    updateGateAngle(angle);
                } else {
                    Toast.makeText(DamControlActivity.this, "Control is not enabled", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnBack.setOnClickListener(v -> finish());

        fetchDamStatus();
        fetchSensorData();
        startFetchingWaterLevel();
    }

    private void fetchDamStatus() {
        Query lastEntryQuery = damDatabase.orderByKey().limitToLast(1);
        lastEntryQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Boolean isEnabledValue = snapshot.child("is_enabled").getValue(Boolean.class);
                        Integer gateAngle = snapshot.child("gate_1").getValue(Integer.class);

                        if (isEnabledValue != null) {
                            isEnabled = isEnabledValue;
                            btnEnableControl.setText(isEnabled ? "Disable Control" : "Enable Control");
                        }
                        if (gateAngle != null) {
                            seekBarGateAngle.setProgress((int) (gateAngle / 0.9)); // Map 0-90 degrees to 0-100%
                            tvGateAngle.setText(gateAngle + "°");
                        }
                        seekBarGateAngle.setEnabled(isEnabled);
                        tvStatus.setText(isEnabled ? "Control Enabled" : "Control Disabled");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DamControl", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void fetchSensorData() {
        Query latestSensorDataQuery = sensorDatabase.orderByKey().limitToLast(1);
        latestSensorDataQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String rainStatus = snapshot.child("rain_status").getValue(String.class);
                        Integer rainLevel = snapshot.child("rain_level").getValue(Integer.class);

                        if (rainStatus != null) {
                            tvRainStatus.setText("Rain Status: " + rainStatus);
                            if ("Rain".equals(rainStatus)) {
                                rainStatusIcon.setImageResource(R.drawable.ic_rainy);
                            } else {
                                rainStatusIcon.setImageResource(R.drawable.ic_sunny);
                            }
                        }

                        if (rainLevel != null) {
                            int rainLevelInMl = rainLevel * 1000; // Convert mm to ml
                            tvRainLevel.setText("Rain Level: " + rainLevelInMl + " ml");
                            if (rainLevel > 50) {
                                rainLevelIcon.setImageResource(R.drawable.ic_high_water_lvl);
                            } else {
                                rainLevelIcon.setImageResource(R.drawable.ic_water_level);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DamControl", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void startFetchingWaterLevel() {
        fetchWaterLevelTask = new Runnable() {
            @Override
            public void run() {
                fetchWaterLevel();
                handler.postDelayed(this, 3000); // Fetch every 3 seconds
            }
        };
        handler.post(fetchWaterLevelTask);
    }

    private void fetchWaterLevel() {
        waterLevelDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String waterLevelStr = dataSnapshot.getValue(String.class);
                    updateWaterLevel(waterLevelStr);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DamControl", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void updateWaterLevel(String waterLevelStr) {
        int waterLevel = Integer.parseInt(waterLevelStr.replace("%", ""));
        tvWaterLevel.setText("Dam Water Level: " + waterLevelStr);

        if (waterLevel > 70)
        {
            tvWaterLevel.setTextColor(Color.RED);
        }
        else if (waterLevel > 50)
        {
            tvWaterLevel.setTextColor(Color.GREEN);
        }
        else
        {
            tvWaterLevel.setTextColor(Color.BLUE);
        }
    }

    private void toggleControl() {
        isEnabled = !isEnabled;
        damDatabase.child("is_enabled").setValue(isEnabled);
        seekBarGateAngle.setEnabled(isEnabled);
        tvStatus.setText(isEnabled ? "Control Enabled" : "Control Disabled");
        btnEnableControl.setText(isEnabled ? "Disable Control" : "Enable Control");
    }

    private void updateGateAngle(int angle) {
        damDatabase.child("gate_1").setValue(angle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(fetchWaterLevelTask);
    }
}