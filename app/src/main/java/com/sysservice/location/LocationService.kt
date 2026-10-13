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
import android.os.PowerManager
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
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        var isServiceRunning = false
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_MODE = "extra_mode"
        const val MODE_CONTINUOUS = "mode_continuous"
        const val MODE_FREEZE = "mode_freeze"
        const val CHANNEL_ID = "LocationServiceChannel"
        const val TAG = "LocationService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_CONTINUOUS
        intent?.let {
            if (it.hasExtra(EXTRA_LAT) && it.hasExtra(EXTRA_LON)) {
                baseLatitude = it.getDoubleExtra(EXTRA_LAT, baseLatitude)
                baseLongitude = it.getDoubleExtra(EXTRA_LON, baseLongitude)
            }
        }

        if (mode == MODE_FREEZE) {
            // Режим мгновенной заморозки точки в кэш и отключение
            executeFreezeMode()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            isServiceRunning = true
            startForegroundServiceAndMock()
        }
        return START_STICKY
    }

    private fun executeFreezeMode() {
        serviceScope.launch {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

            for (provider in providers) {
                try {
                    try { locationManager.removeTestProvider(provider) } catch (e: Exception) {}
                    locationManager.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1)
                    locationManager.setTestProviderEnabled(provider, true)
                } catch (e: Exception) {}
            }

            // Шлем точку несколько раз, чтобы забить все системные буферы
            for (i in 1..3) {
                for (provider in providers) {
                    val mockLocation = Location(provider).apply {
                        latitude = baseLatitude
                        longitude = baseLongitude
                        altitude = 15.0
                        time = System.currentTimeMillis()
                        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        accuracy = 3.0f
                        speed = 0.0f
                        bearing = 0.0f
                    }
                    try {
                        locationManager.setTestProviderLocation(provider, mockLocation)
                    } catch (e: Exception) {}
                }
                delay(300)
            }

            // Сразу же удаляем тестовые провайдеры (как переключение в "Ничего"), оставляя кэш
            for (provider in providers) {
                try {
                    locationManager.removeTestProvider(provider)
                } catch (e: Exception) {}
            }

            isServiceRunning = false
            stopSelf()
        }
    }

    private fun startForegroundServiceAndMock() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Utility (Driver Mode)")
            .setContentText("Постоянная подмена активна")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notification)

        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SystemUtility::Wakelock")
            wakeLock?.acquire(10 * 60 * 60 * 1000L)
        } catch (e: Exception) {}

        serviceScope.launch {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

            fun initProviders() {
                for (provider in providers) {
                    try {
                        try { locationManager.removeTestProvider(provider) } catch (e: Exception) {}
                        locationManager.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1)
                        locationManager.setTestProviderEnabled(provider, true)
                    } catch (e: Exception) {}
                }
            }

            initProviders()
            var counter = 0

            while (isRunning) {
                try {
                    if (counter % 3 == 0) {
                        for (provider in providers) {
                            try { locationManager.setTestProviderEnabled(provider, true) } catch (e: Exception) {}
                        }
                    }
                    counter++

                    val jitterLat = baseLatitude + (Random.nextDouble() - 0.5) * 0.00008
                    val jitterLon = baseLongitude + (Random.nextDouble() - 0.5) * 0.00008

                    for (provider in providers) {
                        val mockLocation = Location(provider).apply {
                            latitude = jitterLat
                            longitude = jitterLon
                            altitude = 12.0 + Random.nextDouble() * 4.0
                            time = System.currentTimeMillis()
                            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                            accuracy = 2.0f + Random.nextFloat() * 2.0f
                            speed = 0.0f
                            bearing = 0.0f
                        }
                        try {
                            locationManager.setTestProviderLocation(provider, mockLocation)
                        } catch (e: Exception) {
                            initProviders()
                        }
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
        try { wakeLock?.release() } catch (e: Exception) {}
        serviceScope.cancel()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try { locationManager.removeTestProvider(provider) } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
