package com.ashutosh.entrance_mapper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.entrance_mapper.data.model.NodeType

private fun nodeColor(type: NodeType): Color = when (type) {
    NodeType.ROOM     -> Color(0xFF4A90D9)
    NodeType.JUNCTION -> Color(0xFF7B68EE)
    NodeType.STAIRS   -> Color(0xFFE67E22)
    NodeType.LIFT     -> Color(0xFF27AE60)
    NodeType.ENTRANCE -> Color(0xFFE74C3C)
}

private fun nodeLabel(type: NodeType): String = when (type) {
    NodeType.ROOM     -> "Room"
    NodeType.JUNCTION -> "Junction"
    NodeType.STAIRS   -> "Stairs"
    NodeType.LIFT     -> "Lift"
    NodeType.ENTRANCE -> "Entrance"
}

@Composable
fun NodeTypeSelector(
    selected: NodeType,
    onTypeSelected: (NodeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NodeType.entries.forEach { type ->
            val isSelected = type == selected
            val color = nodeColor(type)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) color else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.name.first().toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = nodeLabel(type),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) color else Color(0xFF757575)
                )
            }
        }
    }
}
