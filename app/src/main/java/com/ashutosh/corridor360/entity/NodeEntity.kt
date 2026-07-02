package com.ashutosh.corridor360.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val nodeId: String,
    val name: String,
    val floor: Int,
    val x: Float,
    val y: Float,
    val status: String,        // "unmapped" or "mapped"
    val panoramaPath: String? = null
)