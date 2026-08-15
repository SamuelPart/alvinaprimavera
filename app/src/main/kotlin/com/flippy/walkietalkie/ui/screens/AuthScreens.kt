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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.gotrue.SessionStatus

// ============================================================
//  Pantalla de bienvenida: inicio de sesión solo con correo
//  (correo + contraseña: no se envía ningún email ni código,
//  100% gratis incluso sin SMTP personalizado)
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
            text = "Entras con tu correo y una contraseña.\nSin códigos ni correos: 100% gratis.",
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
//  Login con correo + contraseña (Entrar / Crear cuenta)
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
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        )

        AuthErrorAndLoading(uiState.error, uiState.loading)

        Button(
            onClick = {
                if (isRegister) authViewModel.signUpWithEmailPassword(email, password)
                else authViewModel.signInWithEmailPassword(email, password)
            },
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
        ) {
            Text(
                text = if (isRegister) "Crear cuenta" else "Entrar",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
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
