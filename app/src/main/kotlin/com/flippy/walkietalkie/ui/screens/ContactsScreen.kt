package com.flippy.walkietalkie.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyUiState
import com.flippy.walkietalkie.ui.viewmodel.FlippyViewModel.DeviceContactMatch
import kotlinx.coroutines.launch

// ============================================================
//  Pantalla «Contactos» · todo para encontrar a tu gente:
//  🔍 buscador · 🧭 y 📱 ventanitas · 4 opciones · cards de
//  sugerencias · invitar desde contactos · barra alfabética
// ============================================================

@Composable
fun ContactsScreen(
    uiState: FlippyUiState,
    deviceContacts: List<DeviceContactMatch>,
    hasContactsPermission: Boolean,
    onRefreshDeviceContacts: () -> Unit,
    onSelectContact: (User) -> Unit,
    onAddContactById: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenPhoneVerify: () -> Unit,
    onCreateGroup: () -> Unit,
    onChatWithAi: () -> Unit,
    requestCount: Int,
    onOpenRequests: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showInfoCard by rememberSaveable { mutableStateOf(true) }
    var showVerifyCard by rememberSaveable { mutableStateOf(true) }
    var contactsGranted by remember { mutableStateOf(hasContactsPermission) }
    var dismissedSuggestions by remember { mutableStateOf(setOf<String>()) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted
    }

    LaunchedEffect(contactsGranted) {
        if (contactsGranted) onRefreshDeviceContacts()
    }

    val inviteText = remember(uiState.userProfile.id) {
        "¡Hablemos por Flippy! 📻 Es un walkie-talkie por internet. " +
            "Descárgala y agrégame con mi ID: ${uiState.userProfile.id}"
    }

    // ── Datos derivados de la búsqueda ──
    val flipIdCandidate = searchQuery.trim().uppercase()
    val showFlipIdShortcut =
        flipIdCandidate.startsWith("FLIP-") && flipIdCandidate.length >= 6

    val filteredFriends = remember(uiState.contacts, searchQuery) {
        uiState.contacts.filter {
            searchQuery.isBlank() ||
                it.username.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredDevice = remember(deviceContacts, searchQuery) {
        deviceContacts.filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone?.contains(searchQuery) == true ||
                it.email?.contains(searchQuery, ignoreCase = true) == true
        }
    }
    // Sugerencias: tienen Flippy y AÚN no están en tu lista
    val suggestions = filteredDevice.filter { dc ->
        dc.matched != null &&
            uiState.contacts.none {
                it.id.equals(dc.matched!!.flippy_id, ignoreCase = true)
            } &&
            dc.matched!!.flippy_id !in dismissedSuggestions
    }
    val withoutFlippy = filteredDevice.filter { it.matched == null }

    val verifyCardVisible = showVerifyCard && uiState.profilePhone.isNullOrBlank()

    // ── Lista como datos (para que la barra A→Z calcule saltos) ──
    val rowItems = remember(
        showFlipIdShortcut, flipIdCandidate, showInfoCard, verifyCardVisible,
        filteredFriends, uiState.activeContact?.id, uiState.isChannelMode,
        contactsGranted, suggestions, filteredDevice, withoutFlippy, searchQuery
    ) {
        buildList {
            add(RowItem.TopBar)
            add(RowItem.Search)
            if (showFlipIdShortcut) add(RowItem.FlipId(flipIdCandidate))
            if (showInfoCard) add(RowItem.InfoCard)
            if (verifyCardVisible) add(RowItem.VerifyCard)
            add(RowItem.QuickOptions)
            if (filteredFriends.isNotEmpty()) {
                add(RowItem.FriendsTitle)
                filteredFriends.forEach { contact ->
                    add(
                        RowItem.Friend(
                            contact = contact,
                            active = contact.id == uiState.activeContact?.id &&
                                !uiState.isChannelMode
                        )
                    )
                }
            }
            add(RowItem.Divider)
            add(RowItem.SuggestTitle)
            if (!contactsGranted) {
                add(RowItem.Permission)
            } else {
                if (suggestions.isNotEmpty()) {
                    add(RowItem.SuggestCarousel(suggestions))
                }
                add(RowItem.InviteTitle)
                if (filteredDevice.isEmpty()) {
                    add(
                        RowItem.Empty(
                            if (searchQuery.isBlank())
                                "No encontramos contactos en tu teléfono."
                            else
                                "Sin resultados para \"$searchQuery\"."
                        )
                    )
                }
                withoutFlippy.forEach { dc -> add(RowItem.Invite(dc)) }
            }
            add(RowItem.Bottom)
        }
    }

    // Letras para la barra lateral A→Z (sobre la lista de la agenda)
    val letterFirstIndex = remember(rowItems) {
        buildMap<Char, Int> {
            rowItems.forEachIndexed { index, item ->
                if (item is RowItem.Invite) {
                    val letter = item.contact.name.trim().firstOrNull()
                        ?.uppercaseChar()?.takeIf { it.isLetter() }
                    if (letter != null && letter !in this) put(letter, index)
                }
            }
        }
    }
    val alphabet = letterFirstIndex.keys.sorted()
    val letterCellPx = with(LocalDensity.current) { 15.dp.toPx() }

    fun jumpToLetter(letter: Char) {
        val index = letterFirstIndex[letter] ?: return
        scope.launch { listState.animateScrollToItem(index) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Difuminados suaves de fondo (menta arriba, azul abajo)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.04f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.85f),
                    radius = size.width * 0.75f
                )
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 34.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(rowItems, key = { it.key }) { item ->
                when (item) {
                    // ── Atrás · «Contactos» centrado · lector QR ──
                    RowItem.TopBar -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Contactos",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        GlassIconButton(onClick = onOpenScanner) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear código QR",
                                tint = Color.White
                            )
                        }
                    }

                    // ── Buscador de cristal ──
                    RowItem.Search -> OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Agregar o buscar amigos") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.11f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                            cursorColor = Accent,
                            focusedBorderColor = Accent.copy(alpha = 0.9f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                            focusedPlaceholderColor = TextTertiary,
                            unfocusedPlaceholderColor = TextTertiary
                        )
                    )

                    // ── Atajo: agregar directo con un ID de Flippy ──
                    is RowItem.FlipId -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Agregar por ID de Flippy",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = item.candidate,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent
                            )
                        }
                        Button(
                            onClick = {
                                // Envía la SOLICITUD; el mensaje lo muestra quien
                                // llama (MainActivity) según el resultado 📨
                                onAddContactById(item.candidate)
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Agregar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ── Ventanita 1: brújula + X ──
                    RowItem.InfoCard -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Accent.copy(alpha = 0.10f))
                            .padding(start = 14.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Busca por ID de Flippy, número de teléfono o correo " +
                                "para encontrar a tus amigos en Flippy",
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showInfoCard = false },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar aviso",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── Ventanita 2: verifica tu número + botón subrayado ──
                    RowItem.VerifyCard -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentBlue.copy(alpha = 0.10f))
                            .padding(start = 14.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verifica tu número de teléfono para que sea " +
                                    "más fácil para tus amigos encontrarte",
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verificar mi número",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { onOpenPhoneVerify() }
                            )
                        }
                        IconButton(
                            onClick = { showVerifyCard = false },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar aviso",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── 4 opciones rápidas con iconos modernos ──
                    RowItem.QuickOptions -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickOption(
                            icon = Icons.Default.PersonAdd,
                            label = "Añadir\namigos",
                            tint = Accent,
                            onClick = { shareInvite(context, null, inviteText) }
                        )
                        QuickOption(
                            icon = Icons.Default.Groups,
                            label = "Crear\ngrupo",
                            tint = AccentBlue,
                            onClick = onCreateGroup
                        )
                        QuickOption(
                            icon = Icons.Default.SmartToy,
                            label = "Chatea con\nIA",
                            tint = Accent,
                            onClick = onChatWithAi
                        )
                        QuickOption(
                            icon = Icons.Default.MailOutline,
                            label = "Solicitudes",
                            tint = AccentBlue,
                            badge = requestCount,
                            onClick = onOpenRequests
                        )
                    }

                    RowItem.FriendsTitle -> SectionTitle("Tus amigos en Flippy")

                    is RowItem.Friend -> FriendRow(
                        contact = item.contact,
                        isActive = item.active,
                        onHablar = { onSelectContact(item.contact) }
                    )

                    RowItem.Divider -> HorizontalDivider(
                        color = Color.White.copy(alpha = 0.10f)
                    )

                    RowItem.SuggestTitle -> SectionTitle("Personas que podrías conocer")

                    // ── Cards: tienen Flippy y faltan agregar a tu lista ──
                    is RowItem.SuggestCarousel -> LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 6.dp)
                    ) {
                        items(item.people, key = { it.matched!!.flippy_id }) { dc ->
                            SuggestionCard(
                                contact = dc,
                                onAdd = {
                                    // Envía la SOLICITUD 📨 (el toast lo muestra
                                    // MainActivity con el resultado real)
                                    onAddContactById(dc.matched!!.flippy_id)
                                },
                                onDismiss = {
                                    dismissedSuggestions =
                                        dismissedSuggestions + dc.matched!!.flippy_id
                                }
                            )
                        }
                    }

                    RowItem.InviteTitle -> SectionTitle("Invitar desde contactos")

                    RowItem.Permission -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = Accent
                            )
                            Text(
                                text = "Para ver qué amigos ya usan Flippy, " +
                                    "permite el acceso a tus contactos.",
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = TextPrimary
                            )
                        }
                        Button(
                            onClick = {
                                contactsPermissionLauncher.launch(
                                    Manifest.permission.READ_CONTACTS
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Permitir acceso a contactos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    is RowItem.Empty -> Text(
                        text = item.message,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Los que aún no la tienen: avatar «#» y «+ Invitar» blanco ✉️
                    is RowItem.Invite -> InviteContactRow(
                        contact = item.contact,
                        onInvite = { smsInvite(context, item.contact.phone, inviteText) }
                    )

                    RowItem.Bottom -> Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // ── Barra alfabética lateral (A · B · C …) ──
        if (alphabet.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .pointerInput(alphabet) {
                        fun jumpAt(yPx: Float) {
                            val i = (yPx / letterCellPx)
                                .toInt()
                                .coerceIn(0, alphabet.lastIndex)
                            jumpToLetter(alphabet[i])
                        }
                        detectVerticalDragGestures(
                            onDragStart = { offset -> jumpAt(offset.y) }
                        ) { change, _ ->
                            change.consume()
                            jumpAt(change.position.y)
                        }
                    }
            ) {
                alphabet.forEach { letter ->
                    Box(
                        modifier = Modifier
                            .height(15.dp)
                            .width(20.dp)
                            .clickable { jumpToLetter(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Accent
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
//  Modelo de la lista (para la barra alfabética)
// ============================================================

private sealed interface RowItem {
    val key: String

    object TopBar : RowItem { override val key = "topbar" }
    object Search : RowItem { override val key = "search" }
    data class FlipId(val candidate: String) : RowItem { override val key = "flipid" }
    object InfoCard : RowItem { override val key = "infocard" }
    object VerifyCard : RowItem { override val key = "verifycard" }
    object QuickOptions : RowItem { override val key = "quickops" }
    object FriendsTitle : RowItem { override val key = "friends_title" }
    data class Friend(val contact: User, val active: Boolean) : RowItem {
        override val key = "friend_${contact.id}"
    }
    object Divider : RowItem { override val key = "divider" }
    object SuggestTitle : RowItem { override val key = "suggest_title" }
    data class SuggestCarousel(val people: List<DeviceContactMatch>) : RowItem {
        override val key = "suggest_row"
    }
    object InviteTitle : RowItem { override val key = "invite_title" }
    object Permission : RowItem { override val key = "permission" }
    data class Empty(val message: String) : RowItem { override val key = "empty" }
    data class Invite(val contact: DeviceContactMatch) : RowItem {
        override val key = "plain_${contact.phone ?: contact.email ?: contact.name}"
    }
    object Bottom : RowItem { override val key = "bottom" }
}

// ============================================================
//  Componentes internos
// ============================================================

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(top = 2.dp)
    )
}

/** Opción rápida: icono en círculo tintado + etiqueta debajo. */
@Composable
private fun QuickOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    badge: Int = 0
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f))
                    .border(1.dp, tint.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
            // 🔴 Insignia con la cantidad de solicitudes pendientes
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-3).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(RecordRed)
                        .border(2.dp, Background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badge > 9) "9+" else "$badge",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Card de sugerencia: foto circular, X, nombre, «De contactos» y Agregar. */
@Composable
private fun SuggestionCard(
    contact: DeviceContactMatch,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val profile = contact.matched!!
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(128.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(20.dp)
                )
                .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 12.dp)
        ) {
            ContactAvatar(
                photoUrl = SupabaseRepository.avatarPublicUrl(profile.flippy_id),
                name = profile.username,
                sizeDp = 58
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.username,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "De contactos",
                fontSize = 10.sp,
                color = Accent,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Agregar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        // X para quitar la sugerencia
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quitar sugerencia",
                tint = TextSecondary,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

/** Fila de un amigo ya agregado: tocar "Hablar" lo activa en la consola. */
@Composable
private fun FriendRow(
    contact: User,
    isActive: Boolean,
    onHablar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) AccentContainer else Color.White.copy(alpha = 0.06f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContactAvatar(
            photoUrl = SupabaseRepository.avatarPublicUrl(contact.id),
            name = contact.username,
            sizeDp = 44
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.username,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.id,
                fontSize = 12.sp,
                color = Accent,
                maxLines = 1
            )
        }
        Button(
            onClick = onHablar,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Activo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(text = "Hablar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Contacto sin Flippy: avatar «#», nombre, número y botón blanco «+ Invitar». */
@Composable
private fun InviteContactRow(
    contact: DeviceContactMatch,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.phone ?: contact.email ?: "",
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = onInvite,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Invitar",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }
    }
}

/** Avatar circular: foto de Supabase si existe; si no, UNA inicial del nombre. */
@Composable
private fun ContactAvatar(
    photoUrl: String?,
    name: String,
    sizeDp: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(SurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNullOrBlank()) {
            InitialLetter(name, sizeDp)
        } else {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = null,
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
                        InitialLetter(name, sizeDp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialLetter(name: String, sizeDp: Int) {
    Text(
        text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
        fontSize = (sizeDp * 0.42f).sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary
    )
}

// ============================================================
//  Compartir / invitar (intents nativos, sin servicios externos)
// ============================================================

private fun shareInvite(context: Context, packageName: String?, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        packageName?.let { setPackage(it) }
    }
    try {
        context.startActivity(
            if (packageName == null) Intent.createChooser(send, "Compartir Flippy") else send
        )
    } catch (e: ActivityNotFoundException) {
        val generic = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(generic, "Compartir Flippy"))
    }
}

private fun smsInvite(context: Context, phone: String?, text: String) {
    if (phone.isNullOrBlank()) {
        shareInvite(context, null, text)
        return
    }
    val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
        putExtra("sms_body", text)
    }
    try {
        context.startActivity(sms)
    } catch (e: ActivityNotFoundException) {
        shareInvite(context, null, text)
    }
}
