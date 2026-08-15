package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.data.model.User
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import com.flippy.walkietalkie.ui.components.PeerAvatar
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyViewModel

@Composable
fun QrProfileScreen(
    userProfile: User,
    viewModel: FlippyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val qrBitmap = remember(userProfile.id) {
        viewModel.generateQrBitmap(userProfile.id, 512)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Cabecera ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Surface)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
            }
            Text(text = "Mi código Flippy", style = Typography.titleLarge)
        }

        // ── Tarjeta de perfil ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mi foto de perfil (Supabase) o emoji de respaldo
                PeerAvatar(
                    photoUrl = SupabaseRepository.avatarPublicUrl(userProfile.id),
                    fallbackEmoji = userProfile.avatar,
                    sizeDp = 64,
                    backgroundColor = AccentContainer
                )
                Text(text = userProfile.username, style = Typography.headlineMedium)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = userProfile.id,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Accent
                    )
                }
            }

            // Código QR en tarjeta blanca
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Código QR",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = Accent)
                }
            }
        }

        Text(
            text = "Pídele a tu contacto que lo escanee desde Flippy para conectarse al instante, sin número telefónico.",
            style = Typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // ── Acciones ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current

            Button(
                onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Conéctate a mi Walkie-Talkie Flippy")
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "¡Hola! Conéctate a mi canal de radio en Flippy Walkie-Talkie usando mi enlace: https://flippy.app/connect/${userProfile.id} o ingresando mi ID: ${userProfile.id}"
                        )
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir enlace Flippy"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(userProfile.id))
                    copied = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (copied) AccentBlueContainer else SurfaceElevated,
                    contentColor = if (copied) AccentBlue else TextPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (copied) "¡Copiado!" else "Copiar ID",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
