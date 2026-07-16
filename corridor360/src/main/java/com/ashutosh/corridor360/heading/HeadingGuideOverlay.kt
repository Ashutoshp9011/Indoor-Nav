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
        val previewRadius = size.width * 0.15f  // small circular live-camera preview position
        val targetDotRadius = size.width * 0.09f // ENLARGED target dot (was ~16f fixed px)

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