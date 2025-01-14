<!DOCTYPE html>
<html>
<body>
    <h1>🌦️ Weather Station Pro</h1>
    <p><strong>Weather Station Pro</strong> is an advanced IoT and mobile application project for real-time weather monitoring and control systems. This project integrates Firebase Realtime Database for data storage and Android for user interaction, providing timely notifications and controls.</p>
    <h2>✨ Features</h2>
    <ul>
        <li>📡 Real-time weather monitoring including temperature, humidity, wind speed, and rain status.</li>
        <li>🌊 Dam control with gate status and water level monitoring.</li>
        <li>🚨 Notification alerts for critical conditions like heavy rain or high wind speeds.</li>
        <li>📈 Interactive charts for temperature, rain levels, and wind speed history.</li>
        <li>📱 User-friendly Android interface integrated with Firebase.</li>
    </ul>
    <h2>🖼️ Screenshots</h2>
    <img src="https://github.com/user-attachments/assets/362781a8-648a-4c8b-8997-52ebe82c2579" alt="Home - Realtime Updates" style="width:18%; margin:5px;" />
    <img src="https://github.com/user-attachments/assets/f0d212d4-1b8f-4e4a-bfda-d8c6eed24072" alt="Temperature History Chart" style="width:18%; margin:5px;" />
    <img src="https://github.com/user-attachments/assets/8d646364-14db-4768-ab5d-aa202658bdf8" alt="Rain History Chart" style="width:18%; margin:5px;" />
    <img src="https://github.com/user-attachments/assets/d03a7e63-6da2-4d56-a6b9-7fe2f7034a99" alt="Wind Speed History Chart" style="width:18%; margin:5px;" />
    <img src="https://github.com/user-attachments/assets/b212c35d-94b5-47c1-9402-26a8eedd4336" alt="Dam Controller" style="width:18%; margin:5px;" />
    <h2>🏗️ System Architecture</h2>
    <img src="path-to-your-architecture-diagram.png" alt="System Architecture Diagram" />
    <h2>🔧 Hardware Circuit Diagram</h2>
    <img src="https://github.com/user-attachments/assets/0c5ccf37-c443-43c0-ba8e-2a21518ec1ff" alt="Circuit Diagram" />
    <h2>📂 Real-Time Firebase Data</h2>
    <p>Data is fetched from the following paths in the Firebase Realtime Database:</p>
    <ul>
        <li><code>/realtime_update</code></li>
        <li><code>/dam_controller</code></li>
        <li><code>/sensor</code></li>
    </ul>
    <h2>🚀 Getting Started</h2>
    <ol>
        <li>📥 Clone the repository: <code>git clone https://github.com/RyanSilva2004/WeatherStationPRO.git</code></li>
        <li>🛠️ Open the Android project in Android Studio.</li>
        <li>🔗 Set up Firebase Realtime Database and connect your application to Firebase.</li>
        <li>📲 Run the app on your Android device or emulator.</li>
    </ol>
    <h2>📂 APK Download</h2>
    <p>
        <a href="https://github.com/RyanSilva2004/WeatherStationPRO/raw/main/WeatherStationPRO.apk" 
           download="https://drive.google.com/file/d/1D7Jv43ACDggKeHedTFvjP25q1_AdVypw/view?usp=sharing" 
           style="display:inline-block; padding:10px 20px; font-size:16px; color:white; background-color:#007BFF; text-decoration:none; border-radius:5px;">
           📥 Download APK
        </a>
    </p>
    <h2>🗂️ Firebase JSON Structure</h2>
    <pre>
<code>
[
  "realtime_update": {
    "dam_status": false,
    "dam_water_level": "0%",
    "humidity": 76.9,
    "rain_level": 0,
    "rain_status": "Dry",
    "temp": 27.4375,
    "wind_speed": 0
  },
  "dam_controller": {
    "gate_1": 0,
    "is_enabled": false
  },
  "sensor": {
    "2025-01-12T01:20:20": {
      "Altitude": 4.7478,
      "bmp_temperature": 30,
      "dht_temperature": 28,
      "ds18b20_temperature": 27,
      "humidity": 75,
      "pressure": 1012,
      "rain_status": "Rain",
      "water_level": 5,
      "wind_speed": 0
    }
  }
]
</code>
    </pre>
    <h2>📜 License</h2>
    <p>This project is licensed under the MIT License - see the LICENSE file for details.</p>
</body>
</html>
