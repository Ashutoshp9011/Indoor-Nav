package com.ashutosh.corridor360.Data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EdgeDao {
    @Query("SELECT * FROM edges")
    fun getAllEdges(): Flow<List<EdgeEntity>>

    @Query("SELECT * FROM edges WHERE edgeId = :id")
    suspend fun getEdgeById(id: String): EdgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: EdgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<EdgeEntity>)

    @Update
    suspend fun updateEdge(edge: EdgeEntity)

    @Query("DELETE FROM edges WHERE edgeId = :id")
    suspend fun deleteEdgeById(id: String)   // ✅ matches EdgeEntity.edgeId

    @Query("DELETE FROM edges")
    suspend fun deleteAllEdges()
}
