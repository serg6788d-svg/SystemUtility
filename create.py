import os

files = {
    # Файл конфигурации GitHub Actions с исправленной версией v4
    ".github/workflows/build.yml": """name: Build Android APK

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build APK with Gradle
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
""",

    "app/build.gradle": """plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    namespace 'com.sysservice.location'
    compileSdk 34

    defaultConfig {
        applicationId "com.sysservice.location"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
}
""",

    "app/src/main/AndroidManifest.xml": """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" tools:ignore="MockLocation" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" tools:ignore="ForegroundServicePermission" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_menu_mylocation"
        android:label="System Utility"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".LocationService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="location" />

    </application>
</manifest>
""",

    "app/src/main/res/layout/activity_main.xml": """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Введите координаты (Широта, Долгота):"
        android:textSize="16sp"
        android:layout_marginBottom="8dp"/>

    <EditText
        android:id="@+id/etCoordinates"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="55.654885, 37.828655"
        android:inputType="text"
        android:layout_marginBottom="24dp"/>

    <Button
        android:id="@+id/btnStart"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:text="ЗАПУСТИТЬ"
        android:textSize="16sp"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/btnStop"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:text="ОСТАНОВИТЬ"
        android:textSize="16sp"/>

</LinearLayout>
""",

    "app/src/main/java/com/sysservice/location/LocationService.kt": """package com.sysservice.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class LocationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    private var latitude: Double = 55.755864
    private var longitude: Double = 37.617698

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val CHANNEL_ID = "LocationServiceChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            latitude = it.getDoubleExtra(EXTRA_LAT, latitude)
            longitude = it.getDoubleExtra(EXTRA_LON, longitude)
        }
        if (!isRunning) {
            isRunning = true
            startForegroundServiceAndMock()
        }
        return START_STICKY
    }

    private fun startForegroundServiceAndMock() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Utility")
            .setContentText("Идет подмена координат в фоновом режиме")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notification)

        serviceScope.launch {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val provider = LocationManager.GPS_PROVIDER
            try {
                locationManager.addTestProvider(provider, false, false, false, false, true, true, true, 0, 1)
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: Exception) {}

            while (isRunning) {
                try {
                    val mockLocation = Location(provider).apply {
                        this.latitude = this@LocationService.latitude
                        this.longitude = this@LocationService.longitude
                        altitude = 0.0
                        time = System.currentTimeMillis()
                        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        accuracy = 3.0f
                    }
                    locationManager.setTestProviderLocation(provider, mockLocation)
                } catch (e: Exception) {}
                delay(1000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Location Mock Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
""",

    "app/src/main/java/com/sysservice/location/MainActivity.kt": """package com.sysservice.location

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCoordinates = findViewById<EditText>(R.id.etCoordinates)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            val input = etCoordinates.text.toString().trim()
            val parts = input.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lon = parts[1].trim().toDoubleOrNull()
                if (lat != null && lon != null) {
                    val serviceIntent = Intent(this, LocationService::class.java).apply {
                        putExtra(LocationService.EXTRA_LAT, lat)
                        putExtra(LocationService.EXTRA_LON, lon)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    Toast.makeText(this, "Подмена запущена в фоне!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Неверный формат чисел", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Введите координаты в формате: 55.654885, 37.828655", Toast.LENGTH_LONG).show()
            }
        }

        btnStop.setOnClickListener {
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)
            Toast.makeText(this, "Подмена остановлена", Toast.LENGTH_SHORT).show()
        }
    }
}
"""
}

for filepath, content in files.items():
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Обновлен/создан файл: {filepath}")

print("Готово! Все файлы записаны успешно.")