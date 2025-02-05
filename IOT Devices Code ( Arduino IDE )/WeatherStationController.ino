#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BMP085_U.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <TimeLib.h>
#include "DHT.h"
#include <OneWire.h>
#include <DallasTemperature.h>

// Replace these with your actual Wi-Fi and Firebase credentials
#define WIFI_SSID "YOUR WIFI_SSID"
#define WIFI_PASSWORD "YOUR WIFI_PASSWORD"
#define API_KEY "AIzaSyDCh2Yya6EbNEsMFpCQeemgYMR61gYZvsA"
#define DATABASE_URL "https://weather-station-e0804-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL ""
#define USER_PASSWORD ""

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

unsigned long sendDataPrevMillis = 0;

Adafruit_BMP085_Unified bmp = Adafruit_BMP085_Unified(10085);

// NTP client setup
WiFiUDP udp;
const long UTC_OFFSET_SECONDS = 19800; // Offset for Sri Lanka (UTC+5:30)
NTPClient timeClient(udp, "pool.ntp.org", UTC_OFFSET_SECONDS, 60000);

// Define the input pin where the signal from the encoder is connected
#define SENSOR_PIN 14
#define WHEEL_RADIUS 0.009
const float SECONDS_IN_HOUR = 3600;
const float METERS_IN_KM = 1000.0;

volatile int pulseCount = 0;
unsigned long lastTime = 0;
unsigned long interval = 1000;
float wheelSpeedRPM = 0.0;
float wheelSpeedKMPH = 0.0;

// DHT sensor setup
#define DHTPIN 17
#define DHTTYPE DHT22

DHT dht(DHTPIN, DHTTYPE);

// DS18B20 sensor setup
#define ONE_WIRE_BUS 4
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature ds18b20(&oneWire);

// Rain sensor setup
#define RAIN_SENSOR_ANALOG_PIN 33 // Updated pin definition
#define RAIN_THRESHOLD 3000 // Threshold for wet/dry status

// Water level sensor setup
#define WATER_LEVEL_SENSOR_PIN 34 // Updated pin definition

// Interrupt service routine to count pulses
void IRAM_ATTR pulseISR() {
  pulseCount++;
}

void setup() {
  Serial.begin(115200);

  pinMode(SENSOR_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(SENSOR_PIN), pulseISR, RISING);
  lastTime = millis();

  Serial.println("Initializing sensors and Wi-Fi connection...");

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());

  config.api_key = API_KEY;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  config.database_url = DATABASE_URL;
  config.token_status_callback = tokenStatusCallback;
  Firebase.reconnectNetwork(true);
  fbdo.setBSSLBufferSize(4096, 1024);
  fbdo.setResponseSize(2048);
  Firebase.begin(&config, &auth);
  Firebase.setDoubleDigits(5);
  config.timeout.serverResponse = 10 * 1000;

  if (!bmp.begin()) {
    Serial.println("Could not find a valid BMP180 sensor, check wiring!");
    while (1);
  }

  timeClient.begin();
  timeClient.update();

  dht.begin();
  ds18b20.begin();
}

void loop() {
  unsigned long currentTime = millis();

  if (currentTime - lastTime >= interval) {
    wheelSpeedRPM = pulseCount * 60.0;
    float circumference = 2 * PI * WHEEL_RADIUS;
    float speedMPS = (wheelSpeedRPM / 60.0) * circumference;
    wheelSpeedKMPH = (speedMPS * SECONDS_IN_HOUR) / METERS_IN_KM;

    Serial.print("Wheel Speed: ");
    Serial.print(wheelSpeedKMPH);
    Serial.println(" km/h");

    pulseCount = 0;
    lastTime = currentTime;
  }

  if (Firebase.ready() && (millis() - sendDataPrevMillis > 10000 || sendDataPrevMillis == 0)) {
    sendDataPrevMillis = millis();

    sensors_event_t event;
    bmp.getEvent(&event);

    if (event.pressure) {
      float pressure = event.pressure;
      float seaLevelPressure = 1014.25;
      float altitude = bmp.pressureToAltitude(seaLevelPressure, event.pressure)+10.0;
      float temperature;
      bmp.getTemperature(&temperature);

      float dhtTemperature = dht.readTemperature();
      float dhtHumidity = dht.readHumidity();

      ds18b20.requestTemperatures();
      float ds18b20Temperature = ds18b20.getTempCByIndex(0);
      if (ds18b20Temperature == DEVICE_DISCONNECTED_C) {
        ds18b20Temperature = 0.0;
      }

      if (isnan(dhtTemperature) || isnan(dhtHumidity)) {
        Serial.println("Failed to read from DHT sensor!");
        return;
      }

      // Read rain sensor
     // int rainAnalogValue = analogRead(RAIN_SENSOR_ANALOG_PIN);
      //int rainfallPercentage = map(rainAnalogValue, 0, 2000, 0, 100);
       int rainAnalogValue = analogRead(RAIN_SENSOR_ANALOG_PIN);
        Serial.printf("Rain Analog Value: %d\n", rainAnalogValue);
        String rainStatus = (rainAnalogValue > RAIN_THRESHOLD) ? "Dry" : "Rain";





      int waterLevelSensorValue = analogRead(WATER_LEVEL_SENSOR_PIN);
Serial.printf("Water level analog value :    %d\n", waterLevelSensorValue);
// Custom calibration logic
int waterLevel;
if (waterLevelSensorValue < 200) {
  waterLevel = map(waterLevelSensorValue, 0, 1000, 0, 150);  // First part of the curve
} else if (waterLevelSensorValue < 600) {
  waterLevel = map(waterLevelSensorValue, 1000, 1800, 150, 300);  // Middle part of the curve
} else {
  waterLevel = map(waterLevelSensorValue, 1800, 2261, 300, 400);  // Upper part of the curve
}

Serial.printf("Water Level: %d mm\n", waterLevel);  // Maps the value to a range of 0-40mm

      String formattedDateTime = getFormattedDateTime();

      Serial.printf("Pressure: %f hPa\n", pressure);
      Serial.printf("Altitude: %f m\n", altitude);
      Serial.printf("Temperature (BMP180): %f °C\n", temperature);
      Serial.printf("DHT Temperature: %f °C\n", dhtTemperature);
      Serial.printf("Humidity: %f %%\n", dhtHumidity);
      Serial.printf("DS18B20 Temperature: %f °C\n", ds18b20Temperature);
      //Serial.printf("Rainfall Intensity: %d %%\n", rainfallPercentage);
      //Serial.printf("Rain Status: %s\n", rainStatus.c_str());
      Serial.printf("Rain Analog Value: %d, Threshold: %d, Status: %s\n", 
              rainAnalogValue, RAIN_THRESHOLD, rainStatus.c_str());
      Serial.printf("Water Level: %d mm\n", waterLevel);
      Serial.printf("Wind Speed: %f km/h\n", wheelSpeedKMPH);

      String path = "/sensor/";
      String fullPath = path + formattedDateTime;
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/pressure", pressure);
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/altitude", altitude);
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/bmp_temperature", temperature);
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/dht_temperature", dhtTemperature);
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/humidity", dhtHumidity);
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/ds18b20_temperature", ds18b20Temperature);
      Firebase.RTDB.setString(&fbdo, fullPath + "/rain_status", rainStatus);
     // Firebase.RTDB.setInt(&fbdo, fullPath + "/rainfall_intensity", rainfallPercentage);
      Firebase.RTDB.setInt(&fbdo, fullPath + "/water_level", waterLevel);
      Firebase.RTDB.setFloat(&fbdo, fullPath + "/wind_speed", wheelSpeedKMPH);

       // Update real-time paths
      if (!Firebase.RTDB.setFloat(&fbdo, "/realtime_update/humidity", dhtHumidity)) {
        Serial.print("Failed to update humidity: ");
        Serial.println(fbdo.errorReason());
      }

      if (!Firebase.RTDB.setInt(&fbdo, "/realtime_update/rain_level", waterLevel)) {
        Serial.print("Failed to update rain level: ");
        Serial.println(fbdo.errorReason());
      }

      if (!Firebase.RTDB.setString(&fbdo, "/realtime_update/rain_status", rainStatus)) {
        Serial.print("Failed to update rain status: ");
        Serial.println(fbdo.errorReason());
      }

      if (!Firebase.RTDB.setFloat(&fbdo, "/realtime_update/temp", ds18b20Temperature)) {
        Serial.print("Failed to update temperature: ");
        Serial.println(fbdo.errorReason());
      }

      if (!Firebase.RTDB.setFloat(&fbdo, "/realtime_update/wind_speed", wheelSpeedKMPH)) {
        Serial.print("Failed to update wind speed: ");
        Serial.println(fbdo.errorReason());
      }
    } else {
      Serial.println("Sensor error");
    }
  }

  delay(1000);  // Delay for 1 second
}

String getFormattedDateTime() {
  timeClient.update();
  unsigned long epochTime = timeClient.getEpochTime();
  setTime(epochTime);

  String formattedDateTime = String(year()) + "-" +
                             (month() < 10 ? "0" : "") + String(month()) + "-" +
                             (day() < 10 ? "0" : "") + String(day()) + "T" +
                             (hour() < 10 ? "0" : "") + String(hour()) + ":" +
                             (minute() < 10 ? "0" : "") + String(minute()) + ":" +
                             (second() < 10 ? "0" : "") + String(second());
  return formattedDateTime;
}

