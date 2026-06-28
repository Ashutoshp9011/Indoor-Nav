package com.ashutosh.entrance_mapper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NodeType { ROOM, JUNCTION, STAIRS, LIFT, ENTRANCE }
enum class NodeStatus { UNMAPPED, MAPPED, INVALIDATED }

@Entity(tableName = "nodes")
data class Node(
    @PrimaryKey val id: String,
    val floor: Int,
    val label: String,
    val type: NodeType,
    val x: Float,
    val y: Float,
    val status: NodeStatus = NodeStatus.UNMAPPED,
    val createdAt: Long = System.currentTimeMillis()
)
