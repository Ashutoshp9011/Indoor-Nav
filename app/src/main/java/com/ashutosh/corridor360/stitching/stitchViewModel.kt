package com.ashutosh.corridor360.stitching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class StitchUiState {
    object Idle : StitchUiState()
    object Stitching : StitchUiState()
    data class Done(val outputPath: String) : StitchUiState()
    data class Error(val message: String) : StitchUiState()
}

/**
 * Call runStitch() from wherever "Finish Segment" is wired (e.g. after
 * CorridorCaptureHost's onFinishSegment callback), passing the frame paths
 * for the completed segment — likely fetched from
 * CorridorCaptureRepositoryImpl.framesForCurrentSegment().
 */
class StitchViewModel(
    private val stitcher: PanoramaStitcher = PanoramaStitcher(),
    private val outputDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow<StitchUiState>(StitchUiState.Idle)
    val uiState: StateFlow<StitchUiState> = _uiState.asStateFlow()

    fun runStitch(segmentId: String, framePaths: List<String>) {
        _uiState.value = StitchUiState.Stitching

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                stitcher.stitch(
                    framePaths = framePaths,
                    outputDir = outputDir,
                    outputFileName = "panorama_$segmentId.jpg"
                )
            }

            _uiState.value = when (result) {
                is StitchResult.Success -> StitchUiState.Done(result.outputPath)
                is StitchResult.Failure -> StitchUiState.Error(result.reason)
            }
        }
    }

    fun reset() {
        _uiState.value = StitchUiState.Idle
    }
}