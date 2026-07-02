package com.ashutosh.corridor360.capture // TODO: match your package

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

/**
 * Corridor360 capture screen.
 *
 * - CameraX PreviewView shows the live feed.
 * - ARCore drives pose/distance updates via viewModel.onPoseUpdated(...), which you
 *   should call from wherever your ARCore frame loop already lives (e.g. a
 *   Choreographer callback wrapping ARCoreSessionHelper.latestPose()).
 * - The coverage ring fills as the user moves; capture button enables once
 *   readyToCapture is true.
 */
@Composable
fun CorridorCaptureScreen(
    viewModel: CorridorCaptureViewModel,
    currentPose: () -> CapturePose?, // TODO: wire to your ARCore frame loop
    onFinishSegment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                startCamera(ctx, lifecycleOwner, previewView) { capture ->
                    imageCapture = capture
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Coverage ring + distance readout overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringRadius = size.minDimension / 6f
            val center = Offset(size.width / 2f, size.height * 0.75f)

            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 8f)
            )
            drawArc(
                color = if (uiState.readyToCapture) Color.Green else Color.Yellow,
                startAngle = -90f,
                sweepAngle = 360f * uiState.coverageProgress,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 8f)
            )
        }

        // Distance readout
        Text(
            text = uiState.distanceToSurfaceMeters?.let { "%.2f m to surface".format(it) }
                ?: "Finding surface…",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Frame count + finish button
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.framesCaptured} frames",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Capture + finish controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Button(
                enabled = uiState.readyToCapture,
                onClick = {
                    val pose = currentPose() ?: return@Button
                    val capture = imageCapture ?: return@Button
                    val outputFile = File(
                        context.getExternalFilesDir("corridor360_frames"),
                        "frame_${System.currentTimeMillis()}.jpg"
                    )
                    val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    capture.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModel.captureFrame(pose, outputFile.absolutePath)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                // TODO: surface to uiState.errorMessage
                            }
                        }
                    )
                }
            ) {
                Text("Capture")
            }

            OutlinedButton(
                enabled = uiState.readyToStitch,
                onClick = onFinishSegment
            ) {
                Text("Finish Segment")
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        onImageCaptureReady(imageCapture)
    }, ContextCompat.getMainExecutor(context))
}