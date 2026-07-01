package com.ashutosh.corridor360.Data.repository

import com.ashutosh.corridor360.Data.local.NodeDao
import com.ashutosh.corridor360.Data.local.NodeEntity
import kotlinx.coroutines.flow.Flow

class NodeRepository(private val nodeDao: NodeDao) {

    val allNodes: Flow<List<NodeEntity>> = nodeDao.getAllNodes()

    suspend fun getNode(id: String): NodeEntity? = nodeDao.getNodeById(id)

    suspend fun addNode(node: NodeEntity) = nodeDao.insertNode(node)

    suspend fun addNodes(nodes: List<NodeEntity>) = nodeDao.insertNodes(nodes)

    suspend fun updateNode(node: NodeEntity) = nodeDao.updateNode(node)

    suspend fun deleteNode(id: String) = nodeDao.deleteNodeById(id)

    suspend fun clearAll() = nodeDao.deleteAllNodes()

    // Convenience for MappingScreen: flip "unmapped" -> "mapped" once Corridor360 capture finishes
    suspend fun markMapped(id: String, panoramaPath: String) {
        val node = nodeDao.getNodeById(id) ?: return
        nodeDao.updateNode(node.copy(status = "mapped", panoramaPath = panoramaPath))
    }
}