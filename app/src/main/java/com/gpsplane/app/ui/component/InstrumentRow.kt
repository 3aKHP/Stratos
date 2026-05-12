package com.gpsplane.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gpsplane.app.data.MagneticDeclination
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.ui.format.HeadingRef
import com.gpsplane.app.ui.format.UnitConfig
import com.gpsplane.app.ui.format.fmtAlt
import com.gpsplane.app.ui.format.fmtSpd
import com.gpsplane.app.ui.format.fmtVS
import com.gpsplane.app.ui.format.headingToCardinal

/**
 * Ground speed below which GPS bearing is considered stale and heading
 * widgets should drop their TRK-UP / TRACK rendering. Android's
 * GPS_PROVIDER keeps [android.location.Location.bearing] at its last
 * in-motion value once the device stops moving, so the threshold also
 * gates the TRACK cell (see [PrimaryInstrumentRow]) and SkyPlot's
 * TRK-UP rotation (see GpsScreen).
 */
internal const val MOVING_THRESHOLD_MPS = 1.5f

/**
 * Six-column light metric row: MACH | PITCH | ROLL | ACC | LOAD | TURN.
 * [onLabelClick], if non-null, is invoked when a cell with a matching
 * label is tapped — used today to pop the G-meter min/max bubble on
 * LOAD; ignored for cells whose labels don't match.
 */
@Composable
internal fun LightMetricRow(
    vararg cols: Pair<String, String>,
    onLabelClick: ((String) -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        cols.forEach { (label, value) ->
            val cellModifier = Modifier
                .weight(1f)
                .let { m ->
                    if (onLabelClick != null) m.clickable { onLabelClick(label) } else m
                }
            Column(
                modifier = cellModifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    maxLines = 1, softWrap = false)
                Text(value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * Four-cell primary instrument row: SPEED | ALTITUDE | VERT SPD | TRACK.
 * Each cell shows a dual-unit readout (primary above, secondary below).
 * TRACK reflects the selected heading reference (MAG or TRUE).
 */
@Composable
internal fun PrimaryInstrumentRow(
    gpsData: GpsData,
    attData: AttitudeData,
    unitConfig: UnitConfig,
    declinationDeg: Float,
) {
    // GPS track is always true-north; convert only when MAG is selected.
    // Android's GPS_PROVIDER leaves Location.bearing stuck at the last
    // in-motion value once the device stops moving, so gate on the same
    // threshold SkyPlot uses to drop TRK-UP.
    val hasTrack = gpsData.bearing >= 0f && gpsData.speedMps >= MOVING_THRESHOLD_MPS
    val shownBearing = if (!hasTrack) -1f
        else when (unitConfig.headingRef) {
            HeadingRef.TRUE -> gpsData.bearing
            HeadingRef.MAG -> MagneticDeclination.trueToMagnetic(gpsData.bearing, declinationDeg)
        }
    val bearingStr = if (shownBearing < 0) "—" else "%.0f°".format(shownBearing)
    val cardinalStr = if (shownBearing < 0) "" else headingToCardinal(shownBearing)
    val azimuth = attData.azimuth.takeIf { attData.hasAzimuth }

    Row(modifier = Modifier.fillMaxWidth()) {
        InstrumentCell(
            label = "SPEED",
            v1 = fmtSpd(gpsData.speedMps, unitConfig.speed1),
            u1 = unitConfig.speed1.label,
            v2 = fmtSpd(gpsData.speedMps, unitConfig.speed2),
            u2 = unitConfig.speed2.label,
            modifier = Modifier.weight(1f),
        )
        InstrumentCell(
            label = "ALTITUDE",
            v1 = fmtAlt(gpsData.altitudeMeters, unitConfig.alt1),
            u1 = unitConfig.alt1.label,
            v2 = fmtAlt(gpsData.altitudeMeters, unitConfig.alt2),
            u2 = unitConfig.alt2.label,
            modifier = Modifier.weight(1f),
        )
        InstrumentCell(
            label = "VERT SPD",
            v1 = fmtVS(gpsData.verticalSpeedMps, unitConfig.vs1),
            u1 = unitConfig.vs1.label,
            v2 = fmtVS(gpsData.verticalSpeedMps, unitConfig.vs2),
            u2 = unitConfig.vs2.label,
            modifier = Modifier.weight(1f),
        )
        InstrumentCell(
            label = "TRACK·${unitConfig.headingRef.label}",
            v1 = bearingStr,
            u1 = cardinalStr,
            v2 = azimuth?.let { "%.0f°".format(it) } ?: "",
            u2 = if (azimuth != null) "compass" else "",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InstrumentCell(
    label: String, v1: String, u1: String, v2: String, u2: String, modifier: Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines = 1, softWrap = false)
        Text(v1,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            maxLines = 1, softWrap = false)
        Text(u1,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1, softWrap = false)
        Text(v2,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1, softWrap = false)
        Text(u2,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            maxLines = 1, softWrap = false)
    }
}
