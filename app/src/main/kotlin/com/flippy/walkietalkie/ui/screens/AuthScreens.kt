package com.flippy.walkietalkie.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.flippy.walkietalkie.BuildConfig
import com.flippy.walkietalkie.ui.components.GoogleGLogo
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

// ============================================================
//  Pantalla de bienvenida: 3 métodos de inicio de sesión
// ============================================================

@Composable
fun WelcomeScreen(
    authViewModel: AuthViewModel,
    onOpenEmail: () -> Unit,
    onOpenPhone: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

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

        // ── Google: botón blanco destacado con la G de colores ──
        Button(
            onClick = {
                val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (webClientId.isBlank()) {
                    authViewModel.setError("Falta google.web_client_id en local.properties")
                    return@Button
                }
                scope.launch {
                    try {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(webClientId)
                            .build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        val result = credentialManager.getCredential(context, request)
                        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                        authViewModel.signInWithGoogle(credential.idToken)
                    } catch (e: Exception) {
                        authViewModel.setError("Google Sign-In cancelado o no disponible")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !uiState.loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(27.dp)
        ) {
            GoogleGLogo(size = 20.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continuar con Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Teléfono ──
        AuthSecondaryButton(
            text = "Continuar con teléfono",
            onClick = onOpenPhone,
            enabled = !uiState.loading
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Correo electrónico ──
        AuthSecondaryButton(
            text = "Continuar con correo electrónico",
            onClick = onOpenEmail,
            enabled = !uiState.loading
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        if (uiState.loading) {
            Spacer(modifier = Modifier.height(22.dp))
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
        }

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

/** Botón secundario: fondo negro, texto e icono blancos, borde sutil. */
@Composable
private fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    icon: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, OutlineLight),
        shape = RoundedCornerShape(27.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
            TextButton(onClick = { authViewModel.resetFlows() }) {
                Text("Usar otro correo", color = TextSecondary)
            }
        }
    }
}

// ============================================================
//  Login con teléfono: pedir número → código SMS de 6 dígitos
// ============================================================

@Composable
fun PhoneAuthScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) onAuthenticated()
    }

    AuthScaffold(
        title = "Número de teléfono",
        subtitle = if (uiState.phoneCodeSent)
            "Te enviamos un SMS con un código de 6 dígitos"
        else
            "Recibirás un SMS con tu código de acceso",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Número con código de país") },
            placeholder = { Text("+51 999 888 777") },
            singleLine = true,
            enabled = !uiState.phoneCodeSent,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp),
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.phoneCodeSent) {
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
                if (uiState.phoneCodeSent) authViewModel.verifyPhoneCode(phone, code)
                else authViewModel.sendPhoneCode(phone)
            },
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
        ) {
            Text(
                text = if (uiState.phoneCodeSent) "Entrar" else "Enviar SMS",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        if (uiState.phoneCodeSent) {
            TextButton(onClick = { authViewModel.resetFlows() }) {
                Text("Usar otro número", color = TextSecondary)
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
