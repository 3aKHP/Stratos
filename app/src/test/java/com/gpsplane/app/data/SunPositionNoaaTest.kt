package com.gpsplane.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SunPositionNoaaTest {

    private fun utcMs(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    private fun hoursUtc(ms: Long?): Double {
        requireNotNull(ms)
        val msOfDay = ms % 86_400_000L
        return msOfDay / 3_600_000.0
    }

    @Test fun `sunrise is earlier than sunset on a normal day`() {
        val times = SunPositionNoaa.compute(latDeg = 40.0, lonDeg = -74.0, referenceUtcMs = utcMs(2026, 6, 21))
        assertThat(times.sunriseUtcMs).isNotNull()
        assertThat(times.sunsetUtcMs).isNotNull()
        assertThat(times.sunriseUtcMs!!).isLessThan(times.sunsetUtcMs!!)
    }

    @Test fun `equator at equinox is close to 12 hour day`() {
        val times = SunPositionNoaa.compute(latDeg = 0.0, lonDeg = 0.0, referenceUtcMs = utcMs(2026, 3, 20))
        val dayLengthHours = (times.sunsetUtcMs!! - times.sunriseUtcMs!!) / 3_600_000.0
        assertThat(dayLengthHours).isWithin(0.2).of(12.0)
    }

    @Test fun `equator at equinox sunrise near 06Z sunset near 18Z`() {
        val times = SunPositionNoaa.compute(latDeg = 0.0, lonDeg = 0.0, referenceUtcMs = utcMs(2026, 3, 20))
        assertThat(hoursUtc(times.sunriseUtcMs)).isWithin(0.2).of(6.0)
        assertThat(hoursUtc(times.sunsetUtcMs)).isWithin(0.2).of(18.0)
    }

    @Test fun `high Arctic in June returns polar day`() {
        val times = SunPositionNoaa.compute(latDeg = 80.0, lonDeg = 0.0, referenceUtcMs = utcMs(2026, 6, 21))
        assertThat(times.sunriseUtcMs).isNull()
        assertThat(times.sunsetUtcMs).isNull()
        assertThat(times.isPolarDay).isTrue()
        assertThat(times.isPolarNight).isFalse()
    }

    @Test fun `high Arctic in December returns polar night`() {
        val times = SunPositionNoaa.compute(latDeg = 80.0, lonDeg = 0.0, referenceUtcMs = utcMs(2026, 12, 21))
        assertThat(times.sunriseUtcMs).isNull()
        assertThat(times.sunsetUtcMs).isNull()
        assertThat(times.isPolarNight).isTrue()
        assertThat(times.isPolarDay).isFalse()
    }

    @Test fun `Antarctic in June returns polar night`() {
        val times = SunPositionNoaa.compute(latDeg = -80.0, lonDeg = 0.0, referenceUtcMs = utcMs(2026, 6, 21))
        assertThat(times.isPolarNight).isTrue()
    }

    @Test fun `Antarctic in December returns polar day`() {
        val times = SunPositionNoaa.compute(latDeg = -80.0, lonDeg = 0.0, referenceUtcMs = utcMs(2026, 12, 21))
        assertThat(times.isPolarDay).isTrue()
    }

    @Test fun `northern summer day is longer than winter day at mid-latitudes`() {
        val june = SunPositionNoaa.compute(latDeg = 51.5, lonDeg = -0.1, referenceUtcMs = utcMs(2026, 6, 21))
        val december = SunPositionNoaa.compute(latDeg = 51.5, lonDeg = -0.1, referenceUtcMs = utcMs(2026, 12, 21))
        val juneLen = (june.sunsetUtcMs!! - june.sunriseUtcMs!!) / 3_600_000.0
        val decLen = (december.sunsetUtcMs!! - december.sunriseUtcMs!!) / 3_600_000.0
        assertThat(juneLen).isGreaterThan(decLen + 5.0) // ~16.5 h vs ~8 h
    }

    @Test fun `London June 21 sunrise near 03 43Z sunset near 20 21Z`() {
        // Reference: NOAA solcalc, London (51.5, -0.1), 2026-06-21 (UTC).
        val times = SunPositionNoaa.compute(latDeg = 51.5, lonDeg = -0.1, referenceUtcMs = utcMs(2026, 6, 21))
        assertThat(hoursUtc(times.sunriseUtcMs)).isWithin(0.1).of(3.72) // 03:43Z
        assertThat(hoursUtc(times.sunsetUtcMs)).isWithin(0.1).of(20.35) // 20:21Z
    }

    @Test fun `east and west of same meridian give close times`() {
        val east = SunPositionNoaa.compute(latDeg = 0.0, lonDeg = 1.0, referenceUtcMs = utcMs(2026, 3, 20))
        val west = SunPositionNoaa.compute(latDeg = 0.0, lonDeg = -1.0, referenceUtcMs = utcMs(2026, 3, 20))
        // 1° east fires ~4 min earlier.
        val eastHrs = hoursUtc(east.sunriseUtcMs)
        val westHrs = hoursUtc(west.sunriseUtcMs)
        assertThat(westHrs - eastHrs).isWithin(0.05).of(8.0 / 60.0) // 2° × 4 min
    }

    @Test fun `output sunrise is on the same UTC date as reference`() {
        val ref = utcMs(2026, 6, 21, 12)
        val times = SunPositionNoaa.compute(latDeg = 40.0, lonDeg = -74.0, referenceUtcMs = ref)
        // Sunrise should be within the UTC day of 2026-06-21, not the previous one.
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = times.sunriseUtcMs!! }
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(cal.get(Calendar.MONTH) + 1).isEqualTo(6)
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(21)
    }
}
