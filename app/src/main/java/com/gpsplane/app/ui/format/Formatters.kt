package com.gpsplane.app.ui.format

import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.FlightTimerState
import com.gpsplane.app.data.SunTimes
import com.gpsplane.app.util.UnitConverter

/**
 * Pure formatting helpers used by the dashboard composables. All
 * functions are side-effect free and JVM-testable; see
 * `FormattersTest` for the coverage that pins their output.
 */

internal fun fmtSpd(mps: Float, u: SpeedUnit): String = when (u) {
    SpeedUnit.KNOTS -> "%.0f".format(UnitConverter.mpsToKnots(mps))
    SpeedUnit.KMH -> "%.0f".format(UnitConverter.mpsToKmh(mps))
    SpeedUnit.MPH -> "%.0f".format(UnitConverter.mpsToMph(mps))
    SpeedUnit.MPS -> "%.1f".format(mps)
}

internal fun fmtAlt(m: Double, u: AltUnit): String = when (u) {
    AltUnit.FEET -> "%.0f".format(UnitConverter.metersToFeet(m))
    AltUnit.METERS -> "%.0f".format(m)
}

internal fun fmtVS(mps: Float, u: VSpeedUnit): String = when (u) {
    VSpeedUnit.FT_MIN -> "%+.0f".format(UnitConverter.mpsToFtMin(mps))
    VSpeedUnit.M_S -> "%+.1f".format(mps)
}

internal fun fmtCoord(
    lat: Double, lon: Double, f: CoordFormat
): Pair<String, String> = when (f) {
    CoordFormat.DECIMAL -> Pair(
        "%.6f° %s".format(lat, if (lat >= 0) "N" else "S"),
        "%.6f° %s".format(lon, if (lon >= 0) "E" else "W"))
    CoordFormat.DMS -> Pair(
        UnitConverter.decimalToDms(lat) + if (lat >= 0) " N" else " S",
        UnitConverter.decimalToDms(lon) + if (lon >= 0) " E" else " W")
}

/** 16-point compass-rose letter for a bearing. Returns "—" for negatives. */
internal fun headingToCardinal(h: Float): String = when {
    h < 0 -> "—"
    h < 11.25 || h >= 348.75 -> "N"
    h < 33.75 -> "NNE"; h < 56.25 -> "NE"; h < 78.75 -> "ENE"
    h < 101.25 -> "E"; h < 123.75 -> "ESE"; h < 146.25 -> "SE"
    h < 168.75 -> "SSE"; h < 191.25 -> "S"; h < 213.75 -> "SSW"
    h < 236.25 -> "SW"; h < 258.75 -> "WSW"; h < 281.25 -> "W"
    h < 303.75 -> "WNW"; h < 326.25 -> "NW"; else -> "NNW"
}

private val zuluFormatter by lazy {
    java.text.SimpleDateFormat("HH:mm:ss'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
}

/** UTC time-of-day formatted as `HH:mm:ssZ`. */
internal fun formatZulu(ms: Long): String = zuluFormatter.format(java.util.Date(ms))

private val localFormatter by lazy {
    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
}

/** Local civil time (phone timezone) formatted as `HH:mm:ss`. */
internal fun formatLocalTime(ms: Long): String = localFormatter.format(java.util.Date(ms))

/**
 * Local apparent solar time formatted as `HH:mm:ss`, derived purely
 * from GPS UTC time and longitude — no phone timezone involved.
 */
internal fun formatSolarTime(utcMs: Long, lonDeg: Double): String {
    val msOfDay = com.gpsplane.app.data.AstroTime.apparentSolarMs(utcMs, lonDeg)
    val s = (msOfDay / 1000) % 60
    val m = (msOfDay / 60000) % 60
    val h = (msOfDay / 3600000) % 24
    return "%02d:%02d:%02d".format(h, m, s)
}

/** `GROUND` on the ground; `T+HH:MM:SS` in flight. */
internal fun formatFlightTime(state: FlightTimerState): String {
    if (state.phase == FlightPhase.GROUND) return "GROUND"
    val totalSec = state.elapsedMs / 1000L
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "T+%02d:%02d:%02d".format(h, m, s)
}

private val utcHhmmFormatter by lazy {
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
}

private val localHhmmFormatter by lazy {
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
}

/**
 * "SR HH:MMZ  SS HH:MMZ" — sunrise/sunset. The time suffix depends on
 * [ref]: `Z` for UTC, none for Solar or Local.
 * "SR --  SS --" on polar night, "SR ++  SS ++" on polar day.
 */
internal fun formatSunTimes(times: SunTimes, ref: SunTimeRef, lonDeg: Double): String {
    if (times.isPolarDay) return "SR ++  SS ++"
    if (times.isPolarNight) return "SR --  SS --"

    fun fmt(utcMs: Long): String = when (ref) {
        SunTimeRef.UTC -> utcHhmmFormatter.format(java.util.Date(utcMs)) + "Z"
        SunTimeRef.LOCAL -> localHhmmFormatter.format(java.util.Date(utcMs))
        SunTimeRef.SOLAR -> {
            val msOfDay = com.gpsplane.app.data.AstroTime.apparentSolarMs(utcMs, lonDeg)
            "%02d:%02d".format((msOfDay / 3_600_000) % 24, (msOfDay / 60_000) % 60)
        }
    }

    val sr = times.sunriseUtcMs?.let { fmt(it) } ?: "--"
    val ss = times.sunsetUtcMs?.let { fmt(it) } ?: "--"
    return "SR $sr  SS $ss"
}
