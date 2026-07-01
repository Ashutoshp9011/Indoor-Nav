package com.ashutosh.corridor360.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashutosh.corridor360.Data.local.NodeEntity

@Composable
fun MappingScreen(
    viewModel: CorridorViewModel,
    onStartCapture: (NodeEntity) -> Unit   // hands off to Step 5/6/7 pipeline
) {
    val nodes by viewModel.allNodes.collectAsState()
    var selectedNode by remember { mutableStateOf<NodeEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Corridor360 — Mapping") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Pending nodes: ${nodes.count { it.status == "unmapped" }}",
                modifier = Modifier.padding(16.dp)
            )
            NodePendingList(
                nodes = nodes.filter { it.status == "unmapped" },
                onNodeSelected = { node ->
                    selectedNode = node
                    onStartCapture(node)   // triggers CameraX/ARCore flow for this node
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}