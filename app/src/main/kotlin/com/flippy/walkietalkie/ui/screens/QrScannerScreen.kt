package com.flippy.walkietalkie.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

// ============================================================
//  Escáner QR · la cámara se pide SOLO al entrar aquí 📷
//  y también puedes elegir una foto de la galería con el QR 🖼️
// ============================================================

@Composable
fun QrScannerScreen(
    onContactScanned: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraError by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    // 🔎 Decodificador ZXing compartido (cámara y galería)
    fun makeReader() = MultiFormatReader().apply {
        setHints(
            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        )
    }

    fun found(id: String) {
        if (scanned) return
        scanned = true
        ContextCompat.getMainExecutor(context).execute {
            SoundEffects.playIncomingAlert()
            onContactScanned(id)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
    }

    // 🖼️ Elegir una foto de la galería con el QR (no requiere cámara)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && !scanned) {
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                val id = bmp?.let { bitmap ->
                    val w = bitmap.width
                    val h = bitmap.height
                    val pixels = IntArray(w * h)
                    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
                    val source = RGBLuminanceSource(w, h, pixels)
                    try {
                        makeReader()
                            .decode(BinaryBitmap(HybridBinarizer(source)))
                            .text
                    } catch (e: Exception) {
                        null
                    }
                }?.let { extractFlippyId(it) }

                if (id != null) {
                    found(id)
                } else {
                    Toast.makeText(
                        context,
                        "No encontramos un QR de Flippy en esa foto 🖼️",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "No pudimos leer esa foto", Toast.LENGTH_SHORT).show()
            }
        }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
            }
            Text(text = "Escanear QR", style = Typography.titleLarge)
        }

        if (!cameraGranted) {
            // ── Permiso justo a tiempo: solo cuando vas a ESCANEAR ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "📷", fontSize = 30.sp)
                Text(
                    text = "Para escanear códigos QR necesitamos acceso a tu cámara. " +
                        "Solo se usa para escanear.",
                    style = Typography.bodyMedium,
                    color = TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir cámara", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        } else {
            // ── Visor de cámara con análisis ZXing en vivo ──
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black),
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

                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(
                                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                        )
                                        .build()
                                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        if (!scanned) {
                                            analyzeFrame(imageProxy, makeReader())
                                                ?.let { extractFlippyId(it) }
                                                ?.let { found(it) }
                                        }
                                        imageProxy.close()
                                    }

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (e: Exception) {
                                    hasCameraError = true
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Marco de escaneo discreto
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                )

                if (hasCameraError) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(text = "📷", fontSize = 28.sp)
                        Text(
                            text = "No se pudo abrir la cámara.\nConcede el permiso o añade el ID manual en Amigos.",
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = "Apunta la cámara al código QR de otro usuario de Flippy.",
                style = Typography.bodyMedium
            )
        }

        // ── 🖼️ O elige una foto de la galería con el QR ──
        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color.White.copy(alpha = 0.18f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Elegir de la galería",
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }
    }
}

/** Saca un FLIP-ID del contenido del QR (acepta ID directo o enlace). */
private fun extractFlippyId(text: String): String? =
    Regex("FLIP-[0-9A-Z]+", RegexOption.IGNORE_CASE)
        .find(text)
        ?.value
        ?.uppercase()

/** Lee un fotograma YUV de la cámara y lo decodifica con ZXing. */
private fun analyzeFrame(image: ImageProxy, reader: MultiFormatReader): String? {
    val yPlane = image.planes[0].buffer
    val ySize = yPlane.remaining()
    val data = ByteArray(ySize)
    yPlane.get(data, 0, ySize)

    val source = PlanarYUVLuminanceSource(
        data, image.width, image.height,
        0, 0, image.width, image.height, false
    )
    return try {
        reader.decode(BinaryBitmap(HybridBinarizer(source))).text
    } catch (e: Exception) {
        null
    } finally {
        reader.reset()
    }
}
