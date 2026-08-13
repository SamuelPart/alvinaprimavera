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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.UserStatus
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun ContactsScreen(
    userProfile: User,
    contacts: List<User>,
    activeContactId: String?,
    onSelectContact: (User) -> Unit,
    onAddContactById: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenQrProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manualIdInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // My Profile Summary Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonEmerald.copy(alpha = 0.2f))
                        .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = userProfile.avatar, fontSize = 22.sp)
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = userProfile.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF064E3B))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "TÚ",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }
                    }
                    Text(
                        text = userProfile.id,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                }
            }

            Button(
                onClick = onOpenQrProfile,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = BlackBackground),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = "QR", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("MI QR", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        // Add Contact Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpenScanner,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground, contentColor = NeonEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ESCANEAR QR", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            OutlinedTextField(
                value = manualIdInput,
                onValueChange = { manualIdInput = it },
                placeholder = { Text("ID: FLIP-...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            )

            IconButton(
                onClick = {
                    if (manualIdInput.isNotBlank()) {
                        onAddContactById(manualIdInput.trim())
                        manualIdInput = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonEmerald)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = BlackBackground)
            }
        }

        // Contacts List Header
        Text(
            text = "CONTACTOS (${contacts.size}):",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextGray
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                val isSelected = contact.id == activeContactId
                val isOnline = contact.status == UserStatus.ONLINE || contact.isBot

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else CardBackground)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonCyan else BorderDark,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PanelBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = contact.avatar, fontSize = 18.sp)
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = contact.username,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextWhite
                                )
                                if (contact.isBot) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanDark)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "BOT",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isOnline) NeonEmerald else TextGray)
                                )
                                Text(
                                    text = contact.id,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            SoundEffects.playDialTick()
                            onSelectContact(contact)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonCyan else Color(0xFF1E293B),
                            contentColor = if (isSelected) BlackBackground else NeonCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSelected) "SINTONIZADO" else "HABLAR",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
