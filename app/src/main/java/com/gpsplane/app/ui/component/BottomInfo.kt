package com.gpsplane.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.ui.format.UnitConfig
import com.gpsplane.app.ui.format.fmtCoord
import com.gpsplane.app.ui.format.formatZulu

/**
 * Bottom info block: two lines of coordinates plus a timestamp line
 * (`UTC HH:MM:SSZ   local HH:MM:SS   ±X.X m`).
 */
@Composable
internal fun BottomRow(gpsData: GpsData, uc: UnitConfig) {
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

/**
 * Placeholder shown until the first GPS fix arrives.
 */
@Composable
internal fun WaitingState() {
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
