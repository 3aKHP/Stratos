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

class GpsRepository(context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var latestCounts = SatelliteCounts()
    private var latestSatellites: List<SatelliteInfo> = emptyList()

    private val vspeedFilter = VerticalSpeedFilter()

    // TTFF tracking
    private var sessionStartElapsedMs = -1L
    private var ttffMs = -1L
    private var hasFirstFix = false

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = ArrayList<SatelliteInfo>(status.satelliteCount)
            for (i in 0 until status.satelliteCount) {
                list.add(
                    SatelliteInfo(
                        svid = status.getSvid(i),
                        constellationType = status.getConstellationType(i),
                        cn0DbHz = status.getCn0DbHz(i),
                        azimuth = status.getAzimuthDegrees(i),
                        elevation = status.getElevationDegrees(i),
                        usedInFix = status.usedInFix(i),
                        hasEphemeris = status.hasEphemerisData(i),
                        hasAlmanac = status.hasAlmanacData(i)
                    )
                )
            }
            latestCounts = SatelliteStats.countUsedInFix(list)
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

                val vspeed = vspeedFilter.update(loc.altitude, now)
                val counts = latestCounts

                trySend(
                    GpsData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitudeMeters = loc.altitude,
                        speedMps = loc.speed,
                        bearing = loc.bearing,
                        accuracyMeters = loc.accuracy,
                        verticalSpeedMps = vspeed,
                        satelliteCount = counts.total,
                        gpsSatelliteCount = counts.gps,
                        glonassSatelliteCount = counts.glonass,
                        beidouSatelliteCount = counts.beidou,
                        galileoSatelliteCount = counts.galileo,
                        qzssSatelliteCount = counts.qzss,
                        irnssSatelliteCount = counts.irnss,
                        otherSatelliteCount = counts.other,
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
