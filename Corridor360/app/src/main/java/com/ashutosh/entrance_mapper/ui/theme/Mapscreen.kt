package com.ashutosh.entrance_mapper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashutosh.entrance_mapper.ui.canvas.MapCanvas
import com.ashutosh.entrance_mapper.ui.components.*
import com.ashutosh.entrance_mapper.Viewmodel.NodeViewModel

@Composable
fun MapScreen(vm: NodeViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearExportMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F2F5)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Canvas ──────────────────────────────────────────────
            MapCanvas(
                nodes = state.nodes,
                selectedNode = state.selectedNode,
                onTap = { x, y -> vm.onCanvasTap(x, y) },
                onNodeSelected = { vm.selectNode(it) },
                onNodeMoved = { node, x, y -> vm.moveNode(node, x, y) },
                modifier = Modifier.fillMaxSize()
            )

            // ── Left: floor switcher ────────────────────────────────
            FloorSwitcher(
                currentFloor = state.currentFloor,
                onFloorSelected = { vm.setFloor(it) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            )

            // ── Top bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 72.dp, vertical = 12.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Entrance Mapper",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A2E)
                    )
                    Text(
                        text = "Floor ${if (state.currentFloor == 0) "G" else state.currentFloor} · ${state.nodes.size} nodes",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
                IconButton(
                    onClick = { vm.exportDatabase(context) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF1A1A2E))
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export SQLite",
                        tint = Color.White
                    )
                }
            }

            // ── Bottom: node type selector ──────────────────────────
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (state.selectedNode != null) {
                    NodeInfoPanel(
                        node = state.selectedNode!!,
                        onDelete = { vm.deleteNode(state.selectedNode!!) },
                        modifier = Modifier.shadow(8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        NodeTypeSelector(
                            selected = state.pendingNodeType,
                            onTypeSelected = { vm.setPendingType(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Legend hint ─────────────────────────────────────────
            if (state.nodes.isEmpty()) {
                Text(
                    text = "Tap anywhere on the map to place a node",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 80.dp),
                    fontSize = 14.sp,
                    color = Color(0xFFBDBDBD),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Label Dialog ────────────────────────────────────────────
        if (state.isLabelDialogVisible) {
            LabelDialog(
                nodeType = state.pendingNodeType,
                label = state.pendingLabel,
                onLabelChange = { vm.setPendingLabel(it) },
                onConfirm = { vm.confirmNodePlacement() },
                onDismiss = { vm.dismissLabelDialog() }
            )
        }
    }
}
