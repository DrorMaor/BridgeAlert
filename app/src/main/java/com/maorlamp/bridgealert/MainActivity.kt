package com.maorlamp.bridgealert

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlin.collections.mutableListOf

class MainActivity : AppCompatActivity() {

    val AllBridges = mutableListOf<Bridge>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadBridges()
        findBridges(40.8468129197731, -73.93602927417002)

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
        }
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

