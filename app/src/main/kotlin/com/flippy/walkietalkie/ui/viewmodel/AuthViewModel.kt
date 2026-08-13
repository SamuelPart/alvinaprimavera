package com.flippy.walkietalkie.ui.viewmodel

import android.app.Application
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
    val error: String? = null,
    val emailCodeSent: Boolean = false,
    val phoneCodeSent: Boolean = false
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

    // ------------- Correo electrónico (OTP de 6 dígitos) -------------

    fun sendEmailCode(email: String) {
        val clean = email.trim()
        if (clean.isEmpty() || !clean.contains("@")) {
            setError("Escribe un correo válido")
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                SupabaseRepository.sendEmailCode(clean)
                _uiState.update { it.copy(loading = false, emailCodeSent = true) }
            } catch (e: Exception) {
                setError(friendlyAuthError(e.message))
            }
        }
    }

    fun verifyEmailCode(email: String, code: String) {
        val cleanCode = code.trim()
        if (cleanCode.length < 6) {
            setError("El código tiene 6 dígitos")
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                SupabaseRepository.verifyEmailCode(email.trim(), cleanCode)
                _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                setError(friendlyAuthError(e.message))
            }
        }
    }

    // ------------- Teléfono (SMS OTP) -------------

    fun sendPhoneCode(phone: String) {
        val clean = phone.trim().replace(" ", "")
        if (clean.length < 8 || !clean.startsWith("+")) {
            setError("Escribe el número con código de país, ej: +51999888777")
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                SupabaseRepository.sendPhoneCode(clean)
                _uiState.update { it.copy(loading = false, phoneCodeSent = true) }
            } catch (e: Exception) {
                setError(friendlyAuthError(e.message))
            }
        }
    }

    fun verifyPhoneCode(phone: String, code: String) {
        val cleanCode = code.trim()
        if (cleanCode.length < 6) {
            setError("El código tiene 6 dígitos")
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                SupabaseRepository.verifyPhoneCode(phone.trim().replace(" ", ""), cleanCode)
                _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                setError(friendlyAuthError(e.message))
            }
        }
    }

    // ------------- Google (ID token nativo de Android) -------------

    fun signInWithGoogle(idToken: String) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                SupabaseRepository.signInWithGoogleToken(idToken)
                _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                setError(friendlyAuthError(e.message))
            }
        }
    }

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
        raw.contains("Invalid login", true) || raw.contains("invalid", true) && raw.contains("token", true) ->
            "Código incorrecto, inténtalo de nuevo"
        raw.contains("provider is not enabled", true) || raw.contains("Unsupported provider", true) ->
            "Este método no está activado en Supabase (Authentication → Providers)"
        raw.contains("Unable to validate email", true) -> "Correo no válido"
        raw.contains("network", true) || raw.contains("Unable to resolve host", true) ->
            "Sin conexión a internet"
        raw.contains("not configured", true) ->
            "Supabase no está configurado en local.properties"
        else -> "Error: ${raw.take(120)}"
    }
}
