package com.gpsplane.app.data

import com.gpsplane.app.util.UnitConverter

/**
 * Phase of a flight as inferred from GPS speed and altitude.
 */
enum class FlightPhase { GROUND, AIRBORNE }

/**
 * Output of [FlightTimer.update]: the current phase and, if AIRBORNE,
 * the number of milliseconds since the timer started. GROUND carries
 * elapsedMs = 0.
 */
data class FlightTimerState(
    val phase: FlightPhase,
    val elapsedMs: Long,
) {
    companion object {
        val GROUND = FlightTimerState(FlightPhase.GROUND, 0L)
    }
}

/**
 * Inference rules:
 *  - Takeoff: speed > 150 kn AND altitude > 3000 ft sustained ≥ 10 s.
 *  - Landing: speed < 80 kn sustained ≥ 30 s.
 *
 * Hysteresis is intentional — a single GPS speed glitch shouldn't flip
 * the state. Thresholds come from typical airliner profiles: rotation
 * happens well below 150 kn, so 150 kn + 3000 ft reliably catches only
 * the climb phase; the 80 kn gate sits below taxi-fast for most types.
 *
 * Pure state machine — no system clock reads. Callers pass nowMs (usually
 * gpsData.timestampMs) so the logic is fully deterministic for tests.
 */
object FlightTimer {

    private const val TAKEOFF_SPEED_KN = 150f
    private const val TAKEOFF_ALT_FT = 3000f
    private const val TAKEOFF_HOLD_MS = 10_000L

    private const val LANDING_SPEED_KN = 80f
    private const val LANDING_HOLD_MS = 30_000L

    /**
     * Snapshot of the machine. Immutable; [update] returns a new one.
     * [pendingSinceMs] is the timestamp at which the current candidate
     * transition first became eligible (0 = no pending transition).
     */
    data class Snapshot(
        val phase: FlightPhase,
        val airborneSinceMs: Long,
        val pendingSinceMs: Long,
    ) {
        companion object {
            val INITIAL = Snapshot(FlightPhase.GROUND, 0L, 0L)
        }
    }

    /**
     * Advances the machine with one GPS sample. `speedMps` and
     * `altitudeMeters` come straight from [GpsData]; `nowMs` should be
     * the sample's own timestamp (gpsData.timestampMs), not wall clock.
     */
    fun update(
        prev: Snapshot,
        speedMps: Float,
        altitudeMeters: Double,
        nowMs: Long,
    ): Snapshot {
        val speedKn = UnitConverter.mpsToKnots(speedMps)
        val altFt = UnitConverter.metersToFeet(altitudeMeters).toFloat()

        return when (prev.phase) {
            FlightPhase.GROUND -> {
                val takeoffEligible = speedKn > TAKEOFF_SPEED_KN && altFt > TAKEOFF_ALT_FT
                when {
                    !takeoffEligible ->
                        prev.copy(pendingSinceMs = 0L)
                    prev.pendingSinceMs == 0L ->
                        prev.copy(pendingSinceMs = nowMs)
                    nowMs - prev.pendingSinceMs >= TAKEOFF_HOLD_MS ->
                        Snapshot(FlightPhase.AIRBORNE, prev.pendingSinceMs, 0L)
                    else -> prev
                }
            }
            FlightPhase.AIRBORNE -> {
                val landingEligible = speedKn < LANDING_SPEED_KN
                when {
                    !landingEligible ->
                        prev.copy(pendingSinceMs = 0L)
                    prev.pendingSinceMs == 0L ->
                        prev.copy(pendingSinceMs = nowMs)
                    nowMs - prev.pendingSinceMs >= LANDING_HOLD_MS ->
                        Snapshot(FlightPhase.GROUND, 0L, 0L)
                    else -> prev
                }
            }
        }
    }

    /** Derives a display-friendly [FlightTimerState] from a [Snapshot]. */
    fun display(snap: Snapshot, nowMs: Long): FlightTimerState = when (snap.phase) {
        FlightPhase.GROUND -> FlightTimerState.GROUND
        FlightPhase.AIRBORNE -> FlightTimerState(
            FlightPhase.AIRBORNE,
            (nowMs - snap.airborneSinceMs).coerceAtLeast(0L)
        )
    }
}
