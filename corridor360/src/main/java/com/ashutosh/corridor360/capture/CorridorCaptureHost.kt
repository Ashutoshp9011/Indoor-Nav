// CorridorCaptureHost.kt
package com.ashutosh.corridor360.capture

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.ashutosh.corridor360.ar.ARCoreDistanceManager
import com.ashutosh.corridor360.camera.CameraXRecorder

enum class CaptureState {
    CAMERA_ACTIVE,       // CameraX preview live, ready for user to tap Capture
    STOPPING_CAMERA,     // waiting for confirmed CameraX release
    ARCORE_READING,      // ARCore resumed, reading one frame's pose
    RESTARTING_CAMERA,   // ARCore paused, CameraX restarting
    ERROR
}

data class CapturePose(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float,
    val distanceMeters: Float
)

/**
 * Orchestrates sequential camera ownership for a single corridor node capture.
 * Owns CameraXRecorder + ARCoreDistanceManager (reused as-is, no duplicate
 * ARCore session class). Neither CameraX nor ARCore is ever active at the
 * same time — enforced by the state transitions below.
 *
 * Instantiate this from CorridorCaptureScreen (remember { }), scoped to the
 * screen's lifecycle — matches your existing no-DI, manual-wiring pattern
 * (same as CameraXRecorder itself takes Context + LifecycleOwner directly).
 */
class CorridorCaptureHost(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val nodeId: String,
    private val onStateChanged: (CaptureState) -> Unit,
    private val onPoseCaptured: (CapturePose, imagePath: String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val cameraXRecorder = CameraXRecorder(context, lifecycleOwner)
    private val arCoreManager = ARCoreDistanceManager(context)

    private var previewView: PreviewView? = null
    private var state: CaptureState = CaptureState.CAMERA_ACTIVE
        set(value) {
            field = value
            onStateChanged(value)
        }

    /** Step 1: called once when entering the capture screen for this node. */
    fun startPreview(previewView: PreviewView) {
        this.previewView = previewView
        cameraXRecorder.startCamera(
            previewView,
            onReady = { state = CaptureState.CAMERA_ACTIVE },
            onError = { onError("Camera start failed: ${it.message}") }
        )
    }

    /**
     * Steps 2–10: full sequential handoff triggered by the Capture button.
     * Only proceeds if CameraX is currently the active owner.
     */
    fun onCaptureRequested() {
        if (state != CaptureState.CAMERA_ACTIVE) return // ignore taps mid-sequence

        // Step 2 already happened (this call). Step 3: stop CameraX.
        cameraXRecorder.captureFrame(
            nodeId = nodeId,
            onSaved = { imagePath -> stopCameraThenReadPose(imagePath) },
            onError = { onError("Frame capture failed: ${it.message}") }
        )
    }

    private fun stopCameraThenReadPose(imagePath: String) {
        state = CaptureState.STOPPING_CAMERA
        // Step 3: stop CameraX completely, wait for confirmed release
        cameraXRecorder.stopCamera(onStopped = {
            resumeArCoreAndReadFrame(imagePath)
        })
    }

    private fun resumeArCoreAndReadFrame(imagePath: String) {
        state = CaptureState.ARCORE_READING

        // Step 4: resume ARCore session
        val resumed = arCoreManager.resume()
        if (!resumed) {
            onError("ARCore session could not resume (camera still busy or ARCore unavailable)")
            restartCamera()
            return
        }

        // Step 5: update one ARCore frame
        val frame = arCoreManager.update()
        if (frame == null) {
            onError("ARCore frame update failed")
            pauseArCoreAndRestartCamera()
            return
        }

        // Step 6: read pose/distance
        val distance = arCoreManager.computeDistanceMeters()
        val cameraPose = frame.camera.pose
        val pose = CapturePose(
            x = cameraPose.tx(),
            y = cameraPose.ty(),
            z = cameraPose.tz(),
            yawDegrees = cameraPose.rotationQuaternion.let { q ->
                // yaw from quaternion — matches ViewModel's yawDegrees usage
                Math.toDegrees(
                    Math.atan2(
                        2.0 * (q[3] * q[1] + q[0] * q[2]),
                        1.0 - 2.0 * (q[1] * q[1] + q[2] * q[2])
                    )
                ).toFloat()
            },
            distanceMeters = distance
        )

        // Step 7: save pose metadata (delegated to caller -> ViewModel -> repository)
        onPoseCaptured(pose, imagePath)

        // Step 8: pause ARCore, Step 9: restart CameraX
        pauseArCoreAndRestartCamera()
    }

    private fun pauseArCoreAndRestartCamera() {
        arCoreManager.pause()
        restartCamera()
    }

    private fun restartCamera() {
        state = CaptureState.RESTARTING_CAMERA
        val view = previewView
        if (view == null) {
            onError("PreviewView not available to restart camera")
            return
        }
        // Step 9: restart CameraX preview
        cameraXRecorder.startCamera(
            view,
            onReady = { state = CaptureState.CAMERA_ACTIVE }, // Step 10: ready for next capture
            onError = { onError("Camera restart failed: ${it.message}") }
        )
    }

    /** Call from the Screen's DisposableEffect onDispose. */
    fun release() {
        cameraXRecorder.stopCamera()
        arCoreManager.destroy()
    }
}