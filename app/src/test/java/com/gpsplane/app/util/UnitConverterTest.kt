package com.gpsplane.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class UnitConverterTest {

    @Test
    fun `meters to feet`() {
        assertThat(UnitConverter.metersToFeet(1.0)).isWithin(1e-6).of(3.28084)
        assertThat(UnitConverter.metersToFeet(0.0)).isEqualTo(0.0)
        assertThat(UnitConverter.metersToFeet(10000.0)).isWithin(0.5).of(32808.4)
    }

    @Test
    fun `feet to meters`() {
        assertThat(UnitConverter.feetToMeters(3.28084)).isWithin(1e-6).of(1.0)
        assertThat(UnitConverter.feetToMeters(0.0)).isEqualTo(0.0)
    }

    @Test
    fun `mps to knots`() {
        assertThat(UnitConverter.mpsToKnots(1.0f)).isWithin(1e-4f).of(1.94384f)
        assertThat(UnitConverter.mpsToKnots(0.0f)).isEqualTo(0.0f)
        // 250 m/s ≈ 486 knots
        assertThat(UnitConverter.mpsToKnots(250f)).isWithin(0.5f).of(485.96f)
    }

    @Test
    fun `mps to kmh`() {
        assertThat(UnitConverter.mpsToKmh(1.0f)).isWithin(1e-4f).of(3.6f)
        assertThat(UnitConverter.mpsToKmh(0.0f)).isEqualTo(0.0f)
    }

    @Test
    fun `mps to mph`() {
        assertThat(UnitConverter.mpsToMph(1.0f)).isWithin(1e-4f).of(2.23694f)
    }

    @Test
    fun `mps to ft per min`() {
        assertThat(UnitConverter.mpsToFtMin(1.0f)).isWithin(1e-2f).of(196.8504f)
        assertThat(UnitConverter.mpsToFtMin(0.0f)).isEqualTo(0.0f)
    }

    @Test
    fun `decimal to DMS — positive latitude`() {
        val dms = UnitConverter.decimalToDms(45.75)
        assertThat(dms).contains("45°")
        assertThat(dms).contains("45'")
        // 45.75° = 45°45'0"
        assertThat(dms).isEqualTo("45°45'0\"")
    }

    @Test
    fun `decimal to DMS — negative longitude`() {
        val dms = UnitConverter.decimalToDms(-126.63)
        assertThat(dms).startsWith("-")
        assertThat(dms).contains("126°")
        assertThat(dms).contains("37'")
    }

    @Test
    fun `sound speed at sea level`() {
        val c = UnitConverter.soundSpeedMs(0.0)
        assertThat(c).isWithin(1.0).of(340.3)
    }

    @Test
    fun `sound speed at cruise altitude`() {
        // At 10,000m, ISA temp ≈ 223K, sound speed ≈ 299 m/s
        val c = UnitConverter.soundSpeedMs(10000.0)
        assertThat(c).isWithin(5.0).of(299.5)
    }

    @Test
    fun `sound speed decreases with altitude`() {
        val c0 = UnitConverter.soundSpeedMs(0.0)
        val c10k = UnitConverter.soundSpeedMs(10000.0)
        assertThat(c10k).isLessThan(c0)
    }

    @Test
    fun `mach at cruise speed`() {
        // 250 m/s at 10,000m → ~0.83 Mach
        val m = UnitConverter.mach(250f, 10000.0)
        assertThat(m).isWithin(0.02).of(0.83)
    }

    @Test
    fun `mach zero when speed zero`() {
        assertThat(UnitConverter.mach(0f, 10000.0)).isEqualTo(0.0)
    }
}
