package com.toneatti.app.data.auth.dto

import kotlinx.serialization.Serializable

/**
 * Cuerpo de error `application/problem+json` (RFC 9457) que devuelve Servicio-turnos.
 *
 * Todos los campos son opcionales: el RFC no obliga a ninguno, y algunas respuestas
 * (por ejemplo el 401 de Spring Security) ni siquiera traen cuerpo.
 *
 * El texto de [detail] está en inglés y es para diagnóstico: la app muestra sus
 * propios mensajes en español según el código de estado.
 */
@Serializable
data class ProblemDetail(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
)
