package com.flippy.walkietalkie.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.R
import com.flippy.walkietalkie.ui.components.*
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyUiState
import kotlinx.coroutines.delay

// ============================================================
//  Pantalla principal — simple y limpia:
//  💬+ Nuevo chat
//  📦 Invita a tus amigos o crea un nuevo grupo
//  🎙️ Micrófono circular (sin texto)
// ============================================================

@Composable
fun MainConsoleScreen(
    uiState: FlippyUiState,
    onStartPtt: () -> Unit,
    onStopPtt: () -> Unit,
    onOpenScanner: () -> Unit,
    onAddFriends: () -> Unit = {},
    onToggleAutoPlay: () -> Unit = {},
    onOpenPlaybackModes: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddFriendsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 💫 Difuminados de fondo estilo iPhone (menta arriba, azul abajo)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.05f),
                    radius = size.width * 0.72f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.06f, size.height * 0.88f),
                    radius = size.width * 0.78f
                )
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                                Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
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
                // Agregar amigos: abre la ventana emergente
                IconButton(
                    onClick = { showAddFriendsDialog = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiPeople,
                        contentDescription = "Agregar amigos",
                        tint = Accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Añadir contactos: escáner QR directo
                IconButton(onClick = onOpenScanner, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Añadir contactos",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // ── 💬+ Nuevo chat · tarjeta grande estilo iPhone ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.09f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.16f),
                    RoundedCornerShape(32.dp)
                )
                .clickable(onClick = onAddFriends)
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono GRANDE centrado con halo difuminado
            Box(contentAlignment = Alignment.Center) {
                // Halo menta difuminado detrás del círculo
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Accent.copy(alpha = 0.38f),
                                    AccentBlue.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Círculo de vidrio
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.13f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.24f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
                // Insignia «+» en círculo, abajo a la derecha
                Box(
                    modifier = Modifier
                        .offset(x = 34.dp, y = 34.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(2.dp, Background, CircleShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nuevo chat",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Caja de vidrio: invita a tus amigos o crea un nuevo grupo ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.09f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.16f),
                    RoundedCornerShape(26.dp)
                )
                .clickable(onClick = onAddFriends)
                .padding(vertical = 22.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Invita a tus amigos o crea un nuevo grupo",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Espacio flexible: el micrófono queda en la zona inferior
        Spacer(modifier = Modifier.weight(1f))

        // ── Estado puntual: solo aparece al transmitir o recibir ──
        if (uiState.isRecording || uiState.isIncoming) {
            StatusPill(uiState)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 🎙️ Micrófono circular (sin texto) ──
        PttButton(
            isRecording = uiState.isRecording,
            onPressStart = onStartPtt,
            onPressEnd = onStopPtt
        )

        Spacer(modifier = Modifier.height(26.dp))
    }

    // ── Ventana emergente: agregar amigos ──
    if (showAddFriendsDialog) {
        AddFriendsDialog(
            onAddFriend = {
                showAddFriendsDialog = false
                onAddFriends()
            },
            onDismiss = { showAddFriendsDialog = false }
        )
    }
    } // cierra el Box de fondo con difuminados
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

// ============================================================
//  Ventana emergente «Añade a un amigo» · estilo iPhone
//  Imagen de referencia + título + subtítulo + 2 botones que
//  cambian de color al seleccionarse, con X para cerrar.
// ============================================================

@Composable
private fun AddFriendsDialog(
    onAddFriend: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    // El botón seleccionado cambia de color un instante y luego actúa
    LaunchedEffect(selected) {
        when (selected) {
            "add" -> {
                delay(280)
                onAddFriend()
            }
            "later" -> {
                delay(280)
                onDismiss()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF2171D27))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                .clickable(enabled = false) {} // no cerrar al tocar la tarjeta
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── X para cerrar ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Imagen de referencia para agregar amigos ──
            Image(
                painter = painterResource(id = R.drawable.add_friends_ref),
                contentDescription = "Agregar amigos",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Añade a un amigo para empezar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Añade a un amigo para empezar a usar momentos.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── Botón principal: menta → blanco al seleccionarse ──
            Button(
                onClick = { selected = "add" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "add") Color.White else Accent,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Añadir amigo",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Botón secundario de cristal: se ilumina al tocarse ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (selected == "later") Color.White.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable { selected = "later" }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Quizá más tarde",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
