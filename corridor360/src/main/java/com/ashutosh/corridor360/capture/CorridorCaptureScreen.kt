package com.ashutosh.corridor360.capture

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ashutosh.corridor360.heading.SensorHeadingProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.PhotoLibrary

private fun guidanceMessage(state: CaptureState, framesCaptured: Int, total: Int): String {
    if (state == CaptureState.STITCHING || state == CaptureState.STITCH_COMPLETE) {
        return "Great! Uploading your images"
    }
    val progress = framesCaptured.toFloat() / total
    return when {
        framesCaptured == 0 -> "Stand in the center of the room"
        progress < 0.5f -> "Rotate slowly to the left"
        progress < 0.9f -> "Keep the phone level"
        else -> "Almost complete"
    }
}

@Composable
fun CorridorCaptureScreen(
    viewModel: CorridorCaptureViewModel,
    nodeId: String,
    onFinishSegment: (outputPath: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val headingProvider = remember { SensorHeadingProvider(context) }

    var currentYaw by remember { mutableStateOf(0f) }

    val intervalDeg = 45f
    var capturedBuckets by remember { mutableStateOf(setOf<Int>()) }
    var flashOn by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val started = headingProvider.start()
        if (!started) {
            viewModel.onError("No rotation sensor found on this device — auto-capture can't work.")
        }
        onDispose { headingProvider.stop() }
    }

    val host = remember(nodeId) {
        CorridorCaptureHost(
            context = context,
            lifecycleOwner = lifecycleOwner,
            nodeId = nodeId,
            onStateChanged = { viewModel.onCaptureStateChanged(it) },
            onPoseCaptured = { pose, imagePath -> viewModel.onPoseCaptured(pose, imagePath) },
            onError = { viewModel.onError(it) }
        )
    }

    DisposableEffect(host) {
        onDispose { host.release() }
    }

    // Heading updates + automatic capture
    LaunchedEffect(Unit) {
        headingProvider.headingDeg.collect { yaw ->
            currentYaw = yaw

            if (uiState.captureState == CaptureState.CAMERA_ACTIVE && uiState.framesCaptured < 8) {
                val normalizedYaw = ((yaw % 360f) + 360f) % 360f
                val bucket = (normalizedYaw / intervalDeg).toInt()

                if (bucket !in capturedBuckets) {
                    capturedBuckets = capturedBuckets + bucket
                    host.onCaptureRequested()
                }
            }
        }
    }

    LaunchedEffect(uiState.captureState) {
        if (uiState.captureState != CaptureState.CAMERA_ACTIVE) {
            capturedBuckets = emptySet()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is CaptureEvent.NavigateToStitching -> onFinishSegment(event.outputPath)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Full-screen live camera preview (replaces small circular preview + grid background)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    host.startPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Heading overlay: center crosshair, horizon line, target dot + dashed arrow
        if (uiState.captureState == CaptureState.CAMERA_ACTIVE) {
            HeadingGuideOverlay(
                currentYawDeg = currentYaw,
                framesCaptured = uiState.framesCaptured,
                capturedBuckets = capturedBuckets,
                totalTargets = 8
            )
        }

        // Guidance banner — top center, mirrors the reference app's status pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .padding(horizontal = 24.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Text(
                text = guidanceMessage(uiState.captureState, uiState.framesCaptured, 8),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Sphere capture model — top-view radar, top-right corner
        if (uiState.captureState == CaptureState.CAMERA_ACTIVE) {
            SphereCaptureRadar(
                currentYawDeg = currentYaw,
                capturedBuckets = capturedBuckets,
                totalTargets = 8,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 16.dp)
                    .size(72.dp)
            )
        }

        // STOP button — top-left, only control besides error Dismiss
        TextButton(
            onClick = { viewModel.onStitchRequested() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Stop", tint = Color(0xFFFF5A5A))
            Text(" STOP", color = Color(0xFFFF5A5A), fontWeight = FontWeight.Bold)
        }

        // Flash toggle — top-right
        IconButton(
            onClick = {
                flashOn = !flashOn
                host.toggleFlash(flashOn)
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 40.dp)
        ) {
            Icon(
                imageVector = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle flash",
                tint = Color.White
            )
        }

        // Gallery preview (bottom-left) + progress ring (bottom-right) — mirrors reference layout
        IconButton(
            onClick = { showGallery = true },
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
        ) {
            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery preview", tint = Color.White)
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { uiState.framesCaptured / 8f },
                color = Color(0xFF4CAF50),
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "${uiState.framesCaptured}/8",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Frames counter + error handling — kept from your existing file
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            uiState.errorMessage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    TextButton(
                        onClick = { viewModel.onCaptureStateChanged(CaptureState.CAMERA_ACTIVE) }
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        Text(" Dismiss", color = Color.White)
                    }
                }
            }

            Text(
                text = "Frames: ${uiState.framesCaptured}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            uiState.lastDistanceMeters?.let {
                Text(
                    text = "Last Distance: %.2fm".format(it),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Stitching/Finalizing overlay — unchanged from your existing file
        if (uiState.captureState != CaptureState.CAMERA_ACTIVE && uiState.captureState != CaptureState.ERROR) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uiState.captureState == CaptureState.STITCHING)
                            "Stitching Panorama..." else "Finalizing...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showGallery) {
            CapturedFramesGallery(
                framePaths = uiState.framePaths,
                onDismiss = { showGallery = false }
            )
        }
    }
}