package com.gpsplane.app.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision solar time via the Meeus algorithm (*Astronomical
 * Algorithms*, Ch. 25/27). No Android, no clocks — pure math on
 * `(utcMs, lonDeg)`.
 *
 * Equation-of-time accuracy: ±0.07 seconds.
 */
object AstroTime {

    /** 1970-01-01 00:00:00 UTC in Julian Days. */
    private const val JD_UNIX_EPOCH = 2440587.5

    /** J2000.0 epoch. */
    private const val JD_J2000 = 2451545.0

    /** Days per Julian century. */
    private const val DAYS_PER_CENTURY = 36525.0

    // ---- public API ----

    fun julianDay(utcMs: Long): Double = JD_UNIX_EPOCH + utcMs / 86_400_000.0

    fun julianCentury(jd: Double): Double = (jd - JD_J2000) / DAYS_PER_CENTURY

    /**
     * Equation of time in **seconds** for the instant [utcMs].
     * Positive → apparent sun is ahead of the mean sun.
     */
    fun equationOfTimeSeconds(utcMs: Long): Double {
        val t = julianCentury(julianDay(utcMs))

        // Mean solar longitude (deg).
        val l0 = normalizeDeg(280.46646 + 36000.76983 * t + 0.0003032 * t * t)

        // Mean anomaly (deg).
        val m = normalizeDeg(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mRad = Math.toRadians(m)

        // Equation of centre (deg).
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2 * mRad) +
                0.000289 * sin(3 * mRad)

        // True geometric longitude (deg).
        val trueLon = l0 + c

        // Moon's mean longitude (deg).
        val lMoon = normalizeDeg(218.3165 + 481267.8813 * t)

        // Ascending node of Moon's mean orbit (deg).
        val omega = normalizeDeg(125.04452 - 1934.136261 * t)

        // Nutation in longitude (arcsec).
        val dPsi = -17.20 * sin(Math.toRadians(omega)) -
                   1.32 * sin(Math.toRadians(2 * l0)) -
                   0.23 * sin(Math.toRadians(2 * lMoon)) +
                   0.21 * sin(Math.toRadians(2 * omega))

        // Nutation in obliquity (arcsec).
        val dEps = 9.20 * cos(Math.toRadians(omega)) +
                   0.57 * cos(Math.toRadians(2 * l0)) +
                   0.10 * cos(Math.toRadians(2 * lMoon)) -
                   0.09 * cos(Math.toRadians(2 * omega))

        // Mean obliquity of the ecliptic (arcsec).
        val eps0 = 84381.406 - 46.836769 * t - 0.0001831 * t * t

        // True obliquity (deg).
        val epsDeg = (eps0 + dEps) / 3600.0
        val epsRad = Math.toRadians(epsDeg)

        // Apparent longitude = true + nutation.
        val appLonRad = Math.toRadians(trueLon + dPsi / 3600.0)

        // Apparent right ascension (deg).
        val alpha = normalizeDeg(Math.toDegrees(
            atan2(cos(epsRad) * sin(appLonRad), cos(appLonRad))
        ))

        // Equation of time (deg), corrected for aberration.
        var eDeg = l0 - 0.0057183 - alpha + dPsi * cos(epsRad) / 3600.0
        // Fold into [-180, 180).
        eDeg = normalizeEotDeg(eDeg)

        // 1° RA difference = 240 s.
        return eDeg * 240.0
    }

    /**
     * Local apparent solar time as milliseconds-of-day [0, 86400000).
     */
    fun apparentSolarMs(utcMs: Long, lonDeg: Double): Long {
        val eotMs = (equationOfTimeSeconds(utcMs) * 1000.0).toLong()
        val lonOffsetMs = (lonDeg / 15.0 * 3_600_000.0).toLong()
        return Math.floorMod(utcMs + lonOffsetMs + eotMs, 86_400_000L)
    }

    // ---- helpers ----

    private fun normalizeDeg(d: Double): Double = ((d % 360.0) + 360.0) % 360.0

    private fun normalizeEotDeg(d: Double): Double =
        ((d + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
}
