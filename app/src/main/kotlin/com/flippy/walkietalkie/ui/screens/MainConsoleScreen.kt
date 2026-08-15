package com.flippy.walkietalkie.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.ui.components.*
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyUiState

@Composable
fun MainConsoleScreen(
    uiState: FlippyUiState,
    micLevel: Float,
    waveformSamples: FloatArray,
    onStartPtt: () -> Unit,
    onStopPtt: () -> Unit,
    onSelectFilter: (com.flippy.walkietalkie.data.model.VoiceFilter) -> Unit,
    onOpenQr: () -> Unit,
    onOpenScanner: () -> Unit,
    onToggleAutoPlay: () -> Unit = {},
    onOpenPlaybackModes: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Cabecera: avatar conectado + modo de reproducción ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Imagen de usuario circular (foto o iniciales) + punto verde;
                // tocar abre la configuración de perfil (o Mi QR si ya está creado)
                val profileInitials = remember(uiState.userProfile.username) {
                    uiState.userProfile.username
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifEmpty { "F" }
                }
                val profilePhoto = remember(uiState.profilePhotoPath) {
                    uiState.profilePhotoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                }
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable { onAvatarClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Accent, AccentBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePhoto != null) {
                            Image(
                                bitmap = profilePhoto,
                                contentDescription = "Mi perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = profileInitials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (profileInitials.length > 1) 16.sp else 18.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .border(2.dp, Background, CircleShape)
                            .background(Color(0xFF4ADE80))
                    )
                }

                // Botón de modo: tocar alterna reproducción↔silencio,
                // el icono ⇅ abre «Seleccionar modo»
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceElevated)
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onToggleAutoPlay() }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.autoPlayIncoming)
                                Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = if (uiState.autoPlayIncoming) Accent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.autoPlayIncoming)
                                "Reproducción automática" else "Modo silencio",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onOpenPlaybackModes,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Seleccionar modo",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onOpenScanner, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear QR",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onOpenQr, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Mi QR",
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onLogout, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = RecordRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Tarjeta de estado del canal ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Estado + temporizador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(uiState)

                Text(
                    text = if (uiState.isRecording)
                        String.format("0:%02d / 0:30", uiState.recordDurationSec.toInt())
                    else
                        "0:00",
                    style = Typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.isRecording) RecordRed else TextTertiary
                )
            }

            // Canal o contacto activo
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (uiState.isChannelMode)
                        uiState.activeChannel.name
                    else
                        uiState.activeContact?.username ?: "Directo",
                    style = Typography.headlineMedium
                )
                Text(
                    text = if (uiState.isChannelMode)
                        "${uiState.activeChannel.frequency} · ${uiState.activeChannel.membersCount} en línea"
                    else
                        "Llamada directa · ${uiState.activeContact?.id ?: ""}",
                    style = Typography.bodyMedium
                )
            }

            // Forma de onda en vivo
            OscilloscopeCanvas(
                waveform = waveformSamples,
                isLive = uiState.isRecording || uiState.isIncoming,
                isSpeaking = uiState.isRecording
            )

            VuMeter(
                level = if (uiState.isRecording) micLevel else if (uiState.isIncoming) 55f else 0f
            )
        }

        // ── Filtros de voz ─────────────────────────────────────
        VoiceFilterSelector(
            selectedFilter = uiState.selectedFilter,
            onSelectFilter = onSelectFilter
        )

        // ── Botón PTT ──────────────────────────────────────────
        PttButton(
            isRecording = uiState.isRecording,
            recordDurationSec = uiState.recordDurationSec,
            onPressStart = onStartPtt,
            onPressEnd = onStopPtt
        )

        // ── Respuestas rápidas ─────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Respuestas rápidas",
                style = Typography.labelMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("10-4 👍", "🚨 SOS", "📻 Libre", "Cambio").forEach { preset ->
                    OutlinedButton(
                        onClick = { SoundEffects.playRogerBeep() },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                    ) {
                        Text(text = preset, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // ── Diálogo: servidor de respaldo ──────────────────────────
}

@Composable
private fun StatusPill(uiState: FlippyUiState) {
    val bgColor: androidx.compose.ui.graphics.Color
    val fgColor: androidx.compose.ui.graphics.Color
    val label: String

    when {
        uiState.isRecording -> {
            bgColor = RecordRedContainer
            fgColor = RecordRed
            label = "Transmitiendo"
        }
        uiState.isIncoming -> {
            bgColor = AccentBlueContainer
            fgColor = AccentBlue
            label = "Recibiendo · ${uiState.incomingSenderName}"
        }
        else -> {
            bgColor = AccentContainer
            fgColor = Accent
            label = "En línea"
        }
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(fgColor)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            maxLines = 1
        )
    }
}
