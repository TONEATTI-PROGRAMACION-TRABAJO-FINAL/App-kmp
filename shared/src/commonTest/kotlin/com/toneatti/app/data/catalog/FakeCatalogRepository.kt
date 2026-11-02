package com.toneatti.app.data.catalog

import com.toneatti.app.data.catalog.model.Professional
import com.toneatti.app.data.catalog.model.ProfessionalCategory
import kotlinx.coroutines.CompletableDeferred

/**
 * [CatalogRepository] programable para los tests de los ViewModels.
 *
 * @property categoriesResult resultado que devolverá [categories].
 */
class FakeCatalogRepository(
    var categoriesResult: CatalogResult<List<ProfessionalCategory>> = CatalogResult.Success(emptyList()),
) : CatalogRepository {

    /** Resultado de [professionals] por categoría; las que no figuran devuelven una lista vacía. */
    val professionalsResults = mutableMapOf<Long, CatalogResult<List<Professional>>>()

    /**
     * Respuestas demoradas por categoría: [professionals] espera a que el test complete
     * el `CompletableDeferred`, para simular una respuesta lenta.
     */
    val pendingProfessionals = mutableMapOf<Long, CompletableDeferred<CatalogResult<List<Professional>>>>()

    /** Cantidad de llamadas a [categories]. */
    var categoriesCalls = 0
        private set

    /** Categorías recibidas en cada llamada a [professionals], en orden. */
    val professionalsCalls = mutableListOf<Long>()

    override suspend fun categories(): CatalogResult<List<ProfessionalCategory>> {
        categoriesCalls++
        return categoriesResult
    }

    override suspend fun professionals(categoryId: Long): CatalogResult<List<Professional>> {
        professionalsCalls += categoryId
        pendingProfessionals[categoryId]?.let { return it.await() }
        return professionalsResults[categoryId] ?: CatalogResult.Success(emptyList())
    }
}
