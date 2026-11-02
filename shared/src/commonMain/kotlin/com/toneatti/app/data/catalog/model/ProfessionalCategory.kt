package com.toneatti.app.data.catalog.model

/**
 * Categoría de profesionales tal como la usa la app.
 *
 * Es independiente del JSON del backend ([com.toneatti.app.data.catalog.dto.CategoryResponse]):
 * si el contrato cambia, solo cambia la conversión del repositorio, no la UI.
 *
 * @property id identificador de la categoría.
 * @property name nombre visible.
 * @property description descripción, o `null` si no tiene.
 */
data class ProfessionalCategory(
    val id: Long,
    val name: String,
    val description: String?,
)
