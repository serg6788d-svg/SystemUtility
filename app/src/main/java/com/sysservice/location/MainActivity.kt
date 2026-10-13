package com.sysservice.location

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
    private lateinit var btnFreeze: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCoordinates = findViewById(R.id.etCoordinates)
        btnStart = findViewById(R.id.btnStart)
        btnFreeze = findViewById(R.id.btnFreeze)
        btnStop = findViewById(R.id.btnStop)
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
            Toast.makeText(this, "Постоянная подмена запущена!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Точка успешно заморожена в кэше системы!", Toast.LENGTH_LONG).show()
        }

        btnStop.setOnClickListener {
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)
            updateUIState(false)
            Toast.makeText(this, "Подмена остановлена", Toast.LENGTH_SHORT).show()
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
        btnStop.isEnabled = isRunning
        etCoordinates.isEnabled = !isRunning

        if (isRunning) {
            tvStatus.text = "Статус: Постоянная подмена активна"
            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tvStatus.text = "Статус: Остановлено / Кэш готов"
            tvStatus.setTextColor(Color.parseColor("#1976D2"))
        }
    }
}
