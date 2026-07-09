package com.ashutosh.corridor360.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashutosh.corridor360.Data.local.dao.FrameDao
import com.ashutosh.corridor360.Data.repository.CorridorCaptureRepository
import com.ashutosh.corridor360.Data.repository.CorridorCaptureRepositoryImpl

/**
 * Manual factory, consistent with the rest of the project's DI-free approach.
 * Construct in your Activity/Fragment/nav destination like:
 *
 *   val factory = CaptureViewModelFactory(frameDao = database.frameDao(), nodeId = segmentId)
 *   val viewModel: CorridorCaptureViewModel = viewModel(factory = factory)
 */
class CaptureViewModelFactory(
    private val frameDao: FrameDao,
    private val nodeId: String
) : ViewModelProvider.Factory
{

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CorridorCaptureViewModel::class.java)) {
            val repository = CorridorCaptureRepositoryImpl(frameDao, nodeId)
            @Suppress("UNCHECKED_CAST")
            return CorridorCaptureViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}