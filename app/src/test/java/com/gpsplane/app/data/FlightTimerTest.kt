package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import com.gpsplane.app.util.UnitConverter
import org.junit.Test

class FlightTimerTest {

    // 160 kn well above the 150 kn threshold.
    private val cruiseSpeedMps = 160f / 1.94384f
    private val slowTaxiMps = 40f / 1.94384f
    // 5000 ft well above the 3000 ft threshold.
    private val highAltMeters = UnitConverter.feetToMeters(5000.0)
    private val lowAltMeters = UnitConverter.feetToMeters(500.0)

    @Test
    fun `initial snapshot is ground`() {
        assertThat(FlightTimer.Snapshot.INITIAL.phase).isEqualTo(FlightPhase.GROUND)
    }

    @Test
    fun `stays on ground below thresholds`() {
        val s = FlightTimer.update(FlightTimer.Snapshot.INITIAL, cruiseSpeedMps, lowAltMeters, 0L)
        assertThat(s.phase).isEqualTo(FlightPhase.GROUND)
        assertThat(s.pendingSinceMs).isEqualTo(0L)
    }

    @Test
    fun `stays on ground with fast speed but low altitude`() {
        // Fast taxi / rejected takeoff should NOT trip airborne.
        val s = FlightTimer.update(FlightTimer.Snapshot.INITIAL, cruiseSpeedMps, lowAltMeters, 0L)
        assertThat(s.phase).isEqualTo(FlightPhase.GROUND)
    }

    @Test
    fun `starts pending when both thresholds exceeded`() {
        val s = FlightTimer.update(FlightTimer.Snapshot.INITIAL, cruiseSpeedMps, highAltMeters, 1000L)
        assertThat(s.phase).isEqualTo(FlightPhase.GROUND)
        assertThat(s.pendingSinceMs).isEqualTo(1000L)
    }

    @Test
    fun `resets pending when threshold momentarily drops`() {
        val pending = FlightTimer.update(FlightTimer.Snapshot.INITIAL, cruiseSpeedMps, highAltMeters, 1000L)
        // Momentary dip.
        val reset = FlightTimer.update(pending, slowTaxiMps, highAltMeters, 2000L)
        assertThat(reset.pendingSinceMs).isEqualTo(0L)
        assertThat(reset.phase).isEqualTo(FlightPhase.GROUND)
    }

    @Test
    fun `transitions to airborne after hold window elapses`() {
        var s = FlightTimer.Snapshot.INITIAL
        s = FlightTimer.update(s, cruiseSpeedMps, highAltMeters, 1000L)
        s = FlightTimer.update(s, cruiseSpeedMps, highAltMeters, 5000L)
        // Still pending at 5s.
        assertThat(s.phase).isEqualTo(FlightPhase.GROUND)
        // At t = 11s, 10s has elapsed since pending started (1s).
        s = FlightTimer.update(s, cruiseSpeedMps, highAltMeters, 11_000L)
        assertThat(s.phase).isEqualTo(FlightPhase.AIRBORNE)
        assertThat(s.airborneSinceMs).isEqualTo(1000L)
    }

    @Test
    fun `stays airborne while fast`() {
        val airborne = FlightTimer.Snapshot(FlightPhase.AIRBORNE, 0L, 0L)
        val s = FlightTimer.update(airborne, cruiseSpeedMps, highAltMeters, 60_000L)
        assertThat(s.phase).isEqualTo(FlightPhase.AIRBORNE)
    }

    @Test
    fun `starts landing pending when slowing below threshold`() {
        val airborne = FlightTimer.Snapshot(FlightPhase.AIRBORNE, 0L, 0L)
        val s = FlightTimer.update(airborne, slowTaxiMps, lowAltMeters, 100_000L)
        assertThat(s.phase).isEqualTo(FlightPhase.AIRBORNE)
        assertThat(s.pendingSinceMs).isEqualTo(100_000L)
    }

    @Test
    fun `transitions to ground after landing hold`() {
        var s = FlightTimer.Snapshot(FlightPhase.AIRBORNE, 0L, 0L)
        s = FlightTimer.update(s, slowTaxiMps, lowAltMeters, 100_000L)
        // 30s later, still slow.
        s = FlightTimer.update(s, slowTaxiMps, lowAltMeters, 130_000L)
        assertThat(s.phase).isEqualTo(FlightPhase.GROUND)
        assertThat(s.airborneSinceMs).isEqualTo(0L)
    }

    @Test
    fun `speed bump during landing resets pending`() {
        var s = FlightTimer.Snapshot(FlightPhase.AIRBORNE, 0L, 0L)
        s = FlightTimer.update(s, slowTaxiMps, lowAltMeters, 100_000L)
        // Quick spike above threshold — resets pending.
        s = FlightTimer.update(s, cruiseSpeedMps, highAltMeters, 110_000L)
        assertThat(s.phase).isEqualTo(FlightPhase.AIRBORNE)
        assertThat(s.pendingSinceMs).isEqualTo(0L)
    }

    @Test
    fun `display reports elapsed for airborne`() {
        val snap = FlightTimer.Snapshot(FlightPhase.AIRBORNE, 1_000L, 0L)
        val d = FlightTimer.display(snap, 61_000L)
        assertThat(d.phase).isEqualTo(FlightPhase.AIRBORNE)
        assertThat(d.elapsedMs).isEqualTo(60_000L)
    }

    @Test
    fun `display reports zero for ground`() {
        val d = FlightTimer.display(FlightTimer.Snapshot.INITIAL, 99_999L)
        assertThat(d.phase).isEqualTo(FlightPhase.GROUND)
        assertThat(d.elapsedMs).isEqualTo(0L)
    }
}
