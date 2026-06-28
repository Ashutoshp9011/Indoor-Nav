package com.ashutosh.entrance_mapper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.entrance_mapper.data.model.Node

@Composable
fun NodeInfoPanel(
    node: Node,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = node.label,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1A1A2E)
            )
            Text(
                text = node.type.name + " · Floor ${if (node.floor == 0) "G" else node.floor}",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )
            Text(
                text = "ID: ${node.id}",
                fontSize = 10.sp,
                color = Color(0xFFBDBDBD)
            )
            Text(
                text = "Status: ${node.status.name}",
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
        Spacer(Modifier.width(12.dp))
        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete node",
                tint = Color(0xFFE74C3C)
            )
        }
    }
}
