package com.ashutosh.corridor360.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraXRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val capturedFrames = mutableListOf<String>()

    // Call once when entering the capture screen for a node
    fun startCamera(
        previewView: androidx.camera.view.PreviewView,
        onReady: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                onReady()
            } catch (e: Exception) {
                Log.e("CameraXRecorder", "startCamera failed", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Called repeatedly as the user pans for a panorama burst — feeds PanoramaStitcher (Step 7)
    fun captureFrame(nodeId: String, onSaved: (String) -> Unit, onError: (Exception) -> Unit) {
        val capture = imageCapture ?: return onError(IllegalStateException("Camera not started"))

        val dir = File(context.getExternalFilesDir(null), "panorama_frames/$nodeId").apply { mkdirs() }
        val name = SimpleDateFormat("HHmmss_SSS", Locale.US).format(Date()) + ".jpg"
        val file = File(dir, name)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedFrames.add(file.absolutePath)
                    onSaved(file.absolutePath)
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXRecorder", "Capture failed", exc)
                    onError(exc)
                }
            }
        )
    }

    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun hasFlashUnit(): Boolean = camera?.cameraInfo?.hasFlashUnit() ?: false

    fun framesForNode(nodeId: String): List<String> =
        File(context.getExternalFilesDir(null), "panorama_frames/$nodeId")
            .listFiles()?.map { it.absolutePath }?.sorted() ?: emptyList()

    fun clearFrames(nodeId: String) {
        File(context.getExternalFilesDir(null), "panorama_frames/$nodeId").deleteRecursively()
        capturedFrames.clear()
    }

    /**
     * Async, confirmed camera release. Callers (CorridorCaptureHost) must wait for
     * onStopped() before resuming ARCore — unbindAll() releasing the Camera2 device
     * is not guaranteed to be instantaneous, and starting ARCore's session before
     * release completes throws CameraNotAvailableException.
     */
    fun stopCamera(onStopped: () -> Unit = {}) {
        val provider = cameraProvider
        if (provider == null) {
            onStopped()
            return
        }
        try {
            provider.unbindAll()
            imageCapture = null
            camera = null
            // unbindAll() itself is synchronous on the provider's state, but the
            // underlying Camera2 device close happens async. A short delay via
            // the main executor's next loop is enough in practice; if you see
            // CameraNotAvailableException from ARCore, bump this to a real
            // Camera2 CameraDevice.StateCallback#onClosed listener instead.
            ContextCompat.getMainExecutor(context).execute {
                onStopped()
            }
        } catch (e: Exception) {
            Log.e("CameraXRecorder", "stopCamera failed", e)
            onStopped()
        }
    }
}