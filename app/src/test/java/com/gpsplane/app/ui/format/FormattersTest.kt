package com.gpsplane.app.ui.format

import com.google.common.truth.Truth.assertThat
import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.FlightTimerState
import com.gpsplane.app.ui.format.SunTimeRef
import org.junit.Test

class FormattersTest {

    // ── fmtSpd ─────────────────────────────────────────────────────────

    @Test
    fun `fmtSpd knots rounds to integer knots`() {
        // 100 m/s → ~194 kn
        assertThat(fmtSpd(100f, SpeedUnit.KNOTS)).isEqualTo("194")
    }

    @Test
    fun `fmtSpd m per s keeps one decimal`() {
        assertThat(fmtSpd(3.14f, SpeedUnit.MPS)).isEqualTo("3.1")
    }

    @Test
    fun `fmtSpd zero is zero`() {
        assertThat(fmtSpd(0f, SpeedUnit.KMH)).isEqualTo("0")
    }

    // ── fmtAlt ─────────────────────────────────────────────────────────

    @Test
    fun `fmtAlt feet converts and rounds`() {
        // 1000 m → ~3281 ft
        assertThat(fmtAlt(1000.0, AltUnit.FEET)).isEqualTo("3281")
    }

    @Test
    fun `fmtAlt meters rounds to integer meters`() {
        assertThat(fmtAlt(1234.56, AltUnit.METERS)).isEqualTo("1235")
    }

    // ── fmtVS ──────────────────────────────────────────────────────────

    @Test
    fun `fmtVS includes explicit plus sign on climb`() {
        assertThat(fmtVS(5f, VSpeedUnit.M_S)).isEqualTo("+5.0")
    }

    @Test
    fun `fmtVS shows negative on descent`() {
        assertThat(fmtVS(-2.5f, VSpeedUnit.M_S)).isEqualTo("-2.5")
    }

    @Test
    fun `fmtVS ft per min rounds to integer`() {
        // 5 m/s → ~984 ft/min
        assertThat(fmtVS(5f, VSpeedUnit.FT_MIN)).isEqualTo("+984")
    }

    // ── fmtCoord ───────────────────────────────────────────────────────

    @Test
    fun `fmtCoord decimal has N or S suffix based on sign`() {
        val (lat, lon) = fmtCoord(39.9, 116.4, CoordFormat.DECIMAL)
        assertThat(lat).isEqualTo("39.900000° N")
        assertThat(lon).isEqualTo("116.400000° E")
    }

    @Test
    fun `fmtCoord decimal handles southern and western hemispheres`() {
        val (lat, lon) = fmtCoord(-33.86, -70.66, CoordFormat.DECIMAL)
        assertThat(lat).contains(" S")
        assertThat(lon).contains(" W")
        assertThat(lat).startsWith("-33.860000")
    }

    // ── headingToCardinal ──────────────────────────────────────────────

    @Test
    fun `headingToCardinal maps cardinal degrees to single letters`() {
        assertThat(headingToCardinal(0f)).isEqualTo("N")
        assertThat(headingToCardinal(90f)).isEqualTo("E")
        assertThat(headingToCardinal(180f)).isEqualTo("S")
        assertThat(headingToCardinal(270f)).isEqualTo("W")
    }

    @Test
    fun `headingToCardinal wraps near 360 to N`() {
        assertThat(headingToCardinal(359f)).isEqualTo("N")
        assertThat(headingToCardinal(350f)).isEqualTo("N")
    }

    @Test
    fun `headingToCardinal returns em dash for negatives`() {
        assertThat(headingToCardinal(-1f)).isEqualTo("—")
    }

    @Test
    fun `headingToCardinal picks intercardinal letters for boundaries`() {
        assertThat(headingToCardinal(45f)).isEqualTo("NE")
        assertThat(headingToCardinal(135f)).isEqualTo("SE")
        assertThat(headingToCardinal(225f)).isEqualTo("SW")
        assertThat(headingToCardinal(315f)).isEqualTo("NW")
    }

    // ── formatZulu ─────────────────────────────────────────────────────

    @Test
    fun `formatZulu shows UTC time of day with trailing Z`() {
        // 2026-05-10 10:32:07 UTC = 1778236327000 ms since epoch.
        assertThat(formatZulu(1778236327000L)).isEqualTo("10:32:07Z")
    }

    // ── formatFlightTime ───────────────────────────────────────────────

    @Test
    fun `formatFlightTime shows GROUND for grounded phase`() {
        val state = FlightTimerState(FlightPhase.GROUND, 0L)
        assertThat(formatFlightTime(state)).isEqualTo("GROUND")
    }

    @Test
    fun `formatFlightTime shows T-plus for airborne phase`() {
        // 1 hour 23 minutes 45 seconds in millis.
        val ms = ((1L * 3600 + 23 * 60 + 45) * 1000)
        val state = FlightTimerState(FlightPhase.AIRBORNE, ms)
        assertThat(formatFlightTime(state)).isEqualTo("T+01:23:45")
    }

    @Test
    fun `formatFlightTime zero-pads small values`() {
        val state = FlightTimerState(FlightPhase.AIRBORNE, 5_000L)
        assertThat(formatFlightTime(state)).isEqualTo("T+00:00:05")
    }

    // ── formatSunTimes ─────────────────────────────────────────────────

    @Test
    fun `formatSunTimes renders both times in HH mm Z`() {
        // 2026-06-21T09:23:00Z  and  2026-06-21T22:22:00Z
        val sunrise = 1_782_033_780_000L
        val sunset = 1_782_080_520_000L
        val times = com.gpsplane.app.data.SunTimes(sunriseUtcMs = sunrise, sunsetUtcMs = sunset)
        assertThat(formatSunTimes(times, SunTimeRef.UTC, 0.0)).isEqualTo("SR 09:23Z  SS 22:22Z")
    }

    @Test
    fun `formatSunTimes shows double-dash on polar night`() {
        val night = com.gpsplane.app.data.SunPositionNoaa.compute(latDeg = 80.0, lonDeg = 0.0, referenceUtcMs = 1_671_537_600_000L)
        assertThat(formatSunTimes(night, SunTimeRef.UTC, 0.0)).isEqualTo("SR --  SS --")
    }

    @Test
    fun `formatSunTimes shows double-plus on polar day`() {
        val day = com.gpsplane.app.data.SunPositionNoaa.compute(latDeg = 80.0, lonDeg = 0.0, referenceUtcMs = 1_655_769_600_000L)
        assertThat(formatSunTimes(day, SunTimeRef.UTC, 0.0)).isEqualTo("SR ++  SS ++")
    }

    // ── formatSolarTime ────────────────────────────────────────────────

    @Test fun `formatSolarTime returns valid HH MM SS`() {
        val ms = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear(); set(2026, 2, 20, 12, 0, 0) // March = 2
        }.timeInMillis
        val result = formatSolarTime(ms, 0.0)
        // Must match HH:MM:SS with valid ranges.
        val parts = result.split(":")
        assertThat(parts).hasSize(3)
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val s = parts[2].toInt()
        assertThat(h).isIn(0..23)
        assertThat(m).isIn(0..59)
        assertThat(s).isIn(0..59)
        // At Greenwich noon (EoT ~ -7.5 min on Mar 20), solar time ≈ 11:52:30.
        assertThat(h).isEqualTo(11)
        assertThat(m).isIn(48..56) // ±4 min tolerance
    }
}
