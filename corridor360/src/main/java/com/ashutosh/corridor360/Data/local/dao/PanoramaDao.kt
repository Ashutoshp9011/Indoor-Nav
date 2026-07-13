package com.ashutosh.corridor360.Data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.ashutosh.corridor360.entity.PanoramaEntity

@Dao
interface PanoramaDao {
    @Insert
    suspend fun insert(panorama: PanoramaEntity)
}