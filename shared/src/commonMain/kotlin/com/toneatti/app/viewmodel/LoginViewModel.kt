package com.toneatti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toneatti.app.data.auth.AuthRepository
import com.toneatti.app.data.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de inicio de sesión.
 *
 * - Expone el estado como [StateFlow] de [LoginUiState]; la pantalla no guarda estado propio.
 * - Recibe los eventos de la UI como métodos ([onEmailChange], [onPasswordChange], [submit]).
 * - El éxito se informa con `loggedIn = true` en el estado: quien conoce la navegación
 *   lo observa y cambia de pantalla.
 *
 * La contraseña vive solo en memoria de este ViewModel: no se guarda en `rememberSaveable`
 * ni en `SavedStateHandle`, que la escribirían en el estado guardado de la Activity.
 *
 * @param authRepository operaciones de autenticación.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    /** Estado actual de la pantalla. */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** El usuario editó el email: se borran los errores, que ya no corresponden al valor nuevo. */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    /** El usuario editó la contraseña. */
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    /**
     * Valida el formulario y, si es válido, inicia sesión.
     *
     * Se ignora si ya hay un login en curso (doble toque en el botón) o si ya terminó bien.
     */
    fun submit() {
        val state = _uiState.value
        if (state.isLoading || state.loggedIn) return

        // El backend también recorta el email, pero valida el valor recibido: se envía ya recortado.
        val email = state.email.trim()
        val emailError = AuthFormValidator.loginEmailError(email)
        val passwordError = AuthFormValidator.loginPasswordError(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.login(email, state.password)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, loggedIn = true) }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = AuthErrorMessages.messageFor(result.error))
                }
            }
        }
    }
}
