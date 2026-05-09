package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VerticalSpeedFilterTest {

    // Simulated System.currentTimeMillis() base — any non-zero value, since
    // the filter uses `lastTimestampMs > 0` to detect "no prior sample".
    private val t0 = 1_700_000_000_000L

    @Test
    fun `first sample seeds state and returns zero`() {
        val f = VerticalSpeedFilter()
        assertThat(f.update(1000.0, t0)).isEqualTo(0f)
    }

    @Test
    fun `samples less than MIN_DT apart produce zero raw`() {
        val f = VerticalSpeedFilter()
        f.update(1000.0, t0)
        // dt = 0.3s (< 0.5s MIN_DT) — raw stays at 0, smoothed stays at 0.
        assertThat(f.update(1100.0, t0 + 300)).isEqualTo(0f)
    }

    @Test
    fun `cold start snaps to first strong reading instead of easing in`() {
        val f = VerticalSpeedFilter()
        f.update(1000.0, t0)
        // 100m climb in 1s = +100 m/s, well above the 10 m/s cold-start threshold.
        val out = f.update(1100.0, t0 + 1000)
        assertThat(out).isWithin(1e-4f).of(100f)
    }

    @Test
    fun `steady state EMA eases toward raw value`() {
        val f = VerticalSpeedFilter()
        // Seed the filter with a weak climb (5 m/s < 10 m/s jump threshold),
        // so the cold-start branch does NOT snap and smoothed stays near 0.
        f.update(0.0, t0)
        f.update(5.0, t0 + 1000) // raw=5, smoothed = 0*0.7 + 5*0.3 = 1.5
        // After one EMA step smoothed is 1.5, above the 0.01 eps — further
        // updates go through the EMA branch regardless of raw magnitude.
        val after1 = f.update(10.0, t0 + 2000) // raw=5, smoothed = 1.5*0.7 + 5*0.3 = 2.55
        assertThat(after1).isWithin(1e-4f).of(2.55f)
        val after2 = f.update(15.0, t0 + 3000) // raw=5, smoothed = 2.55*0.7 + 5*0.3 = 3.285
        assertThat(after2).isWithin(1e-4f).of(3.285f)
    }

    @Test
    fun `descent produces negative smoothed output`() {
        val f = VerticalSpeedFilter()
        f.update(10000.0, t0)
        val out = f.update(9900.0, t0 + 1000) // -100 m/s — cold-start snap to negative
        assertThat(out).isWithin(1e-4f).of(-100f)
    }
}
