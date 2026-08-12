package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.data.model.User
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
            .background(BlackBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextWhite)
            }
            Text(
                text = "MI IDENTIFICACIÓN FLIPPY",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextWhite
            )
        }

        // Profile Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonEmerald.copy(alpha = 0.2f))
                    .border(2.dp, NeonEmerald, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = userProfile.avatar, fontSize = 32.sp)
            }
            Text(
                text = userProfile.username,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextWhite
            )
            Text(
                text = userProfile.id,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NeonEmerald
            )
        }

        // High-Res QR Bitmap
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .border(2.dp, NeonEmerald, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Código QR",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = NeonEmerald)
            }
        }

        Text(
            text = "Pídele a tu contacto que escanee este código desde Flippy para conectarse al instante sin número telefónico.",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current

            Button(
                onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_SUBJECT,
                            "Conéctate a mi Walkie-Talkie Flippy"
                        )
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "¡Hola! Conéctate a mi canal de radio en Flippy Walkie-Talkie usando mi enlace: https://flippy.app/connect/${userProfile.id} o ingresando mi ID: ${userProfile.id}"
                        )
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir enlace Flippy"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = BlackBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "COMPARTIR ENLACE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(userProfile.id))
                    copied = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (copied) NeonCyan else CardBackground,
                    contentColor = if (copied) BlackBackground else TextWhite
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (copied) "¡COPIADO!" else "COPIAR ID",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
