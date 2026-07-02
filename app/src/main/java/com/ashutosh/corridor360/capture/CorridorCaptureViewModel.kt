package com.ashutosh.corridor360.capture // TODO: match your package

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Coverage thresholds — tune these against real capture tests. Corridor scanning
 * is translation-dominant (walking forward) rather than pure rotation, so both
 * matter, whichever triggers first counts as "enough movement for a new frame".
 */
private const val YAW_THRESHOLD_DEGREES = 15f
private const val TRANSLATION_THRESHOLD_METERS = 0.5f
private const val MIN_FRAMES_TO_ENABLE_STITCH = 6

data class CaptureUiState(
    val isSessionReady: Boolean = false,
    val distanceToSurfaceMeters: Float? = null,
    val framesCaptured: Int = 0,
    val coverageProgress: Float = 0f, // 0f..1f, drives the on-screen ring
    val readyToCapture: Boolean = false, // enough movement since last frame
    val readyToStitch: Boolean = false,
    val errorMessage: String? = null
)

class CorridorCaptureViewModel(
    // TODO: inject your actual repository / DAO instead of this placeholder
    private val repository: CorridorCaptureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var lastCapturedPose: CapturePose? = null

    fun onSessionReady() {
        _uiState.value = _uiState.value.copy(isSessionReady = true)
    }

    fun onSessionFailed(message: String) {
        _uiState.value = _uiState.value.copy(isSessionReady = false, errorMessage = message)
    }

    /** Call this on every ARCore frame update (e.g. from a Choreographer callback). */
    fun onPoseUpdated(pose: CapturePose?, distanceMeters: Float?) {
        if (pose == null) return

        val movedEnough = lastCapturedPose?.let { last ->
            val yawDelta = abs(pose.yawDegrees - last.yawDegrees)
            val translationDelta = sqrt(
                (pose.x - last.x) * (pose.x - last.x) +
                        (pose.y - last.y) * (pose.y - last.y) +
                        (pose.z - last.z) * (pose.z - last.z)
            )
            yawDelta >= YAW_THRESHOLD_DEGREES || translationDelta >= TRANSLATION_THRESHOLD_METERS
        } ?: true // first frame is always ready

        val progress = lastCapturedPose?.let { last ->
            val yawDelta = abs(pose.yawDegrees - last.yawDegrees) / YAW_THRESHOLD_DEGREES
            val translationDelta = sqrt(
                (pose.x - last.x) * (pose.x - last.x) +
                        (pose.y - last.y) * (pose.y - last.y) +
                        (pose.z - last.z) * (pose.z - last.z)
            ) / TRANSLATION_THRESHOLD_METERS
            maxOf(yawDelta, translationDelta).coerceIn(0f, 1f)
        } ?: 1f

        _uiState.value = _uiState.value.copy(
            distanceToSurfaceMeters = distanceMeters,
            readyToCapture = movedEnough,
            coverageProgress = progress
        )
    }

    /**
     * Call when the user taps the capture button (or auto-capture fires once
     * readyToCapture is true, if you want a hands-free flow).
     */
    fun captureFrame(pose: CapturePose, imagePath: String) {
        viewModelScope.launch {
            repository.saveFrame(
                imagePath = imagePath,
                x = pose.x,
                y = pose.y,
                z = pose.z,
                yawDegrees = pose.yawDegrees
            )
            lastCapturedPose = pose
            val newCount = _uiState.value.framesCaptured + 1
            _uiState.value = _uiState.value.copy(
                framesCaptured = newCount,
                readyToCapture = false,
                coverageProgress = 0f,
                readyToStitch = newCount >= MIN_FRAMES_TO_ENABLE_STITCH
            )
        }
    }

    fun resetSegment() {
        lastCapturedPose = null
        _uiState.value = CaptureUiState(isSessionReady = _uiState.value.isSessionReady)
    }
}

/** TODO: replace with your real Room-backed repository. */
interface CorridorCaptureRepository {
    suspend fun saveFrame(imagePath: String, x: Float, y: Float, z: Float, yawDegrees: Float)
}