package com.gpsplane.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.FlightTimer
import com.gpsplane.app.data.MagneticDeclination
import com.gpsplane.app.data.PressureMath
import com.gpsplane.app.ui.component.CompactSignalBars
import com.gpsplane.app.ui.component.LightMetricRow
import com.gpsplane.app.ui.component.PrimaryInstrumentRow
import com.gpsplane.app.ui.component.SkyPlot
import com.gpsplane.app.ui.component.constellationColor
import com.gpsplane.app.ui.component.constellationLabel
import com.gpsplane.app.ui.format.AltUnit
import com.gpsplane.app.ui.format.CoordFormat
import com.gpsplane.app.ui.format.HeadingRef
import com.gpsplane.app.ui.format.SpeedUnit
import com.gpsplane.app.ui.format.UnitConfig
import com.gpsplane.app.ui.format.VSpeedUnit
import com.gpsplane.app.ui.format.fmtAlt
import com.gpsplane.app.ui.format.fmtCoord
import com.gpsplane.app.ui.format.fmtSpd
import com.gpsplane.app.ui.format.fmtVS
import com.gpsplane.app.ui.format.formatFlightTime
import com.gpsplane.app.ui.format.formatZulu
import com.gpsplane.app.ui.format.headingToCardinal
import com.gpsplane.app.util.UnitConverter

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsScreen(
    gpsData: GpsData,
    attData: AttitudeData,
    envData: EnvironmentData,
    flightSnap: FlightTimer.Snapshot,
    declinationDeg: Float,
) {
    var unitConfig by remember { mutableStateOf(UnitConfig()) }
    var showConfig by remember { mutableStateOf(false) }

    if (showConfig) {
        UnitConfigSheet(
            config = unitConfig,
            onConfigChange = { unitConfig = it },
            onDismiss = { showConfig = false }
        )
    }

    if (!gpsData.hasFix) {
        WaitingState()
        return
    }

    // GPS samples already arrive ~1 Hz, so the flight timer and the bottom
    // clocks advance at the cadence users expect without a separate ticker.
    val nowMs = gpsData.timestampMs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar ──
        TopBar(
            gpsData = gpsData,
            flightState = FlightTimer.display(flightSnap, nowMs),
            onSettingsClick = { showConfig = true },
        )

        Spacer(Modifier.height(6.dp))

        // ── Sky plot ──
        val hasAzimuth = attData.hasAzimuth && !attData.azimuth.isNaN()
        val isMoving = gpsData.speedMps >= 1.5f
        // rotationDeg: world bearing placed at the top of the dial.
        // Moving → TRK-UP (top = GPS track)
        // Stationary + compass valid → HDG-UP (top = phone heading)
        // Stationary + no compass → NORTH-UP (0°) + "NO HEADING" degrade banner
        val rotationDeg = when {
            isMoving && gpsData.bearing >= 0f -> gpsData.bearing
            hasAzimuth -> attData.azimuth
            else -> 0f
        }
        // Fan means "phone pointing relative to the top of the dial".
        // - HDG-UP: phone heading IS the top → fan would always point up → useless.
        // - TRK-UP: fan shows the phone's angle vs the track → informative.
        // - NORTH-UP degrade: no azimuth to draw → hidden.
        val showOrientationFan = isMoving && hasAzimuth
        val showNoHeadingLabel = !isMoving && !hasAzimuth

        SkyPlot(
            satellites = gpsData.satellites,
            azimuth = attData.azimuth.takeIf { attData.hasAzimuth },
            pitch = attData.pitch,
            roll = attData.roll,
            rotationDeg = rotationDeg,
            declinationDeg = declinationDeg,
            showOrientationFan = showOrientationFan,
            showNoHeadingLabel = showNoHeadingLabel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        // ── Compact signal bars ──
        CompactSignalBars(gpsData.satellites)

        Spacer(Modifier.height(6.dp))

        // ── Mach | Pitch | Roll | Accel | G | Turn ──
        val mach = UnitConverter.mach(gpsData.speedMps, gpsData.altitudeMeters)
        LightMetricRow(
            "MACH" to "%.2f".format(mach),
            "PITCH" to "%+.1f°".format(attData.pitch),
            "ROLL" to "%+.1f°".format(attData.roll),
            "ACC" to "%.2fg".format(attData.accelerationG),
            "LOAD" to if (attData.loadFactorG.isNaN()) "—" else "%.2fg".format(attData.loadFactorG),
            "TURN" to if (attData.turnRateDegPerSec.isNaN()) "—"
                      else "%+.0f°/s".format(attData.turnRateDegPerSec),
        )

        Spacer(Modifier.height(6.dp))

        // ── Speed | Altitude | Vert Spd | Track ──
        PrimaryInstrumentRow(
            gpsData = gpsData,
            attData = attData,
            unitConfig = unitConfig,
            declinationDeg = declinationDeg,
        )

        Spacer(Modifier.height(6.dp))

        // ── Cabin (barometer) | Static (GPS-derived) ──
        BaroRow(gpsData = gpsData, envData = envData)

        Spacer(Modifier.height(6.dp))

        // ── Bottom: coordinates + accuracy + time ──
        BottomRow(gpsData, unitConfig)
    }
}

// ── Top bar ─────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    gpsData: GpsData,
    flightState: com.gpsplane.app.data.FlightTimerState,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (color, label) = when {
            gpsData.satelliteCount >= 4 -> MaterialTheme.colorScheme.primary to "3D Fix"
            gpsData.hasFix -> MaterialTheme.colorScheme.tertiary to "2D Fix"
            else -> MaterialTheme.colorScheme.error to "No Fix"
        }
        Text(
            "$label · ${gpsData.satelliteCount} sats",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1, softWrap = false,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatFlightTime(flightState),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = if (flightState.phase == FlightPhase.AIRBORNE)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 1, softWrap = false,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp))
        }
    }
}

// ── Barometer row ───────────────────────────────────────────────────────────

@Composable
private fun BaroRow(gpsData: GpsData, envData: EnvironmentData) {
    // Static side is always available (derived from GPS altitude via ISA).
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

// ── Bottom row ──────────────────────────────────────────────────────────────

@Composable
private fun BottomRow(gpsData: GpsData, uc: UnitConfig) {
    val (lat1, lon1) = fmtCoord(gpsData.latitude, gpsData.longitude, uc.coord1)
    val (lat2, lon2) = fmtCoord(gpsData.latitude, gpsData.longitude, uc.coord2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$lat1  $lon1",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            maxLines = 1)
        Text(
            "$lat2  $lon2",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            maxLines = 1)
        Text(
            "UTC %s   %s   ±%.1f m".format(
                formatZulu(gpsData.timestampMs),
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(gpsData.timestampMs)),
                gpsData.accuracyMeters),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            maxLines = 1)
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

// ── Waiting state ───────────────────────────────────────────────────────────

@Composable
private fun WaitingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.SatelliteAlt, contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Text("Waiting for GPS fix…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

// ── Unit config bottom sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitConfigSheet(
    config: UnitConfig,
    onConfigChange: (UnitConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            Text("Display Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            DualUnitRow("Ground Speed", SpeedUnit.entries, config.speed1, config.speed2, { it.label },
                { onConfigChange(config.copy(speed1 = it)) }, { onConfigChange(config.copy(speed2 = it)) })
            DualUnitRow("Altitude", AltUnit.entries, config.alt1, config.alt2, { it.label },
                { onConfigChange(config.copy(alt1 = it)) }, { onConfigChange(config.copy(alt2 = it)) })
            DualUnitRow("Vertical Speed", VSpeedUnit.entries, config.vs1, config.vs2, { it.label },
                { onConfigChange(config.copy(vs1 = it)) }, { onConfigChange(config.copy(vs2 = it)) })
            DualUnitRow("Coordinates", CoordFormat.entries, config.coord1, config.coord2, { it.label },
                { onConfigChange(config.copy(coord1 = it)) }, { onConfigChange(config.copy(coord2 = it)) })
            SingleUnitRow("Heading Reference", HeadingRef.entries, config.headingRef, { it.label },
                { onConfigChange(config.copy(headingRef = it)) })
        }
    }
}

@Composable
private fun <T> DualUnitRow(
    title: String, options: List<T>, sel1: T, sel2: T,
    labelFn: (T) -> String, onChange1: (T) -> Unit, onChange2: (T) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt == sel1 || opt == sel2,
                    onClick = { onChange2(sel1); onChange1(opt) },
                    label = { Text(labelFn(opt), fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun <T> SingleUnitRow(
    title: String, options: List<T>, selected: T,
    labelFn: (T) -> String, onChange: (T) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt == selected,
                    onClick = { onChange(opt) },
                    label = { Text(labelFn(opt), fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.height(32.dp))
            }
        }
    }
}
