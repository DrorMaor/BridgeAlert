package com.maorlamp.bridgealert


import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper()) // Use main looper for UI updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    val AllBridges = mutableListOf<Bridge>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // do location permission
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }

        // do TextToSpeech permission
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.i("TTS", "TextToSpeech YAY")
            } else {
                Log.e("TTS", "TextToSpeech initialization failed with status: $status")
            }
        }

        startTimer()

        loadBridges()
    }

    private fun startTimer() {
        handler.postDelayed(timerRunnable, 60000)
    }

    private val timerRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            getLocationUpdates()
            handler.postDelayed(this, 60000)
        }
    }

    private fun getLocationUpdates() {
        val tvBridge: TextView = findViewById(R.id.tvBridge)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                findBridges(latitude, longitude)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startLocationUpdates()
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 100
    }

    fun findBridges(Lat: Double, Lng: Double) {
        val geoFence = getGeoFence(Lat, Lng)
        val bridgesFound = AllBridges.filter {
            (it.Lng >= geoFence.NW.Lng && it.Lat >= geoFence.NW.Lat)  // NW
                    &&
                    (it.Lng <= geoFence.NE.Lng && it.Lat >= geoFence.NE.Lat)  // NE
                    &&
                    (it.Lng <= geoFence.SE.Lng && it.Lat <= geoFence.SE.Lat)  // SE
                    &&
                    (it.Lng >= geoFence.SW.Lng && it.Lat <= geoFence.SW.Lat)  // SW
        }.take(5)
        soundBridges(bridgesFound)
    }

    fun soundBridges(bridges: List<Bridge>) {
        var bridgeText = ""
        for (bridge in bridges) {
            bridgeText = "You are nearby " + bridge.Road + " @ " + bridge.Pass + ". \n"
            bridgeText += "Lowest rating is " + bridge.LowRate.toString() + " out of 9 \n \n"
        }
        val tvBridge: TextView = findViewById(R.id.tvBridge)
        tvBridge.text = bridgeText
        textToSpeech.speak(bridgeText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun getGeoFence(Lat: Double, Lng: Double): GeoFence {
        val mile = 1.0 / 69.0
        val geoFence = GeoFence()
        geoFence.NW.Lat = Lat - mile
        geoFence.NW.Lng = Lng - mile
        geoFence.NE.Lat = Lat - mile
        geoFence.NE.Lng = Lng + mile
        geoFence.SE.Lat = Lat + mile
        geoFence.SE.Lng = Lng + mile
        geoFence.SW.Lat = Lat + mile
        geoFence.SW.Lng = Lng - mile
        return geoFence
    }

    fun loadBridges() {
        val dirName = "gwr"
        val assetManager = assets
        val assetNames = assetManager.list(dirName)
        if (assetNames != null) {
            for (file in assetNames) {
                val inputStream = assetManager.open("$dirName/$file")
                val text = inputStream.bufferedReader().use { it.readText() }
                val textSplit = text.split("\n")
                for (line in textSplit) {
                    val lineSplit = line.toString().split(",")
                    if (lineSplit.count() == 5) {
                        val bridge: Bridge = Bridge()
                        bridge.Pass = lineSplit[0]
                        bridge.Road = lineSplit[1]
                        bridge.Lat = lineSplit[2].toDouble()
                        bridge.Lng = lineSplit[3].toDouble()
                        bridge.LowRate = lineSplit[4].toInt()
                        AllBridges.add(bridge)
                    }
                }
            }
        }
    }
}

