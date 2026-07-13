package com.ashutosh.corridor360.Data.repository

import com.ashutosh.corridor360.Data.local.dao.FrameDao
import com.ashutosh.corridor360.Data.local.dao.PanoramaDao
import com.ashutosh.corridor360.entity.FrameEntity
import com.ashutosh.corridor360.entity.PanoramaEntity

data class CorridorFrame(
    val imagePath: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float
)

interface CorridorCaptureRepository {
    suspend fun saveFrame(imagePath: String, x: Float, y: Float, z: Float, yawDegrees: Float)
    suspend fun getFramesForSession(segmentId: String): List<CorridorFrame>
    suspend fun saveStitchedPanorama(segmentId: String, panoramaPath: String)
}

/**
 * Real Room-backed implementation, replacing the TODO interface stub
 * in CorridorCaptureViewModel.kt. segmentId = nodeId, per your capture flow.
 */
class CorridorCaptureRepositoryImpl(
    private val frameDao: FrameDao,
    private val panoramaDao: PanoramaDao,
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

    override suspend fun getFramesForSession(segmentId: String): List<CorridorFrame> {
        return frameDao.getFramesForSegment(segmentId).map {
            CorridorFrame(
                imagePath = it.imagePath,
                x = it.x,
                y = it.y,
                z = it.z,
                yawDegrees = it.yawDegrees
            )
        }
    }

    override suspend fun saveStitchedPanorama(segmentId: String, panoramaPath: String) {
        panoramaDao.insert(
            PanoramaEntity(
                segmentId = segmentId,
                panoramaPath = panoramaPath
            )
        )
    }
}