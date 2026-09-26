@echo off
chcp 65001 > nul
echo Создание структуры папок...
mkdir app\src\main\java\com\sysservice\location 2>nul
mkdir app\src\main\res\layout 2>nul

echo Запись build.gradle...
(
echo plugins {
echo     id 'com.android.application'
echo     id 'kotlin-android'
echo }
echo 
echo android {
echo     namespace 'com.sysservice.location'
echo     compileSdk 34
echo 
echo     defaultConfig {
echo         applicationId "com.sysservice.location"
echo         minSdk 24
echo         targetSdk 34
echo         versionCode 1
echo         versionName "1.0"
echo     }
echo 
echo     compileOptions {
echo         sourceCompatibility JavaVersion.VERSION_17
echo         targetCompatibility JavaVersion.VERSION_17
echo     }
echo 
echo     kotlinOptions {
echo         jvmTarget = '17'
echo     }
echo }
echo 
echo dependencies {
echo     implementation 'androidx.core:core-ktx:1.12.0'
echo     implementation 'androidx.appcompat:appcompat:1.6.1'
echo     implementation 'com.google.android.material:material:1.10.0'
echo }
) > app\build.gradle

echo Запись AndroidManifest.xml...
(
echo ^<?xml version="1.0" encoding="utf-8"?^>
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android"
echo     xmlns:tools="http://schemas.android.com/tools"^>
echo 
echo     ^<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /^>
echo     ^<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /^>
echo     ^<uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" tools:ignore="MockLocation" /^>
echo     ^<uses-permission android:name="android.permission.FOREGROUND_SERVICE" /^>
echo     ^<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" tools:ignore="ForegroundServicePermission" /^>
echo 
echo     ^<application
echo         android:allowBackup="true"
echo         android:icon="@android:drawable/ic_menu_mylocation"
echo         android:label="System Utility"
echo         android:supportsRtl="true"
echo         android:theme="@style/Theme.AppCompat.Light.DarkActionBar"^>
echo         
echo         ^<activity
echo             android:name=".MainActivity"
echo             android:exported="true"^>
echo             ^<intent-filter^>
echo                 ^<action android:name="android.intent.action.MAIN" /^>
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^>
echo             ^</intent-filter^>
echo         ^</activity^>
echo 
echo         ^<service
echo             android:name=".LocationService"
echo             android:enabled="true"
echo             android:exported="false"
echo             android:foregroundServiceType="location" /^>
echo 
echo     ^</application^>
echo ^</manifest^>
) > app\src\main\AndroidManifest.xml

echo Запись activity_main.xml...
(
echo ^<?xml version="1.0" encoding="utf-8"?^>
echo ^<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
echo     android:layout_width="match_parent"
echo     android:layout_height="match_parent"
echo     android:orientation="vertical"
echo     android:padding="24dp"
echo     android:gravity="center"^>
echo 
echo     ^<TextView
echo         android:layout_width="match_parent"
echo         android:layout_height="wrap_content"
echo         android:text="Введите координаты (Широта, Долгота):"
echo         android:textSize="16sp"
echo         android:layout_marginBottom="8dp"/^>
echo 
echo     ^<EditText
echo         android:id="@+id/etCoordinates"
echo         android:layout_width="match_parent"
echo         android:layout_height="wrap_content"
echo         android:hint="55.654885, 37.828655"
echo         android:inputType="text"
echo         android:layout_marginBottom="24dp"/^>
echo 
echo     ^<Button
echo         android:id="@+id/btnStart"
echo         android:layout_width="match_parent"
echo         android:layout_height="60dp"
echo         android:text="ЗАПУСТИТЬ"
echo         android:textSize="16sp"
echo         android:layout_marginBottom="16dp"/^>
echo 
echo     ^<Button
echo         android:id="@+id/btnStop"
echo         android:layout_width="match_parent"
echo         android:layout_height="60dp"
echo         android:text="ОСТАНОВИТЬ"
echo         android:textSize="16sp"/^>
echo 
echo ^</LinearLayout^>
) > app\src\main\res\layout\activity_main.xml

echo Запись LocationService.kt...
(
echo package com.sysservice.location
echo 
echo import android.app.Notification
echo import android.app.NotificationChannel
echo import android.app.NotificationManager
echo import android.app.Service
echo import android.content.Intent
echo import android.location.Location
echo import android.location.LocationManager
echo import android.os.Build
echo import android.os.IBinder
echo import android.os.SystemClock
echo import androidx.core.app.NotificationCompat
echo import kotlinx.coroutines.*
echo 
echo class LocationService : Service() {
echo     private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
echo     private var isRunning = false
echo     private var latitude: Double = 55.755864
echo     private var longitude: Double = 37.617698
echo 
echo     companion object {
echo         const val EXTRA_LAT = "extra_lat"
echo         const val EXTRA_LON = "extra_lon"
echo         const val CHANNEL_ID = "LocationServiceChannel"
echo     }
echo 
echo     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
echo         intent?.let {
echo             latitude = it.getDoubleExtra(EXTRA_LAT, latitude)
echo             longitude = it.getDoubleExtra(EXTRA_LON, longitude)
echo         }
echo         if (!isRunning) {
echo             isRunning = true
echo             startForegroundServiceAndMock()
echo         }
echo         return START_STICKY
echo     }
echo 
echo     private fun startForegroundServiceAndMock() {
echo         createNotificationChannel()
echo         val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
echo             .setContentTitle("System Utility")
echo             .setContentText("Идет подмена координат в фоновом режиме")
echo             .setSmallIcon(android.R.drawable.ic_menu_mylocation)
echo             .build()
echo         startForeground(1, notification)
echo 
echo         serviceScope.launch {
echo             val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
echo             val provider = LocationManager.GPS_PROVIDER
echo             try {
echo                 locationManager.addTestProvider(provider, false, false, false, false, true, true, true, android.icu.text.UCharacter.UnicodeProperties.ENCODING, 1)
echo                 locationManager.setTestProviderEnabled(provider, true)
echo             } catch (e: Exception) {}
echo 
echo             while (isRunning) {
echo                 try {
echo                     val mockLocation = Location(provider).apply {
echo                         this.latitude = this@LocationService.latitude
echo                         this.longitude = this@LocationService.longitude
echo                         altitude = 0.0
echo                         time = System.currentTimeMillis()
echo                         elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
echo                         accuracy = 3.0f
echo                     }
echo                     locationManager.setTestProviderLocation(provider, mockLocation)
echo                 } catch (e: Exception) { e.printStackTrace() }
echo                 delay(1000)
echo             }
echo         }
echo     }
echo 
echo     private fun createNotificationChannel() {
echo         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
echo             val serviceChannel = NotificationChannel(CHANNEL_ID, "Location Mock Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
echo             val manager = getSystemService(NotificationManager::class.java)
echo             manager?.createNotificationChannel(serviceChannel)
echo         }
echo     }
echo 
echo     override fun onDestroy() {
echo         super.onDestroy()
echo         isRunning = false
echo         serviceScope.cancel()
echo         try {
echo             val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
echo             locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
echo         } catch (e: Exception) {}
echo     }
echo 
echo     override fun onBind(intent: Intent?): IBinder? = null
echo }
) > app\src\main\java\com\sysservice\location\LocationService.kt

echo Запись MainActivity.kt...
(
echo package com.sysservice.location
echo 
echo import android.content.Intent
echo import android.os.Bundle
echo import android.widget.Button
echo import android.widget.EditText
echo import android.widget.Toast
echo import androidx.appcompat.app.AppCompatActivity
echo 
echo class MainActivity : AppCompatActivity() {
echo     override fun onCreate(savedInstanceState: Bundle?) {
echo         super.onCreate(savedInstanceState)
echo         setContentView(R.layout.activity_main)
echo         val etCoordinates = findViewById^<EditText^>(R.id.etCoordinates)
echo         val btnStart = findViewById^<Button^>(R.id.btnStart)
echo         val btnStop = findViewById^<Button^>(R.id.btnStop)
echo 
echo         btnStart.setOnClickListener {
echo             val input = etCoordinates.text.toString().trim()
echo             val parts = input.split(",")
echo             if (parts.size == 2) {
echo                 val lat = parts[0].trim().toDoubleOrNull()
echo                 val lon = parts[1].trim().toDoubleOrNull()
echo                 if (lat != null && lon != null) {
echo                     val serviceIntent = Intent(this, LocationService::class.java).apply {
echo                         putExtra(LocationService.EXTRA_LAT, lat)
echo                         putExtra(LocationService.EXTRA_LON, lon)
echo                     }
echo                     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
echo                         startForegroundService(serviceIntent)
echo                     } else {
echo                         startService(serviceIntent)
echo                     }
echo                     Toast.makeText(this, "Подмена запущена в фоне!", Toast.LENGTH_SHORT).show()
echo                 } else { Toast.makeText(this, "Неверный формат чисел", Toast.LENGTH_SHORT).show() }
echo             } else { Toast.makeText(this, "Введите координаты в формате: 55.654885, 37.828655", Toast.LENGTH_LONG).show() }
echo         }
echo 
echo         btnStop.setOnClickListener {
echo             val serviceIntent = Intent(this, LocationService::class.java)
echo             stopService(serviceIntent)
echo             Toast.makeText(this, "Подмена остановлена", Toast.LENGTH_SHORT).show()
echo         }
echo     }
echo }
) > app\src\main\java\com\sysservice\location\MainActivity.kt

echo Готово! Все файлы успешно созданы.
pause