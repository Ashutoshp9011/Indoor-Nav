package com.ashutosh.corridor360.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.corridor360.Data.local.NodeEntity
import androidx.compose.ui.Alignment

@Composable
fun NodePendingList(
    nodes: List<NodeEntity>,
    onNodeSelected: (NodeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No unmapped nodes — all caught up")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(nodes, key = { it.nodeId }) { node ->
            ListItem(
                headlineContent = { Text(node.name) },
                supportingContent = { Text("Floor ${node.floor} · (${node.x}, ${node.y})") },
                modifier = Modifier.clickable { onNodeSelected(node) }
            )
            Divider()
        }
    }
}