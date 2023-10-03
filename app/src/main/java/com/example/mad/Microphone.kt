package com.example.mad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

// Class that can be used to get user input (via speech) to text format.
// Tutorial followed : https://www.youtube.com/watch?v=a7-Z9awsods

// NOTE : For now this class has UI and stuff for testing & visualization purposes,
// probably when the entire app is being assembled this class and UI will be kept separate.
class Microphone : Activity() {

    private val SPEECH_RECORD_REQUEST_CODE: Int = 102
    public var text: String? = null;

    lateinit private var button: Button
    lateinit private var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_microphone)

        button = findViewById(R.id.speech_capture_button)
        textView = findViewById(R.id.text_result_view)

        button.setOnClickListener {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            }

            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please tell your destination location")
            startActivityForResult(i, SPEECH_RECORD_REQUEST_CODE, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_RECORD_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (results != null && results.isNotEmpty()) {
                val recognizedText = results[0]
                text = recognizedText;
            }
            else {
                text = null;
            }

            textView.text = text;
        }
    }
}