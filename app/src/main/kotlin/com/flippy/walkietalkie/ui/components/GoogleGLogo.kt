package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Colores oficiales de la "G" de Google
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

/** Logotipo "G" de Google dibujado a mano con arcos de colores. */
@Composable
fun GoogleGLogo(size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.19f
        val arcSize = Size(s - stroke, s - stroke)
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val strokeStyle = Stroke(width = stroke, cap = StrokeCap.Butt)

        // Arco azul (derecha)
        drawArc(GoogleBlue, startAngle = -48f, sweepAngle = 116f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = strokeStyle)
        // Arco verde (inferior)
        drawArc(GoogleGreen, startAngle = 68f, sweepAngle = 92f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = strokeStyle)
        // Arco amarillo (inferior izquierda)
        drawArc(GoogleYellow, startAngle = 160f, sweepAngle = 66f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = strokeStyle)
        // Arco rojo (superior izquierda)
        drawArc(GoogleRed, startAngle = 226f, sweepAngle = 88f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = strokeStyle)
        // Barra horizontal azul de la G
        drawRect(
            color = GoogleBlue,
            topLeft = Offset(s / 2f, s / 2f - stroke / 2f),
            size = Size(s / 2f - stroke / 2f, stroke)
        )
    }
}
