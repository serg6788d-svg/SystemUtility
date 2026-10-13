package com.sysservice.location

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var etCoordinates: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnFreeze: Button
    private lateinit var btnOpenDev: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCoordinates = findViewById(R.id.etCoordinates)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnFreeze = findViewById(R.id.btnFreeze)
        btnOpenDev = findViewById(R.id.btnOpenDev)
        tvStatus = findViewById(R.id.tvStatus)

        updateUIState(LocationService.isServiceRunning)

        btnStart.setOnClickListener {
            val (lat, lon) = parseCoords() ?: return@setOnClickListener
            val serviceIntent = Intent(this, LocationService::class.java).apply {
                putExtra(LocationService.EXTRA_LAT, lat)
                putExtra(LocationService.EXTRA_LON, lon)
                putExtra(LocationService.EXTRA_MODE, LocationService.MODE_CONTINUOUS)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            updateUIState(true)
            Toast.makeText(this, "Постоянная подмена включена!", Toast.LENGTH_SHORT).show()
        }

        btnFreeze.setOnClickListener {
            val (lat, lon) = parseCoords() ?: return@setOnClickListener
            val serviceIntent = Intent(this, LocationService::class.java).apply {
                putExtra(LocationService.EXTRA_LAT, lat)
                putExtra(LocationService.EXTRA_LON, lon)
                putExtra(LocationService.EXTRA_MODE, LocationService.MODE_FREEZE)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Точка в кэше! Теперь нажмите кнопку ниже и выберите 'Ничего'.", Toast.LENGTH_LONG).show()
        }

        btnOpenDev.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть настройки разработчика", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)
            updateUIState(false)
            Toast.makeText(this, "Служба остановлена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseCoords(): Pair<Double, Double>? {
        val input = etCoordinates.text.toString().trim()
        val parts = input.split(",")
        if (parts.size == 2) {
            val lat = parts[0].trim().toDoubleOrNull()
            val lon = parts[1].trim().toDoubleOrNull()
            if (lat != null && lon != null) {
                return Pair(lat, lon)
            }
        }
        Toast.makeText(this, "Формат: 55.654885, 37.828655", Toast.LENGTH_LONG).show()
        return null
    }

    override fun onResume() {
        super.onResume()
        updateUIState(LocationService.isServiceRunning)
    }

    private fun updateUIState(isRunning: Boolean) {
        btnStart.isEnabled = !isRunning
        etCoordinates.isEnabled = !isRunning
        
        if (isRunning) {
            tvStatus.text = "Статус: Постоянная подмена активна"
            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tvStatus.text = "Статус: Остановлено"
            tvStatus.setTextColor(Color.parseColor("#C62828"))
        }
    }
}
