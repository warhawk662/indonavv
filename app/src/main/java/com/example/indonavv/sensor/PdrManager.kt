package com.example.indonavv.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.*

data class PdrUpdate(val stepDetected: Boolean, val headingDegrees: Float)

class PdrManager(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var currentHeading = 0f
    private var smoothedHeading = 0f
    private val alphaHeading = 0.08f // Even smoother heading for stability

    // Step Detection Algorithm Variables
    private var lastLinearMag = 0f
    private var isPeak = false
    private val stepMinThreshold = 1.0f // Lowered threshold for better sensitivity
    private var lastStepTime = 0L
    private val stepCooldownMs = 300L 

    // Orientation variables for fallback
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    // Map-aided correction offset
    private var headingOffset = 0f

    fun getPdrUpdates(): Flow<PdrUpdate> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return

                when (event.sensor.type) {
                    Sensor.TYPE_STEP_DETECTOR -> {
                        vibrate(40)
                        trySend(PdrUpdate(true, getCorrectedHeading()))
                    }
                    
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        if (stepDetector == null) {
                            val x = event.values[0]
                            val y = event.values[1]
                            val z = event.values[2]
                            val linearMag = sqrt(x*x + y*y + z*z)
                            
                            val currentTime = System.currentTimeMillis()
                            if (linearMag > stepMinThreshold && !isPeak && (currentTime - lastStepTime > stepCooldownMs)) {
                                if (linearMag < lastLinearMag) { // Peak detected
                                    isPeak = true
                                    lastStepTime = currentTime
                                    vibrate(30)
                                    trySend(PdrUpdate(true, getCorrectedHeading()))
                                }
                            } else if (linearMag < 0.4f) {
                                isPeak = false
                            }
                            lastLinearMag = linearMag
                        }
                    }

                    Sensor.TYPE_ROTATION_VECTOR -> {
                        updateHeadingFromRotationVector(event.values)
                        trySend(PdrUpdate(false, getCorrectedHeading()))
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        if (rotationVector == null) {
                            gravity = event.values.clone()
                            updateHeadingFromAccelMag()
                            trySend(PdrUpdate(false, getCorrectedHeading()))
                        }
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (rotationVector == null) {
                            geomagnetic = event.values.clone()
                            updateHeadingFromAccelMag()
                            trySend(PdrUpdate(false, getCorrectedHeading()))
                        }
                    }
                }
            }

            private fun updateHeadingFromRotationVector(rotationValues: FloatArray) {
                val matrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix, rotationValues)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(matrix, orientation)
                
                val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentHeading = (rawAzimuth + 360f) % 360f
                smoothedHeading = smoothAngle(smoothedHeading, currentHeading, alphaHeading)
            }

            private fun updateHeadingFromAccelMag() {
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    currentHeading = (rawAzimuth + 360f) % 360f
                    smoothedHeading = smoothAngle(smoothedHeading, currentHeading, alphaHeading)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register best available high-frequency sensors
        sensorManager.registerListener(listener, stepDetector ?: linearAccel ?: accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        if (rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun getCorrectedHeading(): Float {
        return (smoothedHeading + headingOffset + 360f) % 360f
    }

    fun applyMapAidedCorrection(mapHeading: Float) {
        // Calculate the difference between intended map heading and current sensor smoothed heading
        val diff = (mapHeading - smoothedHeading + 540f) % 360f - 180f
        headingOffset = (diff + 360f) % 360f
    }

    private fun smoothAngle(prev: Float, target: Float, alpha: Float): Float {
        var diff = target - prev
        while (diff < -180) diff += 360
        while (diff > 180) diff -= 360
        return (prev + alpha * diff + 360f) % 360f
    }

    private fun vibrate(durationMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMillis)
        }
    }
}
