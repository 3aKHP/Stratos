package com.gpsplane.app.data

import kotlin.math.sqrt

/** Euler angles derived from a rotation matrix, degrees. */
data class EulerDegrees(val azimuth: Float, val pitch: Float, val roll: Float)

object AttitudeMath {

    /**
     * Convert the three Euler values that [android.hardware.SensorManager.getOrientation]
     * returns (radians: [azimuth, pitch, roll]) into degrees.
     *
     * The platform azimuth is already compass-style (CW-positive, 0 at
     * north, +π/2 at east). We only need to convert to degrees and wrap
     * into [0, 360). Early versions of this code incorrectly negated the
     * raw value under the belief that it was CCW-positive — that mistake
     * shipped into alpha.1–3 and caused the sky plot to read ~180° off
     * from the true phone heading.
     */
    fun orientationRadiansToDegrees(orientation: FloatArray): EulerDegrees {
        val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            .let { if (it < 0) it + 360f else it }
        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()
        return EulerDegrees(azimuthDeg, pitchDeg, rollDeg)
    }

    /**
     * Magnitude of an acceleration vector, divided by g.
     *
     * Used for two distinct readings that share the same math:
     *  - `TYPE_LINEAR_ACCELERATION` input → net (gravity-removed) acceleration in g,
     *    the phone/airframe's own motion. Reads ~0 at rest.
     *  - `TYPE_ACCELEROMETER` input → load factor in g, the aviation standard reading.
     *    Reads ~1 at rest, >1 in pull-ups, <1 in push-overs or free fall.
     */
    fun magnitudeInG(x: Float, y: Float, z: Float): Float =
        sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY_MPS2

    /**
     * Raw gyroscope z-axis (rad/s, +CCW viewed from the screen-up side) to
     * aviation-convention yaw rate (°/s, positive = right turn). The sign
     * flip accounts for Android's body-frame convention differing from the
     * aviation one.
     */
    fun gyroZToTurnRateDegPerSec(gyroZRadPerSec: Float): Float =
        Math.toDegrees(-gyroZRadPerSec.toDouble()).toFloat()

    const val STANDARD_GRAVITY_MPS2 = 9.81f
}
