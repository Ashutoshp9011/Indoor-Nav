package com.ashutosh.corridor360.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.corridor360.Data.repository.CorridorCaptureRepository
import com.ashutosh.corridor360.stitching.PanoramaStitcher
import com.ashutosh.corridor360.stitching.StitchResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CaptureUiState(
    val framesCaptured: Int = 0,
    val framePaths: List<String> = emptyList(),
    val lastDistanceMeters: Float? = null,
    val captureState: CaptureState = CaptureState.CAMERA_ACTIVE,
    val errorMessage: String? = null,
    val readyToStitch: Boolean = false
)

sealed class CaptureEvent {
    data class NavigateToStitching(val outputPath: String) : CaptureEvent()
}

class CorridorCaptureViewModel(
    private val repository: CorridorCaptureRepository,
    private val panoramaStitcher: PanoramaStitcher,
    private val segmentId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CaptureEvent>()
    val events: SharedFlow<CaptureEvent> = _events.asSharedFlow()

    init {
        // A prior attempt for this node may have left rows behind if stitching
        // failed (only cleared on success) — start this session with a clean slate
        // so old frames don't get counted or fed into the next stitch.
        viewModelScope.launch {
            repository.clearSession(segmentId)
        }
    }

    fun onCaptureStateChanged(state: CaptureState) {
        _uiState.update { it.copy(captureState = state) }
    }

    // Called by CorridorCaptureHost after a real CameraX+ARCore capture completes
    fun onPoseCaptured(pose: CapturePose, imagePath: String) {
        viewModelScope.launch {
            repository.saveFrame(
                imagePath = imagePath,
                x = pose.x,
                y = pose.y,
                z = pose.z,
                yawDegrees = pose.yawDegrees
            )
            val frames = repository.getFramesForSession(segmentId)
            _uiState.update {
                it.copy(
                    framesCaptured = frames.size,
                    framePaths = frames.map { f -> f.imagePath },
                    lastDistanceMeters = pose.distanceMeters,
                    readyToStitch = frames.size >= 2
                )
            }
        }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(errorMessage = message, captureState = CaptureState.ERROR) }
    }

    fun onStitchRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(captureState = CaptureState.STITCHING) }
            val frames = repository.getFramesForSession(segmentId)
            val imagePaths = frames.map { it.imagePath }

            when (val result = panoramaStitcher.stitch(imagePaths)) {
                is StitchResult.Success -> {
                    repository.saveStitchedPanorama(segmentId, result.outputPath)
                    _uiState.update { it.copy(captureState = CaptureState.STITCH_COMPLETE) }
                    _events.emit(CaptureEvent.NavigateToStitching(result.outputPath))
                }
                is StitchResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Stitch failed: ${result.reason}",
                            captureState = CaptureState.ERROR
                        )
                    }
                }
            }
        }
    }
}