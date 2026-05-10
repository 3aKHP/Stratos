package com.gpsplane.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsplane.app.data.model.SatelliteInfo

/**
 * Condensed per-constellation signal indicator: one chip per
 * constellation with an SNR bar and the fix count.
 */
@Composable
internal fun CompactSignalBars(satellites: List<SatelliteInfo>) {
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
