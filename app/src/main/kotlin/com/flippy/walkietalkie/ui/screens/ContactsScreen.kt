package com.flippy.walkietalkie.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.flippy.walkietalkie.R
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyUiState
import com.flippy.walkietalkie.ui.viewmodel.FlippyViewModel.DeviceContactMatch

// ============================================================
//  Pantalla «Añadir amigos» (estilo iPhone: vidrio + difuminados)
//  · Buscar por ID de Flippy, correo o teléfono
//  · Compartir tu invitación por apps de mensajería
//  · Detecta qué contactos de tu teléfono ya tienen Flippy
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
    onOpenQrProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showInfoCard by rememberSaveable { mutableStateOf(true) }
    var contactsGranted by remember { mutableStateOf(hasContactsPermission) }

    // El permiso se pidió en el onboarding (2 de 3); si lo negó,
    // volvemos a pedirlo desde la tarjeta explicativa de abajo.
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

    // ── Datos derivados de la búsqueda (fuera del LazyColumn:
    //    su bloque de contenido no es componible) ──
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
    val withFlippy = filteredDevice.filter { it.matched != null }
    val withoutFlippy = filteredDevice.filter { it.matched == null }

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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Barra superior: atrás · título · escáner QR ──
            item(key = "topbar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Añadir amigos",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
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
            }

            // ── Buscador de cristal ──
            item(key = "search") {
                OutlinedTextField(
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
            }

            // ── Atajo: agregar directo con un ID de Flippy ──
            if (showFlipIdShortcut) {
                item(key = "flipid") {
                    Row(
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
                                text = flipIdCandidate,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent
                            )
                        }
                        Button(
                            onClick = {
                                onAddContactById(flipIdCandidate)
                                searchQuery = ""
                                Toast.makeText(
                                    context, "¡Amigo agregado! 🎉", Toast.LENGTH_SHORT
                                ).show()
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
                }
            }

            // ── Ventanita informativa (descartable con la X) ──
            if (showInfoCard) {
                item(key = "infocard") {
                    Row(
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
                            text = "Buscar por ID de Flippy, número de teléfono o correo " +
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
                }
            }

            // ── Compartir tu invitación (deslizable →) ──
            item(key = "share") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ShareActionItem(
                        label = "Compartir",
                        onClick = { shareInvite(context, null, inviteText) }
                    ) {
                        ColorShareCircle(Accent) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    ShareActionItem(
                        label = "Mi QR",
                        onClick = onOpenQrProfile
                    ) {
                        ColorShareCircle(AccentBlue) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    BrandShareItem(
                        label = "WhatsApp",
                        drawableRes = R.drawable.ic_share_whatsapp,
                        packageName = "com.whatsapp",
                        inviteText = inviteText
                    )
                    BrandShareItem(
                        label = "Telegram",
                        drawableRes = R.drawable.ic_share_telegram,
                        packageName = "org.telegram.messenger",
                        inviteText = inviteText
                    )
                    BrandShareItem(
                        label = "Instagram",
                        drawableRes = R.drawable.ic_share_instagram,
                        packageName = "com.instagram.android",
                        inviteText = inviteText
                    )
                    BrandShareItem(
                        label = "Messenger",
                        drawableRes = R.drawable.ic_share_messenger,
                        packageName = "com.facebook.orca",
                        inviteText = inviteText
                    )
                }
            }

            item(key = "divider") {
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            }

            // ── Tus amigos ya agregados: toca «Hablar» para activarlos ──
            if (filteredFriends.isNotEmpty()) {
                item(key = "friends_title") {
                    SectionTitle("Tus amigos en Flippy")
                }
                items(filteredFriends, key = { "friend_${it.id}" }) { contact ->
                    FriendRow(
                        contact = contact,
                        isActive = contact.id == uiState.activeContact?.id &&
                            !uiState.isChannelMode,
                        onHablar = { onSelectContact(contact) }
                    )
                }
                item(key = "spacer1") { Spacer(modifier = Modifier.height(2.dp)) }
            }

            // ── Personas que podrías conocer (contactos del teléfono) ──
            item(key = "discover_title") {
                SectionTitle("Personas que podrías conocer")
            }

            if (!contactsGranted) {
                item(key = "permission") {
                    Column(
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
                }
            } else {
                if (filteredDevice.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = if (searchQuery.isBlank())
                                "No encontramos contactos en tu teléfono."
                            else
                                "Sin resultados para \"$searchQuery\".",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Los que YA tienen Flippy van arriba de todos ✨
                items(withFlippy, key = { "match_${it.matched!!.flippy_id}" }) { dc ->
                    FlippyContactRow(
                        contact = dc,
                        alreadyAdded = uiState.contacts.any {
                            it.id.equals(dc.matched!!.flippy_id, ignoreCase = true)
                        },
                        onAdd = {
                            onAddContactById(dc.matched!!.flippy_id)
                            Toast.makeText(
                                context, "¡${dc.matched!!.username} agregado! 🎉",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // Los que aún no la tienen: invítalos por SMS ✉️
                items(
                    withoutFlippy,
                    key = { "plain_${it.phone ?: it.email ?: it.name}" }
                ) { dc ->
                    InviteContactRow(
                        contact = dc,
                        onInvite = { smsInvite(context, dc.phone, inviteText) }
                    )
                }
            }

            item(key = "bottom") { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
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
private fun ColorShareCircle(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
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

/** Opción de compartir con icono + texto debajo (toda la zona es tocable). */
@Composable
private fun ShareActionItem(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        icon()
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}

/** Icono de marca (WhatsApp, Telegram…) que comparte la invitación en esa app. */
@Composable
private fun BrandShareItem(
    label: String,
    drawableRes: Int,
    packageName: String,
    inviteText: String
) {
    val context = LocalContext.current
    ShareActionItem(label = label, onClick = {
        shareInvite(context, packageName, inviteText)
    }) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
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

/** Contacto del teléfono que YA tiene Flippy: foto, nombre de perfil y «Agregar». */
@Composable
private fun FlippyContactRow(
    contact: DeviceContactMatch,
    alreadyAdded: Boolean,
    onAdd: () -> Unit
) {
    val profile = contact.matched!!
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContactAvatar(
            photoUrl = SupabaseRepository.avatarPublicUrl(profile.flippy_id),
            name = profile.username,
            sizeDp = 46
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.username,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "De tus contactos",
                fontSize = 12.sp,
                color = Accent,
                maxLines = 1
            )
        }
        if (alreadyAdded) {
            Button(
                onClick = { },
                enabled = false,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                    disabledContentColor = TextSecondary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Agregado", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Agregar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Contacto del teléfono sin Flippy: nombre, número y botón blanco «+ Invitar». */
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
        ContactAvatar(photoUrl = null, name = contact.name, sizeDp = 44)
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
            Text(text = "+ Invitar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        // Esa app no está instalada → chooser genérico
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
