package com.flippy.walkietalkie.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.gotrue.SessionStatus

// ============================================================
//  Pantalla de bienvenida · estilo iPhone
//  Fondo negro con difuminados de color, logo moderno tipo
//  ícono de app iOS y píldora blanca de alto contraste.
// ============================================================

@Composable
fun WelcomeScreen(
    authViewModel: AuthViewModel,
    onOpenEmail: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) onAuthenticated()
    }

    // Entrada suave: fundido + zoom mínimo (estilo iOS)
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Difuminados de fondo: halos suaves menta y azul ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.10f),
                    radius = size.width * 0.80f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.90f),
                    radius = size.width * 0.85f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            FlippyAppLogo(
                modifier = Modifier.graphicsLayer {
                    alpha = entrance.value
                    val s = 0.88f + 0.12f * entrance.value
                    scaleX = s
                    scaleY = s
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Flippy",
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer { alpha = entrance.value }
            )

            Spacer(modifier = Modifier.weight(1.25f))

            // ── Botón estilo iPhone: píldora blanca, texto negro puro ──
            Button(
                onClick = onOpenEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { alpha = entrance.value },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Correo electrónico",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = RecordRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(34.dp))
        }
    }
}

// ── Logo moderno de Flippy: ícono estilo app iOS (cuadrado
//    redondeado con degradado menta→azul, brillo de cristal y
//    glifo blanco de ondas de transmisión) ──
@Composable
private fun FlippyAppLogo(modifier: Modifier = Modifier, logoSize: Dp = 96.dp) {
    Canvas(modifier = modifier.size(logoSize)) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(w * 0.26f, w * 0.26f)

        // Ícono de app: cuadrado redondeado con degradado diagonal
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Accent, AccentBlue),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            cornerRadius = corner
        )

        // Brillo sutil arriba (efecto cristal tipo iOS)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                startY = 0f,
                endY = h * 0.55f
            ),
            cornerRadius = corner
        )

        // Glifo: punto central + ondas de transmisión a ambos lados
        val cx = w / 2f
        val cy = h / 2f
        val wavesStyle = Stroke(width = w * 0.055f, cap = StrokeCap.Round)

        drawCircle(color = Color.White, radius = w * 0.075f, center = Offset(cx, cy))

        listOf(w * 0.16f, w * 0.26f).forEach { r ->
            val arcTopLeft = Offset(cx - r, cy - r)
            val arcSize = Size(r * 2f, r * 2f)
            drawArc(
                color = Color.White,
                startAngle = -55f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = wavesStyle
            )
            drawArc(
                color = Color.White,
                startAngle = 125f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = wavesStyle
            )
        }
    }
}

// ============================================================
//  Login con correo + contraseña · estilo iPhone
//  Campos de vidrio esmerilado, difuminados de fondo y
//  píldora blanca de alto contraste.
// ============================================================

@Composable
fun EmailAuthScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegister by remember { mutableStateOf(false) }

    // Entrada suave (fundido, estilo iOS)
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) onAuthenticated()
    }

    AuthScaffold(
        title = if (isRegister) "Crear cuenta" else "Entrar",
        subtitle = if (isRegister)
            "Elige tu correo y una contraseña — sin códigos ni correos de verificación"
        else
            "Entra con tu correo y contraseña",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Tu correo") },
            placeholder = { Text("nombre@correo.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = entrance.value }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            placeholder = { Text("Mínimo 6 caracteres") },
            singleLine = true,
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Ocultar" else "Ver",
                        color = Accent,
                        fontSize = 13.sp
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = entrance.value }
        )

        AuthErrorAndLoading(uiState.error, uiState.loading)

        // ── Botón principal estilo iPhone: píldora blanca, texto negro ──
        Button(
            onClick = {
                if (isRegister) authViewModel.signUpWithEmailPassword(email, password)
                else authViewModel.signInWithEmailPassword(email, password)
            },
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = entrance.value },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.55f),
                disabledContentColor = Color.Black.copy(alpha = 0.55f)
            )
        ) {
            Text(
                text = if (isRegister) "Crear cuenta" else "Entrar",
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Cambiar entre modo Entrar y modo Crear cuenta
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = {
                isRegister = !isRegister
                authViewModel.clearError()
            }) {
                Text(
                    text = if (isRegister)
                        "¿Ya tienes cuenta? Entra aquí"
                    else
                        "¿Primera vez? Crea tu cuenta",
                    color = Accent,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ============================================================
//  Piezas compartidas de las pantallas de auth
// ============================================================

@Composable
private fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
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
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 0.75f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.95f),
                    radius = size.width * 0.80f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Botón atrás de cristal ──
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 14.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}

@Composable
private fun AuthErrorAndLoading(error: String?, loading: Boolean) {
    if (loading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp))
        }
    }
    error?.let {
        Text(text = it, color = RecordRed, fontSize = 12.sp)
    }
}

// ── Campos estilo iPhone: vidrio esmerilado translúcido ──
@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
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
