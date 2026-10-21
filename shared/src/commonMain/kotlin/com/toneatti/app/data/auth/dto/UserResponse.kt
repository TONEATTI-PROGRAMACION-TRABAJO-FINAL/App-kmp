package com.toneatti.app.data.auth.dto

import kotlinx.serialization.Serializable

/**
 * Usuario devuelto por `GET /users/me` y `POST /users` (`UserResponse` de Servicio-turnos).
 *
 * @property externalId identificador público del usuario (el `sub` del JWT).
 * @property email email normalizado (minúsculas, sin espacios).
 * @property firstName nombre.
 * @property lastName apellido.
 */
@Serializable
data class UserResponse(
    val externalId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
)
