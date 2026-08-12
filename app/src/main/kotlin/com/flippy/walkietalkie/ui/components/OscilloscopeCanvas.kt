package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun OscilloscopeCanvas(
    waveform: FloatArray,
    isLive: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF030A06))
            .border(1.dp, LcdGreenBorder, RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw grid
        val gridStepX = width / 12
        for (i in 0..12) {
            val x = i * gridStepX
            drawLine(
                color = LcdGreenBorder.copy(alpha = 0.4f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }
        drawLine(
            color = LcdGreenBorder.copy(alpha = 0.6f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )

        // Draw Waveform
        val path = Path()
        if (waveform.isNotEmpty() && isLive) {
            val step = width / (waveform.size - 1)
            path.moveTo(0f, centerY - (waveform[0] * centerY * 0.85f))
            for (i in 1 until waveform.size) {
                val x = i * step
                val y = centerY - (waveform[i] * centerY * 0.85f)
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = if (isSpeaking) NeonRed else NeonEmerald,
                style = Stroke(width = 3.dp.toPx())
            )
        } else {
            // Idle carrier baseline
            path.moveTo(0f, centerY)
            path.lineTo(width, centerY)
            drawPath(
                path = path,
                color = NeonEmerald.copy(alpha = 0.5f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
