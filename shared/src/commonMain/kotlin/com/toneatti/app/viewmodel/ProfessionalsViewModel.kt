package com.toneatti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toneatti.app.data.catalog.CatalogRepository
import com.toneatti.app.data.catalog.CatalogResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de búsqueda de profesionales.
 *
 * - Carga las categorías al crearse.
 * - Al elegir una categoría carga sus profesionales.
 * - Cada carga que falla se puede reintentar.
 *
 * Una sesión vencida o rechazada llega como un error más: la vuelta al login la resuelve
 * `SessionViewModel` para toda la app, así que esta pantalla no hace nada especial.
 *
 * @param catalogRepository consultas del catálogo.
 */
class ProfessionalsViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfessionalsUiState())

    /** Estado actual de la pantalla. */
    val uiState: StateFlow<ProfessionalsUiState> = _uiState.asStateFlow()

    /** Carga de profesionales en curso, para cancelarla si se elige otra categoría. */
    private var professionalsJob: Job? = null

    init {
        loadCategories()
    }

    /** Vuelve a pedir las categorías. Solo tiene efecto si la carga anterior falló. */
    fun retryCategories() {
        if (_uiState.value.categories is LoadState.Error) loadCategories()
    }

    /**
     * El usuario eligió una categoría: se cargan sus profesionales.
     *
     * Elegir la que ya está elegida no hace nada, para no repetir la consulta.
     *
     * @param categoryId categoría elegida.
     */
    fun onCategorySelected(categoryId: Long) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        loadProfessionals(categoryId)
    }

    /** Vuelve a pedir los profesionales de la categoría elegida. Solo si la carga anterior falló. */
    fun retryProfessionals() {
        val state = _uiState.value
        val categoryId = state.selectedCategoryId ?: return
        if (state.professionals is LoadState.Error) loadProfessionals(categoryId)
    }

    private fun loadCategories() {
        _uiState.update { it.copy(categories = LoadState.Loading) }
        viewModelScope.launch {
            val categories = catalogRepository.categories().toLoadState()
            _uiState.update { it.copy(categories = categories) }
        }
    }

    private fun loadProfessionals(categoryId: Long) {
        // La respuesta de la categoría anterior ya no interesa.
        professionalsJob?.cancel()
        _uiState.update { it.copy(professionals = LoadState.Loading) }
        professionalsJob = viewModelScope.launch {
            val professionals = catalogRepository.professionals(categoryId).toLoadState()
            _uiState.update {
                // Por si la respuesta llegó justo cuando el usuario cambió de categoría.
                if (it.selectedCategoryId == categoryId) it.copy(professionals = professionals) else it
            }
        }
    }

    private fun <T> CatalogResult<T>.toLoadState(): LoadState<T> = when (this) {
        is CatalogResult.Success -> LoadState.Ready(value)
        is CatalogResult.Failure -> LoadState.Error(CatalogErrorMessages.messageFor(error))
    }
}
