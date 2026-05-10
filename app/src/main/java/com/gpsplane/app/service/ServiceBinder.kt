package com.gpsplane.app.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Composable-side binder that connects to the already-started
 * [GpsTrackingService] and exposes it as Compose state. Callers use
 * `val service by rememberBoundService()` and then collect StateFlows
 * from `service?.gps`, etc.
 *
 * Unbinds on dispose — service stays alive because the caller also
 * invoked [GpsTrackingService.start] (via `startForegroundService`),
 * which owns the service's lifetime independently of the binding.
 */
@Composable
fun rememberBoundService(): State<GpsTrackingService?> {
    val state = remember { mutableStateOf<GpsTrackingService?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                state.value = (binder as? GpsTrackingService.LocalBinder)?.service()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                state.value = null
            }
        }
        val intent = Intent(context, GpsTrackingService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(connection)
            state.value = null
        }
    }
    return state
}
