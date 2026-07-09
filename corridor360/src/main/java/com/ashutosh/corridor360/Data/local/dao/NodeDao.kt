package com.ashutosh.corridor360.Data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ashutosh.corridor360.entity.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: NodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NodeEntity>)

    @Update
    suspend fun updateNode(node: NodeEntity)

    @Query("SELECT * FROM nodes")
    fun getAllNodes(): Flow<List<NodeEntity>>

    @Query("DELETE FROM nodes")
    suspend fun deleteAllNodes()

    @Query("SELECT * FROM nodes WHERE nodeId = :id")
    suspend fun getNodeById(id: String): NodeEntity?

    @Query("DELETE FROM nodes WHERE nodeId = :id")
    suspend fun deleteNodeById(id: String)
}