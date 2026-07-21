package com.ashutosh.corridor360.Data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ashutosh.corridor360.entity.FrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FrameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(frame: FrameEntity)

    @Update
    suspend fun update(frame: FrameEntity)

    @Query("SELECT * FROM frames WHERE segmentId = :segmentId ORDER BY timestamp ASC")
    suspend fun getFramesForSegment(segmentId: String): List<FrameEntity>

    // Frames within one segment span multiple tilt rings (layers), captured
    // sequentially. Stitching must pull one layer at a time — PanoramaStitcher
    // chains adjacent frames assuming a single rotational sweep, so mixing
    // layers into one call would try to homography-match the last frame of one
    // ring against the first frame of the next, which don't overlap.
    @Query("SELECT * FROM frames WHERE segmentId = :segmentId AND layer = :layer ORDER BY timestamp ASC")
    suspend fun getFramesForSegmentLayer(segmentId: String, layer: String): List<FrameEntity>

    @Query("SELECT DISTINCT layer FROM frames WHERE segmentId = :segmentId")
    suspend fun getLayersForSegment(segmentId: String): List<String>

    @Query("SELECT * FROM frames WHERE segmentId = :segmentId ORDER BY timestamp ASC")
    fun observeFramesForSegment(segmentId: String): Flow<List<FrameEntity>>

    @Query("SELECT * FROM frames WHERE synced = 0")
    suspend fun getUnsyncedFrames(): List<FrameEntity>

    @Query("UPDATE frames SET synced = 1 WHERE id = :frameId")
    suspend fun markSynced(frameId: String)

    @Query("DELETE FROM frames WHERE segmentId = :segmentId")
    suspend fun deleteFramesForSegment(segmentId: String)
}
