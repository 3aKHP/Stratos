package com.gpsplane.app.ui.component

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
import com.gpsplane.app.data.PressureMath
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.util.UnitConverter

/**
 * Four-cell barometer row: CABIN ALT | STATIC ALT | CABIN P | STATIC P.
 * Cabin values come from the device's pressure sensor (shown as "—" /
 * "no baro" when absent); static values come from the GPS altitude via
 * the ISA standard atmosphere and are always available on a valid fix.
 */
@Composable
internal fun BaroRow(gpsData: GpsData, envData: EnvironmentData) {
    val staticPressureHpa = PressureMath.altitudeToPressure(gpsData.altitudeMeters)
    val staticAltFt = UnitConverter.metersToFeet(gpsData.altitudeMeters)

    val hasBaro = envData.hasPressure && !envData.cabinPressureHpa.isNaN()
    val cabinAltFt = if (hasBaro) UnitConverter.metersToFeet(envData.cabinAltitudeM.toDouble()) else null

    Row(modifier = Modifier.fillMaxWidth()) {
        BaroCell(
            label = "CABIN ALT",
            primary = if (cabinAltFt != null) "%.0f ft".format(cabinAltFt) else "—",
            secondary = if (hasBaro) "%.0f m".format(envData.cabinAltitudeM) else "no baro",
            modifier = Modifier.weight(1f),
        )
        BaroCell(
            label = "STATIC ALT",
            primary = "%.0f ft".format(staticAltFt),
            secondary = "%.0f m".format(gpsData.altitudeMeters),
            modifier = Modifier.weight(1f),
        )
        BaroCell(
            label = "CABIN P",
            primary = if (hasBaro) "%.1f hPa".format(envData.cabinPressureHpa) else "—",
            secondary = if (hasBaro) "%.2f inHg".format(UnitConverter.hpaToInHg(envData.cabinPressureHpa)) else "",
            modifier = Modifier.weight(1f),
        )
        BaroCell(
            label = "STATIC P",
            primary = "%.1f hPa".format(staticPressureHpa),
            secondary = "%.2f inHg".format(UnitConverter.hpaToInHg(staticPressureHpa)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BaroCell(label: String, primary: String, secondary: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines = 1, softWrap = false)
        Text(primary,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 1, softWrap = false)
        Text(secondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp,
                fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines = 1, softWrap = false)
    }
}
