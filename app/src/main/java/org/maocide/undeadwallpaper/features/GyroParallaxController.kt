package org.maocide.undeadwallpaper.features

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.max

class GyroParallaxController(
    context: Context,
    private var config: EnhancedWallpaperConfig
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var hasBaseline = false
    private var basePitch = 0f
    private var baseRoll = 0f

    @Volatile var offsetX: Float = 0f
        private set
    @Volatile var offsetY: Float = 0f
        private set

    private var isListening = false

    val requiredMinZoom: Float
        get() {
            if (!config.gyroParallaxEnabled) return 1.0f
            val maxShift = max(config.gyroStrengthX, config.gyroStrengthY)
            return 1.0f + maxShift * 2.0f
        }

    fun updateConfig(newConfig: EnhancedWallpaperConfig, isVisible: Boolean) {
        config = newConfig
        if (!config.gyroParallaxEnabled) {
            stop()
            offsetX = 0f
            offsetY = 0f
        } else if (isVisible) {
            start()
        }
    }

    fun start() {
        if (isListening || !config.gyroParallaxEnabled || rotationSensor == null) return
        hasBaseline = false
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val pitch = orientation[1]
        val roll = orientation[2]

        if (!hasBaseline) {
            basePitch = pitch
            baseRoll = roll
            hasBaseline = true
            return
        }

        basePitch += (pitch - basePitch) * 0.005f
        baseRoll += (roll - baseRoll) * 0.005f

        val deltaRoll = (roll - baseRoll).coerceIn(-0.45f, 0.45f) / 0.45f
        val deltaPitch = (pitch - basePitch).coerceIn(-0.45f, 0.45f) / 0.45f

        val targetX = if (abs(deltaRoll) < 0.02f) 0f else deltaRoll * config.gyroStrengthX
        val targetY = if (abs(deltaPitch) < 0.02f) 0f else deltaPitch * config.gyroStrengthY

        val alpha = config.gyroSmoothing
        offsetX += (targetX - offsetX) * alpha
        offsetY += (targetY - offsetY) * alpha
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
