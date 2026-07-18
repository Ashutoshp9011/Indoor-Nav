package com.ashutosh.corridor360.capture

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.ashutosh.corridor360.camera.CameraXRecorder

/**
 * PHASE 1 (current): ARCore disabled — no supported test device available.
 * Capture goes straight CAMERA_ACTIVE -> save frame -> CAMERA_ACTIVE, no
 * stop/restart cycle, pose saved as a placeholder (0,0,0,0).
 *
 * PHASE 2 (future, when an ARCore-supported device is available): re-enable
 * the sequential handoff by restoring the commented-out block in
 * onCaptureRequested() below, which stops CameraX, resumes ARCoreDistanceManager,
 * reads one real pose, pauses ARCore, and restarts CameraX before saving.
 * CorridorCaptureScreen.kt does NOT need to change for this — it only calls
 * startPreview(), onCaptureRequested(), and release(), and only observes
 * captureState/framesCaptured/errorMessage via the ViewModel. The state
 * machine transitions (STOPPING_CAMERA/ARCORE_READING/RESTARTING_CAMERA) are
 * already defined in CaptureState below so the enum doesn't need to change
 * either — only this class's internal logic does.
 */

class CorridorCaptureHost(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val nodeId: String,
    private val onStateChanged: (CaptureState) -> Unit,
    private val onPoseCaptured: (CapturePose, imagePath: String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val cameraRecorder = CameraXRecorder(context, lifecycleOwner)
    private var previewView: PreviewView? = null

    fun startPreview(view: PreviewView) {
        previewView = view
        // A prior attempt for this node may have left frames behind if stitching
        // failed (clearFrames is only called on success) — start clean so those
        // don't get mixed into this session's stitch input.
        cameraRecorder.clearFrames(nodeId)
        onStateChanged(CaptureState.CAMERA_ACTIVE)
        cameraRecorder.startCamera(
            previewView = view,
            onReady = { onStateChanged(CaptureState.CAMERA_ACTIVE) },
            onError = { e -> onError("Camera failed to start: ${e.message}") }
        )
    }

    /**
     * PHASE 1: captures directly, no ARCore step. Pose is a placeholder —
     * downstream code (Room, PanoramaStitcher) already expects a CapturePose,
     * so this keeps that contract intact rather than making pose nullable
     * everywhere, which would need undoing again in Phase 2.
     */
    fun onCaptureRequested() {
        cameraRecorder.captureFrame(
            nodeId = nodeId,
            onSaved = { imagePath ->
                val placeholderPose = CapturePose(x = 0f, y = 0f, z = 0f, yawDegrees = 0f, distanceMeters = null)
                onPoseCaptured(placeholderPose, imagePath)
                onStateChanged(CaptureState.CAMERA_ACTIVE)
            },
            onError = { e -> onError("Capture failed: ${e.message}") }
        )

        /*
        // PHASE 2 — restore this block and delete the direct-capture code above
        // once you have an ARCore-supported test device:
        //
        // onStateChanged(CaptureState.STOPPING_CAMERA)
        // cameraRecorder.stopCamera(onStopped = {
        //     onStateChanged(CaptureState.ARCORE_READING)
        //     arManager.resume()
        //     val frame = arManager.update()
        //     val pose = frame?.let { arManager.latestPose(it) }
        //     arManager.pause()
        //     onStateChanged(CaptureState.RESTARTING_CAMERA)
        //     previewView?.let { view ->
        //         cameraRecorder.startCamera(view, onReady = {
        //             if (pose != null) {
        //                 cameraRecorder.captureFrame(nodeId,
        //                     onSaved = { path -> onPoseCaptured(pose, path); onStateChanged(CaptureState.CAMERA_ACTIVE) },
        //                     onError = { e -> onError("Capture failed: ${e.message}") }
        //                 )
        //             } else {
        //                 onError("ARCore tracking not stable — try again")
        //                 onStateChanged(CaptureState.CAMERA_ACTIVE)
        //             }
        //         }, onError = { e -> onError("Camera restart failed: ${e.message}") })
        //     }
        // })
        */
    }

    fun toggleFlash(enabled: Boolean) {
        cameraRecorder.setTorchEnabled(enabled)
    }

    fun release() {
        cameraRecorder.stopCamera()
    }
}