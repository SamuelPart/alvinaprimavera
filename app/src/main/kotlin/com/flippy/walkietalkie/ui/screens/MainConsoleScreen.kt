package com.flippy.walkietalkie.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.flippy.walkietalkie.R
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.model.VoiceMessage
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
    onCreateGroup: () -> Unit = {},
    onToggleAutoPlay: () -> Unit = {},
    onOpenPlaybackModes: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onSelectContact: (User) -> Unit = {},
    onPlayMessage: (VoiceMessage) -> Unit = {},
    onStopPlayback: () -> Unit = {},
    onOpenFriendChat: (String) -> Unit = {},
    currentPlayingId: String? = null,
    modifier: Modifier = Modifier
) {
    var showAddFriendsDialog by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }

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
                // Contactos: abre la pantalla de contactos (arriba-derecha)
                IconButton(onClick = onAddFriends, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Contactos",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        if (uiState.contacts.isEmpty()) {
            // ── SIN amigos aún: diseño original (tarjeta grande + invitar) ──
            NewChatCardContent(
                onClick = { showNewChatSheet = true },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            InviteFriendsCard(
                onClick = onAddFriends,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // ── CON amigos: CARRUSEL estilo iPhone 🎠 ──
            // Adelante: el amigo seleccionado (foto o iniciales).
            // Atrás-izquierda: «Nuevo chat». Atrás-derecha: otro amigo.
            var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
            val autoIndex = uiState.contacts
                .indexOfFirst { it.id.equals(uiState.activeContact?.id, ignoreCase = true) }
                .takeIf { it >= 0 }?.plus(1) ?: 1
            val frontIndex = (if (selectedIndex >= 0) selectedIndex else autoIndex)
                .coerceIn(0, uiState.contacts.size)

            // Sincroniza «con quién hablas» con el micrófono PTT 🎙️
            LaunchedEffect(frontIndex, uiState.contacts.size) {
                if (frontIndex > 0) {
                    uiState.contacts.getOrNull(frontIndex - 1)?.let { friend ->
                        if (uiState.activeContact?.id?.equals(friend.id, ignoreCase = true) != true) {
                            onSelectContact(friend)
                        }
                    }
                }
            }

            FriendsCarousel(
                contacts = uiState.contacts,
                frontIndex = frontIndex,
                onSelectIndex = { selectedIndex = it },
                onNewChat = { showNewChatSheet = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (frontIndex == 0) {
                // «Nuevo chat» al frente → caja de invitar (como antes)
                InviteFriendsCard(
                    onClick = onAddFriends,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Amigo al frente → su chat: último audio con etiqueta y hora 💬
                val friend = uiState.contacts.getOrNull(frontIndex - 1)
                if (friend != null) {
                    val myId = uiState.userProfile.id
                    val lastMessage = remember(uiState.messages, friend.id) {
                        uiState.messages.firstOrNull { m ->
                            m.senderId.equals(friend.id, ignoreCase = true) ||
                                (m.senderId.equals(myId, ignoreCase = true) &&
                                    m.targetId.equals(friend.id, ignoreCase = true))
                        }
                    }
                    ChatPreviewCard(
                        friend = friend,
                        message = lastMessage,
                        myId = myId,
                        isPlaying = lastMessage != null && lastMessage.id == currentPlayingId,
                        onPlay = { lastMessage?.let { onPlayMessage(it) } },
                        onStop = onStopPlayback,
                        onOpenChat = { onOpenFriendChat(friend.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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

    // ── Hoja de opciones «Nuevo chat» (estilo iPhone) ──
    if (showNewChatSheet) {
        NewChatActionSheet(
            onAddFriends = {
                showNewChatSheet = false
                onAddFriends()
            },
            onCreateGroup = {
                showNewChatSheet = false
                onCreateGroup()
            },
            onScanQr = {
                showNewChatSheet = false
                onOpenScanner()
            },
            onDismiss = { showNewChatSheet = false }
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

// ============================================================
//  Hoja de opciones «Nuevo chat» · action sheet estilo iPhone:
//  tarjeta de vidrio con 3 opciones + botón Cancelar aparte.
// ============================================================

@Composable
private fun NewChatActionSheet(
    onAddFriends: () -> Unit,
    onCreateGroup: () -> Unit,
    onScanQr: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .clickable(enabled = false) {}, // no cerrar al tocar la hoja
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Tarjeta de vidrio con las 3 opciones ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF2171D27))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                ActionSheetOption(
                    icon = Icons.Default.PersonAdd,
                    label = "Añadir amigos",
                    tint = Accent,
                    onClick = onAddFriends
                )
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 66.dp)
                )
                ActionSheetOption(
                    icon = Icons.Default.Groups,
                    label = "Crear grupo",
                    tint = AccentBlue,
                    onClick = onCreateGroup
                )
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 66.dp)
                )
                ActionSheetOption(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Escanear código QR",
                    tint = Accent,
                    onClick = onScanQr
                )
            }

            // ── Botón Cancelar (blanco, texto negro) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancelar",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActionSheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

// ============================================================
//  🎠 CARRUSEL DE AMIGOS · el amigo seleccionado va ADELANTE,
//  «Nuevo chat» asoma atrás a la izquierda y otro amigo
//  atrás a la derecha. Desliza o toca los lados para cambiar.
// ============================================================

@Composable
private fun FriendsCarousel(
    contacts: List<User>,
    frontIndex: Int, // 0 = Nuevo chat · 1..n = contacts[frontIndex-1]
    onSelectIndex: (Int) -> Unit,
    onNewChat: () -> Unit
) {
    AnimatedContent(
        targetState = frontIndex,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith
                fadeOut(animationSpec = tween(150))
        },
        label = "friends_carousel",
        modifier = Modifier.fillMaxWidth()
    ) { front ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .pointerInput(front, contacts.size) {
                    // Deslizar: ← siguiente · → anterior
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (drag < -45f && front < contacts.size) onSelectIndex(front + 1)
                            else if (drag > 45f && front > 0) onSelectIndex(front - 1)
                            drag = 0f
                        }
                    ) { change, amount ->
                        change.consume()
                        drag += amount
                    }
                }
        ) {
            // ◀ ATRÁS-IZQUIERDA: elemento anterior (p. ej. «Nuevo chat»)
            if (front > 0) {
                PeekCard(
                    contacts = contacts,
                    index = front - 1,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.54f),
                    onClick = { onSelectIndex(front - 1) }
                )
            }
            // ▶ ATRÁS-DERECHA: siguiente amigo
            if (front < contacts.size) {
                PeekCard(
                    contacts = contacts,
                    index = front + 1,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.54f),
                    onClick = { onSelectIndex(front + 1) }
                )
            }
            // ★ PRIMER PLANO: tarjeta seleccionada (se dibuja al final = encima)
            CarouselCard(
                front = true,
                onClick = {
                    if (front == 0) onNewChat() // tocar «Nuevo chat» abre la hoja
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.66f)
                    .fillMaxHeight()
            ) {
                if (front == 0) {
                    NewChatGlyph(big = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nuevo chat",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    val user = contacts[front - 1]
                    FriendAvatarHalo(user = user, size = 82.dp, fontSize = 26.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user.username,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.id,
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

/** Tarjeta PEQUEÑA que asoma por detrás (izquierda o derecha). */
@Composable
private fun PeekCard(
    contacts: List<User>,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CarouselCard(
        front = false,
        onClick = onClick,
        modifier = modifier.fillMaxHeight(0.86f)
    ) {
        if (index == 0) {
            NewChatGlyph(big = false)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nuevo chat",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        } else {
            val user = contacts.getOrNull(index - 1)
            if (user != null) {
                FriendAvatar(user = user, size = 56.dp, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.username,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Base visual de las tarjetas del carrusel (vidrio, redondeadas). */
@Composable
private fun CarouselCard(
    front: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(if (front) 30.dp else 24.dp)
    Column(
        modifier = modifier
            .graphicsLayer { alpha = if (front) 1f else 0.72f }
            .clip(shape)
            .background(Color.White.copy(alpha = if (front) 0.10f else 0.06f))
            .border(
                1.dp,
                Color.White.copy(alpha = if (front) 0.18f else 0.10f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/** Icono «Nuevo chat»: burbuja de vidrio con halo difuminado + insignia «+». */
@Composable
private fun NewChatGlyph(big: Boolean) {
    val halo = if (big) 110.dp else 78.dp
    val circle = if (big) 80.dp else 54.dp
    val icon = if (big) 38.dp else 26.dp
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(halo)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Accent.copy(alpha = 0.35f),
                            AccentBlue.copy(alpha = 0.13f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(circle)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.13f))
                .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(icon)
            )
        }
        Box(
            modifier = Modifier
                .offset(
                    x = (if (big) 30.dp else 21.dp),
                    y = (if (big) 30.dp else 21.dp)
                )
                .size(if (big) 26.dp else 19.dp)
                .clip(CircleShape)
                .border(2.dp, Background, CircleShape)
                .background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(if (big) 15.dp else 12.dp)
            )
        }
    }
}

/** Avatar del carrusel: FOTO de perfil si tiene; si no, sus INICIALES. */
@Composable
private fun FriendAvatar(user: User, size: Dp, fontSize: androidx.compose.ui.unit.TextUnit) {
    val initials = remember(user.username) { initialsOf(user.username) }
    val photoUrl = remember(user.id) { SupabaseRepository.avatarPublicUrl(user.id) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AccentBlue, Accent))),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = "Foto de ${user.username}",
                modifier = Modifier.fillMaxSize()
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize
                        )
                    }
                }
            }
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

/** Avatar con halo difuminado detrás (para la tarjeta del frente). */
@Composable
private fun FriendAvatarHalo(user: User, size: Dp, fontSize: androidx.compose.ui.unit.TextUnit) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * 1.38f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AccentBlue.copy(alpha = 0.35f),
                            Accent.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
        FriendAvatar(user = user, size = size, fontSize = fontSize)
    }
}

/** Iniciales de un nombre: «Andrea Salas» → «AS», «Pipo» → «P». */
private fun initialsOf(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "F" }

/** Tarjeta grande «Nuevo chat» (diseño original, reutilizable). */
@Composable
private fun NewChatCardContent(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.16f),
                RoundedCornerShape(32.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NewChatGlyph(big = true)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nuevo chat",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** Caja de vidrio: «Invita a tus amigos o crea un nuevo grupo». */
@Composable
private fun InviteFriendsCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.16f),
                RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onClick)
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
}

// ============================================================
//  💬 TARJETA DE CHAT del amigo seleccionado:
//  «Tú · 14:32» o «Andrea · 2 días atrás» + texto transcrito,
//  a la derecha ⏵ con la duración y debajo «Chat >».
// ============================================================

@Composable
private fun ChatPreviewCard(
    friend: User,
    message: VoiceMessage?,
    myId: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Izquierda: etiqueta (quién · cuándo) + texto transcrito ──
        Column(modifier = Modifier.weight(1f)) {
            if (message == null) {
                Text(
                    text = "Aún sin audios con ${friend.username.substringBefore(" ")} 🎙️",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mantén presionado el micrófono para enviar el primero",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = lastMessageLabel(message, myId),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.transcription.ifBlank { "🎙️ Mensaje de voz" },
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(
                        onClick = { if (isPlaying) onStop() else onPlay() }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Derecha: ⏵ + duración · debajo «Chat >» ──
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (message != null) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .clickable(onClick = { if (isPlaying) onStop() else onPlay() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop
                        else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Detener" else "Reproducir audio",
                        tint = Color(0xFF0B1512),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatAudioDuration(message.duration),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenChat)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Chat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/** «Tú · 14:32» (mismo día) · «Andrea · 1 día atrás» · «Leo · 6 días atrás». */
private fun lastMessageLabel(message: VoiceMessage, myId: String): String {
    val mine = message.senderId.equals(myId, ignoreCase = true)
    val who = if (mine) "Tú"
    else message.senderName.ifBlank { message.senderId }.substringBefore(" ")
    val whenText = runCatching {
        val zdt = Instant.ofEpochMilli(message.timestamp).atZone(ZoneId.systemDefault())
        val days = ChronoUnit.DAYS.between(
            zdt.toLocalDate(),
            LocalDate.now(ZoneId.systemDefault())
        )
        when {
            days <= 0L -> "%02d:%02d".format(zdt.hour, zdt.minute)
            days == 1L -> "1 día atrás"
            else -> "$days días atrás"
        }
    }.getOrDefault("")
    return if (whenText.isEmpty()) who else "$who · $whenText"
}

/** Duración del audio: segundos → «0:12», minutos → «2:14». */
private fun formatAudioDuration(secondsFloat: Float): String {
    val total = secondsFloat.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
