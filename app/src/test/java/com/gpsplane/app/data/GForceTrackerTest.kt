package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GForceTrackerTest {

    @Test fun `GROUND phase returns EMPTY`() {
        val r = GForceTracker.update(GForceRange.EMPTY, 1.0f, FlightPhase.GROUND, 0L)
        assertThat(r).isEqualTo(GForceRange.EMPTY)
    }

    @Test fun `first AIRBORNE sample seeds min and max to the sample value`() {
        val r = GForceTracker.update(GForceRange.EMPTY, 1.2f, FlightPhase.AIRBORNE, 1000L)
        assertThat(r.minG).isEqualTo(1.2f)
        assertThat(r.maxG).isEqualTo(1.2f)
        assertThat(r.sampleCount).isEqualTo(1)
        assertThat(r.airborneSinceMs).isEqualTo(1000L)
    }

    @Test fun `subsequent samples widen the range`() {
        var r = GForceRange.EMPTY
        r = GForceTracker.update(r, 1.0f, FlightPhase.AIRBORNE, 100L)
        r = GForceTracker.update(r, 1.4f, FlightPhase.AIRBORNE, 100L)
        r = GForceTracker.update(r, 0.6f, FlightPhase.AIRBORNE, 100L)
        r = GForceTracker.update(r, 1.1f, FlightPhase.AIRBORNE, 100L)
        assertThat(r.minG).isEqualTo(0.6f)
        assertThat(r.maxG).isEqualTo(1.4f)
        assertThat(r.sampleCount).isEqualTo(4)
    }

    @Test fun `new airborne segment resets the range`() {
        var r = GForceRange.EMPTY
        r = GForceTracker.update(r, 1.4f, FlightPhase.AIRBORNE, 100L)
        r = GForceTracker.update(r, 2.0f, FlightPhase.AIRBORNE, 200L) // new takeoff
        assertThat(r.minG).isEqualTo(2.0f)
        assertThat(r.maxG).isEqualTo(2.0f)
        assertThat(r.sampleCount).isEqualTo(1)
        assertThat(r.airborneSinceMs).isEqualTo(200L)
    }

    @Test fun `NaN sample is ignored`() {
        var r = GForceRange.EMPTY
        r = GForceTracker.update(r, 1.0f, FlightPhase.AIRBORNE, 100L)
        r = GForceTracker.update(r, Float.NaN, FlightPhase.AIRBORNE, 100L)
        assertThat(r.minG).isEqualTo(1.0f)
        assertThat(r.maxG).isEqualTo(1.0f)
        assertThat(r.sampleCount).isEqualTo(1)
    }

    @Test fun `NaN sample on empty range does not mark data present`() {
        val r = GForceTracker.update(GForceRange.EMPTY, Float.NaN, FlightPhase.AIRBORNE, 100L)
        assertThat(r.hasData).isFalse()
        assertThat(r.airborneSinceMs).isEqualTo(100L)
    }

    @Test fun `transition back to GROUND resets to EMPTY`() {
        var r = GForceRange.EMPTY
        r = GForceTracker.update(r, 1.4f, FlightPhase.AIRBORNE, 100L)
        r = GForceTracker.update(r, 0.8f, FlightPhase.GROUND, 100L)
        assertThat(r).isEqualTo(GForceRange.EMPTY)
    }
}
