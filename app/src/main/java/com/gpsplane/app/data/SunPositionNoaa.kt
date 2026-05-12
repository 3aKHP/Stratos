package com.gpsplane.app.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pair of sunrise/sunset times for a given date and position.
 * Times are Unix epoch milliseconds (UTC). Either can be `null` in a
 * polar night (sun never rises) or polar day (sun never sets) at the
 * given latitude and date.
 */
data class SunTimes(val sunriseUtcMs: Long?, val sunsetUtcMs: Long?) {
    val isPolarDay: Boolean get() = sunriseUtcMs == null && sunsetUtcMs == null && polarCase == PolarCase.DAY
    val isPolarNight: Boolean get() = sunriseUtcMs == null && sunsetUtcMs == null && polarCase == PolarCase.NIGHT

    internal var polarCase: PolarCase = PolarCase.NORMAL

    internal enum class PolarCase { NORMAL, DAY, NIGHT }

    companion object {
        val UNKNOWN = SunTimes(null, null).apply { polarCase = PolarCase.NORMAL }
        internal fun polarDay() = SunTimes(null, null).apply { polarCase = PolarCase.DAY }
        internal fun polarNight() = SunTimes(null, null).apply { polarCase = PolarCase.NIGHT }
    }
}

/**
 * NOAA solar position algorithm (the version published on
 * `gml.noaa.gov/grad/solcalc`). Pure Kotlin; no Android, no clocks.
 * Returns UTC sunrise/sunset timestamps for the local UTC date of the
 * reference instant at the given lat/lon. Accuracy is ~1 minute for
 * non-polar latitudes; adequate for "when does the sun go below the
 * horizon out the window" use.
 *
 * Zenith angle convention: the 90.833° civil refraction correction is
 * baked in (matches NOAA's online calculator).
 */
object SunPositionNoaa {

    private const val ZENITH_DEG = 90.833

    fun compute(latDeg: Double, lonDeg: Double, referenceUtcMs: Long): SunTimes {
        // Reduce the reference instant to the UTC calendar date.
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = referenceUtcMs
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val startOfDayMs = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(year, month - 1, day, 0, 0, 0)
        }.timeInMillis

        val sunriseEvent = computeEvent(latDeg, lonDeg, year, month, day, rising = true)
        val sunsetEvent = computeEvent(latDeg, lonDeg, year, month, day, rising = false)

        return when {
            sunriseEvent is EventResult.Never && sunsetEvent is EventResult.Never ->
                if (sunIsUpAtNoon(latDeg, lonDeg, year, month, day))
                    SunTimes.polarDay()
                else
                    SunTimes.polarNight()

            else -> {
                val sunriseMs = (sunriseEvent as? EventResult.Ok)?.let {
                    startOfDayMs + (it.utcHours * 3_600_000.0).toLong()
                }
                // Sunset can fall on the NEXT UTC day when the observer's
                // local day straddles the UTC boundary (e.g. US East Coast,
                // where sunset in UTC is around 00:30 the following day).
                // Detect that by checking whether the raw hour is earlier
                // than sunrise's, and advance to the next UTC midnight.
                val sunsetMs = (sunsetEvent as? EventResult.Ok)?.let {
                    val base = startOfDayMs + (it.utcHours * 3_600_000.0).toLong()
                    if (sunriseMs != null && base < sunriseMs) base + 86_400_000L else base
                }
                SunTimes(sunriseMs, sunsetMs)
            }
        }
    }

    private sealed interface EventResult {
        data class Ok(val utcHours: Double) : EventResult
        data object Never : EventResult
    }

    private fun computeEvent(
        latDeg: Double, lonDeg: Double,
        year: Int, month: Int, day: Int,
        rising: Boolean,
    ): EventResult {
        // Day of year N.
        val n = dayOfYear(year, month, day).toDouble()

        // Approximate time.
        val lngHour = lonDeg / 15.0
        val t = if (rising) n + (6 - lngHour) / 24.0 else n + (18 - lngHour) / 24.0

        // Sun's mean anomaly.
        val m = 0.9856 * t - 3.289

        // Sun's true longitude.
        var l = m + 1.916 * sin(Math.toRadians(m)) +
            0.020 * sin(Math.toRadians(2 * m)) + 282.634
        l = wrapDeg(l)

        // Right ascension (quadrant adjusted).
        val tanRA = 0.91764 * tan(Math.toRadians(l))
        var ra = Math.toDegrees(Math.atan(tanRA))
        ra = wrapDeg(ra)
        val lQuadrant = floor(l / 90.0) * 90.0
        val raQuadrant = floor(ra / 90.0) * 90.0
        ra = ra + (lQuadrant - raQuadrant)
        val raHours = ra / 15.0

        // Sun's declination.
        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(Math.asin(sinDec))

        // Hour angle.
        val cosH = (cos(Math.toRadians(ZENITH_DEG)) -
            sinDec * sin(Math.toRadians(latDeg))) /
            (cosDec * cos(Math.toRadians(latDeg)))
        if (cosH > 1.0 || cosH < -1.0) return EventResult.Never

        var h = Math.toDegrees(acos(cosH))
        if (rising) h = 360.0 - h
        val hHours = h / 15.0

        // Local mean time of rising/setting.
        val tEvent = hHours + raHours - 0.06571 * t - 6.622

        // Convert to UTC.
        var utc = tEvent - lngHour
        utc = ((utc % 24.0) + 24.0) % 24.0
        return EventResult.Ok(utc)
    }

    private fun sunIsUpAtNoon(
        latDeg: Double, lonDeg: Double,
        year: Int, month: Int, day: Int,
    ): Boolean {
        val n = dayOfYear(year, month, day).toDouble()
        val m = 0.9856 * (n + 0.5 - lonDeg / 360.0) - 3.289
        var l = m + 1.916 * sin(Math.toRadians(m)) +
            0.020 * sin(Math.toRadians(2 * m)) + 282.634
        l = wrapDeg(l)
        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val decDeg = Math.toDegrees(Math.asin(sinDec))
        // Sun altitude at local solar noon = 90 - |lat - dec|.
        return (90.0 - kotlin.math.abs(latDeg - decDeg)) > 0.0
    }

    private fun wrapDeg(v: Double): Double = ((v % 360.0) + 360.0) % 360.0

    private fun dayOfYear(year: Int, month: Int, day: Int): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(year, month - 1, day)
        }
        return cal.get(Calendar.DAY_OF_YEAR)
    }
}
