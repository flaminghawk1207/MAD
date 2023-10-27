package com.example.mad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi

class VibrationMotor(context: Context) {
    private lateinit var vibrator: Vibrator

    init {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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