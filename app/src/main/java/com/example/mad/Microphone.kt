package com.example.mad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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

        // NOTE : Only for testing purposes of geocoding API in the emulator where microphone access is not present.
        // Remove from here once testing is done.
        // Setup request queue for volley (reference code : https://google.github.io/volley/simple.html).
        var queue = Volley.newRequestQueue(this)

        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/" + "Brookfields Mall coimbatore" + ".json?proximity=ip&access_token=pk.eyJ1IjoidGFydW5yIiwiYSI6ImNsbjZ4NzR6cjBsejMyaWp0b2g4bnc4eHQifQ.laPj-90SZU3jpT_1bdGPcA";
        // Request a string response from the provided URL.
        val jsonRequest = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                val featureCollection = response.getJSONArray("features")
                val placeNames = ArrayList<String>()
                for (i in 0 until featureCollection.length()) {
                    val feature = featureCollection.getJSONObject(i)
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    val latitude = coordinates.getDouble(1)
                    val longitude = coordinates.getDouble(0)
                    val coordinateString = "Latitude: $latitude, Longitude: $longitude"
                    val placeName = feature.getString("place_name") + coordinateString;
                    placeNames.add(placeName)
                }

                val placeNamesText = placeNames.joinToString("\n")
                textView.text = placeNamesText
            },
            Response.ErrorListener { error ->
                // Handle error
            }
        )

        queue.add(jsonRequest)
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


        // Once text has been updated (if not null), send a request to the mapbox geocoding api.
        if (textView.text != "") {
            // Setup request queue for volley (reference code : https://google.github.io/volley/simple.html).
            var queue = Volley.newRequestQueue(this)

            val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/" + textView.text.toString() + ".json?proximity=ip&access_token=pk.eyJ1IjoidGFydW5yIiwiYSI6ImNsbjZ4NzR6cjBsejMyaWp0b2g4bnc4eHQifQ.laPj-90SZU3jpT_1bdGPcA";
            // Request a string response from the provided URL.
            val jsonRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    val featureCollection = response.getJSONArray("features")
                    val placeNames = ArrayList<String>()
                    for (i in 0 until featureCollection.length()) {
                        val feature = featureCollection.getJSONObject(i)
                        val geometry = feature.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val latitude = coordinates.getDouble(1)
                        val longitude = coordinates.getDouble(0)
                        val coordinateString = "Latitude: $latitude, Longitude: $longitude"
                        val placeName = feature.getString("place_name") + coordinateString;
                        placeNames.add(placeName)
                    }

                    val placeNamesText = placeNames.joinToString("\n")
                    textView.text = placeNamesText
                },
                Response.ErrorListener { error ->
                    // Handle error
                }
            )

            queue.add(jsonRequest)


        }
    }
}