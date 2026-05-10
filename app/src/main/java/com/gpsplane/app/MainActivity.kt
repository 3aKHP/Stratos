package com.gpsplane.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gpsplane.app.data.FlightTimer
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.service.GpsTrackingService
import com.gpsplane.app.service.rememberBoundService
import com.gpsplane.app.ui.screen.DownloadScreen
import com.gpsplane.app.ui.screen.GpsScreen
import com.gpsplane.app.ui.screen.MapScreen
import com.gpsplane.app.ui.theme.GpsPlaneTheme

class MainActivity : ComponentActivity() {

    private var hasLocationPermission = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            hasLocationPermission = results.entries
                .filter {
                    it.key == Manifest.permission.ACCESS_FINE_LOCATION ||
                    it.key == Manifest.permission.ACCESS_COARSE_LOCATION
                }
                .all { it.value }
            if (hasLocationPermission) {
                GpsTrackingService.start(this)
                recreate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // One-time cleanup of 0.1.1's tile cache location (filesDir/osmdroid-v2).
        // 0.1.2 moved the cache to noBackupFilesDir — any leftover tiles in the
        // old path are unreachable, waste space, and count against Auto Backup.
        filesDir.resolve("osmdroid-v2").takeIf { it.exists() }?.deleteRecursively()

        hasLocationPermission = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasLocationPermission) GpsTrackingService.start(this)

        setContent {
            GpsPlaneTheme {
                MainScreen(
                    hasPermission = hasLocationPermission,
                    onRequestPermission = { requestPermissions() }
                )
            }
        }
    }

    private fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(perms)
    }
}

@Composable
fun MainScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }

    val service by rememberBoundService()

    val gpsData = service?.gps?.collectAsState()?.value ?: GpsData.EMPTY
    val attData = service?.attitude?.collectAsState()?.value ?: AttitudeData.EMPTY
    val envData = service?.environment?.collectAsState()?.value ?: EnvironmentData.EMPTY
    val flightSnap = service?.flight?.collectAsState()?.value ?: FlightTimer.Snapshot.INITIAL
    val declinationDeg = service?.declinationDeg?.collectAsState()?.value ?: 0f

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text("Map") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    label = { Text("Preload") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (!hasPermission) {
                PermissionPrompt(onRequestPermission)
            } else {
                when (selectedTab) {
                    0 -> GpsScreen(gpsData, attData, envData, flightSnap, declinationDeg)
                    1 -> MapScreen(gpsData)
                    2 -> DownloadScreen(gpsData)
                }
            }
        }
    }
}

@Composable
fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Location Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "This app needs GPS access to show flight data.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onRequest, modifier = Modifier.padding(top = 24.dp)) {
            Text("Grant Permission")
        }
    }
}
