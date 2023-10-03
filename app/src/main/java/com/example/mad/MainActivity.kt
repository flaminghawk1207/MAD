package com.example.mad

// Map (the mapbox abstraction) has name conflict with Map data type, so be sure to call the correct one
// (Especially when using intents)
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var vibrationMotor: VibrationMotor

    private lateinit var microphoneButton: Button
    private lateinit var mapButton: Button

    private lateinit var leftButton: Button
    private lateinit var rightButton: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vibrationMotor = VibrationMotor(this)

        microphoneButton = findViewById(R.id.microphone_button)

        mapButton = findViewById(R.id.map_button)

        mapButton.setOnClickListener {
            var intent = Intent(baseContext, Map::class.java)
            this.startActivity(intent)
        }

        leftButton = findViewById(R.id.left_button)
        rightButton = findViewById(R.id.right_button)

        leftButton.setOnClickListener {
            vibrationMotor.vibrateSingle()
        }

        rightButton.setOnClickListener {
            vibrationMotor.vibrateDouble()
        }

        microphoneButton.setOnClickListener {
            var microphoneIntent = Intent(this, Microphone::class.java)
            this.startActivity(microphoneIntent)
        }
    }
}