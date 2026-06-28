package com.ashutosh.entrance_mapper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

private val FLOORS = listOf(0 to "G", 1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5")

@Composable
fun FloorSwitcher(
    currentFloor: Int,
    onFloorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FLOOR",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9E9E),
            letterSpacing = 1.sp
        )
        FLOORS.asReversed().forEach { (floor, label) ->
            val isSelected = floor == currentFloor
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color(0xFF1A1A2E) else Color(0xFFF5F5F5)
                    )
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onFloorSelected(floor) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF424242)
                )
            }
        }
    }
}
