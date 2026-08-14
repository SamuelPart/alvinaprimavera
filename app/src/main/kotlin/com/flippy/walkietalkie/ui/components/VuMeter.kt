package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun VuMeter(
    level: Float, // 0 a 100
    modifier: Modifier = Modifier
) {
    val totalSegments = 18

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 0 until totalSegments) {
            val threshold = (i + 1) * (100f / totalSegments)
            val isActive = level >= threshold

            val activeColor = when {
                i > 14 -> RecordRed
                i > 11 -> WarningAmber
                else -> Accent
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isActive) activeColor else SurfaceMuted)
            )
        }
    }
}
