package com.ashutosh.corridor360.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2

private val TargetDotColor = Color(0xFFFF6D2E)
private val TargetRingColor = Color(0xFF00E5FF) // was flat white — matches SphereCaptureRadar's cyan
private val ArrowColor = Color(0xFF00E5FF).copy(alpha = 0.85f)
private val GridColor = Color(0xFF00E5FF)

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

        // --- Vignette: darkens the edges of the live camera feed so the neon overlay
        // reads clearly, without hiding the passthrough preview like a full opaque
        // background would (this screen shows the live camera, unlike the radar panel).
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                center = Offset(originX, originY),
                radius = size.maxDimension * 0.75f
            )
        )

        // --- Neon grid rings around the capture sphere's center — same visual language
        // as SphereCaptureRadar's concentric cyan rings, so both panels read as one system.
        for (ringRadius in listOf(previewRadius * 1.6f, previewRadius * 2.4f, previewRadius * 3.2f)) {
            drawCircle(
                color = GridColor.copy(alpha = 0.12f),
                radius = ringRadius,
                center = Offset(originX, originY),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f)))
            )
        }

        // --- Center alignment crosshair: the user's position at the center of the invisible sphere ---
        val crosshairLen = size.width * 0.03f
        drawLine(
            color = GridColor.copy(alpha = 0.9f),
            start = Offset(originX - crosshairLen, originY),
            end = Offset(originX + crosshairLen, originY),
            strokeWidth = 4f
        )
        drawLine(
            color = GridColor.copy(alpha = 0.9f),
            start = Offset(originX, originY - crosshairLen),
            end = Offset(originX, originY + crosshairLen),
            strokeWidth = 4f
        )

        // --- Horizon level line: static, independent of yaw, helps keep phone level for stitching ---
        drawLine(
            color = Color(0xFF4CAF50).copy(alpha = 0.5f), // dimmer, no longer competes with the cyan grid
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

        // Target dot (large, filled orange + glow ring) — SIZE INCREASES as it's approached
        val proximity = 1f - (kotlin.math.abs(delta) / 180f) // 0..1, 1 = aligned
        val dynamicRadius = targetDotRadius * (0.7f + 0.5f * proximity)

        // Soft glow behind the dot, growing with proximity — same treatment as the
        // "current target" node glow in SphereCaptureRadar
        drawCircle(
            color = TargetDotColor.copy(alpha = 0.25f + 0.25f * proximity),
            radius = dynamicRadius * 1.6f,
            center = Offset(targetX, targetY)
        )
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
/**
 * Cosmetic-only: plays while the real PanoramaStitcher runs in the background.
 * OpenCV's Stitcher.SCANS is a black box (no intermediate ORB/pose/bundle-adjustment
 * data to visualize), so this fakes the "images converging into place" beat from the
 * original spec — it does not reflect real stitching progress.
 */
@Composable
fun StitchConvergeAnimation(
    framePaths: List<String>,
    modifier: Modifier = Modifier
) {
    val thumbnails = remember(framePaths) {
        framePaths.mapNotNull { path ->
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(thumbnails) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 900)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (thumbnails.isEmpty()) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val startRadius = size.minDimension * 0.38f
        val thumbSize = size.minDimension * 0.16f * (1f - progress.value * 0.5f) // shrink as it converges
        val alpha = 1f - progress.value // fade out as it reaches center

        thumbnails.forEachIndexed { i, thumb ->
            val bearingDeg = i * (360f / thumbnails.size)
            val angleRad = Math.toRadians(bearingDeg.toDouble())
            val radius = startRadius * (1f - progress.value) // moves toward center
            val px = cx + radius * sin(angleRad).toFloat()
            val py = cy - radius * cos(angleRad).toFloat()

            translate(left = px - thumbSize / 2f, top = py - thumbSize / 2f) {
                drawImage(
                    image = thumb,
                    dstSize = androidx.compose.ui.unit.IntSize(thumbSize.toInt(), thumbSize.toInt()),
                    alpha = alpha.coerceIn(0f, 1f)
                )
            }
        }

        // Glowing merge point at center, growing as images converge into it
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = progress.value * 0.5f),
            radius = thumbSize * (0.5f + progress.value),
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun SphereCaptureRadar(
    currentYawDeg: Float,
    capturedBuckets: Set<Int>,
    bucketToPath: Map<Int, String> = emptyMap(), // node index -> captured frame file path
    currentTargetBucket: Int? = null,             // bucket the guide is steering toward right now
    totalTargets: Int = 8,
    modifier: Modifier = Modifier
) {
    val intervalDeg = 360f / totalTargets

    // Decode each captured frame once per path change, downsampled — this is a small
    // preview thumbnail, not the full-res frame, so inSampleSize keeps it cheap.
    val thumbnails = remember(bucketToPath) {
        bucketToPath.mapValues { (_, path) ->
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ringRadius = size.minDimension * 0.38f
        val dotRadius = size.minDimension * 0.06f
        val thumbSize = dotRadius * 2.4f

        // Neon grid backdrop — a couple of faint concentric rings behind the main
        // ring give the "sphere" depth cue instead of a flat circle.
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.10f),
            radius = ringRadius * 1.25f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.18f),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        )

        // Main sphere ring (dashed)
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        )

        // User at the center of the sphere
        drawCircle(color = Color.White, radius = dotRadius * 0.7f, center = Offset(cx, cy))

        // One node per target bearing: thumbnail if captured, glowing dot otherwise
        for (bucket in 0 until totalTargets) {
            val bearingDeg = bucket * intervalDeg
            val angleRad = Math.toRadians(bearingDeg.toDouble())
            val px = cx + ringRadius * sin(angleRad).toFloat()
            val py = cy - ringRadius * cos(angleRad).toFloat()

            val captured = bucket in capturedBuckets
            val thumb = thumbnails[bucket]

            when {
                captured && thumb != null -> {
                    // Green glow ring behind the thumbnail = "verified" node
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.35f),
                        radius = thumbSize * 0.75f,
                        center = Offset(px, py)
                    )
                    translate(left = px - thumbSize / 2f, top = py - thumbSize / 2f) {
                        drawImage(
                            image = thumb,
                            dstSize = androidx.compose.ui.unit.IntSize(
                                thumbSize.toInt(),
                                thumbSize.toInt()
                            )
                        )
                    }
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = thumbSize / 2f,
                        center = Offset(px, py),
                        style = Stroke(width = 3f)
                    )
                }
                bucket == currentTargetBucket -> {
                    // Orange glow = current target node
                    drawCircle(
                        color = Color(0xFFFF6D2E).copy(alpha = 0.35f),
                        radius = dotRadius * 1.8f,
                        center = Offset(px, py)
                    )
                    drawCircle(color = Color(0xFFFF6D2E), radius = dotRadius, center = Offset(px, py))
                }
                else -> {
                    // Gray = not yet captured
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = dotRadius,
                        center = Offset(px, py),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Live bearing indicator: where the user is currently facing on the sphere
        val yawRad = Math.toRadians(currentYawDeg.toDouble())
        drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(cx, cy),
            end = Offset(
                cx + ringRadius * sin(yawRad).toFloat(),
                cy - ringRadius * cos(yawRad).toFloat()
            ),
            strokeWidth = 4f
        )
    }
}