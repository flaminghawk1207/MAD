package com.example.mad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
class VibrationMotor(context: Context) {
    private lateinit var vibratorManager: VibratorManager
    private lateinit var vibrator: Vibrator

    init {
        vibratorManager = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
    }

    fun vibrateSingle() {
        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrator.vibrate(effect)
    }

    fun vibrateDouble() {
        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        vibrator.vibrate(effect)
    }
}