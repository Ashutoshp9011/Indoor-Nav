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
    suspend fun saveFrame(imagePath: String, x: Float, y: Float, z: Float, yawDegrees: Float, layer: String)
    suspend fun getFramesForSession(segmentId: String, layer: String): List<CorridorFrame>
    suspend fun getLayersForSession(segmentId: String): List<String>
    suspend fun saveStitchedPanorama(segmentId: String, layer: String, panoramaPath: String)
    suspend fun clearSession(segmentId: String)
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

    override suspend fun saveFrame(imagePath: String, x: Float, y: Float, z: Float, yawDegrees: Float, layer: String) {
        frameDao.insert(
            FrameEntity(
                imagePath = imagePath,
                x = x,
                y = y,
                z = z,
                yawDegrees = yawDegrees,
                segmentId = segmentId,
                layer = layer
            )
        )
    }

    override suspend fun getFramesForSession(segmentId: String, layer: String): List<CorridorFrame> {
        return frameDao.getFramesForSegmentLayer(segmentId, layer).map {
            CorridorFrame(
                imagePath = it.imagePath,
                x = it.x,
                y = it.y,
                z = it.z,
                yawDegrees = it.yawDegrees
            )
        }
    }

    override suspend fun getLayersForSession(segmentId: String): List<String> {
        return frameDao.getLayersForSegment(segmentId)
    }

    override suspend fun saveStitchedPanorama(segmentId: String, layer: String, panoramaPath: String) {
        panoramaDao.insert(
            PanoramaEntity(
                segmentId = segmentId,
                layer = layer,
                panoramaPath = panoramaPath
            )
        )
    }

    override suspend fun clearSession(segmentId: String) {
        frameDao.deleteFramesForSegment(segmentId)
    }
}