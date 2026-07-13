package com.ashutosh.corridor360.mapping

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    var showAddNodeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corridor360 — Mapping") },
                actions = {
                    val syncState by viewModel.syncController.syncState.collectAsState()
                    SyncStatusButton(syncState = syncState, onSyncClick = { viewModel.syncController.triggerSync() })
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNodeDialog = true }) {
                Text("+")
            }
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

    if (showAddNodeDialog) {
        AddNodeDialog(
            onDismiss = { showAddNodeDialog = false },
            onConfirm = { name, floor, x, y ->
                viewModel.addNode(name, floor, x, y)
                showAddNodeDialog = false
            }
        )
    }
}

@Composable
private fun AddNodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, floor: Int, x: Float, y: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("0") }
    var x by remember { mutableStateOf("0") }
    var y by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Node") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Floor") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = x,
                    onValueChange = { x = it },
                    label = { Text("X") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = y,
                    onValueChange = { y = it },
                    label = { Text("Y") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(
                        name,
                        floor.toIntOrNull() ?: 0,
                        x.toFloatOrNull() ?: 0f,
                        y.toFloatOrNull() ?: 0f
                    )
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}