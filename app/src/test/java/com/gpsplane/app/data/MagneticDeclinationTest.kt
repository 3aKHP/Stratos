package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MagneticDeclinationTest {

    // GeomagneticField itself can't run on the JVM stub — only the pure
    // trueToMagnetic conversion is covered here. The android.hardware call
    // is exercised on-device during real-device verification.

    @Test
    fun `subtracts east declination from true heading`() {
        // Beijing, ~-7° declination (west). TRUE 0° → MAG 7°.
        assertThat(MagneticDeclination.trueToMagnetic(0f, -7f))
            .isWithin(0.001f).of(7f)
    }

    @Test
    fun `subtracts west declination from true heading`() {
        // New York, ~-13° declination (west). TRUE 90° → MAG 103°.
        assertThat(MagneticDeclination.trueToMagnetic(90f, -13f))
            .isWithin(0.001f).of(103f)
    }

    @Test
    fun `positive east declination decreases magnetic heading`() {
        // Sydney, ~+12° declination (east). TRUE 180° → MAG 168°.
        assertThat(MagneticDeclination.trueToMagnetic(180f, 12f))
            .isWithin(0.001f).of(168f)
    }

    @Test
    fun `wraps across 360 boundary`() {
        // TRUE 5°, declination +10° → -5° → 355°.
        assertThat(MagneticDeclination.trueToMagnetic(5f, 10f))
            .isWithin(0.001f).of(355f)
    }

    @Test
    fun `wraps when result exceeds 360`() {
        // TRUE 355°, declination -10° → 365° → 5°.
        assertThat(MagneticDeclination.trueToMagnetic(355f, -10f))
            .isWithin(0.001f).of(5f)
    }

    @Test
    fun `zero declination leaves heading unchanged`() {
        assertThat(MagneticDeclination.trueToMagnetic(123.4f, 0f))
            .isWithin(0.001f).of(123.4f)
    }
}
