package com.example.mad

import Speaker
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var destinationLocation: String? = null
    private var confirmationRequested: Boolean = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Initialize core components.
        micrphone = Microphone(this)
        speaker = Speaker(this)
        vibrationMotor = VibrationMotor(this)

        val application_begin_button = findViewById<Button>(R.id.fullscreen_start_button)
        application_begin_button.setOnClickListener {
            speaker.speakOut("Please enter the destination location")
            micrphone.startSpeechRecognition("Please enter the destination location")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MICROPHONE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                if (!confirmationRequested) {
                    destinationLocation = micrphone.handleSpeechRecognitionResult(requestCode, resultCode, data)
                    if (destinationLocation != null) {

                        speaker.speakOut("Your desired destination is " + destinationLocation!! + ". Is this correct?")

                        micrphone.startSpeechRecognition("Please say 'yes' or 'no' for confirmation")
                        confirmationRequested = true
                    }
                } else {
                    val confirmationResult = micrphone.handleSpeechRecognitionResult(requestCode, resultCode, data)

                    if (confirmationResult != null) {
                        if (confirmationResult.equals("yes", ignoreCase = true)) {
                            speaker.speakOut("Thank you.")
                            // Go to the map activity.
                            val i = Intent(this, Map::class.java)
                            i.putExtra("destinationLocation",destinationLocation)
                            startActivity(i)
                        } else  {
                            speaker.speakOut("I'm sorry. Please enter the correct destination location.")
                            micrphone.startSpeechRecognition("Please enter the destination location")
                            confirmationRequested = false
                        }
                    }
                }
            }
        }
    }

    companion object {
        public const val MICROPHONE_REQUEST_CODE = 102
        lateinit var micrphone: Microphone
        lateinit var speaker: Speaker
        lateinit var vibrationMotor: VibrationMotor
    }
}
