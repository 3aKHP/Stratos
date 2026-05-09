package com.gpsplane.app.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.gpsplane.app.data.model.EnvironmentData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Environmental sensor readings (barometer today, potentially humidity /
 * outside temperature / magnetic field later). Kept separate from
 * [AttitudeRepository] because the reading cadence and absence semantics
 * differ — attitude sensors are IMU-grade and always present on modern
 * phones; environmental sensors are optional hardware.
 */
class EnvironmentRepository(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    fun observeEnvironment(): Flow<EnvironmentData> = callbackFlow {
        if (pressureSensor == null) {
            trySend(EnvironmentData.EMPTY)
            awaitClose {}
            return@callbackFlow
        }

        // Seed an initial EMPTY so downstream `combine` calls don't block the
        // whole dashboard waiting for the barometer's first callback (which
        // can take 200ms+ at SENSOR_DELAY_NORMAL, longer on cold start).
        trySend(EnvironmentData.EMPTY)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_PRESSURE) return
                val pressureHpa = event.values[0]
                trySend(
                    EnvironmentData(
                        cabinPressureHpa = pressureHpa,
                        cabinAltitudeM = PressureMath.pressureToAltitude(pressureHpa),
                        hasPressure = true,
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
