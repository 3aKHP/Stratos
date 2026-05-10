package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AttitudeMathTest {

    @Test
    fun `orientation due north is zero azimuth`() {
        // azimuth=0 rad → 0° CW
        val e = AttitudeMath.orientationRadiansToDegrees(floatArrayOf(0f, 0f, 0f))
        assertThat(e.azimuth).isWithin(1e-4f).of(0f)
        assertThat(e.pitch).isWithin(1e-4f).of(0f)
        assertThat(e.roll).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `azimuth is converted to degrees and wrapped to 0-360`() {
        // SensorManager.getOrientation already returns a compass-style
        // azimuth (CW-positive, 0 = north). A device pointing east
        // yields +π/2 rad, which should surface as 90°.
        val east = AttitudeMath.orientationRadiansToDegrees(
            floatArrayOf((Math.PI / 2).toFloat(), 0f, 0f)
        )
        assertThat(east.azimuth).isWithin(1e-3f).of(90f)

        // Device pointing west: raw -π/2 → -90° → wrapped to 270°.
        val west = AttitudeMath.orientationRadiansToDegrees(
            floatArrayOf((-Math.PI / 2).toFloat(), 0f, 0f)
        )
        assertThat(west.azimuth).isWithin(1e-3f).of(270f)
    }

    @Test
    fun `pitch and roll pass through with sign preserved`() {
        val e = AttitudeMath.orientationRadiansToDegrees(
            floatArrayOf(0f, (Math.PI / 6).toFloat(), (-Math.PI / 4).toFloat())
        )
        assertThat(e.pitch).isWithin(1e-3f).of(30f)
        assertThat(e.roll).isWithin(1e-3f).of(-45f)
    }

    @Test
    fun `magnitude in g converts an acceleration vector`() {
        // 9.81 m/s² along a single axis == exactly 1g.
        assertThat(AttitudeMath.magnitudeInG(9.81f, 0f, 0f)).isWithin(1e-4f).of(1f)
        // 3-4-5 triangle scaled: (3,4,0) has magnitude 5.
        assertThat(AttitudeMath.magnitudeInG(3f, 4f, 0f))
            .isWithin(1e-4f).of(5f / AttitudeMath.STANDARD_GRAVITY_MPS2)
        // All-zero is 0g.
        assertThat(AttitudeMath.magnitudeInG(0f, 0f, 0f)).isEqualTo(0f)
    }

    @Test
    fun `gyro z to turn rate is sign-flipped and converted to degrees`() {
        // -1 rad/s raw (CW rotation viewed from screen-up) = right turn in
        // aviation convention → positive deg/s.
        assertThat(AttitudeMath.gyroZToTurnRateDegPerSec(-1f))
            .isWithin(1e-3f).of(Math.toDegrees(1.0).toFloat())
        // +1 rad/s raw (Android's CCW / aviation left-turn) → negative deg/s.
        assertThat(AttitudeMath.gyroZToTurnRateDegPerSec(1f))
            .isWithin(1e-3f).of(Math.toDegrees(-1.0).toFloat())
        // Zero rotation rate stays zero.
        assertThat(AttitudeMath.gyroZToTurnRateDegPerSec(0f)).isWithin(0f).of(0f)
    }
}
