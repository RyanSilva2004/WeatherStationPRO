package lk.ryan.weatherstationpro;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
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

public class RainChartActivity extends AppCompatActivity {

    private PieChart pieChart;
    private LineChart lineChart;
    private Spinner spinnerTimeRange;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rain_chart);

        pieChart = findViewById(R.id.pieChart);
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
                List<Entry> waterLevelEntries = new ArrayList<>();
                int rainCount = 0;
                int noRainCount = 0;
                Long lastTimestamp = null;

                for (DataSnapshot timestampSnapshot : dataSnapshot.getChildren()) {
                    try {
                        String timestampStr = timestampSnapshot.getKey();
                        Long timestamp = convertTimestampToLong(timestampStr);
                        String rainStatus = timestampSnapshot.child("rain_status").getValue(String.class);
                        Integer waterLevel = timestampSnapshot.child("water_level").getValue(Integer.class);
                        if (timestamp != null && rainStatus != null && waterLevel != null) {
                            if (rainStatus.equals("Rain")) {
                                rainCount++;
                            } else {
                                noRainCount++;
                            }
                            waterLevelEntries.add(new Entry(timestamp, waterLevel));
                            if (lastTimestamp == null || timestamp > lastTimestamp) {
                                lastTimestamp = timestamp;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("RainChart", "Error parsing data point: " + timestampSnapshot.toString(), e);
                    }
                }

                if (lastTimestamp != null) {
                    long timeRangeInMillis = getTimeRangeInMillis(timeRange, lastTimestamp);
                    long startTime = lastTimestamp - timeRangeInMillis;
                    List<Entry> filteredWaterLevelEntries = new ArrayList<>();

                    for (Entry entry : waterLevelEntries) {
                        if (entry.getX() >= startTime) {
                            filteredWaterLevelEntries.add(entry);
                        }
                    }

                    plotPieChart(rainCount, noRainCount);
                    plotLineChart(filteredWaterLevelEntries);
                } else {
                    Log.e("RainChart", "No valid data points found.");
                }

                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("RainChart", "Database error: " + databaseError.getMessage());
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

    private void plotPieChart(int rainCount, int noRainCount) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(rainCount, "Rained"));
        entries.add(new PieEntry(noRainCount, "No Rain"));

        PieDataSet dataSet = new PieDataSet(entries, "Rain Status");
        dataSet.setColors(new int[]{Color.BLUE, Color.GRAY});
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate(); // Refresh the chart
        Log.d("RainChart", "Pie chart plotted successfully");
    }

    private void plotLineChart(List<Entry> waterLevelEntries) {
        if (waterLevelEntries.isEmpty()) {
            Log.e("RainChart", "No data to plot");
            return;
        }

        LineDataSet dataSet = new LineDataSet(waterLevelEntries, "Water Levels");
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setCircleRadius(4f);

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
        xAxis.setLabelCount(waterLevelEntries.size(), true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(90); // Rotate labels vertically
        xAxis.setAxisMinimum(waterLevelEntries.get(0).getX());
        xAxis.setAxisMaximum(waterLevelEntries.get(waterLevelEntries.size() - 1).getX());

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.invalidate(); // Refresh the chart
        Log.d("RainChart", "Line chart plotted successfully");
    }
}