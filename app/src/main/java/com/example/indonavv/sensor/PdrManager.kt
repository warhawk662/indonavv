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
    private val gameRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var currentHeading = 0f
    private var smoothedHeading = 0f
    private var isInitialized = false
    
    // Adaptive heading alpha
    private var currentAlpha = 0.08f
    private val baseAlpha = 0.05f
    private val turnAlpha = 0.25f

    // Step Detection Algorithm Variables
    private val stepMinThreshold = 0.72f 
    private var lastStepTime = 0L
    private val stepCooldownMs = 300L 
    private var lastLinearMag = 0f
    private var isPeak = false

    // Map-aided correction offset
    private var headingOffset = 0f
    private val correctionAlpha = 0.12f 

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
                                if (linearMag < lastLinearMag) { 
                                    isPeak = true
                                    lastStepTime = currentTime
                                    vibrate(30)
                                    trySend(PdrUpdate(true, getCorrectedHeading()))
                                }
                            } else if (linearMag < 0.35f) {
                                isPeak = false
                            }
                            lastLinearMag = linearMag
                        }
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        val turnRate = abs(event.values[2]) 
                        currentAlpha = if (turnRate > 0.45f) turnAlpha else baseAlpha
                    }

                    Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                        updateHeadingFromRotationVector(event.values)
                        trySend(PdrUpdate(false, getCorrectedHeading()))
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
                
                if (!isInitialized) {
                    smoothedHeading = currentHeading
                    isInitialized = true
                } else {
                    smoothedHeading = smoothAngle(smoothedHeading, currentHeading, currentAlpha)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register sensors with optimized delays
        sensorManager.registerListener(listener, stepDetector ?: linearAccel, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST)
        
        // Prefer Game Rotation Vector for indoors, fallback to standard
        val rotationSensor = gameRotationVector ?: rotationVector
        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun getCorrectedHeading(): Float {
        return (smoothedHeading + headingOffset + 360f) % 360f
    }

    fun applyMapAidedCorrection(mapHeading: Float) {
        val current = getCorrectedHeading()
        var diff = mapHeading - current
        while (diff < -180) diff += 360
        while (diff > 180) diff -= 360
        
        // Nudge the offset toward the map heading
        headingOffset = (headingOffset + correctionAlpha * diff + 360f) % 360f
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
