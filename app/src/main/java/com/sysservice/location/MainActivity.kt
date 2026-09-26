package com.sysservice.location

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCoordinates = findViewById<EditText>(R.id.etCoordinates)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

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
                    Toast.makeText(this, "Подмена запущена в фоне!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Подмена остановлена", Toast.LENGTH_SHORT).show()
        }
    }
}
