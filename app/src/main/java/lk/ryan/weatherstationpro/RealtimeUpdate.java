package lk.ryan.weatherstationpro;

public class RealtimeUpdate {
    public boolean dam_status;
    public String dam_water_level;
    public int humidity;
    public int rain_level;
    public String rain_status;
    public double temp;
    public int wind_speed;

    public RealtimeUpdate() {
        // Default constructor required for calls to DataSnapshot.getValue(RealtimeUpdate.class)
    }

    @Override
    public String toString() {
        return "RealtimeUpdate{" +
                "dam_status=" + dam_status +
                ", dam_water_level='" + dam_water_level + '\'' +
                ", humidity=" + humidity +
                ", rain_level=" + rain_level +
                ", rain_status='" + rain_status + '\'' +
                ", temp=" + temp +
                ", wind_speed=" + wind_speed +
                '}';
    }
}
