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
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCoordinates = findViewById(R.id.etCoordinates)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)

        updateUIState(false)

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
