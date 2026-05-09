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
    fun `azimuth is flipped and wrapped to 0-360`() {
        // SensorManager.getOrientation returns azimuth CCW-positive. A device
        // pointing east (compass 90°) yields azimuth = -π/2. After flipping
        // and wrapping we expect 90°.
        val east = AttitudeMath.orientationRadiansToDegrees(
            floatArrayOf((-Math.PI / 2).toFloat(), 0f, 0f)
        )
        assertThat(east.azimuth).isWithin(1e-3f).of(90f)

        // Device pointing west (compass 270°) → raw azimuth = π/2 → flip to
        // -90° → wrap to 270°.
        val west = AttitudeMath.orientationRadiansToDegrees(
            floatArrayOf((Math.PI / 2).toFloat(), 0f, 0f)
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
    fun `linear acceleration magnitude converts to g`() {
        // 9.81 m/s² along a single axis == exactly 1g.
        assertThat(AttitudeMath.linearAccelerationToG(9.81f, 0f, 0f)).isWithin(1e-4f).of(1f)
        // 3-4-5 triangle scaled: (3,4,0) has magnitude 5.
        assertThat(AttitudeMath.linearAccelerationToG(3f, 4f, 0f))
            .isWithin(1e-4f).of(5f / AttitudeMath.STANDARD_GRAVITY_MPS2)
        // All-zero is 0g.
        assertThat(AttitudeMath.linearAccelerationToG(0f, 0f, 0f)).isEqualTo(0f)
    }
}
