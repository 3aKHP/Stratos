package com.gpsplane.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gpsplane.app.data.GForceRange

/**
 * Transient dialog showing min/max load factor for the current flight.
 * Opens when the user taps the LOAD cell on the dashboard.
 * Min/max reset on every new AIRBORNE segment; on the ground there is
 * no data to show, so [gForce.hasData] guards a neutral message.
 */
@Composable
internal fun GForcePopup(gForce: GForceRange, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Load factor", fontWeight = FontWeight.Bold)
        },
        text = {
            if (!gForce.hasData) {
                Text(
                    "Min/max will appear once the flight timer reaches AIRBORNE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StatCell(label = "MIN", value = "%.2fg".format(gForce.minG))
                        StatCell(label = "MAX", value = "%.2fg".format(gForce.maxG))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${gForce.sampleCount} samples since takeoff.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            ),
        )
    }
}
