package com.ashutosh.entrance_mapper.Viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.entrance_mapper.data.db.AppDatabase
import com.ashutosh.entrance_mapper.data.model.Node
import com.ashutosh.entrance_mapper.data.model.NodeStatus
import com.ashutosh.entrance_mapper.data.model.NodeType
import com.ashutosh.entrance_mapper.data.repository.NodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NodeUiState(
    val nodes: List<Node> = emptyList(),
    val selectedNode: Node? = null,
    val currentFloor: Int = 0,
    val pendingNodeType: NodeType = NodeType.ROOM,
    val pendingLabel: String = "",
    val isLabelDialogVisible: Boolean = false,
    val pendingTapX: Float = 0f,
    val pendingTapY: Float = 0f,
    val exportMessage: String? = null
)

class NodeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NodeRepository(AppDatabase.getInstance(app).nodeDao())

    private val _uiState = MutableStateFlow(NodeUiState())
    val uiState: StateFlow<NodeUiState> = _uiState.asStateFlow()

    init {
        observeFloor(0)
    }

    fun setFloor(floor: Int) {
        _uiState.update { it.copy(currentFloor = floor) }
        observeFloor(floor)
    }

    private fun observeFloor(floor: Int) {
        viewModelScope.launch {
            repo.getNodesForFloor(floor).collect { nodes ->
                _uiState.update { it.copy(nodes = nodes) }
            }
        }
    }

    fun onCanvasTap(x: Float, y: Float) {
        _uiState.update {
            it.copy(
                pendingTapX = x,
                pendingTapY = y,
                pendingLabel = "",
                isLabelDialogVisible = true
            )
        }
    }

    fun setPendingType(type: NodeType) {
        _uiState.update { it.copy(pendingNodeType = type) }
    }

    fun setPendingLabel(label: String) {
        _uiState.update { it.copy(pendingLabel = label) }
    }

    fun confirmNodePlacement() {
        val state = _uiState.value
        val floor = state.currentFloor
        val id = "node_f${floor}_${System.currentTimeMillis()}"
        val node = Node(
            id = id,
            floor = floor,
            label = state.pendingLabel.ifBlank { state.pendingNodeType.name },
            type = state.pendingNodeType,
            x = state.pendingTapX,
            y = state.pendingTapY,
            status = NodeStatus.UNMAPPED
        )
        viewModelScope.launch { repo.insertNode(node) }
        _uiState.update { it.copy(isLabelDialogVisible = false) }
    }

    fun dismissLabelDialog() {
        _uiState.update { it.copy(isLabelDialogVisible = false) }
    }

    fun selectNode(node: Node?) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    fun moveNode(node: Node, newX: Float, newY: Float) {
        viewModelScope.launch {
            repo.updateNode(node.copy(x = newX, y = newY))
        }
    }

    fun deleteNode(node: Node) {
        viewModelScope.launch {
            repo.deleteNode(node)
            _uiState.update { it.copy(selectedNode = null) }
        }
    }

    fun exportDatabase(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val dbFile = context.getDatabasePath("nodes.db")
                val exportDir = context.getExternalFilesDir(null)
                    ?: context.filesDir
                val dest = java.io.File(exportDir, "nodes_export.sqlite")
                dbFile.copyTo(dest, overwrite = true)
                _uiState.update { it.copy(exportMessage = "Exported to: ${dest.absolutePath}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }
}
