package com.toneatti.app.data.auth.dto

import kotlinx.serialization.Serializable

/**
 * Respuesta 200 de `POST /users/login` (`TokenResponse` de Servicio-turnos), con la
 * forma de una respuesta de token OAuth2 (RFC 6749, sección 5.1) en camelCase.
 *
 * @property accessToken JWT del usuario.
 * @property tokenType siempre `"Bearer"`.
 * @property expiresIn vida del token en **segundos**.
 */
@Serializable
data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
) {
    /** El `toString` generado expondría el token en los logs. */
    override fun toString(): String = "TokenResponse(accessToken=****, tokenType=$tokenType, expiresIn=$expiresIn)"
}
