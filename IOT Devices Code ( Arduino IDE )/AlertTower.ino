#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>

// Define Firebase credentials
#define WIFI_SSID "Your WiFi SSID"
#define WIFI_PASSWORD "Your WiFi Password"
#define API_KEY "AIzaSyDCh2Yya6EbNEsMFpCQeemgYMR61gYZvsA"
#define DATABASE_URL "https://weather-station-e0804-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL ""
#define USER_PASSWORD ""

// Define Firebase Data Object
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// Define Pins
#define BUZZER_PIN 18  // Buzzer connected to GPIO18
#define BLUE_LED 26    // Blue LED (High Rain Level)
#define RED_LED 27     // Red LED (Tornado Warning - High Wind)
#define GREEN_LED 14   // Green LED (Normal Condition)

bool onWindSpeedBuzzer = false;
bool onRainLevelBuzzer = false;

void setup() {
    Serial.begin(115200);

    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(BLUE_LED, OUTPUT);
    pinMode(RED_LED, OUTPUT);
    pinMode(GREEN_LED, OUTPUT);

    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(BLUE_LED, LOW);
    digitalWrite(RED_LED, LOW);
    digitalWrite(GREEN_LED, HIGH);  // Default to normal state

    // Connect to WiFi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to WiFi...");
    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        delay(1000);
    }
    Serial.println("\nConnected to WiFi!");

    // Firebase Configuration
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;
    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;

    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
}

void loop() {
    int rainLevel = 0, windSpeed = 0;

    // Fetch data from Firebase
    if (Firebase.RTDB.getInt(&fbdo, "/realtime_update/rain_level")) {
        rainLevel = fbdo.to<int>();
    } else {
        Serial.println("Failed to get rain level: " + fbdo.errorReason());
    }

    if (Firebase.RTDB.getInt(&fbdo, "/realtime_update/wind_speed")) {
        windSpeed = fbdo.to<int>();
    } else {
        Serial.println("Failed to get wind speed: " + fbdo.errorReason());
    }

    Serial.print("Rain Level: ");
    Serial.print(rainLevel);
    Serial.print(" | Wind Speed: ");
    Serial.println(windSpeed);

    // Check rain level
    if (rainLevel > 70 && !onRainLevelBuzzer) {
        onRainLevelBuzzer = true;
        digitalWrite(BUZZER_PIN, HIGH);
        delay(2000);
        digitalWrite(BUZZER_PIN, LOW);
    } else if (rainLevel <= 70) {
        onRainLevelBuzzer = false;
    }

    // Check wind speed
    if (windSpeed > 30 && !onWindSpeedBuzzer) {
        onWindSpeedBuzzer = true;
        digitalWrite(BUZZER_PIN, HIGH);
        delay(2000);
        digitalWrite(BUZZER_PIN, LOW);
    } else if (windSpeed <= 30) {
        onWindSpeedBuzzer = false;
    }

    // LED Control
    if (rainLevel > 70 && windSpeed > 30) {  
        digitalWrite(BLUE_LED, HIGH);
        digitalWrite(RED_LED, HIGH);
        digitalWrite(GREEN_LED, LOW);
    } 
    else if (rainLevel > 70) {  
        digitalWrite(BLUE_LED, HIGH);
        digitalWrite(RED_LED, LOW);
        digitalWrite(GREEN_LED, LOW);
    } 
    else if (windSpeed > 30) {  
        digitalWrite(RED_LED, HIGH);
        digitalWrite(BLUE_LED, LOW);
        digitalWrite(GREEN_LED, LOW);
    } 
    else {  
        digitalWrite(GREEN_LED, HIGH);
        digitalWrite(BLUE_LED, LOW);
        digitalWrite(RED_LED, LOW);
    }

    delay(1000);  // Scan every 1 second
}