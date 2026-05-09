package com.gpsplane.app.data.model

data class EnvironmentData(
    /**
     * Raw barometric pressure at the device, in hPa. NaN if the device has
     * no pressure sensor or hasn't produced a reading yet.
     */
    val cabinPressureHpa: Float,
    /**
     * Cabin altitude derived from [cabinPressureHpa] under ISA (m). In a
     * pressurised airliner this is the "equivalent outside altitude" that
     * the cabin feels like (typically ~1800–2400 m at cruise). NaN if no
     * pressure reading.
     */
    val cabinAltitudeM: Float,
    /** True if the device reported a barometer reading during this session. */
    val hasPressure: Boolean,
) {
    companion object {
        val EMPTY = EnvironmentData(
            cabinPressureHpa = Float.NaN,
            cabinAltitudeM = Float.NaN,
            hasPressure = false,
        )
    }
}
