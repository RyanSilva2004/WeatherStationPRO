package lk.ryan.weatherstationpro;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
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
    private Switch switchGate;
    private Button btnEnableControl;
    private Button btnBack;
    private TextView tvStatus;
    private TextView tvGateStatus;
    private TextView tvRainStatus;
    private TextView tvRainLevel;
    private ImageView rainStatusIcon;
    private ImageView rainLevelIcon;
    private boolean isEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dam_control);

        switchGate = findViewById(R.id.switchGate);
        btnEnableControl = findViewById(R.id.btnEnableControl);
        btnBack = findViewById(R.id.btnBack);
        tvStatus = findViewById(R.id.tvStatus);
        tvGateStatus = findViewById(R.id.tvGateStatus);
        tvRainStatus = findViewById(R.id.tvRainStatus);
        tvRainLevel = findViewById(R.id.tvRainLevel);
        rainStatusIcon = findViewById(R.id.rainStatusIcon);
        rainLevelIcon = findViewById(R.id.rainLevelIcon);

        damDatabase = FirebaseDatabase.getInstance().getReference("dam_controller");
        sensorDatabase = FirebaseDatabase.getInstance().getReference("sensor");

        btnEnableControl.setOnClickListener(v -> toggleControl());

        switchGate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isEnabled) {
                updateGateStatus(isChecked);
            } else {
                Toast.makeText(DamControlActivity.this, "Control is not enabled", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());

        fetchDamStatus();
        fetchSensorData();
    }

    private void fetchDamStatus() {
        Query lastEntryQuery = damDatabase.orderByKey().limitToLast(1);
        lastEntryQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Boolean isEnabledValue = snapshot.child("is_enabled").getValue(Boolean.class);
                        Boolean gate1Status = snapshot.child("gate_1").getValue(Boolean.class);

                        if (isEnabledValue != null) {
                            isEnabled = isEnabledValue;
                            btnEnableControl.setText(isEnabled ? "Disable Control" : "Enable Control");
                        }
                        if (gate1Status != null) {
                            switchGate.setChecked(gate1Status);
                            tvGateStatus.setText(gate1Status ? "Open" : "Close");
                        }
                        switchGate.setEnabled(isEnabled);
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

    private void toggleControl() {
        isEnabled = !isEnabled;
        damDatabase.child("is_enabled").setValue(isEnabled);
        switchGate.setEnabled(isEnabled);
        tvStatus.setText(isEnabled ? "Control Enabled" : "Control Disabled");
        btnEnableControl.setText(isEnabled ? "Disable Control" : "Enable Control");
    }

    private void updateGateStatus(boolean isOpen) {
        damDatabase.child("gate_1").setValue(isOpen);
        tvGateStatus.setText(isOpen ? "Open" : "Close");
    }
}