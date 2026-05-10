package com.gpsplane.app.ui.screen

import android.location.GnssStatus
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
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
import com.gpsplane.app.data.model.SatelliteInfo
import com.gpsplane.app.util.UnitConverter
import kotlinx.coroutines.delay

// ── Unit enums ─────────────────────────────────────────────────────────────

private enum class SpeedUnit(val label: String) { KNOTS("kn"), KMH("km/h"), MPH("mph"), MPS("m/s") }
private enum class AltUnit(val label: String) { FEET("ft"), METERS("m") }
private enum class VSpeedUnit(val label: String) { FT_MIN("ft/min"), M_S("m/s") }
private enum class CoordFormat(val label: String) { DECIMAL("Dec"), DMS("DMS") }
private enum class HeadingRef(val label: String) { MAG("MAG"), TRUE("TRUE") }

private data class UnitConfig(
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

// ── Format helpers ─────────────────────────────────────────────────────────

private fun fmtSpd(mps: Float, u: SpeedUnit) = when (u) {
    SpeedUnit.KNOTS -> "%.0f".format(UnitConverter.mpsToKnots(mps))
    SpeedUnit.KMH -> "%.0f".format(UnitConverter.mpsToKmh(mps))
    SpeedUnit.MPH -> "%.0f".format(UnitConverter.mpsToMph(mps))
    SpeedUnit.MPS -> "%.1f".format(mps)
}

private fun fmtAlt(m: Double, u: AltUnit) = when (u) {
    AltUnit.FEET -> "%.0f".format(UnitConverter.metersToFeet(m))
    AltUnit.METERS -> "%.0f".format(m)
}

private fun fmtVS(mps: Float, u: VSpeedUnit) = when (u) {
    VSpeedUnit.FT_MIN -> "%+.0f".format(UnitConverter.mpsToFtMin(mps))
    VSpeedUnit.M_S -> "%+.1f".format(mps)
}

private fun fmtCoord(lat: Double, lon: Double, f: CoordFormat): Pair<String, String> = when (f) {
    CoordFormat.DECIMAL -> Pair(
        "%.6f° %s".format(lat, if (lat >= 0) "N" else "S"),
        "%.6f° %s".format(lon, if (lon >= 0) "E" else "W"))
    CoordFormat.DMS -> Pair(
        UnitConverter.decimalToDms(lat) + if (lat >= 0) " N" else " S",
        UnitConverter.decimalToDms(lon) + if (lon >= 0) " E" else " W")
}

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

    // Tick once per second so ZULU clock and airborne timer advance smoothly.
    // The sensor flows wake up more often than 1 Hz, but neither of those
    // readings needs sub-second resolution in the top bar.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

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
            nowMs = nowMs,
            onSettingsClick = { showConfig = true },
        )

        Spacer(Modifier.height(6.dp))

        // ── Sky plot ──
        SkyPlot(
            satellites = gpsData.satellites,
            bearing = gpsData.bearing.takeIf { gpsData.speedMps > 1f },
            azimuth = attData.azimuth.takeIf { attData.hasAzimuth },
            pitch = attData.pitch,
            roll = attData.roll,
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

        // ── Speed | Altitude ──
        InstrumentRow(
            labelL = "SPEED",
            valL1 = fmtSpd(gpsData.speedMps, unitConfig.speed1),
            unitL1 = unitConfig.speed1.label,
            valL2 = fmtSpd(gpsData.speedMps, unitConfig.speed2),
            unitL2 = unitConfig.speed2.label,
            labelR = "ALTITUDE",
            valR1 = fmtAlt(gpsData.altitudeMeters, unitConfig.alt1),
            unitR1 = unitConfig.alt1.label,
            valR2 = fmtAlt(gpsData.altitudeMeters, unitConfig.alt2),
            unitR2 = unitConfig.alt2.label,
        )

        Spacer(Modifier.height(6.dp))

        // ── V/S | Heading ──
        Row(modifier = Modifier.fillMaxWidth()) {
            // V/S (dual-unit, left)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VERT SPD",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                Text(fmtVS(gpsData.verticalSpeedMps, unitConfig.vs1),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                Text(unitConfig.vs1.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(fmtVS(gpsData.verticalSpeedMps, unitConfig.vs2),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(unitConfig.vs2.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
            // Heading (right, with phone azimuth as sub-unit)
            HeadingCell(
                modifier = Modifier.weight(1f),
                bearing = gpsData.bearing,
                phoneAzimuth = attData.azimuth.takeIf { attData.hasAzimuth },
                headingRef = unitConfig.headingRef,
                declinationDeg = declinationDeg,
            )
        }

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
    nowMs: Long,
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
            formatZulu(nowMs),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1, softWrap = false,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            formatFlightTime(flightState),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = if (flightState.phase == FlightPhase.AIRBORNE)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 1, softWrap = false,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp))
        }
    }
}

private val zuluFormatter by lazy {
    java.text.SimpleDateFormat("HH:mm:ss'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
}

private fun formatZulu(ms: Long): String = zuluFormatter.format(java.util.Date(ms))

private fun formatFlightTime(state: com.gpsplane.app.data.FlightTimerState): String {
    if (state.phase == FlightPhase.GROUND) return "GROUND"
    val totalSec = state.elapsedMs / 1000L
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "T+%02d:%02d:%02d".format(h, m, s)
}

// ── Sky plot ────────────────────────────────────────────────────────────────

@Composable
private fun SkyPlot(
    satellites: List<SatelliteInfo>,
    bearing: Float?,
    azimuth: Float?,
    pitch: Float,
    roll: Float,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = minOf(cx, cy) - 4.dp.toPx()

        // ── Fixed decorations ───────────────────────────────────────────

        // Background disk
        drawCircle(Color.White.copy(alpha = 0.08f), r, Offset(cx, cy))

        // Elevation rings: 30°, 60°
        for (el in listOf(0.33f, 0.66f)) {
            drawCircle(Color.White.copy(alpha = 0.14f), r * (1f - el), Offset(cx, cy))
        }

        // Outer ring
        drawCircle(Color.White.copy(alpha = 0.2f), r, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

        // Tick marks (every 5°) around perimeter
        // N at top (canvas 90°), so ticks go clockwise: 90°=N, 0°=E, 270°=S, 180°=W
        for (i in 0..71) {
            val deg = i * 5f
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val isCardinal = i % 18 == 0
            val len = if (isCardinal) 10.dp.toPx() else 5.dp.toPx()
            val outer = r
            val inner = outer - len
            val a = if (isCardinal) 0.45f else 0.2f
            drawLine(Color.White.copy(alpha = a),
                Offset(cx + outer * kotlin.math.cos(rad), cy - outer * kotlin.math.sin(rad)),
                Offset(cx + inner * kotlin.math.cos(rad), cy - inner * kotlin.math.sin(rad)),
                strokeWidth = if (isCardinal) 2f else 1f)
        }

        // Cardinal labels: N@top(90°), E@right(0°), S@bottom(270°), W@left(180°).
        // Paint.Align.CENTER handles horizontal centering; for vertical centering
        // we offset the baseline by -(ascent+descent)/2 so the glyph's visual
        // center sits on the target point.
        val cardinalPaint = android.graphics.Paint().apply {
            color = 0x99FFFFFF.toInt()
            textSize = 12.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        val baselineOffset = -(cardinalPaint.fontMetrics.ascent + cardinalPaint.fontMetrics.descent) / 2f
        val labels = mapOf(90f to "N", 0f to "E", 270f to "S", 180f to "W")
        for ((deg, label) in labels) {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val lr = r + 14.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(
                label,
                cx + lr * kotlin.math.cos(rad),
                cy - lr * kotlin.math.sin(rad) + baselineOffset,
                cardinalPaint
            )
        }

        // ── Artificial horizon (pitch / roll) ───────────────────────────

        if (!pitch.isNaN() && !roll.isNaN()) {
            val rollRad = Math.toRadians(roll.toDouble()).toFloat()
            val pitOffset = (pitch * r / 30f).coerceIn(-r * 0.55f, r * 0.55f)
            val hw = r * 0.38f
            val cosR = kotlin.math.cos(rollRad)
            val sinR = kotlin.math.sin(rollRad)
            val lineY = cy + pitOffset
            drawLine(
                Color.White.copy(alpha = 0.45f),
                Offset(cx - hw * cosR, lineY - hw * sinR),
                Offset(cx + hw * cosR, lineY + hw * sinR),
                strokeWidth = 2f
            )
            // Center crosshair mark
            val markLen = 6.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.35f),
                Offset(cx - markLen * cosR, lineY - markLen * sinR),
                Offset(cx + markLen * cosR, lineY + markLen * sinR),
                strokeWidth = 3f)
        }

        // ── Phone orientation fan ──────────────────────────────────────

        if (azimuth != null && !azimuth.isNaN()) {
            val azRad = Math.toRadians(azimuth.toDouble()).toFloat()
            val haRad = Math.toRadians(15.0).toFloat() // ±15° half-angle
            val fanRadius = r * 0.7f
            val fanRect = Rect(cx - fanRadius, cy - fanRadius, cx + fanRadius, cy + fanRadius)
            // arcTo uses canvas angles: 0°=right, clockwise downward
            // Compass az → canvas angle: startAngle = az - 90 - 15
            val startAngleDeg = azimuth - 15f - 90f
            val fanPath = Path().apply {
                moveTo(cx, cy)
                arcTo(fanRect, startAngleDeg, 30f, false)
                close()
            }
            drawPath(fanPath, Color.White.copy(alpha = 0.12f))
            // Thin edge lines for the fan
            drawLine(Color.White.copy(alpha = 0.25f), Offset(cx, cy),
                Offset(cx + fanRadius * kotlin.math.sin(azRad - haRad),
                       cy - fanRadius * kotlin.math.cos(azRad - haRad)), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.25f), Offset(cx, cy),
                Offset(cx + fanRadius * kotlin.math.sin(azRad + haRad),
                       cy - fanRadius * kotlin.math.cos(azRad + haRad)), strokeWidth = 1f)
        }

        // ── Airplane bearing needle (from center) ────────────────────────

        if (bearing != null && bearing >= 0) {
            val brRad = Math.toRadians(bearing.toDouble()).toFloat()
            val sinB = kotlin.math.sin(brRad)
            val cosB = kotlin.math.cos(brRad)
            val tipX = cx + r * 0.88f * sinB
            val tipY = cy - r * 0.88f * cosB
            // Shaft from center to near-tip
            drawLine(Color.Red.copy(alpha = 0.9f), Offset(cx, cy), Offset(tipX, tipY), strokeWidth = 3f)
            // Airplane triangle at tip — base positioned just behind tip
            val wingLen = 12.dp.toPx()
            val baseX = cx + r * 0.74f * sinB
            val baseY = cy - r * 0.74f * cosB
            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX + wingLen * cosB, baseY + wingLen * sinB)
                lineTo(baseX - wingLen * cosB, baseY - wingLen * sinB)
                close()
            }
            drawPath(path, Color.Red.copy(alpha = 0.9f))
        }

        // ── Satellites ───────────────────────────────────────────────────

        satellites.forEach { sat ->
            val el = sat.elevation.coerceIn(0f, 89f)
            val azRad = Math.toRadians(sat.azimuth.toDouble()).toFloat()
            val dist = r * (1f - el / 90f)
            val sx = cx + dist * kotlin.math.sin(azRad)
            val sy = cy - dist * kotlin.math.cos(azRad)
            val c = constellationColor(sat.constellationType)
            val alpha = if (sat.usedInFix) 1f else 0.4f
            val sr = (3.5f + sat.cn0DbHz / 12f).coerceIn(3f, 7f)

            if (sat.usedInFix) {
                drawCircle(c.copy(alpha = 0.35f), sr + 1.5f, Offset(sx, sy))
            }
            drawCircle(c.copy(alpha = alpha), sr, Offset(sx, sy))
        }

        // Center dot
        drawCircle(Color.White.copy(alpha = 0.5f), 2.dp.toPx(), Offset(cx, cy))
    }
}

// ── Compact signal bars ─────────────────────────────────────────────────────

@Composable
private fun CompactSignalBars(satellites: List<SatelliteInfo>) {
    if (satellites.isEmpty()) return

    // Group: best SNR per constellation (usedInFix only, then all)
    val grouped = satellites
        .filter { it.cn0DbHz > 0 }
        .groupBy { it.constellationType }
        .mapValues { (_, list) ->
            val bestFix = list.filter { it.usedInFix }.maxOfOrNull { it.cn0DbHz } ?: 0f
            val fixCount = list.count { it.usedInFix }
            Pair(maxOf(bestFix, 0f), fixCount)
        }
        .filter { it.value.second > 0 || it.value.first > 10f }
        .entries.sortedByDescending { it.value.first }

    if (grouped.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grouped.forEach { (constellation, pair) ->
            val (snr, count) = pair
            val color = constellationColor(constellation)
            val label = constellationLabel(constellation)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = color)
                    Spacer(Modifier.width(3.dp))
                    // Tiny bar
                    Canvas(Modifier.size(20.dp, 8.dp)) {
                        drawRoundRect(color.copy(alpha = 0.6f),
                            size = Size(size.width * (snr / 50f).coerceIn(0f, 1f), size.height),
                            cornerRadius = CornerRadius(2.dp.toPx()))
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = color)
                }
            }
        }
    }
}

// ── Light metric row (Mach / Pitch / Roll / Accel) ────────────────────────

@Composable
private fun LightMetricRow(
    vararg cols: Pair<String, String>
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        cols.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
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

// ── Instrument row (2 columns, dual-unit) ───────────────────────────────────

@Composable
private fun InstrumentRow(
    labelL: String, valL1: String, unitL1: String, valL2: String, unitL2: String,
    labelR: String, valR1: String, unitR1: String, valR2: String, unitR2: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        InstrumentCell(labelL, valL1, unitL1, valL2, unitL2, Modifier.weight(1f))
        InstrumentCell(labelR, valR1, unitR1, valR2, unitR2, Modifier.weight(1f))
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
            style = MaterialTheme.typography.headlineSmall.copy(
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

// ── V/S + Heading row ──────────────────────────────────────────────────────

// ── Heading cell ─────────────────────────────────────────────────────────

@Composable
private fun HeadingCell(
    modifier: Modifier,
    bearing: Float,
    phoneAzimuth: Float?,
    headingRef: HeadingRef,
    declinationDeg: Float,
) {
    // GPS track is always true-north; convert only when MAG is selected.
    val shownBearing = if (bearing < 0) bearing
        else when (headingRef) {
            HeadingRef.TRUE -> bearing
            HeadingRef.MAG -> MagneticDeclination.trueToMagnetic(bearing, declinationDeg)
        }
    val label = "TRACK·${headingRef.label}"
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines = 1, softWrap = false)
        Text("%.0f°".format(shownBearing),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            maxLines = 1, softWrap = false)
        Text(headingToCardinal(shownBearing),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1, softWrap = false)
        if (phoneAzimuth != null && !phoneAzimuth.isNaN()) {
            Text("COMPASS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                maxLines = 1, softWrap = false)
            Text("%.0f°".format(phoneAzimuth),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1, softWrap = false)
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
            "±%.1f m   %s".format(
                gpsData.accuracyMeters,
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(gpsData.timestampMs))),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun headingToCardinal(h: Float) = when {
    h < 0 -> "—"
    h < 11.25 || h >= 348.75 -> "N"
    h < 33.75 -> "NNE"; h < 56.25 -> "NE"; h < 78.75 -> "ENE"
    h < 101.25 -> "E"; h < 123.75 -> "ESE"; h < 146.25 -> "SE"
    h < 168.75 -> "SSE"; h < 191.25 -> "S"; h < 213.75 -> "SSW"
    h < 236.25 -> "SW"; h < 258.75 -> "WSW"; h < 281.25 -> "W"
    h < 303.75 -> "WNW"; h < 326.25 -> "NW"; else -> "NNW"
}

private fun constellationColor(t: Int) = when (t) {
    GnssStatus.CONSTELLATION_GPS -> Color(0xFF4CAF50)
    GnssStatus.CONSTELLATION_GLONASS -> Color(0xFFF44336)
    GnssStatus.CONSTELLATION_BEIDOU -> Color(0xFFFF9800)
    GnssStatus.CONSTELLATION_GALILEO -> Color(0xFF2196F3)
    GnssStatus.CONSTELLATION_QZSS -> Color(0xFF9C27B0)
    GnssStatus.CONSTELLATION_IRNSS -> Color(0xFF00BCD4)
    else -> Color(0xFF9E9E9E)
}

private fun constellationLabel(t: Int) = when (t) {
    GnssStatus.CONSTELLATION_GPS -> "G"
    GnssStatus.CONSTELLATION_GLONASS -> "R"
    GnssStatus.CONSTELLATION_BEIDOU -> "B"
    GnssStatus.CONSTELLATION_GALILEO -> "E"
    GnssStatus.CONSTELLATION_QZSS -> "Q"
    GnssStatus.CONSTELLATION_IRNSS -> "I"
    else -> "?"
}

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
