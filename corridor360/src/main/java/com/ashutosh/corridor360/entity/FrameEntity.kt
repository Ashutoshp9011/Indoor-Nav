package com.ashutosh.corridor360.Data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "frames")
data class FrameEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val imagePath: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val segmentId: String,
    val synced: Boolean = false
)