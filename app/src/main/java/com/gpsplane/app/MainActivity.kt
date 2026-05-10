package com.gpsplane.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.gpsplane.app.data.AttitudeRepository
import com.gpsplane.app.data.EnvironmentRepository
import com.gpsplane.app.data.FlightTimer
import com.gpsplane.app.data.GpsRepository
import com.gpsplane.app.data.MagneticDeclination
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.ui.screen.DownloadScreen
import com.gpsplane.app.ui.screen.GpsScreen
import com.gpsplane.app.ui.screen.MapScreen
import com.gpsplane.app.ui.theme.GpsPlaneTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {

    private var hasLocationPermission = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            hasLocationPermission = results.values.all { it }
            if (hasLocationPermission) recreate()
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

        setContent {
            GpsPlaneTheme {
                MainScreen(
                    hasPermission = hasLocationPermission,
                    gpsRepository = if (hasLocationPermission) GpsRepository(this) else null,
                    attitudeRepository = if (hasLocationPermission) AttitudeRepository(this) else null,
                    environmentRepository = if (hasLocationPermission) EnvironmentRepository(this) else null,
                    onRequestPermission = { requestPermissions() }
                )
            }
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

@Composable
fun MainScreen(
    hasPermission: Boolean,
    gpsRepository: GpsRepository?,
    attitudeRepository: AttitudeRepository?,
    environmentRepository: EnvironmentRepository?,
    onRequestPermission: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var gpsData by remember { mutableStateOf(GpsData.EMPTY) }
    var attData by remember { mutableStateOf(AttitudeData.EMPTY) }
    var envData by remember { mutableStateOf(EnvironmentData.EMPTY) }
    var flightSnap by remember { mutableStateOf(FlightTimer.Snapshot.INITIAL) }
    var declinationDeg by remember { mutableStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Flows are bound to the Activity's STARTED state so GPS / sensors stop
    // when the user backgrounds the app and resume automatically on return.
    LaunchedEffect(hasPermission, gpsRepository, attitudeRepository, environmentRepository, lifecycle) {
        if (hasPermission && gpsRepository != null && attitudeRepository != null && environmentRepository != null) {
            combine(
                gpsRepository.observeLocation(),
                attitudeRepository.observeAttitude(),
                environmentRepository.observeEnvironment(),
            ) { gps, att, env -> Triple(gps, att, env) }
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .catch { e -> Log.w("MainScreen", "Data flow error", e) }
                .collect { (gps, att, env) ->
                    gpsData = gps
                    attData = att
                    envData = env
                    if (gps.hasFix) {
                        flightSnap = FlightTimer.update(
                            flightSnap, gps.speedMps, gps.altitudeMeters, gps.timestampMs
                        )
                        declinationDeg = MagneticDeclination.degreesEast(
                            gps.latitude, gps.longitude, gps.altitudeMeters, gps.timestampMs
                        )
                    }
                }
        }
    }

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
