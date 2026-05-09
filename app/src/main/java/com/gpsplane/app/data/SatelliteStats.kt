package com.gpsplane.app.data

import android.location.GnssStatus
import com.gpsplane.app.data.model.SatelliteInfo

/**
 * Per-constellation counts of satellites currently used in the GPS fix.
 * Mirrors [com.gpsplane.app.data.model.GpsData]'s *SatelliteCount fields.
 */
data class SatelliteCounts(
    val total: Int = 0,
    val gps: Int = 0,
    val glonass: Int = 0,
    val beidou: Int = 0,
    val galileo: Int = 0,
    val qzss: Int = 0,
    val irnss: Int = 0,
    val other: Int = 0,
)

object SatelliteStats {

    /** Count how many sats from each constellation are contributing to the fix. */
    fun countUsedInFix(satellites: List<SatelliteInfo>): SatelliteCounts {
        var total = 0
        var gps = 0; var glonass = 0; var beidou = 0
        var galileo = 0; var qzss = 0; var irnss = 0; var other = 0

        for (sat in satellites) {
            if (!sat.usedInFix) continue
            total++
            when (sat.constellationType) {
                GnssStatus.CONSTELLATION_GPS -> gps++
                GnssStatus.CONSTELLATION_GLONASS -> glonass++
                GnssStatus.CONSTELLATION_BEIDOU -> beidou++
                GnssStatus.CONSTELLATION_GALILEO -> galileo++
                GnssStatus.CONSTELLATION_QZSS -> qzss++
                GnssStatus.CONSTELLATION_IRNSS -> irnss++
                else -> other++
            }
        }

        return SatelliteCounts(total, gps, glonass, beidou, galileo, qzss, irnss, other)
    }
}
