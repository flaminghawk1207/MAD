package com.example.mad

// Primary reference : https://github.com/mapbox/mapbox-navigation-android-examples/blob/main/app/src/main/java/com/mapbox/navigation/examples/standalone/voice/PlayVoiceInstructionsActivity.kt.

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class Navigation : AppCompatActivity() {

    private var currentLat: Double = 0.0
    private var currentLong: Double = 0.0

    private lateinit var route: NavigationRoute
    private val mapboxReplayer = MapboxReplayer()
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var speechApi: MapboxSpeechApi
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            speechApi.clean(value)
        }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                enhancedLocation,
                locationMatcherResult.keyPoints,
            )

            updateCamera(
                Point.fromLngLat(
                    enhancedLocation.longitude,
                    enhancedLocation.latitude
                ),
                enhancedLocation.bearing.toDouble()
            )
        }
    }

    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
        routeLineApi.setNavigationRoutes(
            routeUpdateResult.navigationRoutes
        ) { value ->

            findViewById<MapView>(R.id.mapView).getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesObserver(routesObserver)
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
                mapboxNavigation.startTripSession()
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            }
        },
        onInitialize = this::initNavigation
    )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_mapbox_voice)

        val currentCoordsString = intent.getStringExtra("CurrentCoords")!!.split(",")
        val destinationCoordsString = intent.getStringExtra("DestinationCoords")!!.split(",")

        // Setup request queue for volley.
        val queue = Volley.newRequestQueue(this)

        val currentLat = currentCoordsString[1]
        val currentLong = currentCoordsString[0]
        val destLat = destinationCoordsString[1]
        val destLong = destinationCoordsString[0]

        this.currentLat = currentLat.toDouble()
        this.currentLong = currentLong.toDouble()


        val url = "https://api.mapbox.com/directions/v5/mapbox/walking/$currentLat%2C$currentLong%3B$destLat%2C$destLong?alternatives=true&annotations=distance%2Cduration&continue_straight=true&geometries=polyline6&language=en&overview=full&steps=true&voice_instructions=true&voice_units=imperial&access_token=" + getString(R.string.mapbox_access_token)

        // Request a JSON response from the provided URL.
        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val jsonObject = response
                val routesArray = jsonObject.getJSONArray("routes")
                val firstRoute = routesArray.getJSONObject(0)

                val routeOptions = RouteOptions.builder()
                    .baseUrl("https://api.mapbox.com")
                    .profile(DirectionsCriteria.PROFILE_WALKING)
                    .coordinatesList(
                        listOf(
                            Point.fromLngLat(currentLong.toDouble(), currentLat.toDouble()),
                            Point.fromLngLat(destLong.toDouble(), destLat.toDouble())
                        )
                    )
                    .alternatives(false)
                    .continueStraight(true)
                    .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                    .language("en")
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .steps(true)
                    .build()

                val directionsResponse = DirectionsResponse.fromJson(response.toString());
                val route = directionsResponse.routes()[0]
                val directionsRoute = route.toBuilder().routeOptions(routeOptions).build()

                this.route = directionsRoute.toNavigationRoute(RouterOrigin.Custom())
            },
            { error ->
            }
        )

        queue.add(jsonRequest)

        findViewById<MapView>(R.id.mapView).getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE) {
            findViewById<Button>(R.id.actionButton).visibility = View.VISIBLE
            findViewById<Button>(R.id.actionButton).setOnClickListener {
                mapboxNavigation.setNavigationRoutes(listOf(this.route))
            }
        }

        speechApi = MapboxSpeechApi(
            this,
            getString(R.string.mapbox_access_token),
            Locale.US.toLanguageTag()
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            getString(R.string.mapbox_access_token),
            Locale.US.toLanguageTag()
        )


    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxReplayer.finish()
        speechApi.cancel()
        routeLineView.cancel()
        routeLineApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                // If this line is removed, then the simulation will STOP and the users actual location will be taken into account.
                .locationEngine(replayLocationEngine)
                .build()
        )


        findViewById<MapView>(R.id.mapView).location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        replayOriginLocation()
    }

    private fun replayOriginLocation() {
        // note(rtarun9) : Lat and Long are interchanged in a lot of places, must be changed.
        mapboxReplayer.pushEvents(
            listOf(
                ReplayRouteMapper.mapToUpdateLocation(
                    Date().time.toDouble(),
                    Point.fromLngLat(currentLat, currentLong)
                )
            )
        )
        mapboxReplayer.playFirstLocation()
        mapboxReplayer.playbackSpeed(30.0)
    }

    private fun updateCamera(point: Point, bearing: Double? = null) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()

        findViewById<MapView>(R.id.mapView).camera.easeTo(
            CameraOptions.Builder()
                .center(point).zoom(17.0)
                .bearing(bearing)
                .pitch(45.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptions
        )
    }
}