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
    private lateinit var btnOpenDev: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCoordinates = findViewById(R.id.etCoordinates)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnOpenDev = findViewById(R.id.btnOpenDev)
        tvStatus = findViewById(R.id.tvStatus)

        updateUIState(LocationService.isServiceRunning)

        btnStart.setOnClickListener {
            val input = etCoordinates.text.toString().trim()
            val lat: Double
            val lon: Double
            
            if (input.isNotEmpty()) {
                val parts = input.split(",")
                if (parts.size == 2) {
                    val parsedLat = parts[0].trim().toDoubleOrNull()
                    val parsedLon = parts[1].trim().toDoubleOrNull()
                    if (parsedLat != null && parsedLon != null) {
                        lat = parsedLat
                        lon = parsedLon
                    } else {
                        Toast.makeText(this, "Неверный формат чисел", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } else {
                    Toast.makeText(this, "Формат: Широта, Долгота", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            } else {
                // Если поле пустое, берем дефолтные или предыдущие (либо просим ввести)
                lat = 55.755864
                lon = 37.617698
            }

            val serviceIntent = Intent(this, LocationService::class.java).apply {
                putExtra(LocationService.EXTRA_LAT, lat)
                putExtra(LocationService.EXTRA_LON, lon)
                putExtra(LocationService.EXTRA_MODE, LocationService.MODE_START)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            updateUIState(true)
            Toast.makeText(this, "Подмена запущена!", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            val input = etCoordinates.text.toString().trim()
            var intentLat: Double? = null
            var intentLon: Double? = null
            if (input.isNotEmpty()) {
                val parts = input.split(",")
                if (parts.size == 2) {
                    intentLat = parts[0].trim().toDoubleOrNull()
                    intentLon = parts[1].trim().toDoubleOrNull()
                }
            }

            val serviceIntent = Intent(this, LocationService::class.java).apply {
                putExtra(LocationService.EXTRA_MODE, LocationService.MODE_STOP)
                if (intentLat != null && intentLon != null) {
                    putExtra(LocationService.EXTRA_LAT, intentLat)
                    putExtra(LocationService.EXTRA_LON, intentLon)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            updateUIState(false)
            Toast.makeText(this, "Точка заморожена в кэше! Отключите мок в разработчиках.", Toast.LENGTH_LONG).show()
        }

        btnOpenDev.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть настройки разработчика", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState(LocationService.isServiceRunning)
    }

    private fun updateUIState(isRunning: Boolean) {
        if (isRunning) {
            tvStatus.text = "Статус: Подмена активна"
            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tvStatus.text = "Статус: Остановлено / Кэш готов"
            tvStatus.setTextColor(Color.parseColor("#1976D2"))
        }
    }
}
