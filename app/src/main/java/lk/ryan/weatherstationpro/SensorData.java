package lk.ryan.weatherstationpro;

public class SensorData {
    public double altitude;
    public double pressure;
    public double ds18b20_temperature;
    public double dht_temperature;
    public double bmp_temperature;
    public double humidity;
    public double water_level;
    public double wind_speed;
    public String rain_status;

    public SensorData() {
        // Default constructor required for calls to DataSnapshot.getValue(SensorData.class)
    }

    public boolean isRainy() {
        return "rain".equalsIgnoreCase(rain_status);
    }

    public boolean isTornadoWarning() {
        return wind_speed > 30;
    }

    public boolean isHighTemperature() {
        return getPrimaryTemperature() > 35;
    }

    public boolean isHighHumidity() {
        return humidity > 80;
    }


    public double getPrimaryTemperature() {
        if (!Double.isNaN(ds18b20_temperature)) {
            return ds18b20_temperature;
        } else if (!Double.isNaN(dht_temperature)) {
            return dht_temperature;
        } else {
            return bmp_temperature;
        }
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "altitude=" + altitude +
                ", pressure=" + pressure +
                ", ds18b20_temperature=" + ds18b20_temperature +
                ", dht_temperature=" + dht_temperature +
                ", bmp_temperature=" + bmp_temperature +
                ", humidity=" + humidity +
                ", water_level=" + water_level +
                ", wind_speed=" + wind_speed +
                ", rain_status='" + rain_status + '\'' +
                '}';
    }
}