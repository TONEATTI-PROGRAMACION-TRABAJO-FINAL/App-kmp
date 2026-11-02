package com.toneatti.app.data.catalog.dto

import kotlinx.serialization.Serializable

/**
 * Categoría de `GET /professional-categories` de Servicio-turnos.
 *
 * @property id identificador de la categoría.
 * @property name nombre visible.
 * @property description descripción; opcional para tolerar categorías cargadas sin ella.
 */
@Serializable
data class CategoryResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
)
