package com.ashutosh.corridor360.capture

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException

/**
 * Wraps an ARCore Session and exposes just what Corridor360's capture flow needs:
 * - current camera pose (position + yaw) for coverage tracking
 * - distance-to-surface via hit-test (center of screen) for the on-screen readout
 *
 * Lifecycle: call resume()/pause() from the hosting Activity/Fragment lifecycle,
 * and close() when the screen is destroyed.
 */
class ARCoreSessionHelper(private val context: Context) {

    var session: Session? = null
        private set

    fun createSession(): Boolean {
        return try {
            val newSession = Session(context)
            val config = Config(newSession).apply {
                // Corridor scanning is translation-dominant; disable plane finding
                // extras you don't need to keep frame processing fast.
                depthMode = if (newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
                focusMode = Config.FocusMode.AUTO
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            }
            newSession.configure(config)
            session = newSession
            true
        } catch (e: UnavailableException) {
            session = null
            false
        }
    }

    fun resume() {
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            // Surface to UI via a StateFlow<CaptureError> in the ViewModel if needed
        }
    }

    fun pause() {
        session?.pause()
    }

    fun close() {
        session?.close()
        session = null
    }

    /**
     * Returns the latest frame's camera pose as (x, y, z, yawDegrees), or null if
     * tracking isn't stable yet. Yaw is derived from the pose quaternion around
     * the vertical axis, which is what matters for "have I turned enough since
     * the last capture" coverage checks.
     */
    fun latestPose(): CapturePose? {
        val frame: Frame = session?.update() ?: return null
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) return null

        val pose = camera.pose
        val yawDegrees = Math.toDegrees(
            Math.atan2(
                2.0 * (pose.qw() * pose.qy() + pose.qx() * pose.qz()),
                1.0 - 2.0 * (pose.qy() * pose.qy() + pose.qz() * pose.qz())
            )
        ).toFloat()

        return CapturePose(
            x = pose.tx(),
            y = pose.ty(),
            z = pose.tz(),
            yawDegrees = yawDegrees
        )
    }

    /**
     * Hit-tests the center of the screen against depth/plane data to estimate
     * distance to the nearest surface (e.g. the corridor wall being mapped).
     * Returns null if no hit or tracking isn't stable.
     */
    fun distanceToSurfaceMeters(screenWidth: Int, screenHeight: Int): Float? {
        val frame = session?.update() ?: return null
        if (frame.camera.trackingState != TrackingState.TRACKING) return null

        val hits = frame.hitTest(screenWidth / 2f, screenHeight / 2f)
        val closest = hits.minByOrNull { it.distance } ?: return null
        return closest.distance
    }
}

data class CapturePose(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float
)