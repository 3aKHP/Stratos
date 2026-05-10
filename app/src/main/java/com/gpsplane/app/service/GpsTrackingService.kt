package com.gpsplane.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.gpsplane.app.BuildConfig
import com.gpsplane.app.MainActivity
import com.gpsplane.app.R
import com.gpsplane.app.data.AttitudeRepository
import com.gpsplane.app.data.EnvironmentRepository
import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.FlightTimer
import com.gpsplane.app.data.GpsRepository
import com.gpsplane.app.data.MagneticDeclination
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.data.track.TrackRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Foreground service that owns the live GPS / attitude / environment
 * subscriptions for the entire app, plus the derived flight-timer and
 * magnetic-declination state.
 *
 * Running inside a foreground service keeps GPS alive while the phone's
 * screen is off or the app is backgrounded — the single largest
 * passenger-use complaint from earlier alphas. Data is published via
 * [StateFlow] and consumed by [MainActivity] through a local binder.
 *
 * Lifetime rules:
 *  - `startForegroundService()` first, then `bindService()` from the UI.
 *  - Service stays alive across UI unbind (so a tracked flight survives
 *    task switches) until [stop] is called from the notification action or
 *    the system reclaims it.
 *  - There is only one instance process-wide; repositories are constructed
 *    once in [onCreate].
 */
class GpsTrackingService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    private lateinit var gpsRepository: GpsRepository
    private lateinit var attitudeRepository: AttitudeRepository
    private lateinit var environmentRepository: EnvironmentRepository
    private lateinit var trackRecorder: TrackRecorder

    private val _gps = MutableStateFlow(GpsData.EMPTY)
    private val _attitude = MutableStateFlow(AttitudeData.EMPTY)
    private val _environment = MutableStateFlow(EnvironmentData.EMPTY)
    private val _flight = MutableStateFlow(FlightTimer.Snapshot.INITIAL)
    private val _declinationDeg = MutableStateFlow(0f)
    private val _recording = MutableStateFlow(false)
    private val _recordingEnabled = MutableStateFlow(true)

    val gps: StateFlow<GpsData> get() = _gps.asStateFlow()
    val attitude: StateFlow<AttitudeData> get() = _attitude.asStateFlow()
    val environment: StateFlow<EnvironmentData> get() = _environment.asStateFlow()
    val flight: StateFlow<FlightTimer.Snapshot> get() = _flight.asStateFlow()
    val declinationDeg: StateFlow<Float> get() = _declinationDeg.asStateFlow()
    val recording: StateFlow<Boolean> get() = _recording.asStateFlow()
    val recordingEnabledFlow: StateFlow<Boolean> get() = _recordingEnabled.asStateFlow()

    /**
     * Toggle GPX recording on the fly. When disabled mid-flight the
     * current file is closed and a new one will start only if AIRBORNE
     * is re-entered while enabled.
     */
    fun setRecordingEnabled(enabled: Boolean) {
        _recordingEnabled.value = enabled
        trackRecorder.enabled = enabled
        _recording.value = trackRecorder.state().recording
    }

    inner class LocalBinder : Binder() {
        fun service(): GpsTrackingService = this@GpsTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        gpsRepository = GpsRepository(this)
        attitudeRepository = AttitudeRepository(this)
        environmentRepository = EnvironmentRepository(this)
        trackRecorder = TrackRecorder(
            root = filesDir.resolve("tracks"),
            creator = "Stratos/${BuildConfig.VERSION_NAME}",
        )
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stop()
                return START_NOT_STICKY
            }
        }

        startForegroundCompat(buildNotification(FlightTimer.Snapshot.INITIAL, 0L))
        if (collectJob == null) {
            collectJob = combine(
                gpsRepository.observeLocation(),
                attitudeRepository.observeAttitude(),
                environmentRepository.observeEnvironment(),
            ) { gps, att, env -> Triple(gps, att, env) }
                .onEach { (gps, att, env) ->
                    _gps.value = gps
                    _attitude.value = att
                    _environment.value = env
                    if (gps.hasFix) {
                        val snap = FlightTimer.update(
                            _flight.value, gps.speedMps, gps.altitudeMeters, gps.timestampMs
                        )
                        _flight.value = snap
                        _declinationDeg.value = MagneticDeclination.degreesEast(
                            gps.latitude, gps.longitude, gps.altitudeMeters, gps.timestampMs
                        )
                        trackRecorder.onGpsSample(gps, snap.phase)
                        _recording.value = trackRecorder.state().recording
                        refreshNotification(snap, gps.timestampMs)
                    }
                }
                .launchIn(scope)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        collectJob = null
        scope.cancel()
        if (::trackRecorder.isInitialized) trackRecorder.close()
        super.onDestroy()
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Notification ────────────────────────────────────────────────────

    private fun ensureNotificationChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.tracking_channel_description)
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshNotification(snap: FlightTimer.Snapshot, nowMs: Long) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIFICATION_ID, buildNotification(snap, nowMs))
    }

    private fun buildNotification(snap: FlightTimer.Snapshot, nowMs: Long): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GpsTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = when (snap.phase) {
            FlightPhase.GROUND -> getString(R.string.tracking_status_ground)
            FlightPhase.AIRBORNE -> {
                val ms = (nowMs - snap.airborneSinceMs).coerceAtLeast(0L)
                val secs = ms / 1000
                getString(
                    R.string.tracking_status_airborne,
                    secs / 3600, (secs % 3600) / 60, secs % 60,
                )
            }
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                Notification.Action.Builder(
                    null, getString(R.string.tracking_stop), stopIntent
                ).build()
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gps_tracking"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.gpsplane.app.action.STOP_TRACKING"

        /** Starts the service in the foreground; safe to call repeatedly. */
        fun start(context: Context) {
            val intent = Intent(context, GpsTrackingService::class.java)
            context.startForegroundService(intent)
        }
    }
}
