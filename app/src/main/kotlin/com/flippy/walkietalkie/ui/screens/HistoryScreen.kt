package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.data.model.VoiceMessage
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import com.flippy.walkietalkie.ui.components.PeerAvatar
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun HistoryScreen(
    messages: List<VoiceMessage>,
    currentPlayingId: String?,
    onPlayMessage: (VoiceMessage) -> Unit,
    onStopPlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Cabecera ───────────────────────────────────────────
        Column {
            Text(text = "Audios", style = Typography.headlineMedium)
            Text(
                text = "${messages.size} transmisiones recibidas",
                style = Typography.bodyMedium
            )
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Todavía no hay audios.\nMantén el botón PTT para transmitir.",
                        style = Typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isPlaying = msg.id == currentPlayingId

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isPlaying) AccentContainer else Surface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Emisor + duración
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Foto del remitente desde Supabase (o emoji si no tiene)
                                PeerAvatar(
                                    photoUrl = SupabaseRepository.avatarPublicUrl(msg.senderId),
                                    fallbackEmoji = msg.senderAvatar,
                                    sizeDp = 34,
                                    backgroundColor = SurfaceElevated
                                )
                                Text(
                                    text = msg.senderName,
                                    style = Typography.titleMedium,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = String.format("%.1fs · %s", msg.duration, msg.filter),
                                style = Typography.labelSmall
                            )
                        }

                        // Reproductor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { if (isPlaying) onStopPlayback() else onPlayMessage(msg) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) RecordRed else Accent)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Detener" else "Reproducir",
                                    tint = Background,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Onda decorativa
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val heights = listOf(0.4f, 0.7f, 1f, 0.5f, 0.8f, 0.3f, 0.9f, 0.6f, 0.4f, 0.8f, 0.5f, 0.9f, 0.7f, 0.4f, 0.6f, 0.3f)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((22 * h).dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isPlaying) Accent else TextTertiary)
                                    )
                                }
                            }
                        }

                        // Transcripción real (o etiqueta simple si no hay)
                        Text(
                            text = if (msg.transcription.isBlank()) "🎙️ Audio de voz"
                            else "\"${msg.transcription}\"",
                            style = Typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
