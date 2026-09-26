import os

files = {
    # 1. GitHub Actions Workflow
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

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3
      with:
        gradle-version: '8.4'

    - name: Build APK with Gradle
      run: gradle assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
""",

    # 2. settings.gradle
    "settings.gradle": """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SystemUtility"
include ':app'
""",

    # 3. build.gradle (корневой)
    "build.gradle": """buildscript {
    ext.kotlin_version = '1.9.0'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.0'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
""",

    # 4. gradle.properties
    "gradle.properties": """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
""",

    # 5. gradlew (Bash скрипт для Linux)
    "gradlew": """#!/usr/bin/env sh
org_gradle_java_home=""
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        org_gradle_java_home="$JAVA_HOME"
    fi
fi
exec java $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=gradlew" -classpath "$PRGDIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
""",

    # 6. gradlew.bat (для Windows)
    "gradlew.bat": """@if "%DEBUG%" == "" @echo off
setlocal
set DIRNAME=%~dp5
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
@rem Execute Gradle
java %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal
""",

    # 7. gradle/wrapper/gradle-wrapper.properties
    "gradle/wrapper/gradle-wrapper.properties": """distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipPath=wrapper/dists
""",

    # 8. app/build.gradle
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
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
""",

    # 9. AndroidManifest.xml
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

    # 10. activity_main.xml
    "app/src/main/res/layout/activity_main.xml": """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Статус: Остановлено"
        android:textSize="18sp"
        android:textStyle="bold"
        android:gravity="center"
        android:layout_marginBottom="24dp"/>

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

    # 11. LocationService.kt (со статическим флагом состояния)
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
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.random.Random

class LocationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    private var baseLatitude: Double = 55.755864
    private var baseLongitude: Double = 37.617698

    companion object {
        var isServiceRunning = false
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val CHANNEL_ID = "LocationServiceChannel"
        const val TAG = "LocationService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(EXTRA_LAT) && it.hasExtra(EXTRA_LON)) {
                baseLatitude = it.getDoubleExtra(EXTRA_LAT, baseLatitude)
                baseLongitude = it.getDoubleExtra(EXTRA_LON, baseLongitude)
            }
        }
        if (!isRunning) {
            isRunning = true
            isServiceRunning = true
            startForegroundServiceAndMock()
        }
        return START_STICKY
    }

    private fun startForegroundServiceAndMock() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Utility (Driver Mode)")
            .setContentText("Подмена координат активна")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notification)

        serviceScope.launch {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

            for (provider in providers) {
                try {
                    try {
                        locationManager.removeTestProvider(provider)
                    } catch (e: Exception) {}

                    locationManager.addTestProvider(
                        provider, false, false, false, false,
                        true, true, true, 1, 1
                    )
                    locationManager.setTestProviderEnabled(provider, true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create test provider $provider", e)
                }
            }

            while (isRunning) {
                try {
                    val jitterLat = baseLatitude + (Random.nextDouble() - 0.5) * 0.00008
                    val jitterLon = baseLongitude + (Random.nextDouble() - 0.5) * 0.00008

                    for (provider in providers) {
                        val mockLocation = Location(provider).apply {
                            latitude = jitterLat
                            longitude = jitterLon
                            altitude = 15.0 + Random.nextDouble() * 5.0
                            time = System.currentTimeMillis()
                            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                            accuracy = 2.5f + Random.nextFloat() * 2.0f
                            speed = 0.0f
                            bearing = 0.0f
                        }
                        try {
                            locationManager.setTestProviderLocation(provider, mockLocation)
                        } catch (e: Exception) {}
                    }
                } catch (e: Exception) {}
                delay(1000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Driver Location Mock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isServiceRunning = false
        serviceScope.cancel()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                locationManager.removeTestProvider(provider)
            } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
""",

    # 12. MainActivity.kt (с проверкой onResume для синхронизации)
    "app/src/main/java/com/sysservice/location/MainActivity.kt": """package com.sysservice.location

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var etCoordinates: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCoordinates = findViewById(R.id.etCoordinates)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)

        updateUIState(LocationService.isServiceRunning)

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
                    updateUIState(true)
                    Toast.makeText(this, "Служба запущена в режиме водителя!", Toast.LENGTH_SHORT).show()
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
            updateUIState(false)
            Toast.makeText(this, "Подмена остановлена", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Синхронизируем интерфейс при возврате в приложение или смене режима экрана (разделение экрана)
        updateUIState(LocationService.isServiceRunning)
    }

    private fun updateUIState(isRunning: Boolean) {
        btnStart.isEnabled = !isRunning
        btnStop.isEnabled = isRunning
        etCoordinates.isEnabled = !isRunning

        if (isRunning) {
            tvStatus.text = "Статус: Служба работает (Джиттер активен)"
            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tvStatus.text = "Статус: Остановлено"
            tvStatus.setTextColor(Color.parseColor("#C62828"))
        }
    }
}
"""
}

for filepath, content in files.items():
    dirname = os.path.dirname(filepath)
    if dirname:
        os.makedirs(dirname, exist_ok=True)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Создан/обновлен: {filepath}")

print("Все файлы проекта успешно обновлены!")