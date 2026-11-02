package com.toneatti.app.viewmodel

import com.toneatti.app.data.catalog.model.Professional
import com.toneatti.app.data.catalog.model.ProfessionalCategory

/**
 * Estado de la pantalla de búsqueda de profesionales. Es inmutable:
 * [ProfessionalsViewModel] publica una copia nueva en cada cambio.
 *
 * @property categories categorías habilitadas; se cargan al abrir la pantalla.
 * @property selectedCategoryId categoría elegida, o `null` si todavía no se eligió ninguna.
 * @property professionals profesionales de la categoría elegida, o `null` si no hay
 *   categoría elegida.
 */
data class ProfessionalsUiState(
    val categories: LoadState<List<ProfessionalCategory>> = LoadState.Loading,
    val selectedCategoryId: Long? = null,
    val professionals: LoadState<List<Professional>>? = null,
)
