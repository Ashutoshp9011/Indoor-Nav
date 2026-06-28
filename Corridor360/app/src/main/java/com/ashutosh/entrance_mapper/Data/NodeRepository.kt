package com.ashutosh.entrance_mapper.data.repository

import com.ashutosh.entrance_mapper.data.db.NodeDao
import com.ashutosh.entrance_mapper.data.model.Node
import kotlinx.coroutines.flow.Flow

class NodeRepository(private val dao: NodeDao) {

    fun getNodesForFloor(floor: Int): Flow<List<Node>> =
        dao.getNodesForFloor(floor)

    suspend fun getAllNodes(): List<Node> = dao.getAllNodes()

    suspend fun getNodeById(id: String): Node? = dao.getNodeById(id)

    suspend fun insertNode(node: Node) = dao.insertNode(node)

    suspend fun updateNode(node: Node) = dao.updateNode(node)

    suspend fun deleteNode(node: Node) = dao.deleteNode(node)

    suspend fun deleteById(id: String) = dao.deleteById(id)
}
