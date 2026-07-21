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

private fun guidanceMessage(
    state: CaptureState,
    layer: CaptureLayer,
    layerBucketsCaptured: Int,
    inLayerBand: Boolean,
    framesPerLayer: Int
): String {
    if (state == CaptureState.STITCHING || state == CaptureState.STITCH_COMPLETE) {
        return "Great! Uploading your images"
    }
    if (!inLayerBand) {
        return when (layer) {
            CaptureLayer.MIDDLE -> "Hold the phone level"
            CaptureLayer.TOP -> "Tilt the phone up toward the ceiling"
            CaptureLayer.BOTTOM -> "Tilt the phone down toward the floor"
        }
    }
    val progress = layerBucketsCaptured.toFloat() / framesPerLayer
    return when {
        layerBucketsCaptured == 0 -> "Rotate slowly — capturing the ${layer.label} ring"
        progress < 0.9f -> "Keep rotating"
        else -> "Almost done with this ring"
    }
}

@Composable
fun CorridorCaptureScreen(
    viewModel: CorridorCaptureViewModel,
    nodeId: String,
    onFinishSegment: (outputPathsByLayer: Map<String, String>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val headingProvider = remember { SensorHeadingProvider(context) }

    var currentYaw by remember { mutableStateOf(0f) }
    var currentPitch by remember { mutableStateOf(0f) }

    val intervalDeg = 45f
    val framesPerLayer = 8
    val totalFrames = framesPerLayer * CaptureLayer.scanOrder.size

    var currentLayerIndex by remember { mutableStateOf(0) }
    val currentLayer = CaptureLayer.scanOrder[currentLayerIndex]

    var capturedBucketsByLayer by remember {
        mutableStateOf(CaptureLayer.scanOrder.associateWith { emptySet<Int>() })
    }
    val capturedBuckets = capturedBucketsByLayer[currentLayer] ?: emptySet()
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
            onPoseCaptured = { pose, imagePath, layer -> viewModel.onPoseCaptured(pose, imagePath, layer) },
            onError = { viewModel.onError(it) }
        )
    }

    DisposableEffect(host) {
        onDispose { host.release() }
    }

    // Heading + pitch updates, automatic capture, layer advancement
    LaunchedEffect(Unit) {
        headingProvider.headingDeg.collect { yaw -> currentYaw = yaw }
    }
    LaunchedEffect(Unit) {
        headingProvider.pitchDeg.collect { pitch ->
            currentPitch = pitch

            val totalCaptured = capturedBucketsByLayer.values.sumOf { it.size }
            if (uiState.captureState == CaptureState.CAMERA_ACTIVE &&
                totalCaptured < totalFrames &&
                currentPitch in currentLayer.pitchRangeDeg
            ) {
                val normalizedYaw = ((currentYaw % 360f) + 360f) % 360f
                val bucket = (normalizedYaw / intervalDeg).toInt()
                val layerBuckets = capturedBucketsByLayer[currentLayer] ?: emptySet()

                if (bucket !in layerBuckets) {
                    val updatedBuckets = layerBuckets + bucket
                    capturedBucketsByLayer = capturedBucketsByLayer + (currentLayer to updatedBuckets)
                    host.onCaptureRequested(currentLayer)

                    if (updatedBuckets.size >= framesPerLayer && currentLayerIndex < CaptureLayer.scanOrder.lastIndex) {
                        currentLayerIndex += 1
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.captureState) {
        if (uiState.captureState != CaptureState.CAMERA_ACTIVE) {
            capturedBucketsByLayer = CaptureLayer.scanOrder.associateWith { emptySet() }
            currentLayerIndex = 0
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is CaptureEvent.NavigateToStitching -> onFinishSegment(event.outputPathsByLayer)
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
                framesCaptured = capturedBuckets.size,
                capturedBuckets = capturedBuckets,
                totalTargets = framesPerLayer
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
                text = guidanceMessage(
                    state = uiState.captureState,
                    layer = currentLayer,
                    layerBucketsCaptured = capturedBuckets.size,
                    inLayerBand = currentPitch in currentLayer.pitchRangeDeg,
                    framesPerLayer = framesPerLayer
                ),
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
                totalTargets = framesPerLayer,
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
                progress = { uiState.framesCaptured / totalFrames.toFloat() },
                color = Color(0xFF4CAF50),
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "${uiState.framesCaptured}/$totalFrames",
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