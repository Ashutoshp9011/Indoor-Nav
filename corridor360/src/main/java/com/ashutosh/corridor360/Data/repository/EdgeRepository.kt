package com.ashutosh.corridor360.Data.repository

import com.ashutosh.corridor360.Data.local.dao.EdgeDao
import com.ashutosh.corridor360.Data.local.EdgeEntity
import kotlinx.coroutines.flow.Flow

class EdgeRepository(private val edgeDao: EdgeDao) {

    val allEdges: Flow<List<EdgeEntity>> = edgeDao.getAllEdges()

    suspend fun addEdge(edge: EdgeEntity) = edgeDao.insertEdge(edge)

    suspend fun addEdges(edges: List<EdgeEntity>) = edgeDao.insertEdges(edges)

    suspend fun updateEdge(edge: EdgeEntity) = edgeDao.updateEdge(edge)

    // If EdgeEntity's primary key is an auto-generated Int (Room default for @PrimaryKey(autoGenerate = true) Int),
    // use this signature instead:
    // suspend fun deleteEdge(id: Int) = edgeDao.deleteEdgeById(id)

    // If EdgeEntity's primary key is a String (matching your NodeEntity pattern), keep this:
    suspend fun deleteEdge(id: String) = edgeDao.deleteEdgeById(id)

    suspend fun clearAll() = edgeDao.deleteAllEdges()
}