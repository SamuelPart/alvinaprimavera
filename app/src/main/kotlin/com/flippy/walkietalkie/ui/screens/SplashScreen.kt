package com.flippy.walkietalkie.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.delay

// ============================================================
//  Splash estilo iPhone: pantalla negra, solo «Flippy» en
//  blanco, apareciendo con fundido suave y un zoom mínimo.
//  Sin indicadores de carga ni textos secundarios.
// ============================================================

@Composable
fun SplashScreen(
    sessionStatus: SessionStatus,
    onFinished: (isAuthenticated: Boolean) -> Unit
) {
    // ── Entrada suave: fundido de 0→1 con zoom sutil 0.92→1.0 (estilo iOS) ──
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }
    val appearAlpha = entrance.value
    val appearScale = 0.92f + 0.08f * entrance.value

    // ── Salida a los 2 segundos (y sesión ya cargada) ──
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Flippy",
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer {
                alpha = appearAlpha
                scaleX = appearScale
                scaleY = appearScale
            }
        )
    }
}
