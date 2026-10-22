package com.toneatti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toneatti.app.data.auth.AuthError
import com.toneatti.app.data.auth.AuthRepository
import com.toneatti.app.data.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de registro.
 *
 * Sigue el mismo esquema que [LoginViewModel]: estado como [StateFlow] de
 * [RegisterUiState], eventos de la UI como métodos y el éxito como `loggedIn = true`.
 *
 * Después de crear la cuenta inicia sesión automáticamente con las mismas credenciales
 * (el backend no devuelve token al registrar), para que el usuario entre directo a la app.
 *
 * La contraseña vive solo en memoria de este ViewModel, nunca en el estado guardado.
 *
 * @param authRepository operaciones de autenticación.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())

    /** Estado actual de la pantalla. */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** El usuario editó el nombre. */
    fun onFirstNameChange(firstName: String) = edit {
        it.copy(firstName = firstName, firstNameError = null, errorMessage = null)
    }

    /** El usuario editó el apellido. */
    fun onLastNameChange(lastName: String) = edit {
        it.copy(lastName = lastName, lastNameError = null, errorMessage = null)
    }

    /** El usuario editó el email; también borra el aviso de "ya registrado". */
    fun onEmailChange(email: String) = edit {
        it.copy(email = email, emailError = null, errorMessage = null)
    }

    /** El usuario editó la contraseña. */
    fun onPasswordChange(password: String) = edit {
        it.copy(password = password, passwordError = null, errorMessage = null)
    }

    /**
     * Valida el formulario y, si es válido, crea la cuenta e inicia sesión.
     *
     * Se ignora si ya hay una petición en curso, si ya terminó bien o si la cuenta ya
     * se creó (reenviar daría "email ya registrado").
     */
    fun submit() {
        val state = _uiState.value
        if (state.isLoading || state.loggedIn || state.accountCreated) return

        // El backend recorta nombre y apellido al guardar, pero valida el valor recibido:
        // se envían ya recortados. La contraseña se envía tal cual.
        val firstName = state.firstName.trim()
        val lastName = state.lastName.trim()
        val email = state.email.trim()
        val errors = state.copy(
            firstNameError = AuthFormValidator.firstNameError(firstName),
            lastNameError = AuthFormValidator.lastNameError(lastName),
            emailError = AuthFormValidator.registerEmailError(email),
            passwordError = AuthFormValidator.registerPasswordError(state.password),
        )
        if (listOf(errors.firstNameError, errors.lastNameError, errors.emailError, errors.passwordError)
                .any { it != null }
        ) {
            _uiState.value = errors
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val registered = authRepository.register(email, state.password, firstName, lastName)) {
                is AuthResult.Failure -> showRegisterError(registered.error)
                is AuthResult.Success -> loginAfterRegister(email, state.password)
            }
        }
    }

    /** El email repetido se muestra en su campo, porque se corrige cambiándolo; el resto, abajo. */
    private fun showRegisterError(error: AuthError) {
        _uiState.update {
            if (error == AuthError.EmailAlreadyRegistered) {
                it.copy(isLoading = false, emailError = AuthErrorMessages.messageFor(error))
            } else {
                it.copy(isLoading = false, errorMessage = AuthErrorMessages.messageFor(error))
            }
        }
    }

    private suspend fun loginAfterRegister(email: String, password: String) {
        when (authRepository.login(email, password)) {
            is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, loggedIn = true) }
            is AuthResult.Failure -> _uiState.update {
                it.copy(isLoading = false, accountCreated = true, errorMessage = ACCOUNT_CREATED_LOGIN_FAILED)
            }
        }
    }

    /** Aplica una edición del formulario, salvo que la cuenta ya se haya creado. */
    private fun edit(change: (RegisterUiState) -> RegisterUiState) {
        _uiState.update { if (it.accountCreated) it else change(it) }
    }

    companion object {
        /** Mensaje cuando la cuenta se creó pero el login automático falló. */
        const val ACCOUNT_CREATED_LOGIN_FAILED: String =
            "Tu cuenta se creó, pero no pudimos iniciar sesión. Ingresá desde la pantalla de inicio de sesión."
    }
}
