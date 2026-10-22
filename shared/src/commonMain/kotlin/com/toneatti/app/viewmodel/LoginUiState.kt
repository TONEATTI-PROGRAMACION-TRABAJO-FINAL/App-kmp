package com.toneatti.app.viewmodel

/**
 * Estado de la pantalla de inicio de sesión. Es inmutable: [LoginViewModel] publica
 * una copia nueva en cada cambio y la pantalla se redibuja a partir de ella.
 *
 * @property email texto del campo email.
 * @property password texto del campo contraseña.
 * @property emailError error de validación del email, o `null`.
 * @property passwordError error de validación de la contraseña, o `null`.
 * @property isLoading hay una petición de login en curso.
 * @property errorMessage error del servidor o de conexión para mostrar, o `null`.
 * @property loggedIn el login fue exitoso y el token quedó guardado: hay que ir a la
 *   pantalla principal.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedIn: Boolean = false,
) {
    /** El `toString` generado expondría la contraseña en los logs. */
    override fun toString(): String =
        "LoginUiState(email=$email, password=****, emailError=$emailError, passwordError=$passwordError, " +
            "isLoading=$isLoading, errorMessage=$errorMessage, loggedIn=$loggedIn)"
}
