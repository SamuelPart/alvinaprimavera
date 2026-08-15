package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage

/**
 * Avatar circular de un usuario: carga su foto desde Supabase Storage
 * (avatars/{FLIP-ID}.jpg). Si no tiene foto o está cargando, muestra
 * el emoji como respaldo.
 */
@Composable
fun PeerAvatar(
    photoUrl: String?,
    fallbackEmoji: String,
    sizeDp: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNullOrBlank()) {
            Text(text = fallbackEmoji, fontSize = (sizeDp * 0.45f).sp)
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
                    // Cargando o sin foto (404) → respaldo con emoji
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = fallbackEmoji, fontSize = (sizeDp * 0.45f).sp)
                    }
                }
            }
        }
    }
}
