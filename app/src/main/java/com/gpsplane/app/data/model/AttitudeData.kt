package com.gpsplane.app.data.model

data class AttitudeData(
    /** Phone compass heading in degrees (0–360), NaN if unavailable. */
    val azimuth: Float,
    /** Pitch in degrees — positive = phone tilted forward (top edge down). */
    val pitch: Float,
    /** Roll in degrees — positive = phone tilted right. */
    val roll: Float,
    /**
     * Net (gravity-removed) linear acceleration magnitude, in g.
     * ~0 at rest; reflects airframe motion only.
     */
    val accelerationG: Float,
    /**
     * Aviation-standard load factor, in g. From the raw accelerometer
     * (gravity included). ~1 at rest, >1 in pull-ups, <1 in push-overs.
     */
    val loadFactorG: Float,
    /**
     * Yaw rate in degrees/second from the gyroscope z-axis. Positive =
     * turning right (nose yawing toward the right wing). NaN if the
     * device has no gyroscope.
     */
    val turnRateDegPerSec: Float,
    /** Whether azimuth is currently valid (sensor calibrated & active). */
    val hasAzimuth: Boolean
) {
    companion object {
        val EMPTY = AttitudeData(
            azimuth = Float.NaN, pitch = 0f, roll = 0f,
            accelerationG = 0f, loadFactorG = Float.NaN,
            turnRateDegPerSec = Float.NaN, hasAzimuth = false
        )
    }
}
