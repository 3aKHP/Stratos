package com.gpsplane.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gpsplane.app.data.TilePreloader
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.data.tiles.ArcGISWorldStreetMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

@Composable
fun DownloadScreen(gpsData: GpsData) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var depLat by remember { mutableStateOf("") }
    var depLon by remember { mutableStateOf("") }
    var arrLat by remember { mutableStateOf("") }
    var arrLon by remember { mutableStateOf("") }

    var zoomMin by remember { mutableFloatStateOf(6f) }
    var zoomMax by remember { mutableFloatStateOf(12f) }
    var corridorKm by remember { mutableFloatStateOf(50f) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloaded by remember { mutableIntStateOf(0) }
    var totalTiles by remember { mutableIntStateOf(0) }
    var failedTiles by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var estimate by remember { mutableIntStateOf(0) }

    // Ensure cache dir matches MapScreen (osmdroid-v2)
    LaunchedEffect(context) {
        Configuration.getInstance().apply {
            userAgentValue = "Stratos/0.1.1"
            osmdroidTileCache = context.filesDir.resolve("osmdroid-v2")
        }
    }

    // Estimate tile count whenever inputs change
    LaunchedEffect(depLat, depLon, arrLat, arrLon, zoomMin, zoomMax, corridorKm) {
        val (ok, dep, arr) = parseCoords(depLat, depLon, arrLat, arrLon)
        if (ok) {
            estimate = withContext(Dispatchers.IO) {
                TilePreloader(
                    Configuration.getInstance().osmdroidTileCache,
                    ArcGISWorldStreetMap
                ).estimateTileCount(dep, arr, zoomMin.toInt(), zoomMax.toInt(), corridorKm.toDouble())
            }
        } else {
            estimate = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Tile Preload", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Download map tiles along route for offline use.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(16.dp))

        // Departure
        SectionHeader("Departure")
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = depLat,
                onValueChange = { depLat = it },
                label = { Text("Lat (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isDownloading
            )
            OutlinedTextField(
                value = depLon,
                onValueChange = { depLon = it },
                label = { Text("Lon (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isDownloading
            )
        }
        Button(
            onClick = {
                depLat = "%.6f".format(gpsData.latitude)
                depLon = "%.6f".format(gpsData.longitude)
            },
            enabled = gpsData.hasFix && !isDownloading,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text("Set from GPS")
        }

        Spacer(Modifier.height(12.dp))

        // Destination
        SectionHeader("Destination")
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = arrLat,
                onValueChange = { arrLat = it },
                label = { Text("Lat (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isDownloading
            )
            OutlinedTextField(
                value = arrLon,
                onValueChange = { arrLon = it },
                label = { Text("Lon (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isDownloading
            )
        }

        Spacer(Modifier.height(16.dp))

        // Zoom range
        SectionHeader("Zoom Range: ${zoomMin.toInt()} – ${zoomMax.toInt()}")
        RangeSlider(
            min = 0f, max = 19f,
            start = zoomMin, end = zoomMax,
            steps = 19,
            onStartChange = { zoomMin = it.coerceAtMost(zoomMax - 1f) },
            onEndChange = { zoomMax = it.coerceAtLeast(zoomMin + 1f) },
            enabled = !isDownloading
        )

        // Corridor width
        SectionHeader("Corridor Width: ${corridorKm.toInt()} km")
        Slider(
            value = corridorKm,
            onValueChange = { corridorKm = it },
            valueRange = 10f..200f,
            steps = 18,
            enabled = !isDownloading
        )

        Spacer(Modifier.height(8.dp))

        // Estimate
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                Arrangement.SpaceBetween
            ) {
                Text("Estimated tiles", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (estimate > 0) "~$estimate tiles" else "—",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Download button or progress
        if (!isDownloading && !isComplete) {
            Button(
                onClick = {
                    val (ok, dep, arr) = parseCoords(depLat, depLon, arrLat, arrLon)
                    if (!ok) return@Button

                    isDownloading = true
                    downloaded = 0
                    failedTiles = 0
                    totalTiles = 0

                    scope.launch {
                        val result = TilePreloader(
                            Configuration.getInstance().osmdroidTileCache,
                            ArcGISWorldStreetMap
                        ).preload(
                            departure = dep,
                            destination = arr,
                            zoomMin = zoomMin.toInt(),
                            zoomMax = zoomMax.toInt(),
                            corridorKm = corridorKm.toDouble()
                        ) { progress ->
                            downloaded = progress.downloaded
                            totalTiles = progress.total
                            failedTiles = progress.failed
                        }
                        isDownloading = false
                        isComplete = result.isSuccess
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = estimate > 0
            ) {
                Text("Start Download")
            }
        } else if (isDownloading) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Downloading: $downloaded / $totalTiles",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (failedTiles > 0) {
                        Text(
                            "$failedTiles failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (totalTiles > 0) downloaded.toFloat() / totalTiles else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (isComplete) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Download complete: $downloaded tiles cached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (failedTiles > 0) {
                        Text(
                            "$failedTiles tiles could not be downloaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun RangeSlider(
    min: Float, max: Float,
    start: Float, end: Float,
    steps: Int,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    enabled: Boolean
) {
    // Material3 doesn't have RangeSlider natively in all versions.
    // Use two individual sliders as a simple stand-in.
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Min: ${start.toInt()}", style = MaterialTheme.typography.labelSmall)
            Text("Max: ${end.toInt()}", style = MaterialTheme.typography.labelSmall)
        }
        Slider(value = start, onValueChange = onStartChange, valueRange = min..max, enabled = enabled)
        Slider(value = end, onValueChange = onEndChange, valueRange = min..max, enabled = enabled)
    }
}

private fun parseCoords(
    depLat: String, depLon: String, arrLat: String, arrLon: String
): Triple<Boolean, GeoPoint, GeoPoint> {
    val dLat = depLat.toDoubleOrNull() ?: return Triple(false, GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0))
    val dLon = depLon.toDoubleOrNull() ?: return Triple(false, GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0))
    val aLat = arrLat.toDoubleOrNull() ?: return Triple(false, GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0))
    val aLon = arrLon.toDoubleOrNull() ?: return Triple(false, GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0))
    if (dLat !in -90.0..90.0 || aLat !in -90.0..90.0) return Triple(false, GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0))
    if (dLon !in -180.0..180.0 || aLon !in -180.0..180.0) return Triple(false, GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0))
    return Triple(true, GeoPoint(dLat, dLon), GeoPoint(aLat, aLon))
}
