package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceMuted)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val path = Path()
        if (waveform.isNotEmpty() && isLive) {
            val step = width / (waveform.size - 1)
            path.moveTo(0f, centerY - (waveform[0] * centerY * 0.8f))
            for (i in 1 until waveform.size) {
                val x = i * step
                val y = centerY - (waveform[i] * centerY * 0.8f)
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = if (isSpeaking) RecordRed else Accent,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            // Línea base en reposo, discreta
            drawLine(
                color = TextTertiary.copy(alpha = 0.6f),
                start = Offset(width * 0.1f, centerY),
                end = Offset(width * 0.9f, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
