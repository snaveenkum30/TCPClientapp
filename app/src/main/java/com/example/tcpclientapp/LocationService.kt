package com.example.tcpclientapp

import android.app.*
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.Socket

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val SERVER_IP = "49.207.186.71"
    private val SERVER_PORT = 1345

    companion object {
        var onNewLocation: ((String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val channelId = "location_channel"
        val channel = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Location Sending")
            .setContentText("Sending location to server...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        ).setMinUpdateIntervalMillis(10_000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val lat = loc.latitude
                    val lon = loc.longitude
                    sendLocationToServer(lat, lon)
                }
            }
        }

         fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        val message = "LAT:$lat, LON:$lon"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(SERVER_IP, SERVER_PORT)
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println(message)
                socket.close()
            } catch (e: Exception) {
                // Optionally log error
            }

            // Show sent message in MainActivity
            withContext(Dispatchers.Main) {
                onNewLocation?.invoke(message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
