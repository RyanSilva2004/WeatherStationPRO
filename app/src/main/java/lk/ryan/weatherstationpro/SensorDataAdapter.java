package lk.ryan.weatherstationpro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.SensorDataViewHolder> {

    private List<SensorData> sensorDataList;

    public SensorDataAdapter(List<SensorData> sensorDataList) {
        this.sensorDataList = sensorDataList;
    }

    @NonNull
    @Override
    public SensorDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_data_item, parent, false);
        return new SensorDataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorDataViewHolder holder, int position) {
        SensorData sensorData = sensorDataList.get(position);
        holder.bind(sensorData);
    }

    @Override
    public int getItemCount() {
        return sensorDataList.size();
    }

    public static class SensorDataViewHolder extends RecyclerView.ViewHolder {

        private TextView altitudeTextView;
        private TextView pressureTextView;
        private TextView temperatureTextView;
        private TextView humidityTextView;
        private TextView waterLevelTextView;
        private TextView windSpeedTextView;
        private TextView rainStatusTextView;

        private TextView altitudeMeasureTextView;
        private TextView pressureMeasureTextView;
        private TextView temperatureMeasureTextView;
        private TextView humidityMeasureTextView;
        private TextView waterLevelMeasureTextView;
        private TextView windSpeedMeasureTextView;
        private TextView rainStatusMeasureTextView;

        public SensorDataViewHolder(@NonNull View itemView) {
            super(itemView);
            altitudeTextView = itemView.findViewById(R.id.altitudeTextView);
            pressureTextView = itemView.findViewById(R.id.pressureTextView);
            temperatureTextView = itemView.findViewById(R.id.temperatureTextView);
            humidityTextView = itemView.findViewById(R.id.humidityTextView);
            waterLevelTextView = itemView.findViewById(R.id.waterLevelTextView);
            windSpeedTextView = itemView.findViewById(R.id.windSpeedTextView);
            rainStatusTextView = itemView.findViewById(R.id.rainStatusTextView);

            altitudeMeasureTextView = itemView.findViewById(R.id.altitudeMeasureTextView);
            pressureMeasureTextView = itemView.findViewById(R.id.pressureMeasureTextView);
            temperatureMeasureTextView = itemView.findViewById(R.id.temperatureMeasureTextView);
            humidityMeasureTextView = itemView.findViewById(R.id.humidityMeasureTextView);
            waterLevelMeasureTextView = itemView.findViewById(R.id.waterLevelMeasureTextView);
            windSpeedMeasureTextView = itemView.findViewById(R.id.windSpeedMeasureTextView);
            rainStatusMeasureTextView = itemView.findViewById(R.id.rainStatusMeasureTextView);
        }

        public void bind(SensorData sensorData) {
            altitudeMeasureTextView.setText(sensorData.altitude + " m");
            pressureMeasureTextView.setText(sensorData.pressure + " hPa");
            temperatureMeasureTextView.setText(sensorData.getPrimaryTemperature() + "°C");
            humidityMeasureTextView.setText(sensorData.humidity + "%");
            waterLevelMeasureTextView.setText(sensorData.water_level + " m");
            windSpeedMeasureTextView.setText(sensorData.wind_speed + " km/h");
            rainStatusMeasureTextView.setText(sensorData.rain_status);
        }
    }
}