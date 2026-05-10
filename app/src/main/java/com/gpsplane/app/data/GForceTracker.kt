package com.gpsplane.app.data

/**
 * Session-scoped min/max of the raw load-factor reading, in g. Pure:
 * callers pass every sample and the latest [FlightPhase] plus an
 * `airborneSinceMs` from [FlightTimer] so the tracker resets on every
 * new AIRBORNE segment without reading a clock itself.
 *
 * The tracker only updates while AIRBORNE — passengers care about the
 * bumps during cruise, not the ride to the runway.
 */
data class GForceRange(
    val minG: Float,
    val maxG: Float,
    val sampleCount: Int,
    val airborneSinceMs: Long,
) {
    val hasData: Boolean get() = sampleCount > 0

    companion object {
        val EMPTY = GForceRange(
            minG = Float.NaN, maxG = Float.NaN,
            sampleCount = 0, airborneSinceMs = 0L,
        )
    }
}

object GForceTracker {
    /**
     * Fold one sample into the range. `loadFactorG` may be NaN (device
     * has no accelerometer) — it's ignored and the range is unchanged.
     * Transitions into AIRBORNE with a new `airborneSinceMs` reset the
     * accumulator; transitions out of AIRBORNE return EMPTY so the
     * dashboard stops showing stale min/max on the ground.
     */
    fun update(
        prev: GForceRange,
        loadFactorG: Float,
        phase: FlightPhase,
        airborneSinceMs: Long,
    ): GForceRange {
        if (phase != FlightPhase.AIRBORNE) return GForceRange.EMPTY
        val resetDueToNewFlight = airborneSinceMs != prev.airborneSinceMs
        val base = if (resetDueToNewFlight) GForceRange.EMPTY.copy(airborneSinceMs = airborneSinceMs) else prev
        if (loadFactorG.isNaN()) return base
        val newMin = if (base.hasData) minOf(base.minG, loadFactorG) else loadFactorG
        val newMax = if (base.hasData) maxOf(base.maxG, loadFactorG) else loadFactorG
        return base.copy(
            minG = newMin,
            maxG = newMax,
            sampleCount = base.sampleCount + 1,
        )
    }
}
