package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
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
    onUpdateServerUrl: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showServerDialog by remember { mutableStateOf(false) }
    var tempServerUrl by remember(uiState.serverUrl) { mutableStateOf(uiState.serverUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Cabecera: operador + acciones ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.userProfile.avatar, fontSize = 20.sp)
                }
                Column {
                    Text(
                        text = uiState.userProfile.username,
                        style = Typography.titleMedium
                    )
                    Text(
                        text = uiState.userProfile.id,
                        style = Typography.bodySmall,
                        color = Accent
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = RecordRed
                    )
                }
                IconButton(onClick = { showServerDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Servidor",
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onOpenScanner) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear QR",
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onOpenQr) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Mi QR",
                        tint = Accent
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
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Servidor de respaldo", style = Typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Solo se usa si no configuraste Supabase. La conexión principal ya va por la nube.",
                        style = Typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        placeholder = { Text("ws://… o wss://…") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateServerUrl(tempServerUrl)
                        showServerDialog = false
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Background
                    )
                ) {
                    Text("Guardar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
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
