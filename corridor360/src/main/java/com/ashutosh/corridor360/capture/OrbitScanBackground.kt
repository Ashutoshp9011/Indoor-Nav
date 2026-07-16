package com.ashutosh.corridor360.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

private val BgColor = Color(0xFF1C1C1E)
private val GridLineColor = Color.White.copy(alpha = 0.12f)

/** Solid dark background with faint radial compass lines (no camera passthrough). */
@Composable
fun OrbitScanBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = BgColor)

        val originX = size.width / 2f
        val originY = size.height * 0.55f

        // Radiating lines (compass fan)
        val lineCount = 9
        for (i in 0 until lineCount) {
            val angle = Math.toRadians((i * (180f / (lineCount - 1)) - 90f).toDouble())
            val length = size.height
            val endX = originX + length * kotlin.math.sin(angle).toFloat()
            val endY = originY - length * kotlin.math.cos(angle).toFloat()
            drawLine(
                color = GridLineColor,
                start = Offset(originX, originY),
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
        }

        // Concentric arcs
        listOf(0.3f, 0.6f, 0.9f).forEach { fraction ->
            val radius = size.height * fraction
            drawCircle(
                color = GridLineColor,
                radius = radius,
                center = Offset(originX, originY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
    }
}