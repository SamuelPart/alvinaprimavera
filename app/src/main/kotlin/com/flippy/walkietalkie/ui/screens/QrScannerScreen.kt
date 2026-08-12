package com.flippy.walkietalkie.ui.screens

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun QrScannerScreen(
    onContactScanned: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextWhite)
            }
            Text(
                text = "ESCANEAR CÓDIGO QR",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextWhite
            )
        }

        // CameraX Viewfinder
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(2.dp, NeonEmerald, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCameraError) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (e: Exception) {
                                hasCameraError = true
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scanning Reticle Frame
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .border(2.dp, NeonEmerald.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            )
        }

        Text(
            text = "Apunta la cámara al código QR de otro usuario de Flippy.",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextGray
        )

        // Quick Demo Contacts for easy testing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "O ELIGE UN CONTACTO DEMO DE PRUEBA:",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextGray
            )

            listOf("FLIP-BOT", "FLIP-8921", "FLIP-3142").forEach { demoId ->
                Button(
                    onClick = {
                        SoundEffects.playRogerBeep()
                        onContactScanned(demoId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground, contentColor = NeonEmerald),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Emparejar con $demoId", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
