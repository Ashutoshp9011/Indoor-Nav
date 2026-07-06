package com.ashutosh.corridor360.capture

import com.ashutosh.corridor360.data.local.dao.FrameDao
import com.ashutosh.corridor360.data.local.entity.FrameEntity

/**
 * Real Room-backed implementation, replacing the TODO interface stub
 * in CorridorCaptureViewModel.kt. segmentId = nodeId, per your capture flow.
 */
class RoomCorridorCaptureRepository(
    private val frameDao: FrameDao,
    private val segmentId: String
) : CorridorCaptureRepository {

    override suspend fun saveFrame(imagePath: String, x: Float, y: Float, z: Float, yawDegrees: Float) {
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
}