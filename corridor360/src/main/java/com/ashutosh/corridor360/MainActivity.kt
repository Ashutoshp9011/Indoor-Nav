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
import com.ashutosh.corridor360.ui.CorridorViewModel
import com.ashutosh.corridor360.ui.CorridorViewModelFactory
import com.ashutosh.corridor360.mapping.MappingScreen
import com.ashutosh.corridor360.ui.theme.Corridor360Theme
import com.ashutosh.corridor360.capture.CorridorCaptureScreen
import com.ashutosh.corridor360.capture.CorridorCaptureViewModel
import com.ashutosh.corridor360.capture.CaptureViewModelFactory
import org.opencv.android.OpenCVLoader
import java.io.File

sealed class Screen {
    object Mapping : Screen()
    data class Capture(val node: NodeEntity) : Screen()
}

class MainActivity : ComponentActivity() {

    private var pendingCaptureNode: NodeEntity? = null

    private var cameraPermissionGranted by mutableStateOf(false)
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) Log.e("MainActivity", "Camera permission denied — capture flow blocked")
        onPermissionResult?.invoke(granted)
        onPermissionResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!OpenCVLoader.initDebug()) {
            Log.e("MainActivity", "OpenCV init failed — stitching will crash")
        }

        ensureCameraPermission()
        cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        val dbFile = File(getExternalFilesDir(null), "corridor_graph.sqlite")
        val db = AppDatabase.getInstance(applicationContext, dbFile.absolutePath)
        val nodeRepo = NodeRepository(db.nodeDao())
        val edgeRepo = EdgeRepository(db.edgeDao())
        val frameDao = db.frameDao()
        val panoramaDao = db.panoramaDao()
        val panoramaDir = File(getExternalFilesDir(null), "panoramas")

        setContent {
            Corridor360Theme {
                val viewModel: CorridorViewModel = viewModel(
                    factory = CorridorViewModelFactory(nodeRepo, edgeRepo)
                )
                var screen by remember { mutableStateOf<Screen>(Screen.Mapping) }
                val permissionGranted = cameraPermissionGranted

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    when (val current = screen) {
                        is Screen.Mapping -> MappingScreen(
                            viewModel = viewModel,
                            onStartCapture = { node ->
                                pendingCaptureNode = node
                                if (permissionGranted) {
                                    screen = Screen.Capture(node)
                                } else {
                                    onPermissionResult = { granted ->
                                        if (granted) {
                                            screen = Screen.Capture(node)
                                        } else {
                                            Log.w("MainActivity", "Capture blocked — camera permission denied")
                                            pendingCaptureNode = null
                                        }
                                    }
                                    ensureCameraPermission()
                                }
                            }
                        )
                        is Screen.Capture -> {
                            val captureViewModel: CorridorCaptureViewModel = viewModel(
                                key = current.node.nodeId,
                                factory = CaptureViewModelFactory(
                                    frameDao = frameDao,
                                    panoramaDao = panoramaDao,
                                    nodeId = current.node.nodeId,
                                    outputDir = panoramaDir
                                )
                            )
                            CorridorCaptureScreen(
                                viewModel = captureViewModel,
                                nodeId = current.node.nodeId,
                                onFinishSegment = { outputPathsByLayer ->
                                    // All 3 layers succeeded (onFinishSegment only fires on full
                                    // success). Store the MIDDLE-ring panorama as the node's
                                    // representative thumbnail — the other layers' paths are
                                    // already persisted per-layer via PanoramaDao.
                                    val representativePath = outputPathsByLayer["MIDDLE"]
                                        ?: outputPathsByLayer.values.first()
                                    viewModel.completeMapping(current.node.nodeId, representativePath)
                                    screen = Screen.Mapping
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

}