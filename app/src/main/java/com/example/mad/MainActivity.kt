package com.example.mad

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var vibrationMotor: VibrationMotor

    private lateinit var leftButton: Button
    private lateinit var rightButton: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vibrationMotor = VibrationMotor(this)
        val intent = Intent(this, Map::class.java)
        startActivity(intent)

        leftButton = findViewById(R.id.left_button)
        rightButton = findViewById(R.id.right_button)

        leftButton.setOnClickListener {
            vibrationMotor.vibrateSingle()
        }

        rightButton.setOnClickListener {
            vibrationMotor.vibrateDouble()
        }
    }
}