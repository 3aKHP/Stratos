package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AstroTimeTest {

    private fun utcMs(
        year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0, second: Int = 0
    ): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(year, month - 1, day, hour, minute, second)
    }.timeInMillis

    @Test fun `J2000 point`() {
        // 2000-01-01 12:00:00 UTC = JD 2451545.0, JC 0.
        val ms = utcMs(2000, 1, 1, 12, 0, 0)
        assertThat(AstroTime.julianDay(ms)).isEqualTo(2451545.0)
        assertThat(AstroTime.julianCentury(2451545.0)).isEqualTo(0.0)
    }

    // The ±0.07 sec accuracy is a literature claim (Meeus Ch. 27).  These
    // tests verify the algorithm produces physically correct values on
    // well-known dates; year-to-year drift (~30 sec) and the slight
    // calendar-date mismatch mean sub-second tolerances are inappropriate
    // for date-based EoT spot checks.

    @Test fun `EoT near February minimum`() {
        val ms = utcMs(2026, 2, 11)
        val eot = AstroTime.equationOfTimeSeconds(ms)
        assertThat(eot).isLessThan(-840.0)
        assertThat(eot).isGreaterThan(-870.0)
    }

    @Test fun `EoT near November maximum`() {
        val ms = utcMs(2026, 11, 3)
        val eot = AstroTime.equationOfTimeSeconds(ms)
        assertThat(eot).isGreaterThan(970.0)
        assertThat(eot).isLessThan(1000.0)
    }

    @Test fun `EoT near April zero-crossing`() {
        val ms = utcMs(2026, 4, 16)
        assertThat(Math.abs(AstroTime.equationOfTimeSeconds(ms))).isLessThan(30.0)
    }

    @Test fun `EoT near June zero-crossing`() {
        val ms = utcMs(2026, 6, 13)
        assertThat(Math.abs(AstroTime.equationOfTimeSeconds(ms))).isLessThan(30.0)
    }

    @Test fun `EoT near September zero-crossing`() {
        val ms = utcMs(2026, 9, 1)
        assertThat(Math.abs(AstroTime.equationOfTimeSeconds(ms))).isLessThan(30.0)
    }

    @Test fun `EoT near December zero-crossing`() {
        val ms = utcMs(2026, 12, 25)
        assertThat(Math.abs(AstroTime.equationOfTimeSeconds(ms))).isLessThan(30.0)
    }

    @Test fun `solar time at Greenwich noon`() {
        val ms = utcMs(2026, 3, 20, 12, 0, 0)
        val solarMs = AstroTime.apparentSolarMs(ms, 0.0)
        val solarMin = solarMs / 60_000.0
        assertThat(solarMin).isWithin(20.0).of(720.0) // EoT ≈ -7.5 min → ~712
    }

    @Test fun `solar time at 15deg East`() {
        // UTC 11:00 + lon=15°E → mean local = 12:00; EoT ~ -7.5 min → ~11:52:30 apparent.
        val ms = utcMs(2026, 3, 20, 11, 0, 0)
        val solarMs = AstroTime.apparentSolarMs(ms, 15.0)
        val solarMin = solarMs / 60_000.0
        assertThat(solarMin).isWithin(20.0).of(720.0)
    }

    @Test fun `EoT changes slowly within a day`() {
        val ms1 = utcMs(2026, 6, 21, 0, 0, 0)
        val ms2 = utcMs(2026, 6, 21, 23, 59, 59)
        val diff = Math.abs(AstroTime.equationOfTimeSeconds(ms1) - AstroTime.equationOfTimeSeconds(ms2))
        assertThat(diff).isLessThan(30.0) // max ~30 sec change per day
    }

    @Test fun `solar time wraps correctly near midnight`() {
        // UTC 23:50 at lon=2.5°E → mean local ≈ 00:00.
        // EoT corrects to ~23:52 apparent. Either way, within 30 min of midnight.
        val ms = utcMs(2026, 3, 20, 23, 50, 0)
        val solarMs = AstroTime.apparentSolarMs(ms, 2.5)
        val solarHr = solarMs / 3_600_000.0
        val distFromMidnight = minOf(Math.abs(solarHr - 0.0), Math.abs(solarHr - 24.0))
        assertThat(distFromMidnight).isLessThan(0.5) // 30 min
    }
}
