@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.flippy.walkietalkie.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.flippy.walkietalkie.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
//  Onboarding estilo iPhone: 3 pasos numerados ("1 de 3"...)
//  Pantalla 1 → permiso de micrófono (ícono animado con pulsos)
//  Pantalla 2 → permiso de contactos (encontrar amigos)
//  Pantalla 3 → notificaciones + ignorar optimización de batería
// ============================================================

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

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
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.width * 0.80f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.88f),
                    radius = size.width * 0.85f
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Encabezado: pill de cristal "1 de 3" ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} de 3",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> MicPermissionPage(
                        onContinue = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    1 -> ContactsPermissionPage(
                        onContinue = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    else -> NotificationsBatteryPage(onFinish = onFinish)
                }
            }

            // ── Puntos indicadores ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val active = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (active) 20.dp else 6.dp,
                        animationSpec = tween(300),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(dotWidth)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) Color.White
                                else Color.White.copy(alpha = 0.28f)
                            )
                    )
                }
            }
        }
    }
}

// ============================================================
//  1 de 3 — Permiso de micrófono
// ============================================================

@Composable
private fun MicPermissionPage(onContinue: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }
    var deniedOnce by remember { mutableStateOf(false) }
    var justGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (ok) justGranted = true else deniedOnce = true
    }

    // Solo avanza solo cuando el permiso ACABA de concederse en el diálogo;
    // si ya venía concedido, el usuario pulsa «Continuar».
    LaunchedEffect(justGranted) {
        if (justGranted && granted) {
            delay(600)
            justGranted = false
            onContinue()
        }
    }

    OnboardingPageLayout(
        hero = { AnimatedPermissionHero(icon = Icons.Default.Mic, granted = granted) },
        title = "Activa los mensajes de voz",
        subtitle = "Esto es fundamental para disfrutar la experiencia de voz completa de Flippy.",
        warning = if (deniedOnce && !granted)
            "Sin micrófono no podrás transmitir. Puedes activarlo más tarde en Ajustes."
        else null
    ) {
        if (!granted) {
            OnboardingPrimaryButton(text = "Permitir micrófono") {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            OnboardingGhostButton(text = "Ahora no", onClick = onContinue)
        } else {
            OnboardingPrimaryButton(text = "Continuar", onClick = onContinue)
        }
    }
}

// ============================================================
//  2 de 3 — Permiso de contactos
// ============================================================

@Composable
private fun ContactsPermissionPage(onContinue: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS)) }
    var deniedOnce by remember { mutableStateOf(false) }
    var justGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (ok) justGranted = true else deniedOnce = true
    }

    LaunchedEffect(justGranted) {
        if (justGranted && granted) {
            delay(600)
            justGranted = false
            onContinue()
        }
    }

    OnboardingPageLayout(
        hero = { AnimatedPermissionHero(icon = Icons.Default.Contacts, granted = granted) },
        title = "¡Veamos qué amigos están aquí!",
        subtitle = "Permite el acceso a tus contactos y números: te ayudará a conectarte con tus amigos más fácilmente.",
        warning = if (deniedOnce && !granted)
            "Sin contactos tendrás que agregar amigos a mano con su FLIP-ID o QR."
        else null
    ) {
        if (!granted) {
            OnboardingPrimaryButton(text = "Permitir contactos") {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            OnboardingGhostButton(text = "Ahora no", onClick = onContinue)
        } else {
            OnboardingPrimaryButton(text = "Continuar", onClick = onContinue)
        }
    }
}

// ============================================================
//  3 de 3 — Notificaciones + ignorar optimización de batería
// ============================================================

@Composable
private fun NotificationsBatteryPage(onFinish: () -> Unit) {
    val context = LocalContext.current
    val needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var notifGranted by remember {
        mutableStateOf(!needsNotif || hasPermission(context, Manifest.permission.POST_NOTIFICATIONS))
    }
    var notifSkipped by remember { mutableStateOf(false) }
    var batteryGranted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var batterySkipped by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> notifGranted = ok }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryGranted = isIgnoringBatteryOptimizations(context)
    }

    val notifStep = !notifGranted && !notifSkipped
    val batteryStep = !notifStep && !batteryGranted && !batterySkipped
    val allDone = !notifStep && !batteryStep

    OnboardingPageLayout(
        hero = {
            AnimatedPermissionHero(
                icon = if (batteryStep) Icons.Default.BatteryChargingFull
                       else Icons.Default.Notifications,
                granted = allDone && (notifGranted || batteryGranted)
            )
        },
        title = "No te pierdas los mensajes de tus amigos",
        subtitle = when {
            batteryStep ->
                "Y para seguir escuchando con la pantalla apagada, permite que Flippy ignore la optimización de batería."
            allDone ->
                "Listo: recibirás los mensajes de tus amigos al instante, incluso con la app cerrada."
            else ->
                "Permite el acceso a las notificaciones para recibir los mensajes de tus amigos."
        }
    ) {
        when {
            notifStep -> {
                OnboardingPrimaryButton(text = "Permitir notificaciones") {
                    if (needsNotif) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notifGranted = true
                    }
                }
                OnboardingGhostButton(text = "Ahora no") { notifSkipped = true }
            }
            batteryStep -> {
                OnboardingPrimaryButton(text = "Mantener siempre activa") {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    batteryLauncher.launch(intent)
                }
                OnboardingGhostButton(text = "Ahora no") { batterySkipped = true }
            }
            else -> {
                OnboardingPrimaryButton(text = "Empezar", onClick = onFinish)
            }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

// ============================================================
//  Piezas compartidas del onboarding
// ============================================================

@Composable
private fun OnboardingPageLayout(
    hero: @Composable () -> Unit,
    title: String,
    subtitle: String,
    warning: String? = null,
    buttons: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.55f))

        hero()

        Spacer(modifier = Modifier.height(34.dp))

        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        warning?.let {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = it,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = WarningAmber,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        buttons()

        Spacer(modifier = Modifier.height(10.dp))
    }
}

/** Ícono moderno: círculo con degradado menta→azul + anillos
 *  expansivos animados + badge de check cuando está concedido. */
@Composable
private fun AnimatedPermissionHero(icon: ImageVector, granted: Boolean) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing)
        ),
        label = "pulseValue"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Anillos expansivos (pulso)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inner = 58.dp.toPx()
            val outer = 108.dp.toPx()
            for (i in 0..2) {
                val t = (pulse + i / 3f) % 1f
                drawCircle(
                    color = Accent.copy(alpha = (1f - t) * if (granted) 0.40f else 0.28f),
                    radius = inner + (outer - inner) * t,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Círculo central con degradado y el glifo
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Accent, AccentBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        // Badge de check (permiso concedido)
        if (granted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 42.dp, bottom = 42.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Concedido",
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
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

@Composable
private fun OnboardingGhostButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = text, color = TextSecondary, fontSize = 13.sp)
    }
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
