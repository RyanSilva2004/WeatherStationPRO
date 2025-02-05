#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <ESP32Servo.h>  // Ensure correct library is included for servo control

// Replace these with your actual Wi-Fi and Firebase credentials
#define WIFI_SSID "YOUR WIFI_SSID"
#define WIFI_PASSWORD "YOUR WIFI_PASSWORD"
#define API_KEY "AIzaSyDCh2Yya6EbNEsMFpCQeemgYMR61gYZvsA"
#define DATABASE_URL "https://weather-station-e0804-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL ""
#define USER_PASSWORD ""

// Define the servo pin
#define SERVO_PIN 2  // Change to GPIO 2 (or any valid PWM pin)
#define WATER_SENSOR_PIN 34  // Change to the actual analog input pin connected to the water level sensor

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

Servo myServo;  // Create a servo object using the correct Servo class

void setup() {
  Serial.begin(115200);

  // Connect to Wi-Fi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());

  // Set up Firebase config
  config.api_key = API_KEY;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  config.database_url = DATABASE_URL;
  Firebase.reconnectNetwork(true);
  Firebase.begin(&config, &auth);

  // Attach the servo to the defined pin
  myServo.attach(SERVO_PIN);
  myServo.write(0);  // Initially set the servo to 0 degrees

  // Set the water sensor pin as an input
  pinMode(WATER_SENSOR_PIN, INPUT);
}

void loop() {
  // Check the "is_enabled" status
  String isEnabledPath = "/dam_controller/is_enabled";
  if (Firebase.RTDB.getBool(&fbdo, isEnabledPath)) {
    bool isEnabled = fbdo.boolData();
      // Always read the water sensor value (analog)
        int waterSensorValue = analogRead(WATER_SENSOR_PIN);

        // Map the water sensor value (0-4095) to a percentage (0-100)
        int waterLevelPercent = map(waterSensorValue, 0, 4095, 0, 100);

        // Print the water level percentage for debugging
        Serial.print("Water Level: ");
        Serial.print(waterLevelPercent);
        Serial.println("%");
        // Update the water level in Firebase
        String damWaterLevelPath = "/realtime_update/dam_water_level";
        String waterLevelString = String(waterLevelPercent) + "%";  // Convert percentage to string with '%' suffix
        if (Firebase.RTDB.setString(&fbdo, damWaterLevelPath, waterLevelString)) {
          Serial.print("Updated dam water level in Firebase: ");
          Serial.println(waterLevelString);
        } else {
          Serial.print("Failed to update dam water level in Firebase. Reason: ");
          Serial.println(fbdo.errorReason());
        }

    if (isEnabled) {
      // If "is_enabled" is true, check "gate_1" value
      String gate1Path = "/dam_controller/gate_1";
      if (Firebase.RTDB.getInt(&fbdo, gate1Path)) {
        int gate1Value = fbdo.intData();  // Read gate_1 value as an integer

        // Constrain gate_1 value between 0 and 90
        int servoAngle = constrain(gate1Value, 0, 90);

        // Set the servo position based on gate_1 value
        myServo.write(servoAngle);
        Serial.print("Gate 1 value: ");
        Serial.println(gate1Value);
        Serial.print("Servo set to: ");
        Serial.print(servoAngle);
        Serial.println(" degrees.");

        // Update dam status based on servo angle
        updateDamStatus(servoAngle > 0);  // True if angle > 0, false otherwise
      } else {
        Serial.println("Failed to read 'gate_1' value from Firebase.");
      }
    } else {
      // If "is_enabled" is false, proceed with water level logic
      Serial.println("'is_enabled' is false. Proceeding with water level logic.");


      // Only trigger the servo control if the water level is above 75%
      if (waterLevelPercent > 75) {
        // Path to the specific data in Firebase
        String rainLvlPath = "/realtime_update/rain_level";  // Path to rain level data

        // Fetch the current rain level from Firebase
        if (Firebase.RTDB.getString(&fbdo, rainLvlPath)) {
          String rainLvlStr = fbdo.stringData();  // Read the rain level as string
          int rainLvl = rainLvlStr.toInt();  // Convert the string to integer

          // Print the rain level
          Serial.print("Rain Level: ");
          Serial.println(rainLvl);

          // Initialize the servo angle
          int servoAngle = 0;

          // Determine the servo angle based on the rain level
          if (rainLvl >= 100) {
            servoAngle = (rainLvl - 100) / 10 * 5;  // Increase by 5 degrees for each 10mm after 100mm
            if (servoAngle > 90) {
              servoAngle = 90;  // Maximum angle is 90 degrees
            }
          }

          // Set the servo angle based on rain level
          myServo.write(servoAngle);
          Serial.print("Servo set to: ");
          Serial.print(servoAngle);
          Serial.println(" degrees.");

          // Update dam status based on servo angle
          updateDamStatus(servoAngle > 0);  // True if angle > 0, false otherwise
        } else {
          Serial.println("Failed to get rain level from Firebase.");
        }
      } else {
        myServo.write(0);
        Serial.println("Water level below 75%, servo not triggered. Gate closed");

        // Update dam status to false since gate is closed
        updateDamStatus(false);
      }
    }
  } else {
    Serial.println("Failed to read 'is_enabled' status from Firebase.");
  }

  // Wait for 5 seconds before checking again
  delay(5000);
}

void updateDamStatus(bool isOpen) {
  String damStatusPath = "/realtime_update/dam_status";
  if (Firebase.RTDB.setBool(&fbdo, damStatusPath, isOpen)) {
    Serial.print("Updated dam status in Firebase: ");
    Serial.println(isOpen ? "true (Open)" : "false (Closed)");
  } else {
    Serial.print("Failed to update dam status in Firebase. Reason: ");
    Serial.println(fbdo.errorReason());
  }
}
