package com.gpsplane.app.ui.component

import android.location.GnssStatus
import androidx.compose.ui.graphics.Color

/**
 * Per-constellation palette shared by the sky plot and the signal bars.
 * GNSS constellation type constants come straight from the platform.
 */

internal fun constellationColor(t: Int): Color = when (t) {
    GnssStatus.CONSTELLATION_GPS -> Color(0xFF4CAF50)
    GnssStatus.CONSTELLATION_GLONASS -> Color(0xFFF44336)
    GnssStatus.CONSTELLATION_BEIDOU -> Color(0xFFFF9800)
    GnssStatus.CONSTELLATION_GALILEO -> Color(0xFF2196F3)
    GnssStatus.CONSTELLATION_QZSS -> Color(0xFF9C27B0)
    GnssStatus.CONSTELLATION_IRNSS -> Color(0xFF00BCD4)
    else -> Color(0xFF9E9E9E)
}

internal fun constellationLabel(t: Int): String = when (t) {
    GnssStatus.CONSTELLATION_GPS -> "G"
    GnssStatus.CONSTELLATION_GLONASS -> "R"
    GnssStatus.CONSTELLATION_BEIDOU -> "B"
    GnssStatus.CONSTELLATION_GALILEO -> "E"
    GnssStatus.CONSTELLATION_QZSS -> "Q"
    GnssStatus.CONSTELLATION_IRNSS -> "I"
    else -> "?"
}
