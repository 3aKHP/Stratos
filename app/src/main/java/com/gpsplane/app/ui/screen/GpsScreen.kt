package com.gpsplane.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gpsplane.app.data.model.AttitudeData
import com.gpsplane.app.data.model.EnvironmentData
import com.gpsplane.app.data.model.GpsData
import com.gpsplane.app.data.FlightTimer
import com.gpsplane.app.ui.component.BaroRow
import com.gpsplane.app.ui.component.BottomRow
import com.gpsplane.app.ui.component.CompactSignalBars
import com.gpsplane.app.ui.component.LightMetricRow
import com.gpsplane.app.ui.component.PrimaryInstrumentRow
import com.gpsplane.app.ui.component.SkyPlot
import com.gpsplane.app.ui.component.TopBar
import com.gpsplane.app.ui.component.UnitConfigSheet
import com.gpsplane.app.ui.component.WaitingState
import com.gpsplane.app.ui.format.UnitConfig
import com.gpsplane.app.util.UnitConverter

@Composable
fun GpsScreen(
    gpsData: GpsData,
    attData: AttitudeData,
    envData: EnvironmentData,
    flightSnap: FlightTimer.Snapshot,
    declinationDeg: Float,
    recordingEnabled: Boolean,
    onRecordingEnabledChange: (Boolean) -> Unit,
) {
    var unitConfig by remember { mutableStateOf(UnitConfig()) }
    var showConfig by remember { mutableStateOf(false) }

    if (showConfig) {
        UnitConfigSheet(
            config = unitConfig,
            onConfigChange = { unitConfig = it },
            recordingEnabled = recordingEnabled,
            onRecordingEnabledChange = onRecordingEnabledChange,
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
