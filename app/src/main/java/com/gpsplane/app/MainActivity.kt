package com.gpsplane.app

import android.Manifest
import android.content.Context
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gpsplane.app.data.FlightTimer
import com.gpsplane.app.data.GForceRange
import com.gpsplane.app.data.SunTimes
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.service.GpsTrackingService
import com.gpsplane.app.service.rememberBoundService
import com.gpsplane.app.ui.component.NotificationBanner
import com.gpsplane.app.ui.screen.DownloadScreen
import com.gpsplane.app.ui.screen.GpsScreen
import com.gpsplane.app.ui.screen.MapScreen
import com.gpsplane.app.ui.theme.GpsPlaneTheme

class MainActivity : ComponentActivity() {

    private var hasLocationPermission = false
    private var immersiveActive = false

    // Notification-permission state lives on the Activity because the
    // permission grant is an Activity-level concern (launcher + result
    // callback). Exposed to Compose via a MutableState so the banner
    // recomposes when the grant flips. Refreshed in onResume to catch
    // the user toggling the permission in system Settings. The initial
    // value is a placeholder — onCreate always refreshes before first
    // Compose read.
    private val notificationsGranted = mutableStateOf(false)
    private val bannerDismissed = mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // Snapshot the pre-grant location state so we only recreate
            // (and start the service) when location was actually just
            // granted. A notification-only grant (e.g. the banner's
            // "Grant" button, which launches with just POST_NOTIFICATIONS)
            // must NOT recreate — otherwise selectedTab resets and the
            // user is bounced off their current screen.
            val hadLocation = hasLocationPermission
            refreshPermissionState(results)
            if (!hadLocation && hasLocationPermission) {
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

        // Restore the user's "don't show the notification banner again"
        // choice. Kept in the Activity's private SharedPreferences so it
        // survives process death and config changes within an install
        // (clearing app data or uninstalling resets it — intended).
        bannerDismissed.value = getPreferences(Context.MODE_PRIVATE)
            .getBoolean(KEY_BANNER_DISMISSED, false)

        refreshPermissionState(null)

        if (hasLocationPermission) {
            GpsTrackingService.start(this)
            // Location may already be granted from a prior install, in which
            // case requestPermissions() never runs and POST_NOTIFICATIONS
            // stays un-prompted — the service then runs but the persistent
            // notification is silently suppressed on API 33+. Ask on its own.
            maybeRequestNotificationPermission()
        }

        setContent {
            GpsPlaneTheme {
                var immersive by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(immersive) {
                    immersiveActive = immersive
                    applyImmersive(immersive)
                }
                // Snapshot the Activity-level permission state so Compose
                // recomposes when notificationsGranted.value changes.
                val notifGranted by notificationsGranted
                val bannerGone by bannerDismissed
                MainScreen(
                    hasPermission = hasLocationPermission,
                    immersive = immersive,
                    onImmersiveChange = { immersive = it },
                    onRequestPermission = { requestPermissions() },
                    showNotificationBanner = hasLocationPermission &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notifGranted && !bannerGone,
                    onGrantNotifications = { requestNotificationPermission() },
                    onDismissNotificationBanner = {
                        bannerDismissed.value = true
                        getPreferences(Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_BANNER_DISMISSED, true).apply()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have flipped the notification permission in system
        // Settings while we were paused; re-read the ground truth so the
        // banner reflects reality (and clears once they grant it).
        refreshPermissionState(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Some OEM skins reset the decor UI flags on focus change
        // (e.g. returning from a dialog or the recents screen), which
        // would silently un-hide the system bars while the user's
        // immersive preference is still ON.
        if (hasFocus && immersiveActive) applyImmersive(true)
    }

    private fun applyImmersive(enabled: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!isNotificationGranted()) requestNotificationPermission()
    }

    private fun isNotificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Re-derive the permission flags. [grantResults] is non-null when
     * coming from the permission launcher callback (carries the fresh
     * decision); null means read the system ground truth (onCreate /
     * onResume / focus regain).
     */
    private fun refreshPermissionState(grantResults: Map<String, Boolean>?) {
        val locGranted = grantResults?.let { results ->
            results.entries
                .filter {
                    it.key == Manifest.permission.ACCESS_FINE_LOCATION ||
                    it.key == Manifest.permission.ACCESS_COARSE_LOCATION
                }
                .all { it.value }
        } ?: listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        hasLocationPermission = locGranted
        notificationsGranted.value = grantResults?.get(Manifest.permission.POST_NOTIFICATIONS)
            ?: isNotificationGranted()
    }

    companion object {
        private const val KEY_BANNER_DISMISSED = "notification_banner_dismissed"
    }
}

@Composable
fun MainScreen(
    hasPermission: Boolean,
    immersive: Boolean,
    onImmersiveChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    showNotificationBanner: Boolean,
    onGrantNotifications: () -> Unit,
    onDismissNotificationBanner: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    val service by rememberBoundService()

    val gpsData = service?.gps?.collectAsState()?.value ?: GpsData.EMPTY
    val attData = service?.attitude?.collectAsState()?.value ?: AttitudeData.EMPTY
    val envData = service?.environment?.collectAsState()?.value ?: EnvironmentData.EMPTY
    val flightSnap = service?.flight?.collectAsState()?.value ?: FlightTimer.Snapshot.INITIAL
    val declinationDeg = service?.declinationDeg?.collectAsState()?.value ?: 0f
    val recordingEnabled = service?.recordingEnabledFlow?.collectAsState()?.value ?: true
    val gForce = service?.gForce?.collectAsState()?.value ?: GForceRange.EMPTY
    val sunTimes = service?.sunTimes?.collectAsState()?.value ?: SunTimes.UNKNOWN

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
                Column(Modifier.fillMaxSize()) {
                    if (showNotificationBanner) {
                        NotificationBanner(
                            onGrant = onGrantNotifications,
                            onDismiss = onDismissNotificationBanner,
                        )
                    }
                    when (selectedTab) {
                        0 -> GpsScreen(
                            gpsData, attData, envData, flightSnap, declinationDeg,
                            gForce = gForce,
                            sunTimes = sunTimes,
                            recordingEnabled = recordingEnabled,
                            onRecordingEnabledChange = { service?.setRecordingEnabled(it) },
                            immersive = immersive,
                            onImmersiveChange = onImmersiveChange,
                        )
                        1 -> MapScreen(gpsData)
                        2 -> DownloadScreen(gpsData)
                    }
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
