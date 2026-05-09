package com.gpsplane.app.util

import kotlin.math.roundToInt

/**
 * Stateless unit conversions. All conversion factors are defined once here.
 */
object UnitConverter {

    // Distance
    fun metersToFeet(m: Double): Double = m * 3.28084
    fun feetToMeters(ft: Double): Double = ft / 3.28084

    // Speed
    fun mpsToKmh(mps: Float): Float = mps * 3.6f
    fun mpsToKnots(mps: Float): Float = mps * 1.94384f
    fun mpsToMph(mps: Float): Float = mps * 2.23694f

    // Vertical speed — aviation standard is ft/min
    fun mpsToFtMin(mps: Float): Float = mps * 196.8504f

    // Pressure
    fun hpaToInHg(hpa: Float): Float = hpa * 0.02953f

    // Coordinates
    fun decimalToDms(decimal: Double): String {
        val abs = kotlin.math.abs(decimal)
        val d = abs.toInt()
        val m = ((abs - d) * 60).toInt()
        val s = ((abs - d - m / 60.0) * 3600)
        val dir = if (decimal >= 0) "" else "-"
        return "${dir}${d}°${m}'${s.roundToInt()}\""
    }

    // Mach
    fun soundSpeedMs(altitudeM: Double): Double {
        // ISA standard atmosphere: T = T0 - L·h
        val tempK = 288.15 - 0.0065 * altitudeM
        return 340.3 * kotlin.math.sqrt(tempK / 288.15)
    }

    fun mach(speedMps: Float, altitudeM: Double): Double {
        val c = soundSpeedMs(altitudeM)
        return if (c > 0) speedMps / c else 0.0
    }
}
