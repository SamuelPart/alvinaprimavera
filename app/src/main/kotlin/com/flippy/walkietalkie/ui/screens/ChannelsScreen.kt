package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
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
            .background(BlackBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SINTONIZADOR DE FRECUENCIAS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextWhite
                )
                Text(
                    text = "Canales de Radio y Grupos Tácticos",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextGray
                )
            }

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground, contentColor = NeonEmerald),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NUEVO", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels) { channel ->
                val isActive = channel.id == activeChannelId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) NeonEmerald.copy(alpha = 0.12f) else CardBackground)
                        .border(
                            width = 1.dp,
                            color = if (isActive) NeonEmerald else BorderDark,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            SoundEffects.playDialTick()
                            onSelectChannel(channel)
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) NeonEmerald else PanelBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = if (isActive) BlackBackground else NeonEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = channel.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextWhite
                                )
                                if (!channel.pin.isNullOrEmpty()) {
                                    Icon(Icons.Default.Lock, contentDescription = "Privado", tint = NeonAmber, modifier = Modifier.size(12.dp))
                                }
                            }
                            Text(
                                text = "${channel.frequency} • ${channel.description}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlackBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVO" else "SINTONIZAR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isActive) NeonEmerald else TextGray
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Crear Nueva Frecuencia", fontFamily = FontFamily.Monospace, color = NeonEmerald) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        label = { Text("Nombre del canal") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = channelFreq,
                        onValueChange = { channelFreq = it },
                        label = { Text("Frecuencia (ej: 446.500)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = channelPin,
                        onValueChange = { channelPin = it },
                        label = { Text("PIN de seguridad (opcional)") },
                        singleLine = true
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
                            channelPin = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = BlackBackground)
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
            containerColor = PanelBackground
        )
    }
}
