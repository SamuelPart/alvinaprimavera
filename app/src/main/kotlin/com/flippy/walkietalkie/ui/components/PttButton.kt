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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
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

    // Anillo de pulso suave mientras se transmite
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(RecordRed.copy(alpha = pulseAlpha))
            )
        }

        // Cuerpo del botón: plano y suave (estilo minimalista)
        Box(
            modifier = Modifier
                .size(168.dp)
                .clip(CircleShape)
                .background(if (isRecording) RecordRed else SurfaceElevated)
                .border(
                    width = 1.5.dp,
                    color = if (isRecording) RecordRed else OutlineLight,
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
                    contentDescription = "Mantén para hablar",
                    tint = if (isRecording) TextPrimary else Accent,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRecording) "Suelta para enviar" else "Mantén para hablar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isRecording) TextPrimary else TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isRecording)
                        String.format("%.1fs · máx 30s", recordDurationSec)
                    else
                        "Push-to-talk",
                    fontSize = 11.sp,
                    color = if (isRecording) TextPrimary.copy(alpha = 0.75f) else TextTertiary
                )
            }
        }
    }
}
