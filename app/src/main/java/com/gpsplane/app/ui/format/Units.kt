package com.gpsplane.app.ui.format

/**
 * Unit and formatting choices exposed through the dashboard settings
 * sheet. All composable-facing code in `ui/` refers to these types;
 * nothing in `data/` should depend on them.
 */

internal enum class SpeedUnit(val label: String) {
    KNOTS("kn"), KMH("km/h"), MPH("mph"), MPS("m/s")
}

internal enum class AltUnit(val label: String) {
    FEET("ft"), METERS("m")
}

internal enum class VSpeedUnit(val label: String) {
    FT_MIN("ft/min"), M_S("m/s")
}

internal enum class CoordFormat(val label: String) {
    DECIMAL("Dec"), DMS("DMS")
}

internal enum class HeadingRef(val label: String) {
    MAG("MAG"), TRUE("TRUE")
}

internal data class UnitConfig(
    val speed1: SpeedUnit = SpeedUnit.KNOTS,
    val speed2: SpeedUnit = SpeedUnit.KMH,
    val alt1: AltUnit = AltUnit.FEET,
    val alt2: AltUnit = AltUnit.METERS,
    val vs1: VSpeedUnit = VSpeedUnit.FT_MIN,
    val vs2: VSpeedUnit = VSpeedUnit.M_S,
    val coord1: CoordFormat = CoordFormat.DECIMAL,
    val coord2: CoordFormat = CoordFormat.DMS,
    val headingRef: HeadingRef = HeadingRef.MAG,
)
