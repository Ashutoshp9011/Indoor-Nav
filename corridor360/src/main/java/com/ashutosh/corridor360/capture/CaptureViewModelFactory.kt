package com.ashutosh.corridor360.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashutosh.corridor360.Data.local.dao.FrameDao
import com.ashutosh.corridor360.Data.local.dao.PanoramaDao
import com.ashutosh.corridor360.Data.repository.CorridorCaptureRepository
import com.ashutosh.corridor360.Data.repository.CorridorCaptureRepositoryImpl
import com.ashutosh.corridor360.stitching.PanoramaStitcher
import java.io.File

/**
 * Manual factory, consistent with the rest of the project's DI-free approach.
 * Construct in your Activity/Fragment/nav destination like:
 *
 *   val factory = CaptureViewModelFactory(
 *       frameDao = database.frameDao(),
 *       panoramaDao = database.panoramaDao(),
 *       nodeId = segmentId,
 *       outputDir = context.filesDir
 *   )
 *   val viewModel: CorridorCaptureViewModel = viewModel(factory = factory)
 */
class CaptureViewModelFactory(
    private val frameDao: FrameDao,
    private val panoramaDao: PanoramaDao,
    private val nodeId: String,
    private val outputDir: File
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CorridorCaptureViewModel::class.java)) {
            val repository: CorridorCaptureRepository =
                CorridorCaptureRepositoryImpl(frameDao, panoramaDao, nodeId)
            val panoramaStitcher = PanoramaStitcher(outputDir)
            @Suppress("UNCHECKED_CAST")
            return CorridorCaptureViewModel(
                repository = repository,
                panoramaStitcher = panoramaStitcher,
                segmentId = nodeId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}