package com.sysservice.location

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
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val CHANNEL_ID = "LocationServiceChannel"
        const val TAG = "LocationService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            baseLatitude = it.getDoubleExtra(EXTRA_LAT, baseLatitude)
            baseLongitude = it.getDoubleExtra(EXTRA_LON, baseLongitude)
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
                    Log.d(TAG, "Test provider $provider created successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create test provider $provider. Did you select mock location app in developer options?", e)
                }
            }

            while (isRunning) {
                try {
                    // Антидетект джиттер (микро-колебания ±4 метра)
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
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to set mock location for $provider", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in mock loop", e)
                }
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
