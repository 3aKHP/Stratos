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

        // Seed an initial EMPTY so downstream `combine` calls don't block on
        // the first sensor callback before surfacing the dashboard.
        trySend(AttitudeData.EMPTY)

        // Gyroscope and raw accelerometer fire at ~50Hz with visible jitter
        // (sub-degree and sub-0.01g noise) — smooth on the way in so the UI
        // isn't constantly redrawing the last digit. α values chosen to
        // settle in a few hundred ms while still tracking real maneuvers.
        val turnRateFilter = EmaFilter(alpha = 0.15f)
        val loadFactorFilter = EmaFilter(alpha = 0.1f)

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
                        latestLoadFactor = loadFactorFilter.update(
                            AttitudeMath.magnitudeInG(
                                event.values[0], event.values[1], event.values[2]
                            )
                        )
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        latestTurnRate = turnRateFilter.update(
                            AttitudeMath.gyroZToTurnRateDegPerSec(event.values[2])
                        )
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
