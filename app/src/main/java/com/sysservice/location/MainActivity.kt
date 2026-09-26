package com.sysservice.location

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager
    private var timer: Timer? = null
    private val providerName = LocationManager.GPS_PROVIDER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val etLat = findViewById<EditText>(R.id.etLat)
        val etLng = findViewById<EditText>(R.id.etLng)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            val lat = etLat.text.toString().toDoubleOrNull()
            val lng = etLng.text.toString().toDoubleOrNull()
            if (lat != null && lng != null) {
                startMocking(lat, lng)
                Toast.makeText(this, "Служба запущена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Введите координаты", Toast.LENGTH_SHORT).show()
            }
        }
        btnStop.setOnClickListener {
            stopMocking()
            Toast.makeText(this, "Служба остановлена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMocking(lat: Double, lng: Double) {
        stopMocking()
        try {
            locationManager.addTestProvider(providerName, false, false, false, false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_FINE)
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Выберите приложение в меню разработчика!", Toast.LENGTH_LONG).show()
            return
        } catch (e: Exception) {}

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val loc = Location(providerName).apply {
                    latitude = lat
                    longitude = lng
                    altitude = 160.0
                    time = System.currentTimeMillis()
                    accuracy = 2.5f
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                try { locationManager.setTestProviderLocation(providerName, loc) } catch (e: Exception) {}
            }
        }, 0, 1000)
    }

    private fun stopMocking() {
        timer?.cancel()
        timer = null
        try { locationManager.removeTestProvider(providerName) } catch (e: Exception) {}
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopMocking()
    }
}