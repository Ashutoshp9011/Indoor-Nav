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
import com.ashutosh.corridor360.capture.CorridorCaptureScreen
import com.ashutosh.corridor360.capture.CorridorCaptureViewModel
import com.ashutosh.corridor360.capture.CaptureViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import java.io.File
import com.ashutosh.corridor360.stitching.StitchResult
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

        val dbFile = File(getExternalFilesDir(null), "corridor_graph.sqlite")
        val db = AppDatabase.getInstance(applicationContext, dbFile.absolutePath)
        val nodeRepo = NodeRepository(db.nodeDao())
        val edgeRepo = EdgeRepository(db.edgeDao())
        val frameDao = db.frameDao()

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
                            val captureViewModel: CorridorCaptureViewModel = viewModel(
                                factory = CaptureViewModelFactory(
                                    frameDao,
                                    current.node.nodeId
                                )
                            )
                            CorridorCaptureScreen(
                                viewModel = captureViewModel,
                                nodeId = current.node.nodeId,
                                onFinishSegment = {
                                    scope.launch {
                                        finishCapture(node = current.node, viewModel = viewModel) {
                                            screen = Screen.Mapping
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } // <-- was missing: closes setContent block before onCreate ends
    } // <-- was missing: closes onCreate itself

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
            is StitchResult.Success -> {
                viewModel.completeMapping(node.nodeId, result.outputPath)
                cameraXRecorder.clearFrames(node.nodeId)
            }
            is StitchResult.Failure -> {
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