package com.gpsplane.app.data

/**
 * Stateful exponential moving average for a scalar float stream.
 * Identity semantics: NaN input is ignored (keeps the last smoothed value);
 * the first non-NaN sample seeds the filter without easing.
 */
class EmaFilter(private val alpha: Float) {
    private var smoothed: Float = Float.NaN

    fun update(sample: Float): Float {
        if (sample.isNaN()) return smoothed
        smoothed = if (smoothed.isNaN()) sample
                   else smoothed * (1f - alpha) + sample * alpha
        return smoothed
    }

    fun reset() {
        smoothed = Float.NaN
    }
}
