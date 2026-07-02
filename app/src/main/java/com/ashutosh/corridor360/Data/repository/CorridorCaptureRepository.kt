package com.ashutosh.corridor360.data.repository

import com.ashutosh.corridor360.capture.CorridorCaptureRepository
import com.ashutosh.corridor360.data.local.dao.FrameDao
import com.ashutosh.corridor360.data.local.entity.FrameEntity

/**
 * Room-backed implementation of the repository interface stubbed in
 * CorridorCaptureViewModel.kt. Construct this with a real FrameDao (from your
 * CorridorDatabase) via your manual ViewModel factory.
 */
class CorridorCaptureRepositoryImpl(
    private val frameDao: FrameDao,
    // Set by MappingScreen/nav args before capture starts — which corridor
    // segment these frames belong to.
    private val segmentId: String
) : CorridorCaptureRepository {

    override suspend fun saveFrame(
        imagePath: String,
        x: Float,
        y: Float,
        z: Float,
        yawDegrees: Float
    ) {
        frameDao.insert(
            FrameEntity(
                imagePath = imagePath,
                x = x,
                y = y,
                z = z,
                yawDegrees = yawDegrees,
                segmentId = segmentId
            )
        )
    }

    suspend fun framesForCurrentSegment(): List<FrameEntity> =
        frameDao.getFramesForSegment(segmentId)

    suspend fun clearCurrentSegment() =
        frameDao.deleteFramesForSegment(segmentId)
}