package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.components.PeerAvatar
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyViewModel.FriendRequest

// ============================================================
//  Pantalla «Solicitudes» 📨
//  Cada persona que te agrega aparece aquí con su foto, su
//  nombre y «Solicitud de …». Tú decides: ACEPTAR o RECHAZAR.
// ============================================================

@Composable
fun FriendRequestsScreen(
    requests: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Barra superior: regresar + título centrado ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = TextPrimary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(9.dp)
            )
            Text(
                text = "Solicitudes",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (requests.isEmpty()) {
            // ── Estado vacío ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
            ) {
                Text("📭", fontSize = 62.sp)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "No tienes solicitudes pendientes",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cuando alguien te agregue en Flippy,\naparecerá aquí para que decidas ✨",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = requests, key = { it.fromId }) { request ->
                    FriendRequestCard(
                        request = request,
                        onAccept = { onAccept(request.fromId) },
                        onReject = { onReject(request.fromId) }
                    )
                }
            }
        }
    }
}

/** Card de solicitud: foto, nombre, «Solicitud de …» + Aceptar/Rechazar. */
@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PeerAvatar(
                photoUrl = request.photoUrl,
                fallbackEmoji = request.fromAvatar,
                sizeDp = 52,
                backgroundColor = SurfaceElevated
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.fromName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Solicitud de ${request.fromName} 📨",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = request.fromId,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── Rechazar (vidrio) ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(22.dp)
                    )
                    .clickable(onClick = onReject)
            ) {
                Text(
                    text = "Rechazar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RecordRed
                )
            }

            // ── Aceptar (menta) ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Accent, Accent.copy(alpha = 0.85f))
                        )
                    )
                    .clickable(onClick = onAccept)
            ) {
                Text(
                    text = "Aceptar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0B1512)
                )
            }
        }
    }
}
