package com.example.tcpclientapp

import android.app.*
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.Socket

class LocationService : Service() {

    private fun generateFormattedLocationLine(lat: Double, lon: Double): String {
        val imei = "866758041740438"
        val gpsStatus = "A"
        val ignStatus = "IGNOFF"
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"

        val absLat = String.format("%.2f", kotlin.math.abs(lat) * 100)
        val absLon = String.format("%.2f", kotlin.math.abs(lon) * 100)

        return "&PEIS,N,VTS,LP,IPC_MTC_v1.23_c,$imei,$ignStatus,0," +
                "$time,$date,$gpsStatus,$absLat,$latDir,$absLon,$lonDir," +
                "0,31,303UP,0.00,1.07,1,1,1,1,0"
    }

    // 🔁 List of 3 servers (replace IPs and ports with actual values)
    private val publicIP = "49.207.186.71"
    private val portList = listOf(2456 , 1345, 1443)


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        var onNewLocation: ((String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val channelId = "location_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

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
                    sendLocationToAllServers(lat, lon)

                }
            }
        }

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        }
    }

    private fun sendLocationToAllServers(lat: Double, lon: Double) {
        val message = generateFormattedLocationLine(lat, lon)

        portList.forEach { port ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val socket = Socket(publicIP, port)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(message)
                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 👇 This updates the UI log
        onNewLocation?.invoke(message)
    }



        override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
