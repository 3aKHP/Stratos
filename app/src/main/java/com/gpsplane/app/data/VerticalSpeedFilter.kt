package com.gpsplane.app.data

import kotlin.math.abs

/**
 * Exponential moving average on GPS altitude deltas.
 *
 * Commercial cabins are pressurised, so barometric V/S reads near zero at
 * cruise; GPS altitude differencing is noisy but directionally correct when
 * smoothed. The cold-start branch lets us snap to the first strong reading
 * instead of easing in over several seconds.
 */
class VerticalSpeedFilter(
    private val alpha: Float = DEFAULT_ALPHA,
    private val coldStartJumpThreshold: Float = DEFAULT_COLD_START_JUMP,
) {
    private var lastAltitude: Double = Double.NaN
    private var lastTimestampMs: Long = 0
    private var smoothed: Float = 0f

    /**
     * Feed a new altitude sample. Returns the current smoothed V/S in m/s.
     * Needs at least two samples more than [MIN_DT_SECONDS] apart before it
     * produces a meaningful reading; the first call just seeds state.
     */
    fun update(altitudeMeters: Double, timestampMs: Long): Float {
        var raw = 0f
        if (!lastAltitude.isNaN() && lastTimestampMs > 0) {
            val dt = (timestampMs - lastTimestampMs) / 1000.0
            if (dt > MIN_DT_SECONDS) {
                raw = ((altitudeMeters - lastAltitude) / dt).toFloat()
            }
        }

        smoothed = if (abs(smoothed) < COLD_START_SMOOTHED_EPS && abs(raw) > coldStartJumpThreshold) {
            raw
        } else {
            smoothed * (1f - alpha) + raw * alpha
        }

        lastAltitude = altitudeMeters
        lastTimestampMs = timestampMs
        return smoothed
    }

    companion object {
        const val DEFAULT_ALPHA = 0.3f
        const val DEFAULT_COLD_START_JUMP = 10f
        const val MIN_DT_SECONDS = 0.5
        const val COLD_START_SMOOTHED_EPS = 0.01f
    }
}
