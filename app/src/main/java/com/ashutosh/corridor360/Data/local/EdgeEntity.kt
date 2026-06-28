package com.ashutosh.corridor360.Data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edges")
data class EdgeEntity(
    @PrimaryKey(autoGenerate = true) val edgeId: Int = 0,
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Float
)