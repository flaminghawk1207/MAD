package com.example.mad

// Class that can be used to get user input (via speech) to text format.
// Tutorial followed : https://www.youtube.com/watch?v=a7-Z9awsods

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast

class Microphone(private val activity: Activity) {
    fun startSpeechRecognition(prompt: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500000) // 50 seconds
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500000) // 50 seconds
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)

        activity.startActivityForResult(intent, MainActivity.MICROPHONE_REQUEST_CODE)
    }

    // Call from the activity's onActivityResult function.
    // Returns null  if speech could not be converted to text.
    fun handleSpeechRecognitionResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == MainActivity.MICROPHONE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            return results?.get(0)
        } else {
            return null
        }
    }
}
