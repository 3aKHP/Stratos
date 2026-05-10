package com.gpsplane.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.FlightTimerState
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.ui.format.formatFlightTime

/**
 * Dashboard top bar: fix status + satellite count on the left,
 * flight timer on the right, settings cog at the end.
 */
@Composable
internal fun TopBar(
    gpsData: GpsData,
    flightState: FlightTimerState,
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
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp))
        }
    }
}
