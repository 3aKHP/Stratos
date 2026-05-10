package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Hard-coded ground-truth tests for sky plot angle math.
 *
 * These assertions do NOT follow from a derivation — they ARE the
 * specification. If the implementation produces different values, the
 * implementation is wrong. Every test below corresponds to a mental
 * picture any aviation user would agree on:
 *
 *  - HDG-UP dial, phone pointing east. Top-of-dial = east, so TRUE
 *    north (up in the world) must appear on the LEFT of the plot.
 *  - HDG-UP dial, phone pointing west. TRUE north must appear on the
 *    RIGHT.
 *
 * Output coordinates are normalized screen offsets from the plot
 * centre: (x=+1, y=0) = right, (x=0, y=-1) = top, (x=-1, y=0) = left,
 * (x=0, y=+1) = bottom. This matches screen space where +y is DOWN.
 */
class SkyProjectionTest {

    private fun assertOffset(
        actual: Pair<Float, Float>,
        expectedX: Float,
        expectedY: Float,
    ) {
        assertThat(actual.first).isWithin(1e-4f).of(expectedX)
        assertThat(actual.second).isWithin(1e-4f).of(expectedY)
    }

    // ── No rotation (NORTH-UP) ──────────────────────────────────────────

    @Test
    fun `north up — a northern satellite is at the top`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 0f, rotationDeg = 0f),
            0f, -1f)
    }

    @Test
    fun `north up — an eastern satellite is on the right`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 90f, rotationDeg = 0f),
            1f, 0f)
    }

    @Test
    fun `north up — a southern satellite is at the bottom`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 180f, rotationDeg = 0f),
            0f, 1f)
    }

    @Test
    fun `north up — a western satellite is on the left`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 270f, rotationDeg = 0f),
            -1f, 0f)
    }

    // ── HDG-UP east (phone pointing east; top of dial is east) ─────────

    @Test
    fun `hdg-up east — world east is at the top`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 90f, rotationDeg = 90f),
            0f, -1f)
    }

    @Test
    fun `hdg-up east — true north is on the LEFT`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 0f, rotationDeg = 90f),
            -1f, 0f)
    }

    @Test
    fun `hdg-up east — world south is on the RIGHT`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 180f, rotationDeg = 90f),
            1f, 0f)
    }

    @Test
    fun `hdg-up east — world west is at the bottom`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 270f, rotationDeg = 90f),
            0f, 1f)
    }

    // ── HDG-UP west (phone pointing west; top of dial is west) ─────────

    @Test
    fun `hdg-up west — world west is at the top`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 270f, rotationDeg = 270f),
            0f, -1f)
    }

    @Test
    fun `hdg-up west — true north is on the RIGHT`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 0f, rotationDeg = 270f),
            1f, 0f)
    }

    // ── HDG-UP south (phone pointing south; top of dial is south) ──────

    @Test
    fun `hdg-up south — true north is at the BOTTOM`() {
        assertOffset(SkyProjection.projectOnRing(worldAzDeg = 0f, rotationDeg = 180f),
            0f, 1f)
    }

    // ── Elevation scaling ──────────────────────────────────────────────

    @Test
    fun `project with elevation — satellite at zenith is at the centre`() {
        val (x, y) = SkyProjection.projectWithElevation(
            worldAzDeg = 90f, elevationDeg = 90f, rotationDeg = 0f
        )
        assertThat(x).isWithin(1e-4f).of(0f)
        assertThat(y).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `project with elevation — satellite at horizon is at the ring`() {
        val (x, y) = SkyProjection.projectWithElevation(
            worldAzDeg = 90f, elevationDeg = 0f, rotationDeg = 0f
        )
        assertThat(x).isWithin(1e-4f).of(1f)
        assertThat(y).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `project with elevation — 45 degrees elevation is half-way out`() {
        val (x, y) = SkyProjection.projectWithElevation(
            worldAzDeg = 90f, elevationDeg = 45f, rotationDeg = 0f
        )
        assertThat(x).isWithin(1e-4f).of(0.5f)
        assertThat(y).isWithin(1e-4f).of(0f)
    }
}
