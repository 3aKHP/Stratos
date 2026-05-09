package com.gpsplane.app.data.model

data class SatelliteInfo(
    val svid: Int,
    val constellationType: Int,
    val cn0DbHz: Float,
    val azimuth: Float,
    val elevation: Float,
    val usedInFix: Boolean,
    val hasEphemeris: Boolean,
    val hasAlmanac: Boolean
)

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMps: Float,
    val bearing: Float,
    val accuracyMeters: Float,
    val verticalSpeedMps: Float,
    val satelliteCount: Int,
    val gpsSatelliteCount: Int,
    val glonassSatelliteCount: Int,
    val beidouSatelliteCount: Int,
    val galileoSatelliteCount: Int,
    val qzssSatelliteCount: Int,
    val irnssSatelliteCount: Int,
    val otherSatelliteCount: Int,
    val satellites: List<SatelliteInfo>,
    val ttffSeconds: Float,
    val timestampMs: Long,
    val hasFix: Boolean
) {
    companion object {
        val EMPTY = GpsData(
            latitude = 0.0, longitude = 0.0,
            altitudeMeters = 0.0, speedMps = 0f, bearing = 0f,
            accuracyMeters = 0f, verticalSpeedMps = 0f,
            satelliteCount = 0,
            gpsSatelliteCount = 0, glonassSatelliteCount = 0,
            beidouSatelliteCount = 0, galileoSatelliteCount = 0,
            qzssSatelliteCount = 0, irnssSatelliteCount = 0,
            otherSatelliteCount = 0, satellites = emptyList(),
            ttffSeconds = -1f, timestampMs = 0L, hasFix = false
        )
    }
}
