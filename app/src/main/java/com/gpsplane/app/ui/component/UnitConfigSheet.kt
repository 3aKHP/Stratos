package com.gpsplane.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsplane.app.ui.format.AltUnit
import com.gpsplane.app.ui.format.CoordFormat
import com.gpsplane.app.ui.format.HeadingRef
import com.gpsplane.app.ui.format.SpeedUnit
import com.gpsplane.app.ui.format.SunTimeRef
import com.gpsplane.app.ui.format.UnitConfig
import com.gpsplane.app.ui.format.VSpeedUnit

/**
 * Bottom-sheet that lets the user pick display units per metric (ground
 * speed, altitude, vertical speed, coordinates), the heading reference
 * (magnetic or true), whether GPX tracks are recorded automatically
 * when AIRBORNE, and whether the dashboard hides system bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitConfigSheet(
    config: UnitConfig,
    onConfigChange: (UnitConfig) -> Unit,
    recordingEnabled: Boolean,
    onRecordingEnabledChange: (Boolean) -> Unit,
    immersive: Boolean,
    onImmersiveChange: (Boolean) -> Unit,
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
            SingleUnitRow("Sunrise/Sunset Time", SunTimeRef.entries, config.sunTimeRef, { it.label },
                { onConfigChange(config.copy(sunTimeRef = it)) })

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Display", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Immersive mode", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Hide status and navigation bars. Swipe from the edge to reveal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Switch(checked = immersive, onCheckedChange = onImmersiveChange)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Recording", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-record GPX", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Write a new .gpx file under app storage when airborne.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Switch(checked = recordingEnabled, onCheckedChange = onRecordingEnabledChange)
            }
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
