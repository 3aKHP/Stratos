package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmaFilterTest {

    @Test
    fun `first non-NaN sample seeds the filter`() {
        val f = EmaFilter(alpha = 0.3f)
        assertThat(f.update(5f)).isEqualTo(5f)
    }

    @Test
    fun `NaN input is ignored and returns last smoothed value`() {
        val f = EmaFilter(alpha = 0.3f)
        f.update(5f)
        assertThat(f.update(Float.NaN)).isEqualTo(5f)
    }

    @Test
    fun `before any sample NaN input stays NaN`() {
        val f = EmaFilter(alpha = 0.3f)
        assertThat(f.update(Float.NaN)).isNaN()
    }

    @Test
    fun `steady input converges toward it`() {
        val f = EmaFilter(alpha = 0.3f)
        f.update(0f)
        var v = 0f
        repeat(50) { v = f.update(10f) }
        // 50 iterations of α=0.3 drives the value arbitrarily close to 10.
        assertThat(v).isWithin(1e-4f).of(10f)
    }

    @Test
    fun `single step applies the EMA formula`() {
        val f = EmaFilter(alpha = 0.3f)
        f.update(0f)
        // smoothed = 0*0.7 + 10*0.3 = 3
        assertThat(f.update(10f)).isWithin(1e-4f).of(3f)
    }

    @Test
    fun `reset wipes state`() {
        val f = EmaFilter(alpha = 0.3f)
        f.update(5f)
        f.reset()
        // After reset, the next non-NaN sample seeds again instead of easing.
        assertThat(f.update(42f)).isEqualTo(42f)
    }
}
