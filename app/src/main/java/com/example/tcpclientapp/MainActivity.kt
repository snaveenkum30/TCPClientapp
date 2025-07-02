package com.example.tcpclientapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var locationServiceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationServiceIntent = Intent(this, LocationService::class.java)

        setContent {
            var statusText by remember { mutableStateOf("Service not running") }
            var locationLog by remember { mutableStateOf("") }
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()

            // Update UI when new location is received
            DisposableEffect(Unit) {
                LocationService.onNewLocation = { message ->
                    locationLog += "$message\n"
                }
                onDispose {
                    LocationService.onNewLocation = null
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        "\ud83d\udcf1 TCP Location Sender",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // \u25b6 Start Button
                    Button(onClick = {
                        if (
                            ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ),
                                100
                            )
                        } else {
                            startService(locationServiceIntent)
                            statusText = "Service Started \u2705"
                        }
                    }) {
                        Text("\u25b6 Start")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // \u26d4 Stop Button
                    Button(onClick = {
                        stopService(locationServiceIntent)
                        statusText = "Service Stopped \u274c"
                    }) {
                        Text("\u26d4 Stop")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // \ud83d\udd04 Refresh Button
                    Button(onClick = {
                        locationLog = ""
                        statusText = "Log cleared \ud83e\uddf9"
                    }) {
                        Text("\ud83d\udd04 Refresh Log")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // \u2b07 Download Button
                    Button(onClick = {
                        saveCSVWithMediaStore(locationLog)
                    }) {
                        Text("\u2b07 Download as Excel")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = statusText)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("\ud83d\udccd Sent Location Data:")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .verticalScroll(scrollState)
                            .padding(8.dp)
                            .border(1.dp, Color.Gray)
                    ) {
                        Text(locationLog)
                    }
                }
            }
        }
    }

    private fun saveCSVWithMediaStore(logData: String) {
        val csvContent = buildString {
            append("Latitude,Longitude\n")
            logData.lines().forEach { line ->
                val parts = line.split(",")
                if (parts.size > 13) {
                    val lat = parts[11]
                    val lon = parts[13]
                    append("$lat,$lon\n")
                }
            }
        }

        val resolver = applicationContext.contentResolver
        val filename = "location_log_${System.currentTimeMillis()}.csv"
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = resolver.insert(collectionUri, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri).use { outputStream ->
                outputStream?.write(csvContent.toByteArray())
                Toast.makeText(this, "CSV saved to Downloads ✅", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to create file ❌", Toast.LENGTH_SHORT).show()
        }
    }
}