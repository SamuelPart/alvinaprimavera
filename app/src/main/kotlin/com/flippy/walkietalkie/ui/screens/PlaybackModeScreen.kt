package com.flippy.walkietalkie.ui.screens

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyUiState

// ============================================================
//  «Seleccionar modo» · estilo iPhone
//  Dos opciones grandes (automática / silencio), divisor y
//  dos ajustes con toggle: No molestar y horario de silencio.
// ============================================================

@Composable
fun PlaybackModeScreen(
    uiState: FlippyUiState,
    onSelectAutoPlay: (Boolean) -> Unit,
    onSetDndSync: (Boolean) -> Unit,
    onSetQuietHours: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Difuminados de fondo estilo iPhone ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.08f),
                    radius = size.width * 0.75f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.92f),
                    radius = size.width * 0.80f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Encabezado: botón atrás de cristal + título ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Seleccionar modo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Opción 1: Reproducción automática ──
            ModeOptionCard(
                selected = uiState.autoPlayIncoming,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Reproducción automática",
                subtitle = "Los mensajes de voz se reproducen automáticamente",
                onClick = { onSelectAutoPlay(true) }
            )

            // ── Opción 2: Modo silencio ──
            ModeOptionCard(
                selected = !uiState.autoPlayIncoming,
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                title = "Modo silencio",
                subtitle = "Reproducción automática de mensajes de voz deshabilitada",
                onClick = { onSelectAutoPlay(false) }
            )

            // ── Divisor ──
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // ── Toggle 1: Sincronizar con No molestar ──
            ToggleRow(
                title = "Sincronizar con No molestar",
                subtitle = "Cuando la sincronización está activada, la reproducción automática de Flippy se desactivará en el modo No molestar.",
                checked = uiState.dndSyncEnabled,
                onChange = onSetDndSync
            )

            // ── Toggle 2: Configurar horario de silencio ──
            ToggleRow(
                title = "Configurar horario de silencio",
                subtitle = "Deshabilitar automáticamente la reproducción de mensajes de voz en horarios seleccionados.",
                checked = uiState.quietHoursEnabled,
                onChange = onSetQuietHours
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── 🔋 Batería: que Flippy suene aunque cierres la app ──
            BatteryOptimizationCard()
        }
    }
}

/** Tarjeta de opción grande con ícono, texto y check cuando está activa. */
@Composable
private fun ModeOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (selected) 0.11f else 0.06f))
            .border(
                width = 1.2.dp,
                color = if (selected) Accent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Accent else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Círculo de selección (check)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (selected) Accent else Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                )
                .background(if (selected) Accent else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** Fila de ajuste con Switch estilo iPhone (verde menta cuando está on). */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.24f)
            )
        )
    }
}

/** ¿Flippy ya está fuera del ahorro de batería? */
private fun isBatteryFree(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

// ============================================================
//  🔋 Tarjeta «Evitar ahorro de batería»
//  Si aún no está libre: botón menta que abre el ajuste del
//  sistema (100% permitido por Play Store, sin permisos raros).
//  Si ya está: muestra «Listo ✓».
// ============================================================

@Composable
private fun BatteryOptimizationCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var batteryFree by remember { mutableStateOf(isBatteryFree(context)) }

    // Al volver de los Ajustes, actualiza el estado automáticamente
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryFree = isBatteryFree(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        (if (batteryFree) Accent else AccentBlue).copy(alpha = 0.16f)
                    )
                    .border(
                        1.dp,
                        (if (batteryFree) Accent else AccentBlue).copy(alpha = 0.40f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (batteryFree) Accent else AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Evitar ahorro de batería",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (batteryFree)
                        "✓ Flippy puede sonar aunque cierres la app"
                    else
                        "Para que los audios suenen con la app cerrada, quita la optimización de batería",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextSecondary
                )
            }
        }

        if (!batteryFree) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Accent)
                    .clickable {
                        // Busca «Flippy» en la lista y elige «Sin restricciones» 🔓
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        }
                    }
            ) {
                Text(
                    text = "Abrir ajustes de batería ⚡",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0B1512)
                )
            }
        }
    }
}
