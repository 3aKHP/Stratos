package com.gpsplane.app.data

import kotlin.math.sqrt

/** Euler angles derived from a rotation matrix, degrees. */
data class EulerDegrees(val azimuth: Float, val pitch: Float, val roll: Float)

object AttitudeMath {

    /**
     * Convert the three Euler values that [android.hardware.SensorManager.getOrientation]
     * returns (radians: [azimuth, pitch, roll], azimuth CCW-positive) into degrees
     * with a compass-style azimuth (CW-positive, 0..360).
     */
    fun orientationRadiansToDegrees(orientation: FloatArray): EulerDegrees {
        val azimuthDeg = Math.toDegrees(-orientation[0].toDouble()).toFloat()
            .let { if (it < 0) it + 360f else it }
        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()
        return EulerDegrees(azimuthDeg, pitchDeg, rollDeg)
    }

    /** |a| / 9.81 — m/s² of net (gravity-removed) acceleration to g-force units. */
    fun linearAccelerationToG(x: Float, y: Float, z: Float): Float =
        sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY_MPS2

    const val STANDARD_GRAVITY_MPS2 = 9.81f
}
