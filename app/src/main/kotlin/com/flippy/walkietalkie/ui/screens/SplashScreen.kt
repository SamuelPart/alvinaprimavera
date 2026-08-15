package com.flippy.walkietalkie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.Accent
import com.flippy.walkietalkie.ui.theme.TextSecondary
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    sessionStatus: SessionStatus,
    onFinished: (isAuthenticated: Boolean) -> Unit
) {
    // ── 1. Barrido de luz diagonal sobre el nombre (de esquina a esquina) ──
    val sweep = remember { Animatable(-0.35f) }
    LaunchedEffect(Unit) {
        sweep.animateTo(
            targetValue = 1.35f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    // ── 2. Giro permanente del anillo ──
    val spin = rememberInfiniteTransition(label = "spin")
    val ringRotation by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    // ── 3. Salida a los 2 segundos (y sesión ya cargada) ──
    var minTimeDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2000)
        minTimeDone = true
    }
    LaunchedEffect(minTimeDone, sessionStatus) {
        if (minTimeDone && sessionStatus !is SessionStatus.LoadingFromStorage) {
            onFinished(sessionStatus is SessionStatus.Authenticated)
        }
    }

    // Degradado con una banda brillante viajando en diagonal (↘ de esquina a esquina)
    val p = sweep.value
    val shineBrush = Brush.linearGradient(
        colorStops = monotonicStops(
            0f to Color(0xFF5F6B78),
            p - 0.18f to Color(0xFF5F6B78),
            p to Color.White,
            p + 0.06f to Accent,
            p + 0.18f to Color(0xFF5F6B78),
            1f to Color(0xFF5F6B78)
        ),
        start = Offset(0f, 0f),
        end = Offset(620f, 620f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Anillo girando alrededor del nombre
        Canvas(modifier = Modifier.size(190.dp)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            rotate(degrees = ringRotation) {
                drawArc(
                    color = Accent,
                    startAngle = 0f,
                    sweepAngle = 265f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = 285f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Flippy",
                style = TextStyle(
                    brush = shineBrush,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Walkie-talkie en la nube",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

/** Garantiza posiciones 0..1 no decrecientes (requisito de linearGradient). */
private fun monotonicStops(vararg stops: Pair<Float, Color>): Array<Pair<Float, Color>> {
    val out = ArrayList<Pair<Float, Color>>(stops.size)
    var last = 0f
    for ((pos, color) in stops) {
        val clamped = pos.coerceIn(last, 1f)
        out += clamped to color
        last = clamped
    }
    return out.toTypedArray()
}
