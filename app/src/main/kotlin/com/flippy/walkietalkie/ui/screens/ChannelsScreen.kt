package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.data.model.Channel
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun ChannelsScreen(
    channels: List<Channel>,
    activeChannelId: String,
    onSelectChannel: (Channel) -> Unit,
    onCreateChannel: (String, String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var channelName by remember { mutableStateOf("") }
    var channelFreq by remember { mutableStateOf("") }
    var channelDesc by remember { mutableStateOf("") }
    var channelPin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Cabecera ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Canales", style = Typography.headlineMedium)
                Text(
                    text = "${channels.size} frecuencias disponibles",
                    style = Typography.bodyMedium
                )
            }

            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Background
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nuevo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // ── Lista de canales ───────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels) { channel ->
                val isActive = channel.id == activeChannelId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) AccentContainer else Surface)
                        .clickable {
                            SoundEffects.playDialTick()
                            onSelectChannel(channel)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) Accent else SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = if (isActive) Background else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = channel.name,
                                style = Typography.titleMedium,
                                maxLines = 1
                            )
                            if (!channel.pin.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Privado",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = if (channel.description.isNotEmpty())
                                "${channel.frequency} · ${channel.description}"
                            else
                                channel.frequency,
                            style = Typography.bodySmall,
                            maxLines = 1
                        )
                    }

                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Canal activo",
                            tint = Accent,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = "Sintonizar",
                            style = Typography.labelMedium,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo: crear canal ───────────────────────────────────
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo canal", style = Typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        label = { Text("Nombre del canal") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = channelFreq,
                        onValueChange = { channelFreq = it },
                        label = { Text("Frecuencia (ej: 446.500)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = channelDesc,
                        onValueChange = { channelDesc = it },
                        label = { Text("Descripción (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = channelPin,
                        onValueChange = { channelPin = it },
                        label = { Text("PIN de seguridad (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (channelName.isNotBlank() && channelFreq.isNotBlank()) {
                            onCreateChannel(channelName, channelFreq, channelDesc, channelPin.ifEmpty { null })
                            showDialog = false
                            channelName = ""
                            channelFreq = ""
                            channelDesc = ""
                            channelPin = ""
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Background
                    )
                ) {
                    Text("Crear", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
