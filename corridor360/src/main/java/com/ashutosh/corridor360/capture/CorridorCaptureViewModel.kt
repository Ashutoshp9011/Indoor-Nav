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
    // One output path per successfully stitched layer (e.g. "MIDDLE" -> path).
    // Layers with too few frames or a stitch failure are omitted here and
    // surfaced instead via errorMessage, naming which ring needs a rescan.
    data class NavigateToStitching(val outputPathsByLayer: Map<String, String>) : CaptureEvent()
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
    fun onPoseCaptured(pose: CapturePose, imagePath: String, layer: CaptureLayer) {
        viewModelScope.launch {
            repository.saveFrame(
                imagePath = imagePath,
                x = pose.x,
                y = pose.y,
                z = pose.z,
                yawDegrees = pose.yawDegrees,
                layer = layer.name
            )
            val layers = repository.getLayersForSession(segmentId)
            val allFrames = layers.flatMap { repository.getFramesForSession(segmentId, it) }
            _uiState.update {
                it.copy(
                    framesCaptured = allFrames.size,
                    framePaths = allFrames.map { f -> f.imagePath },
                    lastDistanceMeters = pose.distanceMeters,
                    readyToStitch = allFrames.size >= 2
                )
            }
        }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(errorMessage = message, captureState = CaptureState.ERROR) }
    }

    // Stitches each captured layer (tilt ring) independently — PanoramaStitcher
    // chains adjacent frames assuming one continuous rotational sweep, so
    // layers must never be mixed into a single stitch() call.
    fun onStitchRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(captureState = CaptureState.STITCHING) }

            val layers = repository.getLayersForSession(segmentId)
            val succeeded = mutableMapOf<String, String>()
            val failedLayers = mutableListOf<String>()

            for (layer in layers) {
                val imagePaths = repository.getFramesForSession(segmentId, layer).map { it.imagePath }
                if (imagePaths.size < 2) {
                    failedLayers += layer
                    continue
                }
                when (val result = panoramaStitcher.stitch(imagePaths, fileName = "${segmentId}_$layer")) {
                    is StitchResult.Success -> {
                        repository.saveStitchedPanorama(segmentId, layer, result.outputPath)
                        succeeded[layer] = result.outputPath
                    }
                    is StitchResult.Failure -> failedLayers += layer
                }
            }

            if (failedLayers.isEmpty() && succeeded.isNotEmpty()) {
                _uiState.update { it.copy(captureState = CaptureState.STITCH_COMPLETE) }
                _events.emit(CaptureEvent.NavigateToStitching(succeeded))
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = if (failedLayers.isNotEmpty())
                            "Stitch failed for: ${failedLayers.joinToString()} — rescan ${if (failedLayers.size > 1) "these layers" else "this layer"}"
                        else "No frames captured",
                        captureState = CaptureState.ERROR
                    )
                }
            }
        }
    }
}