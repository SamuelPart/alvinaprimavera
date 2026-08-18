package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.VoiceMessage
import com.flippy.walkietalkie.ui.components.PttButton
import com.flippy.walkietalkie.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// ============================================================
//  Pantalla «Chat» con un amigo 💬
//  Tus audios a la DERECHA (menta), los suyos a la IZQUIERDA
//  (vidrio), cada burbuja con ▶, duración y hora; micrófono
//  abajo para responder al instante.
// ============================================================

@Composable
fun ChatContactScreen(
    friend: User,
    myId: String,
    messages: List<VoiceMessage>,
    isRecording: Boolean,
    currentPlayingId: String?,
    onStartPtt: () -> Unit,
    onStopPtt: () -> Unit,
    onPlay: (VoiceMessage) -> Unit,
    onStopPlayback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatMessages = remember(messages, friend.id) {
        messages.filter {
            it.senderId.equals(friend.id, ignoreCase = true) ||
                (it.senderId.equals(myId, ignoreCase = true) &&
                    it.targetId.equals(friend.id, ignoreCase = true))
        }.sortedBy { it.timestamp } // más antiguo arriba, más nuevo abajo
    }
    val listState = rememberLazyListState()

    // Al entrar (y al llegar un audio nuevo) baja hasta el último mensaje
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Barra superior: regresar · avatar · nombre ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = TextPrimary,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(9.dp)
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentBlue, Accent))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifEmpty { "F" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = friend.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = friend.id,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }

        // ── Mensajes ──
        if (chatMessages.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(40.dp)
            ) {
                Text("📻", fontSize = 58.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aún no hay audios con ${friend.username.substringBefore(" ")}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mantén presionado el micrófono\nde abajo y envía el primero 🎙️",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(items = chatMessages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        mine = message.senderId.equals(myId, ignoreCase = true),
                        friendName = friend.username.substringBefore(" "),
                        isPlaying = message.id == currentPlayingId,
                        onToggle = {
                            if (message.id == currentPlayingId) onStopPlayback()
                            else onPlay(message)
                        }
                    )
                }
            }
        }

        // ── Micrófono para hablar con ESTE amigo 🎙️ ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            PttButton(
                isRecording = isRecording,
                onPressStart = onStartPtt,
                onPressEnd = onStopPtt
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRecording) "Grabando… suelta para enviar"
                else "Mantén presionado para hablar",
                fontSize = 12.sp,
                color = if (isRecording) RecordRed else TextTertiary
            )
        }
    }
}

/** Burbuja de audio: mía (menta, derecha) o del amigo (vidrio, izquierda). */
@Composable
private fun ChatBubble(
    message: VoiceMessage,
    mine: Boolean,
    friendName: String,
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Quién envía: «Tú» o el nombre del amigo
        Text(
            text = if (mine) "Tú" else friendName,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (mine) Accent else AccentBlue,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )

        val shape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = if (mine) 22.dp else 6.dp,
            bottomEnd = if (mine) 6.dp else 22.dp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(shape)
                .background(
                    if (mine) Brush.horizontalGradient(
                        listOf(Accent, Accent.copy(alpha = 0.85f))
                    )
                    else Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.07f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (mine) Color.Transparent
                    else Color.White.copy(alpha = 0.14f),
                    shape
                )
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ▶ / ■
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (mine) Color(0xFF0B1512)
                            else Accent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop
                        else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (mine) Accent else Color(0xFF0B1512),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Ondas decorativas (como en iMessage/WhatsApp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        waveformHeights(message.id).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(h.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (mine) Color(0xFF0B1512).copy(alpha = 0.55f)
                                        else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }
                    Text(
                        text = chatDurationLabel(message.duration),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (mine) Color(0xFF0B1512).copy(alpha = 0.75f)
                        else TextSecondary
                    )
                }
            }

            // Transcripción dentro de la burbuja
            val transcript = message.transcription
            if (transcript.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = transcript,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (mine) Color(0xFF0B1512) else TextPrimary
                )
            }
        }

        // Hora: «14:32» hoy · «1 día atrás 14:32» si pasó tiempo
        Text(
            text = chatTimeLabel(message.timestamp),
            fontSize = 10.sp,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** Alturas de ondas estables según el mensaje (para que no baile). */
private fun waveformHeights(seed: String): List<Int> {
    val base = abs(seed.hashCode())
    return List(14) { i -> 6 + ((base shr (i % 24)) % 14) }
}

private fun chatDurationLabel(secondsFloat: Float): String {
    val total = secondsFloat.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun chatTimeLabel(timestamp: Long): String {
    return runCatching {
        val zdt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        val hm = "%02d:%02d".format(zdt.hour, zdt.minute)
        val days = ChronoUnit.DAYS.between(
            zdt.toLocalDate(),
            LocalDate.now(ZoneId.systemDefault())
        )
        when {
            days <= 0L -> hm
            days == 1L -> "1 día atrás · $hm"
            else -> "$days días atrás · $hm"
        }
    }.getOrDefault("")
}
