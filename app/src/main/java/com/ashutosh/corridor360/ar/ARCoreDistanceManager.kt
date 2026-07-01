package com.ashutosh.corridor360.ar

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import kotlin.math.sqrt

class ARCoreDistanceManager(private val context: Context) {

    private var session: Session? = null
    private var installRequested = false

    private var pointA: FloatArray? = null   // world-space translation [x,y,z]
    private var pointB: FloatArray? = null

    val isPointASet: Boolean get() = pointA != null
    val isPointBSet: Boolean get() = pointB != null

    // Call in onResume of the capture screen. Returns false if ARCore isn't ready (handle install/update flow).
    fun resume(): Boolean {
        if (session == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(context as android.app.Activity, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return false
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> { /* continue */ }
                }
                session = Session(context).apply {
                    val config = Config(this).apply {
                        depthMode = if (isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                            Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                        focusMode = Config.FocusMode.AUTO
                    }
                    configure(config)
                }
            } catch (e: UnavailableException) {
                Log.e("ARCoreDistanceManager", "ARCore unavailable", e)
                return false
            }
        }
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e("ARCoreDistanceManager", "Camera in use — is CameraX still bound?", e)
            session = null
            return false
        }
        return true
    }

    fun pause() {
        session?.pause()
    }

    fun destroy() {
        session?.close()
        session = null
    }

    // Call every frame from your GL render loop (or a Handler loop if headless)
    fun update(): Frame? {
        return try {
            session?.update()
        } catch (e: CameraNotAvailableException) {
            Log.e("ARCoreDistanceManager", "update failed", e)
            null
        }
    }

    // Pass tap coordinates (from onTouch) + current Frame to place point A then point B
    fun placePoint(frame: Frame, motionEvent: MotionEvent): Boolean {
        val hits = frame.hitTest(motionEvent)
        val hit = hits.firstOrNull {
            it.trackable is Plane && (it.trackable as Plane).isPoseInPolygon(it.hitPose)
        } ?: hits.firstOrNull { it.trackable is Point }
        ?: return false

        val translation = hit.hitPose.translation // [x, y, z] in meters, world space
        if (pointA == null) {
            pointA = translation
        } else if (pointB == null) {
            pointB = translation
        }
        return true
    }

    // Returns straight-line distance once both points are placed, else null
    fun computeDistanceMeters(): Float? {
        val a = pointA ?: return null
        val b = pointB ?: return null
        val dx = a[0] - b[0]; val dy = a[1] - b[1]; val dz = a[2] - b[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun reset() {
        pointA = null
        pointB = null
    }
}