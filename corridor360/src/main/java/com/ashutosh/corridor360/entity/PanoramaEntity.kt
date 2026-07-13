package com.ashutosh.corridor360.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "panoramas")

data class PanoramaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: String,
    val panoramaPath: String
)
