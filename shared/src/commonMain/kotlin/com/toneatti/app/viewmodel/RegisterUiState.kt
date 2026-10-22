package com.toneatti.app.viewmodel

/**
 * Estado de la pantalla de registro. Es inmutable: [RegisterViewModel] publica una
 * copia nueva en cada cambio y la pantalla se redibuja a partir de ella.
 *
 * @property firstName texto del campo nombre.
 * @property lastName texto del campo apellido.
 * @property email texto del campo email.
 * @property password texto del campo contraseña.
 * @property firstNameError error de validación del nombre, o `null`.
 * @property lastNameError error de validación del apellido, o `null`.
 * @property emailError error del email (de validación o porque ya está registrado), o `null`.
 * @property passwordError error de validación de la contraseña, o `null`.
 * @property isLoading hay una petición de registro o de login en curso.
 * @property errorMessage error del servidor o de conexión para mostrar, o `null`.
 * @property accountCreated la cuenta se creó pero el login automático falló: el
 *   formulario queda bloqueado, porque reenviarlo daría "email ya registrado".
 * @property loggedIn registro y login automático exitosos: hay que ir a la pantalla principal.
 */
data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val accountCreated: Boolean = false,
    val loggedIn: Boolean = false,
) {
    /** El `toString` generado expondría la contraseña en los logs. */
    override fun toString(): String =
        "RegisterUiState(firstName=$firstName, lastName=$lastName, email=$email, password=****, " +
            "firstNameError=$firstNameError, lastNameError=$lastNameError, emailError=$emailError, " +
            "passwordError=$passwordError, isLoading=$isLoading, errorMessage=$errorMessage, " +
            "accountCreated=$accountCreated, loggedIn=$loggedIn)"
}
