package com.ashutosh.corridor360.Data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ashutosh.corridor360.Data.local.entity.FrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FrameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(frame: FrameEntity)

    @Update
    suspend fun update(frame: FrameEntity)

    @Query("SELECT * FROM frames WHERE segmentId = :segmentId ORDER BY timestamp ASC")
    suspend fun getFramesForSegment(segmentId: String): List<FrameEntity>

    @Query("SELECT * FROM frames WHERE segmentId = :segmentId ORDER BY timestamp ASC")
    fun observeFramesForSegment(segmentId: String): Flow<List<FrameEntity>>

    @Query("SELECT * FROM frames WHERE synced = 0")
    suspend fun getUnsyncedFrames(): List<FrameEntity>

    @Query("UPDATE frames SET synced = 1 WHERE id = :frameId")
    suspend fun markSynced(frameId: String)

    @Query("DELETE FROM frames WHERE segmentId = :segmentId")
    suspend fun deleteFramesForSegment(segmentId: String)
}
