package com.ashutosh.corridor360.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.corridor360.Data.local.EdgeEntity
import com.ashutosh.corridor360.entity.NodeEntity
import com.ashutosh.corridor360.Data.repository.EdgeRepository
import com.ashutosh.corridor360.Data.repository.NodeRepository
import com.ashutosh.corridor360.mapping.GitHubSyncManagerContract
import com.ashutosh.corridor360.mapping.SyncController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CorridorViewModel(
    private val nodeRepo: NodeRepository,
    private val edgeRepo: EdgeRepository
) : ViewModel() {

    // Placeholder sync controller to satisfy MappingScreen.kt
    val syncController = SyncController(
        syncManager = object : GitHubSyncManagerContract {
            override suspend fun syncNow() {
                // TODO: Implement actual GitHub sync
            }
        },
        scope = viewModelScope
    )

    val allNodes = nodeRepo.allNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEdges = edgeRepo.allEdges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived list the UI actually wants — feed this to NodePendingList
    val pendingNodes get() = allNodes.value.filter { it.status == "unmapped" }

    fun addNode(node: NodeEntity) = viewModelScope.launch {
        nodeRepo.addNode(node)
    }

    fun addEdge(edge: EdgeEntity) = viewModelScope.launch {
        edgeRepo.addEdge(edge)
    }

    // Called when Corridor360's ARCore/CameraX/Panorama pipeline finishes for a node
    fun completeMapping(nodeId: String, panoramaPath: String) = viewModelScope.launch {
        nodeRepo.markMapped(nodeId, panoramaPath)
    }

    fun deleteNode(nodeId: String) = viewModelScope.launch {
        nodeRepo.deleteNode(nodeId)
    }
}

// Manual factory since you're not using Hilt/Koin yet
class CorridorViewModelFactory(
    private val nodeRepo: NodeRepository,
    private val edgeRepo: EdgeRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CorridorViewModel(nodeRepo, edgeRepo) as T
}