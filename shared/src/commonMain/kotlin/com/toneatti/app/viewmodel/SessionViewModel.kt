package com.toneatti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toneatti.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Decide la pantalla inicial: comprueba la sesión guardada una sola vez, al crearse.
 *
 * Vive lo mismo que la `Activity` (no que una pantalla), así que al rotar no vuelve a
 * consultar al backend.
 *
 * @param authRepository operaciones de autenticación.
 */
class SessionViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState.Checking)

    /** Estado de la sesión; empieza en [SessionState.Checking]. */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = if (authRepository.restoreSession()) SessionState.LoggedIn else SessionState.LoggedOut
        }
    }
}
