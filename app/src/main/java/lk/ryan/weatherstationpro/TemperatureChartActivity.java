package lk.ryan.weatherstationpro;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

public class TemperatureChartActivity extends AppCompatActivity {

    private LineChart lineChart;
    private Spinner spinnerTimeRange;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_chart);

        lineChart = findViewById(R.id.lineChart);
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
        Button btnBack = findViewById(R.id.btnBack);

        mDatabase = FirebaseDatabase.getInstance().getReference("sensor");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading data...");
        progressDialog.setCancelable(false);

        spinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRange = parent.getItemAtPosition(position).toString();
                fetchAndPlotData(selectedRange);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchAndPlotData(String timeRange) {
        progressDialog.show();
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Entry> entries = new ArrayList<>();
                Long lastTimestamp = null;

                for (DataSnapshot timestampSnapshot : dataSnapshot.getChildren()) {
                    try {
                        String timestampStr = timestampSnapshot.getKey();
                        Long timestamp = convertTimestampToLong(timestampStr);
                        Float temperature = timestampSnapshot.child("dht_temperature").getValue(Float.class);
                        if (timestamp != null && temperature != null) {
                            entries.add(new Entry(timestamp, temperature));
                            if (lastTimestamp == null || timestamp > lastTimestamp) {
                                lastTimestamp = timestamp;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("TemperatureChart", "Error parsing data point: " + timestampSnapshot.toString(), e);
                    }
                }

                if (lastTimestamp != null) {
                    long timeRangeInMillis = getTimeRangeInMillis(timeRange, lastTimestamp);
                    long startTime = lastTimestamp - timeRangeInMillis;
                    List<Entry> filteredEntries = new ArrayList<>();

                    for (Entry entry : entries) {
                        if (entry.getX() >= startTime) {
                            filteredEntries.add(entry);
                        }
                    }

                    plotData(filteredEntries, timeRange);
                } else {
                    Log.e("TemperatureChart", "No valid data points found.");
                }

                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TemperatureChart", "Database error: " + databaseError.getMessage());
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

    private long getTimeRangeInMillis(String timeRange, long lastTimestamp) {
        switch (timeRange) {
            case "Daily":
                return 5 * 24 * 60 * 60 * 1000; // 5 days
            case "Per Minute":
                return 5 * 60 * 1000; // 5 minutes
            default:
                return 5 * 60 * 60 * 1000; // 5 hours
        }
    }

    private void plotData(List<Entry> entries, String timeRange) {
        if (entries.isEmpty()) {
            Log.e("TemperatureChart", "No data to plot");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Temperature");
        dataSet.setDrawIcons(false);
        dataSet.setColor(Color.BLACK);
        dataSet.setCircleColor(Color.BLACK);
        dataSet.setLineWidth(1f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFormLineWidth(1f);
        dataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        dataSet.setFormSize(15.f);

        if (android.os.Build.VERSION.SDK_INT >= 18) {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
            dataSet.setFillDrawable(drawable);
        } else {
            dataSet.setFillColor(Color.BLACK);
        }

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

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
        float maxTemp = 30f; // Example temperature value
        leftAxis.setAxisMinimum(maxTemp - 5);
        leftAxis.setAxisMaximum(maxTemp + 5);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.invalidate(); // Refresh the chart
        Log.d("TemperatureChart", "Data plotted successfully");
    }
}