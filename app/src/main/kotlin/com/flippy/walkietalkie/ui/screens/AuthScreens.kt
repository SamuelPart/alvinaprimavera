package com.flippy.walkietalkie.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.gotrue.SessionStatus

// ============================================================
//  Pantalla de bienvenida: inicio de sesión solo con correo
//  (método 100% gratuito: usa el SMTP que Supabase ya incluye)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📻", fontSize = 34.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Flippy",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Walkie-talkie en la nube",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(44.dp))

        // ── Botón principal blanco: entrada por correo ──
        Button(
            onClick = onOpenEmail,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continuar con correo electrónico",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Te enviaremos un código de 6 dígitos.\nSin contraseña y sin tarjetas: 100% gratis.",
            fontSize = 12.sp,
            color = TextTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        uiState.error?.let { err ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = err,
                color = RecordRed,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ============================================================
//  Login con correo: pedir correo → código de 6 dígitos
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
    var code by remember { mutableStateOf("") }

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) onAuthenticated()
    }

    AuthScaffold(
        title = "Correo electrónico",
        subtitle = if (uiState.emailCodeSent)
            "Te enviamos un código de 6 dígitos a $email"
        else
            "Te enviaremos un código de acceso — sin contraseñas",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Tu correo") },
            placeholder = { Text("nombre@correo.com") },
            singleLine = true,
            enabled = !uiState.emailCodeSent,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.emailCodeSent) {
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it.filter(Char::isDigit) },
                label = { Text("Código de 6 dígitos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(16.dp),
                colors = authFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        AuthErrorAndLoading(uiState.error, uiState.loading)

        Button(
            onClick = {
                if (uiState.emailCodeSent) authViewModel.verifyEmailCode(email, code)
                else authViewModel.sendEmailCode(email)
            },
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
        ) {
            Text(
                text = if (uiState.emailCodeSent) "Entrar" else "Enviar código",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        if (uiState.emailCodeSent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    code = ""
                    authViewModel.sendEmailCode(email)
                }) {
                    Text("Reenviar código", color = Accent)
                }
                TextButton(onClick = { authViewModel.resetFlows() }) {
                    Text("Usar otro correo", color = TextSecondary)
                }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface)
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

@Composable
private fun AuthErrorAndLoading(error: String?, loading: Boolean) {
    if (loading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(26.dp))
        }
    }
    error?.let {
        Text(text = it, color = RecordRed, fontSize = 12.sp)
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = OutlineLight,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Accent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
