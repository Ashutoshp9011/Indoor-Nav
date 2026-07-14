package com.ashutosh.corridor360.capture

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun CorridorCaptureScreen(
    viewModel: CorridorCaptureViewModel,
    nodeId: String,
    onFinishSegment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val headingProvider = remember { SensorHeadingProvider(context) }

    var currentYaw by remember {
        mutableStateOf(0f)
    }

    // Auto-capture configuration
    val intervalDeg = 45f
    var capturedBuckets by remember {
        mutableStateOf(setOf<Int>())
    }

    DisposableEffect(Unit) {
        headingProvider.start()

        onDispose {
            headingProvider.stop()
        }
    }

    val host = remember(nodeId) {
        CorridorCaptureHost(
            context = context,
            lifecycleOwner = lifecycleOwner,
            nodeId = nodeId,
            onStateChanged = {
                viewModel.onCaptureStateChanged(it)
            },
            onPoseCaptured = { pose, imagePath ->
                viewModel.onPoseCaptured(pose, imagePath)
            },
            onError = {
                viewModel.onError(it)
            }
        )
    }

    DisposableEffect(host) {
        onDispose {
            host.release()
        }
    }

    // Heading updates + automatic capture
    LaunchedEffect(Unit) {
        headingProvider.headingDeg.collect { yaw ->

            currentYaw = yaw

            if (
                uiState.captureState == CaptureState.CAMERA_ACTIVE &&
                uiState.framesCaptured < 8
            ) {

                val normalizedYaw =
                    ((yaw % 360f) + 360f) % 360f

                val bucket =
                    (normalizedYaw / intervalDeg).toInt()

                if (bucket !in capturedBuckets) {

                    capturedBuckets =
                        capturedBuckets + bucket

                    host.onCaptureRequested()
                }
            }
        }
    }

    // Reset bucket history whenever a new capture session starts
    LaunchedEffect(uiState.captureState) {
        if (uiState.captureState != CaptureState.CAMERA_ACTIVE) {
            capturedBuckets = emptySet()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->

            when (event) {

                is CaptureEvent.NavigateToStitching -> {
                    onFinishSegment()
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            factory = { ctx ->

                PreviewView(ctx).apply {

                    implementationMode =
                        PreviewView.ImplementationMode.COMPATIBLE

                    host.startPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Heading overlay
        if (uiState.captureState == CaptureState.CAMERA_ACTIVE) {

            HeadingGuideOverlay(
                currentYawDeg = currentYaw,
                framesCaptured = uiState.framesCaptured,
                totalTargets = 8
            )
        }

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
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
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

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(
                    onClick = {
                        host.onCaptureRequested()
                    },
                    enabled = uiState.captureState == CaptureState.CAMERA_ACTIVE,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Capture", fontSize = 12.sp)
                }

                if (uiState.readyToStitch) {

                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.onStitchRequested()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finish"
                            )
                        },
                        text = {
                            Text("Finish & Stitch")
                        }
                    )
                }
            }
        }

        if (
            uiState.captureState != CaptureState.CAMERA_ACTIVE &&
            uiState.captureState != CaptureState.ERROR
        ) {
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
    }
}
