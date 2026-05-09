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
    private val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val rawAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun observeAttitude(): Flow<AttitudeData> = callbackFlow {
        val hasAnySensor =
            rotationSensor != null || linearAccelSensor != null ||
            rawAccelSensor != null || gyroSensor != null
        if (!hasAnySensor) {
            trySend(AttitudeData.EMPTY)
            awaitClose {}
            return@callbackFlow
        }

        var latestAzimuth = Float.NaN
        var latestPitch = 0f
        var latestRoll = 0f
        var latestAccel = 0f
        var latestLoadFactor = Float.NaN
        var latestTurnRate = Float.NaN
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
                        latestAccel = AttitudeMath.magnitudeInG(
                            event.values[0], event.values[1], event.values[2]
                        )
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        latestLoadFactor = AttitudeMath.magnitudeInG(
                            event.values[0], event.values[1], event.values[2]
                        )
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        // z-axis rotation rate in rad/s → deg/s. Android reports the
                        // phone-body frame; z positive = CCW when the screen faces up,
                        // which corresponds to a left (nose-left) yaw. Flip the sign so
                        // positive = right turn, matching aviation convention.
                        latestTurnRate = Math.toDegrees(-event.values[2].toDouble()).toFloat()
                    }
                }

                trySend(
                    AttitudeData(
                        azimuth = latestAzimuth,
                        pitch = latestPitch,
                        roll = latestRoll,
                        accelerationG = latestAccel,
                        loadFactorG = latestLoadFactor,
                        turnRateDegPerSec = latestTurnRate,
                        hasAzimuth = hasAzimuth
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        linearAccelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rawAccelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
