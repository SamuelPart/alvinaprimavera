package com.flippy.walkietalkie.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.flippy.walkietalkie.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// ============================================================
//  Configuración de perfil · estilo iPhone (vidrio + difuminados)
//  1 de 2 → ¿Cómo te llamas? (nombre + apellido)
//  2 de 2 → foto de perfil (iniciales por defecto · cámara o
//           galería con recorte cuadrado ✓/✗)
//  Final  → «¡Genial, Nombre! Has configurado tu perfil de Flippy»
// ============================================================

private enum class SetupStep { NAME, PHOTO, CROP, DONE }

@Composable
fun ProfileSetupScreen(
    onFinished: (firstName: String, lastName: String, photoPath: String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var step by remember { mutableStateOf(SetupStep.NAME) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }   // foto sin recortar
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }  // recorte cuadrado final
    var savedPhotoPath by remember { mutableStateOf<String?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // ── Launchers de imagen ──
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            pendingCameraFile?.let { file ->
                val decoded = decodeSampledImage(file)
                if (decoded != null) {
                    pickedBitmap = decoded
                    step = SetupStep.CROP
                }
            }
        }
    }

    val launchCameraInternal: () -> Unit = {
        val file = File(context.cacheDir, "profile_capture.jpg")
        pendingCameraFile = file
        cameraLauncher.launch(
            FileProvider.getUriForFile(context, "com.flippy.walkietalkie.fileprovider", file)
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCameraInternal() }

    val openCameraWithPermission: () -> Unit = {
        val hasCam = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCam) launchCameraInternal()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = decodeSampledImage(context, uri)
            if (decoded != null) {
                pickedBitmap = decoded
                step = SetupStep.CROP
            }
        }
    }

    val initials = listOf(firstName.trim(), lastName.trim())
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Difuminados de fondo estilo iPhone ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.16f, size.height * 0.10f),
                    radius = size.width * 0.80f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.90f),
                    radius = size.width * 0.85f
                )
            )
        }

        when (step) {
            SetupStep.NAME -> NameStep(
                firstName = firstName,
                lastName = lastName,
                onFirstChange = { firstName = it },
                onLastChange = { lastName = it },
                onBack = onCancel,
                onNext = { step = SetupStep.PHOTO }
            )

            SetupStep.PHOTO -> PhotoStep(
                initials = initials,
                photo = croppedBitmap,
                onBack = { step = SetupStep.NAME },
                onOpenSourcePicker = { showSourcePicker = true },
                onNext = {
                    savedPhotoPath = croppedBitmap?.let { saveProfilePhoto(context, it) }
                    step = SetupStep.DONE
                }
            )

            SetupStep.CROP -> {
                val bmp = pickedBitmap
                if (bmp == null) {
                    step = SetupStep.PHOTO
                } else {
                    CropStep(
                        bitmap = bmp,
                        onConfirm = { cropped ->
                            croppedBitmap = cropped
                            pickedBitmap = null
                            step = SetupStep.PHOTO
                        },
                        onCancel = {
                            pickedBitmap = null
                            step = SetupStep.PHOTO
                        }
                    )
                }
            }

            SetupStep.DONE -> DoneStep(
                fullName = listOf(firstName.trim(), lastName.trim())
                    .filter { it.isNotEmpty() }
                    .joinToString(" "),
                initials = initials,
                photo = croppedBitmap,
                onNext = { onFinished(firstName, lastName, savedPhotoPath) }
            )
        }

        // ── Selector de origen de la foto (cámara / galería) ──
        if (showSourcePicker) {
            PhotoSourcePickerOverlay(
                onCamera = {
                    showSourcePicker = false
                    openCameraWithPermission()
                },
                onGallery = {
                    showSourcePicker = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onDismiss = { showSourcePicker = false }
            )
        }
    }
}

// ============================================================
//  1 de 2 — ¿Cómo te llamas?
// ============================================================

@Composable
private fun NameStep(
    firstName: String,
    lastName: String,
    onFirstChange: (String) -> Unit,
    onLastChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSetupTopBar(stepLabel = "1 de 2", onBack = onBack)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "¿Cómo te llamas?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Así es como te verán tus amigos.",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = onFirstChange,
            label = { Text("Nombre") },
            placeholder = { Text("Ej. Samuel") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(16.dp),
            colors = profileFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = onLastChange,
            label = { Text("Apellido") },
            placeholder = { Text("Ej. Lizárraga") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(16.dp),
            colors = profileFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        ProfilePrimaryButton(
            text = "Siguiente",
            enabled = firstName.isNotBlank(),
            onClick = onNext
        )

        Spacer(modifier = Modifier.height(18.dp))
    }
}

// ============================================================
//  2 de 2 — Foto de perfil
// ============================================================

@Composable
private fun PhotoStep(
    initials: String,
    photo: Bitmap?,
    onBack: () -> Unit,
    onOpenSourcePicker: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSetupTopBar(stepLabel = "2 de 2", onBack = onBack)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Añade una foto de perfil",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Así te reconocerán al instante. Si no eliges ninguna, usaremos tus iniciales.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.weight(0.4f))

        // ── Círculo de foto grande + badge de cámara ──
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            ProfileAvatarLarge(initials = initials, photo = photo, size = 168)

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, Color.Black, CircleShape)
                    .clickable(onClick = onOpenSourcePicker),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Cambiar foto",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        ProfilePrimaryButton(text = "Siguiente", enabled = true, onClick = onNext)

        Spacer(modifier = Modifier.height(18.dp))
    }
}

// ============================================================
//  Recorte cuadrado (✓ confirmar / ✗ cancelar) con arrastre
// ============================================================

@Composable
private fun CropStep(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var offsetX by remember(bitmap) { mutableStateOf(0f) }
    var offsetY by remember(bitmap) { mutableStateOf(0f) }
    var containerPx by remember(bitmap) { mutableStateOf(0) }

    val computeCrop: () -> Bitmap = {
        val sq = containerPx.toFloat().coerceAtLeast(1f)
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val scale = maxOf(sq / bw, sq / bh)
        val visible = sq / scale
        val maxTX = (bw * scale - sq) / 2f
        val maxTY = (bh * scale - sq) / 2f
        val srcX = ((maxTX - offsetX) / scale).coerceIn(0f, (bw - visible).coerceAtLeast(0f))
        val srcY = ((maxTY - offsetY) / scale).coerceIn(0f, (bh - visible).coerceAtLeast(0f))
        val s = visible.coerceAtMost(minOf(bw, bh)).roundToInt().coerceAtLeast(1)
        val x = srcX.roundToInt().coerceIn(0, (bitmap.width - s).coerceAtLeast(0))
        val y = srcY.roundToInt().coerceIn(0, (bitmap.height - s).coerceAtLeast(0))
        val square = Bitmap.createBitmap(bitmap, x, y, s, s)
        Bitmap.createScaledBitmap(square, 512, 512, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Recorta tu foto",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Arrastra la imagen para encuadrar",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.weight(0.4f))

        // ── Marco cuadrado con arrastre ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged { containerPx = it.width }
        ) {
            val sqPx = containerPx.toFloat().coerceAtLeast(1f)
            val scale = maxOf(sqPx / bitmap.width, sqPx / bitmap.height)
            val maxTX = (bitmap.width * scale - sqPx) / 2f
            val maxTY = (bitmap.height * scale - sqPx) / 2f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceElevated)
                    .pointerInput(bitmap) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offsetX = (offsetX + drag.x).coerceIn(-maxTX, maxTX)
                            offsetY = (offsetY + drag.y).coerceIn(-maxTY, maxTY)
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto a recortar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }

            // Rejilla sutil estilo cámara
            Canvas(modifier = Modifier.fillMaxSize()) {
                val third = size.width / 3f
                for (i in 1..2) {
                    drawLine(
                        Color.White.copy(alpha = 0.18f),
                        start = Offset(third * i, 0f),
                        end = Offset(third * i, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.18f),
                        start = Offset(0f, third * i),
                        end = Offset(size.width, third * i),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // ── Botones ✗ y ✓ ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onConfirm(computeCrop()) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirmar",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ============================================================
//  Final — Perfil configurado
// ============================================================

@Composable
private fun DoneStep(
    fullName: String,
    initials: String,
    photo: Bitmap?,
    onNext: () -> Unit
) {
    val first = fullName.split(" ").firstOrNull().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ProfileAvatarLarge(initials = initials, photo = photo, size = 150)

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "¡Genial, $first!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Nombre designado por el usuario, bajo la foto
        Text(
            text = fullName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Accent,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Has configurado tu perfil de Flippy",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        ProfilePrimaryButton(text = "Siguiente", enabled = true, onClick = onNext)

        Spacer(modifier = Modifier.height(18.dp))
    }
}

// ============================================================
//  Piezas compartidas
// ============================================================

/** Barra superior: botón atrás de cristal + pill «1 de 2» a la derecha. */
@Composable
private fun ProfileSetupTopBar(stepLabel: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                tint = Color.White
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = stepLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Avatar circular grande: foto recortada o iniciales sobre degradado. */
@Composable
private fun ProfileAvatarLarge(initials: String, photo: Bitmap?, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Accent, AccentBlue)))
            .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 3.2).sp
            )
        }
    }
}

/** Botón píldora blanca estilo iPhone. */
@Composable
private fun ProfilePrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.35f),
            disabledContentColor = Color.Black.copy(alpha = 0.50f)
        )
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Overlay de vidrio: elegir cámara o galería. */
@Composable
private fun PhotoSourcePickerOverlay(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE61C2230))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Foto de perfil",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            SourceOptionRow(
                icon = Icons.Default.PhotoCamera,
                text = "Tomar con la cámara del celular",
                onClick = onCamera
            )
            SourceOptionRow(
                icon = Icons.Default.PhotoLibrary,
                text = "Escoger una de la galería",
                onClick = onGallery
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SourceOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = TextPrimary, fontSize = 14.sp)
    }
}

/** Campos de vidrio esmerilado (mismo estilo que el login). */
@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.White.copy(alpha = 0.11f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
    cursorColor = Accent,
    focusedBorderColor = Accent.copy(alpha = 0.9f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextTertiary,
    unfocusedPlaceholderColor = TextTertiary
)

// ============================================================
//  Utilidades de imagen
// ============================================================

/** Decodifica una imagen de galería reducida (máx ~1600px) y corrige su rotación EXIF. */
private fun decodeSampledImage(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim ||
            bounds.outHeight / (sample * 2) >= maxDim
        ) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        val rotation = context.contentResolver.openInputStream(uri)?.use { input ->
            exifRotationDegrees(ExifInterface(input))
        } ?: 0f
        rotateBitmap(raw, rotation)
    } catch (e: Exception) {
        null
    }
}

/** Decodifica la captura de cámara reducida (máx ~1600px) y corrige su rotación EXIF. */
private fun decodeSampledImage(file: File, maxDim: Int = 1600): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim ||
            bounds.outHeight / (sample * 2) >= maxDim
        ) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        rotateBitmap(raw, exifRotationDegrees(ExifInterface(file.absolutePath)))
    } catch (e: Exception) {
        null
    }
}

private fun exifRotationDegrees(exif: ExifInterface): Float =
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** Guarda el recorte cuadrado como JPEG interno y devuelve su ruta absoluta. */
private fun saveProfilePhoto(context: Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "profile_photo.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
