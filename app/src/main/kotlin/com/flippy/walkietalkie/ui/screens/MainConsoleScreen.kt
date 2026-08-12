package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    modifier: Modifier = Modifier
) {
    var showServerDialog by remember { mutableStateOf(false) }
    var tempServerUrl by remember(uiState.serverUrl) { mutableStateOf(uiState.serverUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Tactical Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonEmerald)
                )
                Text(
                    text = "EN LÍNEA",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "• ${uiState.pingMs}ms",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NeonEmerald
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { showServerDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Enlace del Servidor",
                        tint = NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenScanner,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear QR",
                        tint = NeonEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenQr,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Mi QR",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Server URL Dialog
        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = NeonAmber)
                        Text(
                            text = "Enlace del Servidor WebSocket",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Conecta la app a tu servidor en la nube o red local (ej: Render, Railway, ngrok, o IP local):",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        OutlinedTextField(
                            value = tempServerUrl,
                            onValueChange = { tempServerUrl = it },
                            placeholder = { Text("ws://... o wss://...", fontSize = 11.sp) },
                            singleLine = true,
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
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = BlackBackground)
                    ) {
                        Text("Guardar Enlace", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("Cancelar")
                    }
                },
                containerColor = PanelBackground
            )
        }

        // Tactical LCD Radio Screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LcdGreenBg)
                .border(2.dp, LcdGreenBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LCD Header: Mode & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    uiState.isRecording -> NeonRed
                                    uiState.isIncoming -> NeonAmber
                                    else -> NeonEmerald
                                }
                            )
                    )
                    Text(
                        text = when {
                            uiState.isRecording -> "🔴 TX TRANSMITIENDO"
                            uiState.isIncoming -> "⚡ RX ${uiState.incomingSenderName}"
                            else -> "🟢 CANAL LISTO [ESPERA]"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            uiState.isRecording -> NeonRed
                            uiState.isIncoming -> NeonAmber
                            else -> NeonEmerald
                        }
                    )
                }

                Text(
                    text = if (uiState.isRecording) String.format("00:%02d", uiState.recordDurationSec.toInt()) else "00:00 / 00:30",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = LcdGreenText
                )
            }

            // Target Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0C2417))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isChannelMode) {
                        "📻 CANAL: ${uiState.activeChannel.name} (${uiState.activeChannel.frequency})"
                    } else {
                        "👤 DIRECTO: ${uiState.activeContact?.username} [${uiState.activeContact?.id}]"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LcdGreenText
                )

                Text(
                    text = "FX: ${uiState.selectedFilter.displayName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextGray
                )
            }

            // Oscilloscope Canvas
            OscilloscopeCanvas(
                waveform = waveformSamples,
                isLive = uiState.isRecording || uiState.isIncoming,
                isSpeaking = uiState.isRecording
            )

            // VU Meter
            VuMeter(level = if (uiState.isRecording) micLevel else if (uiState.isIncoming) 60f else 2f)
        }

        // Voice Filter Selector
        VoiceFilterSelector(
            selectedFilter = uiState.selectedFilter,
            onSelectFilter = onSelectFilter
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Giant PTT Button
        PttButton(
            isRecording = uiState.isRecording,
            recordDurationSec = uiState.recordDurationSec,
            onPressStart = onStartPtt,
            onPressEnd = onStopPtt
        )

        // Tactical Presets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "RESPUESTAS RÁPIDAS TÁCTICAS:",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("¡10-4 Copiado!", "🚨 Emergencia", "📻 Canal Libre", "Cambio y Fuera").forEach { preset ->
                    Button(
                        onClick = { SoundEffects.playRogerBeep() },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBackground,
                            contentColor = TextWhite
                        ),
                        contentPadding = PaddingValues(2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
