package com.ashutosh.entrance_mapper.data.db

import androidx.room.*
import com.ashutosh.entrance_mapper.data.model.Node
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {

    @Query("SELECT * FROM nodes WHERE floor = :floor ORDER BY createdAt ASC")
    fun getNodesForFloor(floor: Int): Flow<List<Node>>

    @Query("SELECT * FROM nodes ORDER BY createdAt ASC")
    suspend fun getAllNodes(): List<Node>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: Node)

    @Update
    suspend fun updateNode(node: Node)

    @Delete
    suspend fun deleteNode(node: Node)

    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getNodeById(id: String): Node?
}
