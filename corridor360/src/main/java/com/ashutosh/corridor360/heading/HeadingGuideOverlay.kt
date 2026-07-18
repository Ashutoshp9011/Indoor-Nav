package com.ashutosh.corridor360.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2

private val TargetDotColor = Color(0xFFFF6D2E)
private val TargetRingColor = Color.White
private val ArrowColor = Color.White.copy(alpha = 0.85f)

@Composable
fun HeadingGuideOverlay(
    currentYawDeg: Float,
    framesCaptured: Int,
    capturedBuckets: Set<Int> = emptySet(),
    totalTargets: Int = 8,
    modifier: Modifier = Modifier
) {
    val intervalDeg = 360f / totalTargets
    val nextTargetYaw = (framesCaptured % totalTargets) * intervalDeg

    var delta = nextTargetYaw - currentYawDeg
    delta = ((delta + 540f) % 360f) - 180f // -180..180

    Canvas(modifier = modifier.fillMaxSize()) {
        val originX = size.width / 2f
        val originY = size.height * 0.55f
        // Full-screen preview now, so overlay geometry is sized off full frame, not a small circle
        val previewRadius = size.width * 0.15f  // radius of the invisible sphere's projection, used for arrow math
        val targetDotRadius = size.width * 0.09f // ENLARGED target dot (was ~16f fixed px)

        // --- Center alignment crosshair: the user's position at the center of the invisible sphere ---
        val crosshairLen = size.width * 0.03f
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(originX - crosshairLen, originY),
            end = Offset(originX + crosshairLen, originY),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(originX, originY - crosshairLen),
            end = Offset(originX, originY + crosshairLen),
            strokeWidth = 4f
        )

        // --- Horizon level line: static, independent of yaw, helps keep phone level for stitching ---
        drawLine(
            color = Color.Green.copy(alpha = 0.6f),
            start = Offset(0f, originY),
            end = Offset(size.width, originY),
            strokeWidth = 3f
        )

        val angleRad = Math.toRadians((delta).toDouble())
        val targetX = originX + previewRadius * 3f * sin(angleRad).toFloat()
        val targetY = originY - previewRadius * 3f * cos(angleRad).toFloat()

        // Dashed arrow from preview position toward target
        val start = Offset(originX, originY - previewRadius)
        val end = Offset(targetX, targetY + targetDotRadius)

        drawLine(
            color = ArrowColor,
            start = start,
            end = end,
            strokeWidth = 6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f))
        )

        // Arrowhead
        val arrowAngle = atan2((end.y - start.y), (end.x - start.x))
        val headLength = 30f
        val path = Path().apply {
            moveTo(end.x, end.y)
            lineTo(
                end.x - headLength * cos(arrowAngle - 0.4f),
                end.y - headLength * sin(arrowAngle - 0.4f)
            )
            moveTo(end.x, end.y)
            lineTo(
                end.x - headLength * cos(arrowAngle + 0.4f),
                end.y - headLength * sin(arrowAngle + 0.4f)
            )
        }
        drawPath(path, color = ArrowColor, style = Stroke(width = 6f))

        // Target dot (large, filled orange + white ring) — SIZE INCREASES as it's approached
        val proximity = 1f - (kotlin.math.abs(delta) / 180f) // 0..1, 1 = aligned
        val dynamicRadius = targetDotRadius * (0.7f + 0.5f * proximity)

        drawCircle(color = TargetDotColor, radius = dynamicRadius, center = Offset(targetX, targetY))
        drawCircle(
            color = TargetRingColor,
            radius = dynamicRadius + 10f,
            center = Offset(targetX, targetY),
            style = Stroke(width = 6f)
        )
    }
}

/**
 * Top-view mini radar of the capture model: the user is imagined standing at the
 * center of an invisible sphere, and each capture is a point on that sphere at a
 * fixed bearing (0°, 45°, 90°... for 8 targets). This mirrors the reference app's
 * "TOP VIEW – CAPTURE PATTERN" diagram, rendered live during capture.
 */
@Composable
fun SphereCaptureRadar(
    currentYawDeg: Float,
    capturedBuckets: Set<Int>,
    totalTargets: Int = 8,
    modifier: Modifier = Modifier
) {
    val intervalDeg = 360f / totalTargets

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ringRadius = size.minDimension * 0.38f
        val dotRadius = size.minDimension * 0.06f

        // Sphere ring (dashed, matches the dashed circle in the reference top-view)
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        )

        // User at the center of the sphere
        drawCircle(color = Color.White, radius = dotRadius * 0.7f, center = Offset(cx, cy))

        // One point on the sphere per target bearing
        for (bucket in 0 until totalTargets) {
            val bearingDeg = bucket * intervalDeg
            val angleRad = Math.toRadians(bearingDeg.toDouble())
            val px = cx + ringRadius * sin(angleRad).toFloat()
            val py = cy - ringRadius * cos(angleRad).toFloat()

            val captured = bucket in capturedBuckets
            drawCircle(
                color = if (captured) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f),
                radius = dotRadius,
                center = Offset(px, py)
            )
        }

        // Live bearing indicator: where the user is currently facing on the sphere
        val yawRad = Math.toRadians(currentYawDeg.toDouble())
        drawLine(
            color = Color(0xFFFF6D2E),
            start = Offset(cx, cy),
            end = Offset(
                cx + ringRadius * sin(yawRad).toFloat(),
                cy - ringRadius * cos(yawRad).toFloat()
            ),
            strokeWidth = 4f
        )
    }
}