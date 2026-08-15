package com.flippy.walkietalkie.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    /** Estado global de la sesión (LoadingFromStorage / NotAuthenticated / Authenticated). */
    val sessionStatus: StateFlow<SessionStatus> = SupabaseRepository.sessionStatus

    fun setError(message: String) {
        _uiState.update { it.copy(error = message, loading = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ------------- Correo electrónico + contraseña -------------
    //
    // NO se envía ningún correo ni código: con la opción «Confirm email»
    // desactivada en el dashboard de Supabase, signUp inicia sesión al
    // instante y signIn es directo. Funciona con el plan gratuito.

    /** Entra con una cuenta ya creada. */
    fun signInWithEmailPassword(email: String, password: String) {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            setError("Escribe un correo válido")
            return
        }
        if (password.isEmpty()) {
            setError("Escribe tu contraseña")
            return
        }
        runAuth {
            SupabaseRepository.signInWithEmail(cleanEmail, password)
        }
    }

    /** Crea una cuenta nueva y entra al instante. */
    fun signUpWithEmailPassword(email: String, password: String) {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            setError("Escribe un correo válido")
            return
        }
        if (password.length < 6) {
            setError("La contraseña debe tener al menos 6 caracteres")
            return
        }
        runAuth {
            SupabaseRepository.signUpWithEmail(cleanEmail, password)
        }
    }

    private fun runAuth(block: suspend () -> Unit) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                Log.e("Flippy", "Error de autenticación", e)
                setError(friendlyAuthError(e.message))
            }
        }
    }

    private fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() && email.contains("@") && email.contains(".")

    fun signOut() {
        viewModelScope.launch {
            try {
                SupabaseRepository.signOut()
            } catch (_: Exception) {
            }
        }
    }

    fun resetFlows() {
        _uiState.value = AuthUiState()
    }

    private fun friendlyAuthError(raw: String?): String = when {
        raw == null -> "Error desconocido"
        raw.contains("Invalid login credentials", true) ->
            "Correo o contraseña incorrectos (¿ya creaste tu cuenta?)"
        raw.contains("already registered", true) ->
            "Ese correo ya tiene cuenta: usa «Entrar» en vez de «Crear cuenta»"
        raw.contains("Email not confirmed", true) ->
            "Supabase pide confirmar el correo: desactiva «Confirm email» en Authentication → Sign In / Providers → Email"
        raw.contains("Password", true) && raw.contains("least", true) ->
            "Contraseña muy corta: usa al menos 6 caracteres"
        raw.contains("rate limit", true) ->
            "Demasiados intentos: espera un minuto y vuelve a probar"
        raw.contains("Unable to validate email", true) -> "Correo no válido"
        raw.contains("network", true) || raw.contains("Unable to resolve host", true) ->
            "Sin conexión a internet"
        raw.contains("not configured", true) ->
            "Supabase no está configurado en local.properties"
        else -> "Error: ${raw.take(120)}"
    }
}
