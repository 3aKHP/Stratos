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
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values.clone())
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val euler = AttitudeMath.orientationRadiansToDegrees(orientation)
                        latestAzimuth = euler.azimuth
                        latestPitch = euler.pitch
                        latestRoll = euler.roll
                        hasAzimuth = true
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        latestAccel = AttitudeMath.linearAccelerationToG(
                            event.values[0], event.values[1], event.values[2]
                        )
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
