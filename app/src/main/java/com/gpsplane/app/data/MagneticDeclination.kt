package com.gpsplane.app.data

import android.hardware.GeomagneticField

/**
 * Magnetic declination (variation): the signed angle from true north to
 * magnetic north, positive east. Subtract from a true heading to get
 * magnetic, add to a magnetic heading to get true.
 *
 * Backed by [android.hardware.GeomagneticField], which ships with the
 * platform's World Magnetic Model (WMM) coefficients. On Android 8 (our
 * minSdk) the bundled model is WMM 2015 and is therefore a few years
 * stale — expect ±2° error in high-latitude regions. On newer OS images
 * the model may be refreshed by the OEM.
 *
 * Kept as a thin wrapper so the UI and tests don't reach into
 * `android.hardware` directly, and so we can swap in our own WMM/IGRF
 * implementation later without touching callers.
 */
object MagneticDeclination {

    /**
     * Returns declination in degrees (positive east) for the given
     * location and moment. `timeMillis` is Unix epoch millis; typically
     * [System.currentTimeMillis].
     */
    fun degreesEast(
        latitudeDeg: Double,
        longitudeDeg: Double,
        altitudeMeters: Double,
        timeMillis: Long
    ): Float = GeomagneticField(
        latitudeDeg.toFloat(),
        longitudeDeg.toFloat(),
        altitudeMeters.toFloat(),
        timeMillis
    ).declination

    /**
     * Converts a true-north heading to magnetic, given the declination
     * at the observer's position. Result is normalized to [0, 360).
     */
    fun trueToMagnetic(trueHeadingDeg: Float, declinationDeg: Float): Float =
        normalize(trueHeadingDeg - declinationDeg)

    private fun normalize(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }
}
