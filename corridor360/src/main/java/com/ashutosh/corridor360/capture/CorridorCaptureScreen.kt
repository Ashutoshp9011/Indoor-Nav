package com.ashutosh.corridor360.capture // TODO: match your package

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
import androidx.camera.view.PreviewView

/**
 * Corridor360 capture screen.
 *
 * CameraX preview + sequential ARCore pose reads are fully owned by
 * CorridorCaptureHost — this Composable only renders state and forwards
 * user taps. No continuous pose feed, no coverage ring: ARCore only runs
 * in short bursts between CameraX stop/restart (see CorridorCaptureHost).
 */
@Composable
fun CorridorCaptureScreen(
    viewModel: CorridorCaptureViewModel,
    nodeId: String,
    onFinishSegment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val host = remember(nodeId) {
        CorridorCaptureHost(
            context = context,
            lifecycleOwner = lifecycleOwner,
            nodeId = nodeId,
            onStateChanged = { state -> viewModel.onCaptureStateChanged(state) },
            onPoseCaptured = { pose, imagePath -> viewModel.onPoseCaptured(pose, imagePath) },
            onError = { message -> viewModel.onError(message) }
        )
    }
    DisposableEffect(host) {
        onDispose {
            host.release()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is CaptureEvent.NavigateToStitching -> onFinishSegment()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    host.startPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // UI Overlays
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = "Frames: ${uiState.framesCaptured}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            uiState.lastDistanceMeters?.let { dist ->
                Text(
                    text = "Last Distance: %.2fm".format(dist),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = { host.onCaptureRequested() },
                    enabled = uiState.captureState == CaptureState.CAMERA_ACTIVE,
                    modifier = Modifier.size(80.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text("Capture")
                }

                if (uiState.readyToStitch) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.onStitchRequested() },
                        text = { Text("Finish & Stitch") },
                        icon = { /* Add icon if desired */ }
                    )
                }
            }
        }

        if (uiState.captureState != CaptureState.CAMERA_ACTIVE && uiState.captureState != CaptureState.ERROR) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
