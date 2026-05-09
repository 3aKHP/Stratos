package com.gpsplane.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.data.model.SatelliteInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

class GpsRepository(context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var satelliteCount = 0
    private var gpsSatelliteCount = 0
    private var glonassSatelliteCount = 0
    private var beidouSatelliteCount = 0
    private var galileoSatelliteCount = 0
    private var qzssSatelliteCount = 0
    private var irnssSatelliteCount = 0
    private var otherSatelliteCount = 0

    private var latestSatellites: List<SatelliteInfo> = emptyList()

    private var smoothedVspeed = 0f
    private var lastAltitude = Double.NaN
    private var lastAltitudeTime = 0L

    // TTFF tracking
    private var sessionStartElapsedMs = -1L
    private var ttffMs = -1L
    private var hasFirstFix = false

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = mutableListOf<SatelliteInfo>()
            var total = 0
            var gps = 0; var glonass = 0; var beidou = 0
            var galileo = 0; var qzss = 0; var irnss = 0; var other = 0

            for (i in 0 until status.satelliteCount) {
                val constellation = status.getConstellationType(i)
                val used = status.usedInFix(i)

                if (used) {
                    total++
                    when (constellation) {
                        GnssStatus.CONSTELLATION_GPS -> gps++
                        GnssStatus.CONSTELLATION_GLONASS -> glonass++
                        GnssStatus.CONSTELLATION_BEIDOU -> beidou++
                        GnssStatus.CONSTELLATION_GALILEO -> galileo++
                        GnssStatus.CONSTELLATION_QZSS -> qzss++
                        GnssStatus.CONSTELLATION_IRNSS -> irnss++
                        else -> other++
                    }
                }

                list.add(
                    SatelliteInfo(
                        svid = status.getSvid(i),
                        constellationType = constellation,
                        cn0DbHz = status.getCn0DbHz(i),
                        azimuth = status.getAzimuthDegrees(i),
                        elevation = status.getElevationDegrees(i),
                        usedInFix = used,
                        hasEphemeris = status.hasEphemerisData(i),
                        hasAlmanac = status.hasAlmanacData(i)
                    )
                )
            }

            satelliteCount = total
            gpsSatelliteCount = gps; glonassSatelliteCount = glonass
            beidouSatelliteCount = beidou; galileoSatelliteCount = galileo
            qzssSatelliteCount = qzss; irnssSatelliteCount = irnss
            otherSatelliteCount = other
            latestSatellites = list
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocation(): Flow<GpsData> = callbackFlow {
        // Register GNSS status callback for this flow's lifetime
        locationManager.registerGnssStatusCallback(
            gnssStatusCallback,
            Handler(Looper.getMainLooper())
        )

        sessionStartElapsedMs = SystemClock.elapsedRealtime()
        hasFirstFix = false
        ttffMs = -1L

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val now = System.currentTimeMillis()

                // Track first fix time
                if (!hasFirstFix) {
                    hasFirstFix = true
                    ttffMs = SystemClock.elapsedRealtime() - sessionStartElapsedMs
                }

                // Compute raw vertical speed from altitude delta
                var rawVspeed = 0f
                if (!lastAltitude.isNaN() && lastAltitudeTime > 0) {
                    val dt = (now - lastAltitudeTime) / 1000.0
                    if (dt > 0.5) {
                        rawVspeed = ((loc.altitude - lastAltitude) / dt).toFloat()
                    }
                }

                val alpha = 0.3f
                smoothedVspeed = if (abs(smoothedVspeed) < 0.01f && abs(rawVspeed) > 10f) {
                    rawVspeed
                } else {
                    smoothedVspeed * (1f - alpha) + rawVspeed * alpha
                }

                lastAltitude = loc.altitude
                lastAltitudeTime = now

                trySend(
                    GpsData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitudeMeters = loc.altitude,
                        speedMps = loc.speed,
                        bearing = loc.bearing,
                        accuracyMeters = loc.accuracy,
                        verticalSpeedMps = smoothedVspeed,
                        satelliteCount = satelliteCount,
                        gpsSatelliteCount = gpsSatelliteCount,
                        glonassSatelliteCount = glonassSatelliteCount,
                        beidouSatelliteCount = beidouSatelliteCount,
                        galileoSatelliteCount = galileoSatelliteCount,
                        qzssSatelliteCount = qzssSatelliteCount,
                        irnssSatelliteCount = irnssSatelliteCount,
                        otherSatelliteCount = otherSatelliteCount,
                        satellites = latestSatellites,
                        ttffSeconds = if (ttffMs > 0) ttffMs / 1000f else -1f,
                        timestampMs = now,
                        hasFix = true
                    )
                )
            }

            override fun onProviderDisabled(provider: String) {
                trySend(GpsData.EMPTY.copy(hasFix = false))
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                listener
            )
        } catch (_: SecurityException) {
            trySend(GpsData.EMPTY)
        }

        awaitClose {
            locationManager.removeUpdates(listener)
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        }
    }

    companion object {
        private const val MIN_TIME_MS = 1000L
        private const val MIN_DISTANCE_M = 0f
    }
}
