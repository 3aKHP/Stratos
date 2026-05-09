package com.gpsplane.app.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.gpsplane.app.data.model.AttitudeData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class AttitudeRepository(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun observeAttitude(): Flow<AttitudeData> = callbackFlow {
        if (rotationSensor == null && accelSensor == null) {
            trySend(AttitudeData.EMPTY)
            awaitClose {}
            return@callbackFlow
        }

        var latestAzimuth = Float.NaN
        var latestPitch = 0f
        var latestRoll = 0f
        var latestAccel = 0f
        var hasAzimuth = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val clone = event.values.clone()
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, clone)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        // orientation[0] = azimuth (rad, 0=North, CCW positive → CW negative)
                        // orientation[1] = pitch (rad, -π/2..π/2)
                        // orientation[2] = roll (rad, -π..π)
                        latestAzimuth = Math.toDegrees(-orientation[0].toDouble()).toFloat()
                            .let { if (it < 0) it + 360 else it }
                        latestPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        latestRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                        hasAzimuth = true
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        latestAccel = sqrt(x * x + y * y + z * z) / 9.81f // m/s² → g
                    }
                }

                trySend(
                    AttitudeData(
                        azimuth = latestAzimuth,
                        pitch = latestPitch,
                        roll = latestRoll,
                        accelerationG = latestAccel,
                        hasAzimuth = hasAzimuth
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
