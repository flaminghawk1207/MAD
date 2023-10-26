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

        // Setup request queue for volley (reference code : https://google.github.io/volley/simple.html).
        var queue = Volley.newRequestQueue(this)

        val url = "https://api.mapbox.com/directions/v5/mapbox/walking/$currentCoordsString[0]%2C$currentCoordsString[1]%3B$destinationCoordsString[0]%2C$destinationCoordsString[1]?alternatives=true&continue_straight=true&geometries=geojson&language=en&overview=full&steps=true&access_token=" + getString(R.string.mapbox_access_token)

        // Request a string response from the provided URL.
        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val routes = response.getJSONArray("routes")
                val route = routes.getJSONObject(0)

                val weightName = route.getString("weight_name")
                val duration = route.getDouble("duration")
                val distance = route.getDouble("distance")

                val steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps")

                var directionsString = "Weight Name: $weightName\nDuration: $duration\nDistance: $distance\n\nDirections:\n"

                for (i in 0 until steps.length()) {
                    val step = steps.getJSONObject(i)
                    val name = step.getString("name")
                    val stepDuration = step.getDouble("duration")
                    val stepDistance = step.getDouble("distance")

                    directionsString += "Step $i: Name: $name, Duration: $stepDuration, Distance: $stepDistance\n"
                }

                val directions = findViewById<TextView>(R.id.directions)
                directions.text = directionsString
            },
            { error ->
                // Handle error
            }
        )

        queue.add(jsonRequest)
    }
}
