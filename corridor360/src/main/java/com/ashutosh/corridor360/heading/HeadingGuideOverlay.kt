package com.ashutosh.corridor360.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

private val ScrimColor = Color.Black.copy(alpha = 0.35f)
private val TrackDotColor = Color.White
private val TargetDotColor = Color(0xFFFF6D2E) // orange
private val CoverageArcColor = Color.Black.copy(alpha = 0.55f)

/**
 * Continuous orbit-scan guide overlay.
 * - Dark scrim over camera feed
 * - Dotted track from current heading (white ring) to next auto-capture target (orange dot)
 * - Bottom coverage arc fills as more of the 360° range gets captured
 */
@Composable
fun HeadingGuideOverlay(
    currentYawDeg: Float,
    framesCaptured: Int,
    totalTargets: Int = 8,
    modifier: Modifier = Modifier
) {
    val intervalDeg = 360f / totalTargets
    val nextTargetYaw = ((framesCaptured % totalTargets) * intervalDeg)

    var delta = nextTargetYaw - currentYawDeg
    delta = ((delta + 540f) % 360f) - 180f // normalize -180..180

    val coverageFraction = (framesCaptured.coerceAtMost(totalTargets)) / totalTargets.toFloat()

    Canvas(modifier = modifier.fillMaxSize()) {
        // Scrim
        drawRect(color = ScrimColor)

        // --- Track (top third) ---
        val trackY = size.height * 0.28f
        val trackWidth = size.width * 0.6f
        val centerX = size.width / 2f
        val maxOffset = trackWidth / 2f
        val targetX = centerX + (delta / 180f) * maxOffset

        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(centerX, trackY),
            end = Offset(targetX, trackY),
            strokeWidth = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
        )

        // Current position (hollow ring, fixed center)
        drawCircle(
            color = TrackDotColor,
            radius = 18f,
            center = Offset(centerX, trackY),
            style = Stroke(width = 5f)
        )

        // Target dot (solid orange)
        drawCircle(
            color = TargetDotColor,
            radius = 16f,
            center = Offset(targetX, trackY)
        )

        // --- Coverage arc (bottom) ---
        val arcSize = size.width * 1.4f
        val arcTopLeft = Offset(
            x = (size.width - arcSize) / 2f,
            y = size.height - arcSize / 2f
        )
        drawArc(
            color = CoverageArcColor,
            startAngle = 180f,
            sweepAngle = 180f * coverageFraction,
            useCenter = true,
            topLeft = arcTopLeft,
            size = androidx.compose.ui.geometry.Size(arcSize, arcSize)
        )
    }
}