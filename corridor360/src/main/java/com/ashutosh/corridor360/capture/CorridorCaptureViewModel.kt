package com.ashutosh.corridor360.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MIN_FRAMES_TO_ENABLE_STITCH = 6

data class CaptureUiState(
    val captureState: CaptureState = CaptureState.CAMERA_ACTIVE,
    val lastDistanceMeters: Float? = null,
    val framesCaptured: Int = 0,
    val readyToStitch: Boolean = false,
    val errorMessage: String? = null
)

sealed class CaptureEvent {
    object NavigateToStitching : CaptureEvent()
}

class CorridorCaptureViewModel(
    // TODO: inject your actual repository / DAO instead of this placeholder
    private val repository: CorridorCaptureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CaptureEvent>()
    val events = _events.asSharedFlow()

    /**
     * Forwarded from CorridorCaptureHost's onStateChanged callback (via the Screen).
     * Drives UI feedback during the stop-camera / arcore-read / restart-camera
     * sequence — e.g. disable the Capture button, show a status label.
     */
    fun onCaptureStateChanged(state: CaptureState) {
        _uiState.value = _uiState.value.copy(captureState = state, errorMessage = null)
    }

    /**
     * Forwarded from CorridorCaptureHost's onPoseCaptured callback. One call per
     * completed sequential handoff (CameraX frame + single ARCore pose read).
     */
    fun onPoseCaptured(pose: CapturePose, imagePath: String) {
        viewModelScope.launch {
            repository.saveFrame(
                imagePath = imagePath,
                x = pose.x,
                y = pose.y,
                z = pose.z,
                yawDegrees = pose.yawDegrees
            )
            val newCount = _uiState.value.framesCaptured + 1
            _uiState.value = _uiState.value.copy(
                lastDistanceMeters = pose.distanceMeters,
                framesCaptured = newCount,
                readyToStitch = newCount >= MIN_FRAMES_TO_ENABLE_STITCH
            )
        }
    }

    /** Forwarded from CorridorCaptureHost's onError callback. */
    fun onCaptureError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Triggers the stitching process for the current segment.
     * Only proceeds if the minimum number of frames has been met.
     */
    fun onStitchRequested() {
        if (!_uiState.value.readyToStitch) return
        // TODO: Implement navigation to stitching screen or trigger background processing
    }

    fun resetSegment() {
        _uiState.value = CaptureUiState(captureState = _uiState.value.captureState)
    }
}

/** TODO: replace with your real Room-backed repository. */
interface CorridorCaptureRepository {
    suspend fun saveFrame(imagePath: String, x: Float, y: Float, z: Float, yawDegrees: Float)
}