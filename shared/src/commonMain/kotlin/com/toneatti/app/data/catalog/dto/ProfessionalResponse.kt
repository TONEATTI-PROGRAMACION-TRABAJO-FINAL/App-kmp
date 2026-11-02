package com.toneatti.app.data.catalog.dto

import kotlinx.serialization.Serializable

/**
 * Profesional de `GET /professionals` de Servicio-turnos.
 *
 * @property id identificador del profesional.
 * @property categoryId categoría a la que pertenece.
 * @property firstName nombre.
 * @property lastName apellido.
 * @property enabled si atiende; el listado solo devuelve habilitados, pero el campo viene igual.
 */
@Serializable
data class ProfessionalResponse(
    val id: Long,
    val categoryId: Long,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean,
)
