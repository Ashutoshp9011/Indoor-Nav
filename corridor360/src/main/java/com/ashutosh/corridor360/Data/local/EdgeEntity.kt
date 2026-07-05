package com.ashutosh.corridor360.Data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edges")
data class EdgeEntity(
    @PrimaryKey val edgeId: String = java.util.UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Float
)