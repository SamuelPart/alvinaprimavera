package com.flippy.walkietalkie.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.UserStatus
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import com.flippy.walkietalkie.ui.components.PeerAvatar
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
    myPhotoPath: String? = null,
    modifier: Modifier = Modifier
) {
    var manualIdInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Cabecera ───────────────────────────────────────────
        Column {
            Text(text = "Amigos", style = Typography.headlineMedium)
            Text(
                text = "Habla directo con un operador",
                style = Typography.bodyMedium
            )
        }

        // ── Mi perfil ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mi avatar: foto local si existe; si no, la de Supabase; si no, emoji
                val myPhoto = remember(myPhotoPath) {
                    myPhotoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                }
                if (myPhoto != null) {
                    Image(
                        bitmap = myPhoto,
                        contentDescription = "Mi foto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                } else {
                    PeerAvatar(
                        photoUrl = SupabaseRepository.avatarPublicUrl(userProfile.id),
                        fallbackEmoji = userProfile.avatar,
                        sizeDp = 46,
                        backgroundColor = AccentContainer
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = userProfile.username, style = Typography.titleMedium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Tú",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent
                            )
                        }
                    }
                    Text(
                        text = userProfile.id,
                        style = Typography.bodySmall,
                        color = Accent
                    )
                }
            }

            IconButton(
                onClick = onOpenQrProfile,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Mi QR",
                    tint = Accent
                )
            }
        }

        // ── Añadir contacto ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualIdInput,
                onValueChange = { manualIdInput = it },
                placeholder = { Text("Ingresa un ID: FLIP-…") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (manualIdInput.isNotBlank()) {
                        onAddContactById(manualIdInput.trim())
                        manualIdInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Accent)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Background)
            }

            IconButton(
                onClick = onOpenScanner,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear QR",
                    tint = TextPrimary
                )
            }
        }

        // ── Lista de contactos ─────────────────────────────────
        Text(
            text = "Contactos (${contacts.size})",
            style = Typography.labelMedium
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(contacts) { contact ->
                val isSelected = contact.id == activeContactId
                val isOnline = contact.status == UserStatus.ONLINE || contact.isBot

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) AccentBlueContainer else Surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar del amigo: su foto de Supabase (o emoji si no tiene)
                    PeerAvatar(
                        photoUrl = SupabaseRepository.avatarPublicUrl(contact.id),
                        fallbackEmoji = contact.avatar,
                        sizeDp = 44,
                        backgroundColor = SurfaceElevated
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = contact.username,
                                style = Typography.titleMedium,
                                maxLines = 1
                            )
                            if (contact.isBot) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentBlueContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Bot",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Accent else TextTertiary)
                            )
                            Text(
                                text = if (isOnline) "En línea" else "Desconectado",
                                style = Typography.bodySmall
                            )
                        }
                    }

                    Button(
                        onClick = {
                            SoundEffects.playDialTick()
                            onSelectContact(contact)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentBlue else SurfaceElevated,
                            contentColor = if (isSelected) Background else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSelected) "Activo" else "Hablar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
