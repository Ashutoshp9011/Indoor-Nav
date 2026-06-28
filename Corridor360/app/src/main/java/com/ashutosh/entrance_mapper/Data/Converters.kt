// data/db/Converters.kt
package com.ashutosh.entrance_mapper.data.db

import androidx.room.TypeConverter
import com.ashutosh.entrance_mapper.data.model.NodeStatus
import com.ashutosh.entrance_mapper.data.model.NodeType

class Converters {

    @TypeConverter
    fun fromNodeType(value: NodeType): String = value.name

    @TypeConverter
    fun toNodeType(value: String): NodeType = NodeType.valueOf(value)

    @TypeConverter
    fun fromNodeStatus(value: NodeStatus): String = value.name

    @TypeConverter
    fun toNodeStatus(value: String): NodeStatus = NodeStatus.valueOf(value)
}