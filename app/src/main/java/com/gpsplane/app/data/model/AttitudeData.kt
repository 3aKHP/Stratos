package com.gpsplane.app.data.model

data class AttitudeData(
    /** Phone compass heading in degrees (0–360), NaN if unavailable. */
    val azimuth: Float,
    /** Pitch in degrees — positive = phone tilted forward (top edge down). */
    val pitch: Float,
    /** Roll in degrees — positive = phone tilted right. */
    val roll: Float,
    /** Total linear acceleration magnitude in g-force units. */
    val accelerationG: Float,
    /** Whether azimuth is currently valid (sensor calibrated & active). */
    val hasAzimuth: Boolean
) {
    companion object {
        val EMPTY = AttitudeData(
            azimuth = Float.NaN, pitch = 0f, roll = 0f,
            accelerationG = 0f, hasAzimuth = false
        )
    }
}
