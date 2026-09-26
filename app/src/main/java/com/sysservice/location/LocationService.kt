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
