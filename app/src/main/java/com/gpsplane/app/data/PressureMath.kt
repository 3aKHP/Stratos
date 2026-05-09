package com.gpsplane.app.data

import kotlin.math.pow

/**
 * International Standard Atmosphere conversions between pressure and altitude.
 * Valid within the troposphere (0–11 km); the lapse model breaks down above.
 *
 * Reference: ICAO Doc 7488, ISA 1976.
 */
object PressureMath {

    const val SEA_LEVEL_PRESSURE_HPA = 1013.25f
    const val SEA_LEVEL_TEMP_K = 288.15f
    const val LAPSE_RATE_K_PER_M = 0.0065f
    const val GAS_CONSTANT_SPECIFIC = 287.05f // J/(kg·K) for dry air
    const val GRAVITY_M_PER_S2 = 9.80665f

    private val EXPONENT_P_TO_H = (GAS_CONSTANT_SPECIFIC * LAPSE_RATE_K_PER_M / GRAVITY_M_PER_S2)
    private val EXPONENT_H_TO_P = (GRAVITY_M_PER_S2 / (GAS_CONSTANT_SPECIFIC * LAPSE_RATE_K_PER_M))

    /**
     * Pressure (hPa) → altitude (m) under ISA. Inverse of [altitudeToPressure].
     *
     * In a pressurised airliner cabin this reads as "cabin altitude" — the
     * equivalent outside-air altitude for the cabin's pressure (typically
     * 6,000–8,000 ft at cruise).
     */
    fun pressureToAltitude(pressureHpa: Float): Float {
        if (pressureHpa <= 0f) return Float.NaN
        val ratio = (pressureHpa / SEA_LEVEL_PRESSURE_HPA).toDouble().pow(EXPONENT_P_TO_H.toDouble())
        return (SEA_LEVEL_TEMP_K / LAPSE_RATE_K_PER_M * (1.0 - ratio)).toFloat()
    }

    /**
     * Altitude (m) → pressure (hPa) under ISA. Inverse of [pressureToAltitude].
     *
     * Given GPS altitude, this models the outside ambient ("static") pressure
     * the aircraft is flying through.
     */
    fun altitudeToPressure(altitudeM: Double): Float {
        val base = 1.0 - (LAPSE_RATE_K_PER_M * altitudeM) / SEA_LEVEL_TEMP_K
        if (base <= 0.0) return 0f
        return (SEA_LEVEL_PRESSURE_HPA * base.pow(EXPONENT_H_TO_P.toDouble())).toFloat()
    }
}
