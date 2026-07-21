package com.ashutosh.corridor360.heading

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorHeadingProvider(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isAvailable: Boolean get() = rotationSensor != null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _headingDeg = MutableStateFlow(0f)
    val headingDeg: StateFlow<Float> = _headingDeg

    private val _pitchDeg = MutableStateFlow(0f)
    val pitchDeg: StateFlow<Float> = _pitchDeg

    private val _rollDeg = MutableStateFlow(0f)
    val rollDeg: StateFlow<Float> = _rollDeg

    /** Returns false if there's no rotation-vector sensor on this device — caller should surface an error. */
    fun start(): Boolean {
        val sensor = rotationSensor ?: return false
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        return true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // orientationAngles[0] = azimuth (yaw) in radians, -π to π
        val azimuthRad = orientationAngles[0]
        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()

        // normalize to 0–360
        if (azimuthDeg < 0) azimuthDeg += 360f

        _headingDeg.value = azimuthDeg

        // orientationAngles[1] = pitch in radians, roughly -90..90 for phone held upright
        _pitchDeg.value = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()

        // orientationAngles[2] = roll in radians, -180..180
        _rollDeg.value = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Optional: warn user if accuracy is SENSOR_STATUS_UNRELIABLE (needs figure-8 calibration)
    }
}