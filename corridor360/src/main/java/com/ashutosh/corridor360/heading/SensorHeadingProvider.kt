package com.ashutosh.corridor360.heading

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.ashutosh.corridor360.heading.SensorHeadingProvider

class SensorHeadingProvider(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _headingDeg = MutableStateFlow(0f)
    val headingDeg: StateFlow<Float> = _headingDeg

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
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
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Optional: warn user if accuracy is SENSOR_STATUS_UNRELIABLE (needs figure-8 calibration)
    }
}