package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.data.model.VoiceMessage
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
            .background(BlackBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "REGISTRO DE TRANSMISIONES (${messages.size})",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextWhite
        )

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = TextDarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No hay audios en el registro todavía.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextDarkGray
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPlaying) NeonEmerald.copy(alpha = 0.10f) else CardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isPlaying) NeonEmerald else BorderDark,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PanelBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = msg.senderAvatar, fontSize = 14.sp)
                                }
                                Text(
                                    text = msg.senderName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextWhite
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = msg.filter.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = NeonCyan
                                    )
                                }
                                Text(
                                    text = String.format("%.1fs", msg.duration),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }

                        // Audio Player Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PanelBackground)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { if (isPlaying) onStopPlayback() else onPlayMessage(msg) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isPlaying) NeonRed else NeonEmerald)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = BlackBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Simulated Waveform Bars
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val heights = listOf(0.4f, 0.7f, 1f, 0.5f, 0.8f, 0.3f, 0.9f, 0.6f, 0.4f, 0.8f, 0.5f, 0.9f, 0.7f, 0.4f, 0.6f, 0.3f)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((24 * h).dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(if (isPlaying) NeonEmerald else TextDarkGray)
                                    )
                                }
                            }
                        }

                        // Transcription Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D14))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "TRANSCRIPCIÓN:",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = NeonEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${msg.transcription}\"",
                                fontSize = 11.sp,
                                color = TextWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
