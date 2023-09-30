package com.example.mad

import android.os.Build
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.annotation.RequiresApi
import com.example.mad.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var vibrationMotor: VibrationMotor

    private lateinit var leftButton: Button
    private lateinit var rightButton: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vibrationMotor = VibrationMotor(this)

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