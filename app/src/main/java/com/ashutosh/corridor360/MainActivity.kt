package com.ashutosh.corridor360

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashutosh.corridor360.Data.local.AppDatabase
import com.ashutosh.corridor360.entity.NodeEntity
import com.ashutosh.corridor360.Data.repository.EdgeRepository
import com.ashutosh.corridor360.Data.repository.NodeRepository
import com.ashutosh.corridor360.camera.CameraXRecorder
import com.ashutosh.corridor360.ui.CorridorViewModel
import com.ashutosh.corridor360.ui.CorridorViewModelFactory
import com.ashutosh.corridor360.mapping.MappingScreen
import com.ashutosh.corridor360.ui.theme.Corridor360Theme
import com.ashutosh.corridor360.stitching.PanoramaStitcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import java.io.File

// Simple in-app screen state — swap for NavHost later if it grows
sealed class Screen {
    object Mapping : Screen()
    data class Capture(val node: NodeEntity) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var cameraXRecorder: CameraXRecorder
    private var pendingCaptureNode: NodeEntity? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Log.e("MainActivity", "Camera permission denied — capture flow blocked")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!OpenCVLoader.initDebug()) {
            Log.e("MainActivity", "OpenCV init failed — stitching will crash")
        }

        ensureCameraPermission()

        // Bundled DB path — getInstance signature expects a file path/name
        val dbFile = File(getExternalFilesDir(null), "corridor_graph.sqlite")
        val db = AppDatabase.getInstance(applicationContext, dbFile.absolutePath)
        val nodeRepo = NodeRepository(db.nodeDao())
        val edgeRepo = EdgeRepository(db.edgeDao())

        cameraXRecorder = CameraXRecorder(applicationContext, this)

        setContent {
            Corridor360Theme {
                val viewModel: CorridorViewModel = viewModel(
                    factory = CorridorViewModelFactory(nodeRepo, edgeRepo)
                )
                var screen by remember { mutableStateOf<Screen>(Screen.Mapping) }
                val scope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    when (val current = screen) {
                        is Screen.Mapping -> MappingScreen(
                            viewModel = viewModel,
                            onStartCapture = { node ->
                                pendingCaptureNode = node
                                ensureCameraPermission()
                                screen = Screen.Capture(node)
                            }
                        )
                        is Screen.Capture -> {
                            // In a real implementation, this would be a dedicated Composable
                            // that displays the CameraX preview and a capture button.
                            CaptureScreen(
                                node = current.node,
                                cameraXRecorder = cameraXRecorder,
                                onDone = {
                                    scope.launch {
                                        finishCapture(current.node, viewModel) {
                                            screen = Screen.Mapping
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Runs stitching off main thread, then updates Room via the ViewModel
    private suspend fun finishCapture(
        node: NodeEntity,
        viewModel: CorridorViewModel,
        onDone: () -> Unit
    ) {
        val frames = cameraXRecorder.framesForNode(node.nodeId)
        if (frames.size < 2) {
            Log.w("MainActivity", "Not enough frames captured for ${node.nodeId}")
            onDone()
            return
        }

        val outputDir = File(getExternalFilesDir(null), "panoramas")
        val result = withContext(Dispatchers.Default) {
            PanoramaStitcher().stitch(frames, outputDir, node.nodeId)
        }

        when (result) {
            is PanoramaStitcher.Result.Success -> {
                viewModel.completeMapping(node.nodeId, result.outputPath)
                cameraXRecorder.clearFrames(node.nodeId)
            }
            is PanoramaStitcher.Result.Failure -> {
                Log.e("MainActivity", "Stitch failed: ${result.reason}")
            }
        }
        onDone()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraXRecorder.stopCamera()
    }
}
@Composable
fun CaptureScreen(
    node: NodeEntity,
    cameraXRecorder: CameraXRecorder,
    onDone: () -> Unit
) {
    // Placeholder for the Camera capture UI
    Text(text = "Capturing for node: ${node.nodeId}")
}
