package com.ashutosh.entrance_mapper.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.entrance_mapper.data.model.Node
import com.ashutosh.entrance_mapper.data.model.NodeType

fun nodeColor(type: NodeType): Color = when (type) {
    NodeType.ROOM     -> Color(0xFF4A90D9)
    NodeType.JUNCTION -> Color(0xFF7B68EE)
    NodeType.STAIRS   -> Color(0xFFE67E22)
    NodeType.LIFT     -> Color(0xFF27AE60)
    NodeType.ENTRANCE -> Color(0xFFE74C3C)
}

@Composable
fun NodeCircle(node: Node, isSelected: Boolean, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val color = nodeColor(node.type)

    Canvas(modifier = modifier.size(48.dp)) {
        drawCircle(
            color = if (isSelected) color else color.copy(alpha = 0.85f),
            radius = if (isSelected) 22.dp.toPx() else 18.dp.toPx()
        )
        if (isSelected) {
            drawCircle(
                color = Color.White,
                radius = 22.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }
        drawText(
            textMeasurer = textMeasurer,
            text = node.type.name.first().toString(),
            topLeft = androidx.compose.ui.geometry.Offset(
                x = center.x - 6.dp.toPx(),
                y = center.y - 8.dp.toPx()
            ),
            style = TextStyle(color = Color.White, fontSize = 12.sp)
        )
    }
}