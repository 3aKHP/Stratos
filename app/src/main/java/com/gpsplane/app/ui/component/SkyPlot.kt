package com.gpsplane.app.ui.component

import android.location.GnssStatus
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.gpsplane.app.data.SkyProjection
import com.gpsplane.app.data.model.SatelliteInfo

/**
 * Heading-stabilized sky plot.
 *
 * [rotationDeg] is the world compass bearing placed at the top of the
 * dial:
 *   0°  = NORTH UP
 *   track = TRK UP (in motion)
 *   phone azimuth = HDG UP (stationary)
 *
 * Every angular element (ticks, cardinals, satellites, magnetic-north
 * marker, orientation fan) routes through [SkyProjection] — a single
 * pure function covered by hard-coded ground-truth tests. No Canvas
 * rotation transform is involved, so there is no "trust the device"
 * moment: the tests fix the signs.
 *
 * The artificial horizon (pitch/roll) belongs to the gravity frame and
 * stays aligned to the screen.
 */
@Composable
internal fun SkyPlot(
    satellites: List<SatelliteInfo>,
    azimuth: Float?,
    pitch: Float,
    roll: Float,
    rotationDeg: Float,
    declinationDeg: Float,
    showOrientationFan: Boolean,
    showNoHeadingLabel: Boolean,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = minOf(cx, cy) - 4.dp.toPx()

        // ── Rings and outer stroke (rotation-invariant) ─────────────────

        drawCircle(Color.White.copy(alpha = 0.08f), r, Offset(cx, cy))
        for (el in listOf(0.33f, 0.66f)) {
            drawCircle(Color.White.copy(alpha = 0.14f), r * (1f - el), Offset(cx, cy))
        }
        drawCircle(Color.White.copy(alpha = 0.2f), r, Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

        // ── Tick marks every 5° ─────────────────────────────────────────

        for (i in 0..71) {
            val worldAz = i * 5f
            val isCardinal = i % 18 == 0
            val len = if (isCardinal) 10.dp.toPx() else 5.dp.toPx()
            val a = if (isCardinal) 0.45f else 0.2f
            val (nx, ny) = SkyProjection.projectOnRing(worldAz, rotationDeg)
            drawLine(Color.White.copy(alpha = a),
                Offset(cx + nx * r, cy + ny * r),
                Offset(cx + nx * (r - len), cy + ny * (r - len)),
                strokeWidth = if (isCardinal) 2f else 1f)
        }

        // ── Phone orientation fan ───────────────────────────────────────
        // Under TRK-UP this shows the phone's pointing relative to the
        // flight direction. Under HDG-UP the fan would always be straight
        // up, so the caller hides it. ±15° half-angle.

        if (showOrientationFan && azimuth != null && !azimuth.isNaN()) {
            val fanRadius = r * 0.7f
            val (cxn, cyn) = SkyProjection.projectOnRing(azimuth, rotationDeg)
            val (lxn, lyn) = SkyProjection.projectOnRing(azimuth - 15f, rotationDeg)
            val (rxn, ryn) = SkyProjection.projectOnRing(azimuth + 15f, rotationDeg)
            val cxPt = Offset(cx, cy)
            val lPt = Offset(cx + lxn * fanRadius, cy + lyn * fanRadius)
            val rPt = Offset(cx + rxn * fanRadius, cy + ryn * fanRadius)
            // Triangle approximates the arc closely enough at ±15°.
            val fanPath = Path().apply {
                moveTo(cx, cy)
                lineTo(lPt.x, lPt.y)
                // Mid-ray to curve the triangle toward the arc midpoint.
                lineTo(cx + cxn * fanRadius, cy + cyn * fanRadius)
                lineTo(rPt.x, rPt.y)
                close()
            }
            drawPath(fanPath, Color.White.copy(alpha = 0.12f))
            drawLine(Color.White.copy(alpha = 0.25f), cxPt, lPt, strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.25f), cxPt, rPt, strokeWidth = 1f)
        }

        // ── Satellites ──────────────────────────────────────────────────

        satellites.forEach { sat ->
            val (nx, ny) = SkyProjection.projectWithElevation(
                sat.azimuth, sat.elevation, rotationDeg
            )
            val sx = cx + nx * r
            val sy = cy + ny * r
            val c = constellationColor(sat.constellationType)
            val alpha = if (sat.usedInFix) 1f else 0.4f
            val sr = (3.5f + sat.cn0DbHz / 12f).coerceIn(3f, 7f)

            if (sat.usedInFix) {
                drawCircle(c.copy(alpha = 0.35f), sr + 1.5f, Offset(sx, sy))
            }
            drawCircle(c.copy(alpha = alpha), sr, Offset(sx, sy))
        }

        // ── Centre dot ──────────────────────────────────────────────────

        drawCircle(Color.White.copy(alpha = 0.5f), 2.dp.toPx(), Offset(cx, cy))

        // ── Cardinal labels (upright text) ──────────────────────────────

        val cardinalPaint = android.graphics.Paint().apply {
            color = 0x99FFFFFF.toInt()
            textSize = 12.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        val baselineOffset =
            -(cardinalPaint.fontMetrics.ascent + cardinalPaint.fontMetrics.descent) / 2f
        val lr = r + 14.dp.toPx()
        val cardinals = listOf(0f to "N", 90f to "E", 180f to "S", 270f to "W")
        for ((worldAz, label) in cardinals) {
            val (nx, ny) = SkyProjection.projectOnRing(worldAz, rotationDeg)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                cx + nx * lr,
                cy + ny * lr + baselineOffset,
                cardinalPaint
            )
        }

        // ── Magnetic north marker ───────────────────────────────────────
        // declinationDeg is east-positive (GeomagneticField convention),
        // so magnetic north sits at world compass bearing = declinationDeg.

        if (!declinationDeg.isNaN()) {
            val magNorthPaint = android.graphics.Paint().apply {
                color = 0xCCFF5252.toInt()
                textSize = 10.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            val magBaselineOffset =
                -(magNorthPaint.fontMetrics.ascent + magNorthPaint.fontMetrics.descent) / 2f
            val (nx, ny) = SkyProjection.projectOnRing(declinationDeg, rotationDeg)
            drawContext.canvas.nativeCanvas.drawText(
                "N",
                cx + nx * lr,
                cy + ny * lr + magBaselineOffset,
                magNorthPaint
            )
        }

        // ── "NO HEADING" degrade banner ─────────────────────────────────

        if (showNoHeadingLabel) {
            val warnPaint = android.graphics.Paint().apply {
                color = 0x66FFFFFF
                textSize = 10.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "NO HEADING",
                cx, cy + r * 0.25f, warnPaint
            )
        }

        // ── Artificial horizon (gravity frame — never rotates) ──────────

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
            val markLen = 6.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.35f),
                Offset(cx - markLen * cosR, lineY - markLen * sinR),
                Offset(cx + markLen * cosR, lineY + markLen * sinR),
                strokeWidth = 3f)
        }
    }
}

internal fun constellationColor(t: Int): Color = when (t) {
    GnssStatus.CONSTELLATION_GPS -> Color(0xFF4CAF50)
    GnssStatus.CONSTELLATION_GLONASS -> Color(0xFFF44336)
    GnssStatus.CONSTELLATION_BEIDOU -> Color(0xFFFF9800)
    GnssStatus.CONSTELLATION_GALILEO -> Color(0xFF2196F3)
    GnssStatus.CONSTELLATION_QZSS -> Color(0xFF9C27B0)
    GnssStatus.CONSTELLATION_IRNSS -> Color(0xFF00BCD4)
    else -> Color(0xFF9E9E9E)
}

internal fun constellationLabel(t: Int): String = when (t) {
    GnssStatus.CONSTELLATION_GPS -> "G"
    GnssStatus.CONSTELLATION_GLONASS -> "R"
    GnssStatus.CONSTELLATION_BEIDOU -> "B"
    GnssStatus.CONSTELLATION_GALILEO -> "E"
    GnssStatus.CONSTELLATION_QZSS -> "Q"
    GnssStatus.CONSTELLATION_IRNSS -> "I"
    else -> "?"
}
