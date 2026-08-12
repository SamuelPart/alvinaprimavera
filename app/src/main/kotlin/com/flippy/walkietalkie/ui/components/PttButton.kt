package com.flippy.walkietalkie.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun PttButton(
    isRecording: Boolean,
    recordDurationSec: Float,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Pulsing Wave Animation when Transmitting
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring when transmitting
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(NeonRed.copy(alpha = pulseAlpha))
            )
        }

        // Tactile Button Body
        Box(
            modifier = Modifier
                .size(165.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) {
                        Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                    }
                )
                .border(
                    width = 4.dp,
                    color = if (isRecording) NeonRed else NeonEmerald.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPressStart()

                        waitForUpOrCancellation()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPressEnd()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Push to talk",
                    tint = if (isRecording) TextWhite else NeonEmerald,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRecording) "TRANSMITIENDO" else "PUSH TO TALK",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isRecording) TextWhite else NeonEmerald
                )

                Text(
                    text = if (isRecording) String.format("%.1fs / 30s", recordDurationSec) else "Mantén presionado",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (isRecording) Color(0xFFFFCCCC) else TextGray
                )
            }
        }
    }
}
