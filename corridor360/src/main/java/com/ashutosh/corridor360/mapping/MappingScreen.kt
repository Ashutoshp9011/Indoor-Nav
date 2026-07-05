package com.ashutosh.corridor360.mapping

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.corridor360.entity.NodeEntity
import com.ashutosh.corridor360.ui.CorridorViewModel
import com.ashutosh.corridor360.ui.NodePendingList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingScreen(
    viewModel: CorridorViewModel,
    onStartCapture: (NodeEntity) -> Unit
) {
    val nodes by viewModel.allNodes.collectAsState()
    var selectedNode by remember { mutableStateOf<NodeEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corridor360 — Mapping") },
                actions = {
                    val syncState by viewModel.syncController.syncState.collectAsState()
                    SyncStatusButton(syncState = syncState, onSyncClick = { viewModel.syncController.triggerSync() })
                }
            )
        }
        ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Pending nodes: ${nodes.count { it.status == "unmapped" }}",
                modifier = Modifier.padding(16.dp)
            )
            NodePendingList(
                nodes = nodes.filter { it.status == "unmapped" },
                onNodeSelected = { node ->
                    selectedNode = if (selectedNode == node) null else node
                },
                modifier = Modifier.weight(1f)
            )

            selectedNode?.let { node ->
                Button(
                    onClick = { onStartCapture(node) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("Start Mapping: ${node.name}")
                }
            }
        }
    }
}