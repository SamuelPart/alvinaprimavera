package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun VuMeter(
    level: Float, // 0 to 100
    modifier: Modifier = Modifier
) {
    val totalSegments = 16

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF070B10))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "VU",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = TextDarkGray
        )

        for (i in 0 until totalSegments) {
            val threshold = (i + 1) * (100f / totalSegments)
            val isActive = level >= threshold

            val activeColor = when {
                i > 13 -> NeonRed
                i > 10 -> NeonAmber
                else -> NeonEmerald
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) activeColor else Color(0xFF142018))
            )
        }
    }
}
