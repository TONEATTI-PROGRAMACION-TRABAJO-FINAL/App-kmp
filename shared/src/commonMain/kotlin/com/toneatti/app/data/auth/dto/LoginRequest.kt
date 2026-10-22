package com.toneatti.app.data.auth.dto

import kotlinx.serialization.Serializable

/**
 * Cuerpo de `POST /users/login` (`LoginRequest` de Servicio-turnos).
 *
 * @property email email del usuario, sin espacios alrededor.
 * @property password contraseña tal como la escribió el usuario.
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
) {
    /** El `toString` generado expondría la contraseña en los logs. */
    override fun toString(): String = "LoginRequest(email=$email, password=****)"
}
