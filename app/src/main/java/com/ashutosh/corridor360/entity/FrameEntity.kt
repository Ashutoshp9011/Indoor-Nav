package com.ashutosh.corridor360.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A single captured frame within a corridor segment. Uses a UUID string key,
 * consistent with EdgeEntity, to avoid collisions when multiple team members'
 * devices sync frames from different capture sessions.
 */
@Entity(tableName = "frames")
data class FrameEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val imagePath: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float,
    val timestamp: Long = System.currentTimeMillis(),
    // Links this frame to the corridor segment it belongs to.
    val segmentId: String,
    val synced: Boolean = false
)