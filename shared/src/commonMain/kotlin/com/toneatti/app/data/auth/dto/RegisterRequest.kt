package com.toneatti.app.data.auth.dto

import kotlinx.serialization.Serializable

/**
 * Cuerpo de `POST /users` (`UserRequest` de Servicio-turnos).
 *
 * @property email email del usuario, sin espacios alrededor.
 * @property password contraseña tal como la escribió el usuario.
 * @property firstName nombre, sin espacios alrededor.
 * @property lastName apellido, sin espacios alrededor.
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
) {
    /** El `toString` generado expondría la contraseña en los logs. */
    override fun toString(): String =
        "RegisterRequest(email=$email, password=****, firstName=$firstName, lastName=$lastName)"
}
