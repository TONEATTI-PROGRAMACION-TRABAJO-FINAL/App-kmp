package com.toneatti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toneatti.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sesión del usuario a nivel de toda la app.
 *
 * - Decide la pantalla inicial: comprueba la sesión guardada una sola vez, al crearse.
 * - Avisa cuando la sesión se descarta mientras se usa la app (token vencido o rechazado
 *   con un 401 en cualquier pantalla), para volver al login desde un único lugar.
 *
 * Vive lo mismo que la `Activity` (no que una pantalla), así que al rotar no vuelve a
 * consultar al backend y sigue escuchando los avisos aunque se cambie de pestaña.
 *
 * @param authRepository operaciones de autenticación.
 * @param sessionExpirations avisos de sesión descartada (`SessionManager.expirations`).
 */
class SessionViewModel(
    private val authRepository: AuthRepository,
    sessionExpirations: Flow<Unit>,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState.Checking)

    /** Estado de la sesión al abrir la app; empieza en [SessionState.Checking]. */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)

    /**
     * `true` cuando la sesión se descartó y hay que volver al login. Quien conoce la
     * navegación lo observa, navega y llama a [onSessionExpiredHandled].
     *
     * Es un estado y no un evento suelto para que el aviso no se pierda si llega mientras
     * la UI no está observando (por ejemplo, durante una rotación).
     */
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = if (authRepository.restoreSession()) SessionState.LoggedIn else SessionState.LoggedOut
        }
        viewModelScope.launch {
            sessionExpirations.collect { _sessionExpired.value = true }
        }
    }

    /** La UI ya volvió al login por la sesión descartada. */
    fun onSessionExpiredHandled() {
        _sessionExpired.value = false
    }
}
