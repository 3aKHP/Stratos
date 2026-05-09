package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PressureMathTest {

    @Test
    fun `sea level pressure maps to zero altitude`() {
        val alt = PressureMath.pressureToAltitude(PressureMath.SEA_LEVEL_PRESSURE_HPA)
        assertThat(alt).isWithin(1e-2f).of(0f)
    }

    @Test
    fun `cabin pressure approximates cruise cabin altitude`() {
        // A typical pressurised cabin runs at ~800 hPa, equivalent to ~1950 m
        // (~6400 ft). This is the same range airliners target at cruise.
        val alt = PressureMath.pressureToAltitude(800f)
        assertThat(alt).isWithin(50f).of(1950f)
    }

    @Test
    fun `pressure decreases with altitude`() {
        val p0 = PressureMath.altitudeToPressure(0.0)
        val p5k = PressureMath.altitudeToPressure(5000.0)
        val p10k = PressureMath.altitudeToPressure(10000.0)
        assertThat(p0).isGreaterThan(p5k)
        assertThat(p5k).isGreaterThan(p10k)
    }

    @Test
    fun `pressure at ten thousand meters matches ISA reference`() {
        // ISA table: 10,000 m ≈ 264.4 hPa. Allow a small tolerance.
        val p = PressureMath.altitudeToPressure(10000.0)
        assertThat(p).isWithin(1f).of(264.4f)
    }

    @Test
    fun `pressure and altitude conversions round-trip`() {
        for (altM in listOf(0.0, 500.0, 2000.0, 8000.0, 11000.0)) {
            val p = PressureMath.altitudeToPressure(altM)
            val roundTrip = PressureMath.pressureToAltitude(p)
            assertThat(roundTrip.toDouble()).isWithin(0.1).of(altM)
        }
    }

    @Test
    fun `non-positive pressure yields NaN`() {
        assertThat(PressureMath.pressureToAltitude(0f)).isNaN()
        assertThat(PressureMath.pressureToAltitude(-100f)).isNaN()
    }
}
