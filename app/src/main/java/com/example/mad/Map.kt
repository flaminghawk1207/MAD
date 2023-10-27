package com.example.mad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.mad.utils.LocationPermissionHelper
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.ref.WeakReference

// From : https://github.com/mapbox/mapbox-maps-android/blob/v10.16.0/app/src/main/java/com/mapbox/maps/testapp/examples/LocationTrackingActivity.kt

private val PERMISSION_REQUEST_CODE = 123
class Map : AppCompatActivity() {
    private lateinit var microphone: Microphone
    private val placeNames = ArrayList<String>()
    private val coordinates = ArrayList<String>()
    private var currentCoordinate = String()

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val latitude = point.latitude()
        val longitude = point.longitude()

        currentCoordinate = "$latitude,$longitude".toString()

        Log.d("Location", "Latitude: $latitude, Longitude: $longitude")

        // Update camera position
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(point)
    }


    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        }
        mapView = findViewById(R.id.mapView)


        // Setup the microphone
        microphone = Microphone(this)

        // Retrieve the destination location from the Intent
        val destinationLocation = intent.getStringExtra("destinationLocation")

        // Send a API Request to the Geocoding API of mapbox to get all matching locations.
        // Setup request queue for volley (reference code : https://google.github.io/volley/simple.html).
        var queue = Volley.newRequestQueue(this)

        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/" + destinationLocation + ".json?proximity=ip&access_token=" + getString(R.string.mapbox_access_token);
        // Request a string response from the provided URL.
        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val featureCollection = response.getJSONArray("features")
                for (i in 0 until featureCollection.length()) {
                    val feature = featureCollection.getJSONObject(i)
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    val latitude = coordinates.getDouble(1)
                    val longitude = coordinates.getDouble(0)
                    val coordinateString = "$latitude,$longitude"
                    val placeName = feature.getString("place_name");
                    placeNames.add(placeName)
                    this.coordinates.add(coordinateString)
                }

                // Speak these to the user. The user response will be an index.
                MainActivity.speaker.speakOut("Please say the index of the correct destination, starting from index 0")

                var i = 0
                // For now, only speak the first 2 locations.
                for (place in placeNames) {
                    val index = i.toString()
                    if (i == 2) break;
                    MainActivity.speaker.speakOut("Index $index")
                    MainActivity.speaker.speakOut(place)
                    i++
                }


                // After speaking all locations, start speech recognition
                microphone.startSpeechRecognition("Please tell the index")
            },
            { error ->
                // Handle error
            }
        )

        queue.add(jsonRequest)

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MainActivity.MICROPHONE_REQUEST_CODE) {
            val choiceResult = microphone.handleSpeechRecognitionResult(requestCode, resultCode, data)

            if (choiceResult != null) {
                if (choiceResult.equals("Repeat", ignoreCase = true)) {
                    MainActivity.speaker.speakOut("Please say the index of the correct destination, starting from index 0")
                    var i = 0
                    for (place in placeNames) {
                        MainActivity.speaker.speakOut("Index " + i.toString())
                        MainActivity.speaker.speakOut(place)
                        i = i + 1;
                    }

                    microphone.startSpeechRecognition("Please tell the index")
                    return
                }

                val numericWords = arrayOf("zero", "one", "two", "three", "four", "five")
                val index = numericWords.indexOf(choiceResult.split(" ")[1].lowercase())

                val numericStrings = arrayOf("0", "1", "2", "3", "4", "5")
                val index_2 = numericStrings.indexOf(choiceResult.split(" ")[1].lowercase())

                if (index != null && index >= 0 && index < placeNames.size) {
                    val chosenPlace = placeNames[index]

                    MainActivity.speaker.speakOut("You chose : " + chosenPlace)

                    // Start navigation intent.
                    val i = Intent(this, Navigation::class.java)
                    i.putExtra("CurrentCoords", currentCoordinate)
                    i.putExtra("DestinationCoords", coordinates[index])

                    startActivity(i)
                } else if (index_2 != null && index_2 >= 0 && index_2 < placeNames.size){
                        val chosenPlace = placeNames[index_2]

                        MainActivity.speaker.speakOut("You chose : " + chosenPlace)

                        // Start navigation intent.
                        val i = Intent(this, Navigation::class.java)
                        i.putExtra("CurrentCoords", currentCoordinate)
                        i.putExtra("DestinationCoords", coordinates[index_2])

                        startActivity(i)
                }
                else {
                    microphone.startSpeechRecognition("Please tell the index")
                }
            } else {
                microphone.startSpeechRecognition("Please tell the index")

            }
        }
    }


    private fun onMapReady() {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            initLocationComponent()
            setupGesturesListener()
        }
    }

    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location

        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@Map,
                    R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    this@Map,
                    R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }

    private fun onCameraTrackingDismissed() {
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}