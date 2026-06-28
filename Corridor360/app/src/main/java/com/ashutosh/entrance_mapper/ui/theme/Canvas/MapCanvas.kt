package com.ashutosh.entrance_mapper.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ashutosh.entrance_mapper.data.model.Node
import com.ashutosh.entrance_mapper.data.model.NodeType

private val NODE_RADIUS = 28f

private fun nodeColor(type: NodeType): Color = when (type) {
    NodeType.ROOM     -> Color(0xFF4A90D9)
    NodeType.JUNCTION -> Color(0xFF7B68EE)
    NodeType.STAIRS   -> Color(0xFFE67E22)
    NodeType.LIFT     -> Color(0xFF27AE60)
    NodeType.ENTRANCE -> Color(0xFFE74C3C)
}

@Composable
fun MapCanvas(
    nodes: List<Node>,
    selectedNode: Node?,
    onTap: (x: Float, y: Float) -> Unit,
    onNodeSelected: (Node?) -> Unit,
    onNodeMoved: (Node, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var draggingNode by remember { mutableStateOf<Node?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(nodes) {
                detectTapGestures { offset ->
                    val hit = nodes.firstOrNull { node ->
                        val dx = node.x - offset.x
                        val dy = node.y - offset.y
                        (dx * dx + dy * dy) <= (NODE_RADIUS * NODE_RADIUS * 2.5f)
                    }
                    if (hit != null) {
                        onNodeSelected(if (hit.id == selectedNode?.id) null else hit)
                    } else {
                        onNodeSelected(null)
                        onTap(offset.x, offset.y)
                    }
                }
            }
            .pointerInput(nodes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingNode = nodes.firstOrNull { node ->
                            val dx = node.x - offset.x
                            val dy = node.y - offset.y
                            (dx * dx + dy * dy) <= (NODE_RADIUS * NODE_RADIUS * 3f)
                        }
                        dragOffset = offset
                    },
                    onDrag = { change, _ ->
                        draggingNode?.let {
                            dragOffset = change.position
                        }
                    },
                    onDragEnd = {
                        draggingNode?.let { node ->
                            onNodeMoved(node, dragOffset.x, dragOffset.y)
                        }
                        draggingNode = null
                    },
                    onDragCancel = { draggingNode = null }
                )
            }
    ) {
        drawGridLines()

        nodes.forEach { node ->
            val displayX = if (draggingNode?.id == node.id) dragOffset.x else node.x
            val displayY = if (draggingNode?.id == node.id) dragOffset.y else node.y
            drawNode(
                node = node,
                x = displayX,
                y = displayY,
                isSelected = node.id == selectedNode?.id,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawGridLines() {
    val gridColor = Color(0x1A000000)
    val step = 80f
    var x = 0f
    while (x <= size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

private fun DrawScope.drawNode(
    node: Node,
    x: Float,
    y: Float,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val color = nodeColor(node.type)
    val center = Offset(x, y)

    // Shadow
    drawCircle(Color(0x22000000), radius = NODE_RADIUS + 4f, center = center.copy(y = y + 3f))

    // Fill
    drawCircle(color, radius = NODE_RADIUS, center = center)

    // Selection ring
    if (isSelected) {
        drawCircle(
            color = Color.White,
            radius = NODE_RADIUS + 5f,
            center = center,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = color,
            radius = NODE_RADIUS + 9f,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    // Icon letter
    val letter = node.type.name.first().toString()
    val iconStyle = TextStyle(
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
    val iconLayout = textMeasurer.measure(letter, iconStyle)
    drawText(
        textLayoutResult = iconLayout,
        topLeft = Offset(
            x - iconLayout.size.width / 2f,
            y - iconLayout.size.height / 2f
        )
    )

    // Label below
    val labelStyle = TextStyle(
        color = Color(0xFF1A1A2E),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        background = Color(0xCCFFFFFF)
    )
    val labelLayout = textMeasurer.measure(node.label, labelStyle)
    drawText(
        textLayoutResult = labelLayout,
        topLeft = Offset(
            x - labelLayout.size.width / 2f,
            y + NODE_RADIUS + 6f
        )
    )
}
