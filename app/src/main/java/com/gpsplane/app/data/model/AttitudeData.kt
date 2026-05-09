package com.gpsplane.app.data.model

/**
 * All values are measured in the phone's body frame (not the airframe).
 * They approximate airframe readings only when the phone is held roughly
 * aligned with the aircraft — e.g. flat on a tray table with the top edge
 * pointing forward. Held loosely in a passenger's hand they'll drift with
 * the phone's own motion.
 */
data class AttitudeData(
    /** Phone compass heading in degrees (0–360), NaN if unavailable. */
    val azimuth: Float,
    /** Pitch in degrees — positive = phone tilted forward (top edge down). */
    val pitch: Float,
    /** Roll in degrees — positive = phone tilted right. */
    val roll: Float,
    /**
     * Net (gravity-removed) linear acceleration magnitude, in g.
     * ~0 at rest; reflects phone motion only.
     */
    val accelerationG: Float,
    /**
     * Approximate load factor in g, from the raw accelerometer. ~1 at rest,
     * >1 in pull-ups, <1 in push-overs. This is |a|/g of the phone body —
     * true aviation load factor is the component perpendicular to the wings,
     * so these agree only while the phone stays roughly airframe-aligned.
     * NaN if the device has no accelerometer.
     */
    val loadFactorG: Float,
    /**
     * Yaw rate in degrees/second from the gyroscope z-axis. Positive =
     * turning right. Reflects the phone's body-frame z rotation, not the
     * aircraft's yaw directly; matches airframe yaw when the phone lies
     * flat with the screen facing up. NaN if the device has no gyroscope.
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
