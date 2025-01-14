package lk.ryan.weatherstationpro;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WindSpeedHistoryActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView maxSpeedTextView;
    private TextView minSpeedTextView;
    private TextView avgSpeedTextView;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wind_speed_history);

        lineChart = findViewById(R.id.lineChart);
        maxSpeedTextView = findViewById(R.id.maxSpeedTextView);
        minSpeedTextView = findViewById(R.id.minSpeedTextView);
        avgSpeedTextView = findViewById(R.id.avgSpeedTextView);
        Button btnBack = findViewById(R.id.btnBack);

        mDatabase = FirebaseDatabase.getInstance().getReference("sensor");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading data...");
        progressDialog.setCancelable(false);

        fetchAndPlotData();

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchAndPlotData() {
        progressDialog.show();
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Entry> lineEntries = new ArrayList<>();
                float maxSpeed = Float.MIN_VALUE;
                float minSpeed = Float.MAX_VALUE;
                float totalSpeed = 0;
                int count = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Float windSpeed = snapshot.child("wind_speed").getValue(Float.class);
                        String timestampStr = snapshot.getKey();
                        Long timestamp = convertTimestampToLong(timestampStr);
                        if (windSpeed != null && timestamp != null) {
                            lineEntries.add(new Entry(timestamp, windSpeed));
                            if (windSpeed > maxSpeed) {
                                maxSpeed = windSpeed;
                            }
                            if (windSpeed < minSpeed) {
                                minSpeed = windSpeed;
                            }
                            totalSpeed += windSpeed;
                            count++;
                        }
                    } catch (Exception e) {
                        Log.e("WindSpeedHistory", "Error parsing data point: " + snapshot.toString(), e);
                    }
                }

                if (count > 0) {
                    float avgSpeed = totalSpeed / count;
                    maxSpeedTextView.setText("Max Speed: " + maxSpeed + " km/h");
                    minSpeedTextView.setText("Min Speed: " + minSpeed + " km/h");
                    avgSpeedTextView.setText("Average Speed: " + avgSpeed + " km/h");
                }

                plotLineChart(lineEntries);
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("WindSpeedHistory", "Database error: " + databaseError.getMessage());
                progressDialog.dismiss();
            }
        });
    }

    private Long convertTimestampToLong(String timestampStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        try {
            Date date = sdf.parse(timestampStr);
            if (date != null) {
                return date.getTime();
            }
        } catch (ParseException e) {
            Log.e("RainChart", "Error parsing timestamp: " + timestampStr, e);
        }
        return null;
    }

    private void plotLineChart(List<Entry> entries) {
        if (entries.isEmpty()) {
            Log.e("WindSpeedHistory", "No data to plot");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Max Wind Speed");
        dataSet.setColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.RED);
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });
        xAxis.setLabelCount(entries.size(), true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(90); // Rotate labels vertically
        xAxis.setAxisMinimum(entries.get(0).getX());
        xAxis.setAxisMaximum(entries.get(entries.size() - 1).getX());

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.invalidate(); // Refresh the chart
        Log.d("WindSpeedHistory", "Line chart plotted successfully");
    }
}