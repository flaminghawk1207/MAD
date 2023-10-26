package com.example.mad

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class Navigation : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_navigation)
        val currentCoordsString = intent.getStringExtra("CurrentCoords")!!.split(",")
        val destinationCoordsString = intent.getStringExtra("DestinationCoords")!!.split(",")

        // Setup request queue for volley.
        val queue = Volley.newRequestQueue(this)

        val currentLat = currentCoordsString[1]
        val currentLong = currentCoordsString[0]

        val destLat = destinationCoordsString[1]
        val destLong = destinationCoordsString[0]

        val url = "https://api.mapbox.com/directions/v5/mapbox/walking/$currentLat%2C$currentLong%3B$destLat%2C$destLong?alternatives=true&continue_straight=true&geometries=geojson&language=en&overview=full&steps=true&access_token=" + getString(R.string.mapbox_access_token)

        // Request a JSON response from the provided URL.
        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val routes = response.getJSONArray("routes")
                val route = routes.getJSONObject(0)

                val steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps")

                val directionsList = ArrayList<String>()

                for (i in 0 until steps.length()) {
                    val step = steps.getJSONObject(i)
                    val maneuver = step.getJSONObject("maneuver")
                    val instruction = maneuver.getString("instruction")
                    directionsList.add(instruction)
                }

                displayDirections(directionsList)
            },
            { error ->
                // Handle error
            }
        )

        queue.add(jsonRequest)
    }

    private fun displayDirections(directionsList: List<String>) {
        val directionsTextView = findViewById<TextView>(R.id.directions)
        val directionsText = directionsList.joinToString("\n")
        directionsTextView.text = directionsText
    }
}
