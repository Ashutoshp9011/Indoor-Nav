// CapturePose.kt
package com.ashutosh.corridor360.capture

data class CapturePose(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float,
    val distanceMeters: Float?
)