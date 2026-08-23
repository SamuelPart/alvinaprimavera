package com.flippy.walkietalkie.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FriendSettings

// ============================================================
//  ⚙️ PANEL DEL AMIGO (se desliza de abajo hacia arriba ⬆️)
//  Perfil + acciones rápidas + personalización + notificaciones
//  + traducción + opciones delicadas. Todo se guarda POR AMIGO.
// ============================================================

private val DangerRed = Color(0xFFFF6B6B)
private val WarnAmber = Color(0xFFFFC069)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendOptionsSheet(
    friend: User,
    settings: FriendSettings,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit,
    onAliasChange: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onToggleMuteAutoplay: () -> Unit,
    onToggleMuteNotify: () -> Unit,
    onToggleTranslate: () -> Unit,
    onClearHistory: () -> Unit,
    onSetBlocked: (Boolean) -> Unit,
    onReport: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val firstName = friend.username.substringBefore(" ")
    var showAliasDialog by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var confirmReport by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentColor = TextPrimary,
        dragHandle = {
            // La «manijita» de deslizar, con el color de la casa 🌿
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 👤 Perfil: foto circular + nombre + @su ID ──
            FriendBigAvatar(friend = friend, size = 86.dp, fontSize = 30.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = friend.username,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${friend.id}",
                fontSize = 12.sp,
                color = TextTertiary
            )
            if (settings.blocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⛔ Bloqueado: no recibes nada de esta persona",
                    fontSize = 11.sp,
                    color = WarnAmber
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── ⚡ Acciones: Enviar mensaje · Llamar · Mapa en vivo ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Enviar\nmensaje",
                    tint = Accent
                ) { onOpenChat() }
                QuickAction(
                    icon = Icons.Filled.Call,
                    label = "Llamar",
                    tint = AccentBlue
                ) {
                    Toast.makeText(
                        context, "📞 Las llamadas en vivo llegan muy pronto ✨",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                QuickAction(
                    icon = Icons.Filled.Map,
                    label = "Mapa\nen vivo",
                    tint = WarnAmber
                ) {
                    Toast.makeText(
                        context, "🗺️ El mapa en vivo llega muy pronto ✨",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ── 🎨 Personalización ──
            SheetSection(title = "Personalización") {
                SheetRow(
                    icon = Icons.Filled.Edit,
                    iconTint = Accent,
                    title = "Establecer alias",
                    subtitle = if (settings.alias.isBlank()) "Como TÚ quieras verlo"
                    else "Lo ves como: ${settings.alias}"
                ) { showAliasDialog = true }
                SheetDivider()
                SheetRow(
                    icon = Icons.Filled.GroupAdd,
                    iconTint = Accent,
                    title = "Crear un grupo con $firstName",
                    subtitle = "Un canal privado para ustedes"
                ) { onCreateGroup() }
                SheetDivider()
                SheetRow(
                    icon = Icons.Filled.Share,
                    iconTint = Accent,
                    title = "Compartir contacto",
                    subtitle = "Manda su Flippy ID por donde quieras"
                ) {
                    val text = "📻 Agrega a ${friend.username} en Flippy Walkie-Talkie. " +
                        "Su Flippy ID es: ${friend.id} · Búscalo en la pantalla «Amigos» 🤙"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir contacto"))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 🔔 Notificaciones (interruptores) ──
            SheetSection(title = "Notificaciones") {
                SheetRow(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    iconTint = AccentBlue,
                    title = "Silenciar autoreproducción",
                    subtitle = "Sus audios ya no se reproducirán solos",
                    trailing = { SheetSwitch(settings.mutedAutoplay, onToggleMuteAutoplay) }
                )
                SheetDivider()
                SheetRow(
                    icon = Icons.Filled.NotificationsOff,
                    iconTint = AccentBlue,
                    title = "Silenciar notificación",
                    subtitle = "Sin sonido de aviso cuando te escriba",
                    trailing = { SheetSwitch(settings.mutedNotify, onToggleMuteNotify) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 🌐 Traducción (interruptor) ──
            SheetSection(title = "Traducción") {
                SheetRow(
                    icon = Icons.Filled.Translate,
                    iconTint = Accent,
                    title = "Traducción automática",
                    subtitle = "Traduce automáticamente las transcripciones de texto " +
                        "y mensajes de voz (gratis, en tu teléfono)",
                    trailing = { SheetSwitch(settings.translateAuto, onToggleTranslate) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 💬 Chat ──
            SheetSection(title = "Chat") {
                SheetRow(
                    icon = Icons.Filled.DeleteSweep,
                    iconTint = WarnAmber,
                    title = "Borrar el historial del chat",
                    subtitle = "Solo se borra en tu teléfono"
                ) { confirmClear = true }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── ⚠️ Más opciones ──
            SheetSection(title = "Más opciones") {
                SheetRow(
                    icon = Icons.Filled.Block,
                    iconTint = WarnAmber,
                    title = if (settings.blocked) "Desbloquear" else "Bloquear",
                    subtitle = if (settings.blocked) "Volver a recibir sus audios"
                    else "No recibirás nada de esta persona"
                ) { confirmBlock = true }
                SheetDivider()
                SheetRow(
                    icon = Icons.Filled.Flag,
                    iconTint = AccentBlue,
                    title = "Denunciar",
                    subtitle = "Nos avisas si algo va mal con este contacto"
                ) { confirmReport = true }
                SheetDivider()
                SheetRow(
                    icon = Icons.Filled.PersonRemove,
                    iconTint = DangerRed,
                    title = "Quitar este amigo",
                    titleColor = DangerRed,
                    subtitle = "Desaparece de tu lista de amigos"
                ) { confirmRemove = true }
            }
        }
    }

    // ── ✏️ Diálogo: establecer alias ──
    if (showAliasDialog) {
        var draft by remember { mutableStateOf(settings.alias) }
        AlertDialog(
            onDismissRequest = { showAliasDialog = false },
            containerColor = SurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Alias para $firstName ✏️") },
            text = {
                Column {
                    Text(
                        "Solo TÚ lo verás con este nombre. Déjalo vacío para usar su nombre real.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 24) draft = it },
                        singleLine = true,
                        placeholder = { Text("Ej. Mi socia 🤙") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            cursorColor = Accent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAliasChange(draft)
                    showAliasDialog = false
                    Toast.makeText(
                        context,
                        if (draft.isBlank()) "Alias quitado: verás su nombre real"
                        else "Ahora lo ves como «${draft.trim()}» ✨",
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text("Guardar", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Row {
                    if (settings.alias.isNotBlank()) {
                        TextButton(onClick = {
                            onAliasChange("")
                            showAliasDialog = false
                        }) { Text("Quitar alias", color = WarnAmber) }
                    }
                    TextButton(onClick = { showAliasDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            }
        )
    }

    // ── Confirmaciones ──
    if (confirmClear) {
        SheetConfirm(
            title = "¿Borrar el historial? 🧹",
            text = "Se borran los audios de este chat SOLO en tu teléfono. " +
                "$firstName conserva los suyos.",
            confirmText = "Borrar",
            confirmColor = WarnAmber,
            onConfirm = { confirmClear = false; onClearHistory() },
            onCancel = { confirmClear = false }
        )
    }
    if (confirmBlock) {
        SheetConfirm(
            title = if (settings.blocked) "¿Desbloquear a $firstName?" else "¿Bloquear a $firstName? ⛔",
            text = if (settings.blocked) "Volverás a recibir sus audios y avisos."
            else "No recibirás sus audios ni avisos. Podrás desbloquearlo cuando quieras.",
            confirmText = if (settings.blocked) "Desbloquear" else "Bloquear",
            confirmColor = if (settings.blocked) Accent else WarnAmber,
            onConfirm = { confirmBlock = false; onSetBlocked(!settings.blocked) },
            onCancel = { confirmBlock = false }
        )
    }
    if (confirmReport) {
        SheetConfirm(
            title = "¿Denunciar a $firstName? 🛡️",
            text = "Tu denuncia quedará registrada y la revisaremos. Gracias por cuidar la comunidad.",
            confirmText = "Denunciar",
            confirmColor = AccentBlue,
            onConfirm = { confirmReport = false; onReport() },
            onCancel = { confirmReport = false }
        )
    }
    if (confirmRemove) {
        SheetConfirm(
            title = "¿Quitar a $firstName? 💔",
            text = "Desaparecerá de tu lista de amigos. Si cambian de idea, podrán volver a agregarse.",
            confirmText = "Quitar amigo",
            confirmColor = DangerRed,
            onConfirm = { confirmRemove = false; onRemove() },
            onCancel = { confirmRemove = false }
        )
    }
}

// ============================================================
//  Piezas del panel
// ============================================================

/** Botón redondo de acción rápida con su etiqueta debajo. */
@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Sección tipo «lista agrupada» estilo iPhone: vidrio + título. */
@Composable
private fun SheetSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Accent,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
            content = content
        )
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.07f),
        thickness = 0.6.dp,
        modifier = Modifier.padding(start = 54.dp)
    )
}

/** Fila de opción con ícono, título, subtítulo y acción al tocar. */
@Composable
private fun SheetRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleColor: Color = TextPrimary,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = TextTertiary
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Interruptor con los colores Flippy 🌿 */
@Composable
private fun SheetSwitch(checked: Boolean, onToggle: () -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = { onToggle() },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Accent,
            uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
            checkedBorderColor = Color.Transparent,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

/** Avatar grande del panel: su foto o sus iniciales con degradado. */
@Composable
private fun FriendBigAvatar(friend: User, size: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit) {
    val initials = remember(friend.username) {
        friend.username.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "F" }
    }
    val photoUrl = remember(friend.id) { SupabaseRepository.avatarPublicUrl(friend.id) }
    Box(contentAlignment = Alignment.Center) {
        // Halo difuminado detrás ✨
        Box(
            modifier = Modifier
                .size(size * 1.35f)
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
                    contentDescription = "Foto de ${friend.username}",
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
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize
                        )
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
}

/** Confirmación estándar del panel (botón de acción con color). */
@Composable
private fun SheetConfirm(
    title: String,
    text: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = SurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title) },
        text = { Text(text, fontSize = 13.sp, lineHeight = 18.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

