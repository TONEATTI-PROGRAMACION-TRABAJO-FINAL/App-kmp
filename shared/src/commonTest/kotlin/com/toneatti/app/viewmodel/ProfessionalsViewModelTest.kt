package com.toneatti.app.viewmodel

import com.toneatti.app.data.catalog.CatalogError
import com.toneatti.app.data.catalog.CatalogResult
import com.toneatti.app.data.catalog.FakeCatalogRepository
import com.toneatti.app.data.catalog.model.Professional
import com.toneatti.app.data.catalog.model.ProfessionalCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pruebas de [ProfessionalsViewModel]: carga de categorías, elección de categoría,
 * errores con reintento y respuestas que llegan tarde.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfessionalsViewModelTest {

    private val clinica = ProfessionalCategory(1, "Clinica medica", "Atencion clinica general")
    private val pediatria = ProfessionalCategory(2, "Pediatria", null)
    private val ana = Professional(15, 1, "Ana", "Gomez")
    private val luis = Professional(21, 2, "Luis", "Diaz")

    private val repository = FakeCatalogRepository(CatalogResult.Success(listOf(clinica, pediatria)))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Al crearse carga las categorías, sin ninguna elegida. */
    @Test
    fun cargaLasCategoriasAlCrearse() = runTest {
        val viewModel = ProfessionalsViewModel(repository)

        assertEquals(LoadState.Loading, viewModel.uiState.value.categories)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LoadState.Ready(listOf(clinica, pediatria)), state.categories)
        assertNull(state.selectedCategoryId)
        assertNull(state.professionals)
    }

    /** Sin categorías, la lista queda vacía y no es un error. */
    @Test
    fun sinCategoriasQuedaListaVacia() = runTest {
        repository.categoriesResult = CatalogResult.Success(emptyList())
        val viewModel = ProfessionalsViewModel(repository)

        advanceUntilIdle()

        assertEquals(LoadState.Ready(emptyList()), viewModel.uiState.value.categories)
    }

    /** Un error de categorías muestra el mensaje, y el reintento vuelve a pedirlas. */
    @Test
    fun errorDeCategoriasSeReintenta() = runTest {
        repository.categoriesResult = CatalogResult.Failure(CatalogError.Network)
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()

        assertEquals(
            LoadState.Error(CatalogErrorMessages.messageFor(CatalogError.Network)),
            viewModel.uiState.value.categories,
        )

        repository.categoriesResult = CatalogResult.Success(listOf(clinica))
        viewModel.retryCategories()
        assertEquals(LoadState.Loading, viewModel.uiState.value.categories)
        advanceUntilIdle()

        assertEquals(LoadState.Ready(listOf(clinica)), viewModel.uiState.value.categories)
        assertEquals(2, repository.categoriesCalls)
    }

    /** Reintentar sin un error previo no repite la consulta. */
    @Test
    fun reintentarSinErrorNoHaceNada() = runTest {
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()

        viewModel.retryCategories()
        viewModel.retryProfessionals()
        advanceUntilIdle()

        assertEquals(1, repository.categoriesCalls)
        assertEquals(emptyList(), repository.professionalsCalls)
    }

    /** Elegir una categoría carga sus profesionales. */
    @Test
    fun elegirCategoriaCargaSusProfesionales() = runTest {
        repository.professionalsResults[1] = CatalogResult.Success(listOf(ana))
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()

        viewModel.onCategorySelected(1)
        assertEquals(1L, viewModel.uiState.value.selectedCategoryId)
        assertEquals(LoadState.Loading, viewModel.uiState.value.professionals)
        advanceUntilIdle()

        assertEquals(LoadState.Ready(listOf(ana)), viewModel.uiState.value.professionals)
        assertEquals(listOf(1L), repository.professionalsCalls)
    }

    /** Una categoría sin profesionales queda con la lista vacía, no con un error. */
    @Test
    fun categoriaSinProfesionalesQuedaListaVacia() = runTest {
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()

        viewModel.onCategorySelected(2)
        advanceUntilIdle()

        assertEquals(LoadState.Ready(emptyList()), viewModel.uiState.value.professionals)
    }

    /** Elegir la categoría que ya está elegida no repite la consulta. */
    @Test
    fun elegirLaMismaCategoriaNoRecarga() = runTest {
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()

        viewModel.onCategorySelected(1)
        advanceUntilIdle()
        viewModel.onCategorySelected(1)
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.professionalsCalls)
    }

    /** Un error de profesionales se reintenta con la misma categoría. */
    @Test
    fun errorDeProfesionalesSeReintentaConLaMismaCategoria() = runTest {
        repository.professionalsResults[1] = CatalogResult.Failure(CatalogError.ServiceUnavailable)
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()
        viewModel.onCategorySelected(1)
        advanceUntilIdle()

        assertEquals(
            LoadState.Error(CatalogErrorMessages.messageFor(CatalogError.ServiceUnavailable)),
            viewModel.uiState.value.professionals,
        )

        repository.professionalsResults[1] = CatalogResult.Success(listOf(ana))
        viewModel.retryProfessionals()
        advanceUntilIdle()

        assertEquals(LoadState.Ready(listOf(ana)), viewModel.uiState.value.professionals)
        assertEquals(listOf(1L, 1L), repository.professionalsCalls)
    }

    /** La respuesta lenta de la categoría anterior no pisa la de la categoría elegida después. */
    @Test
    fun unaRespuestaViejaNoPisaLaCategoriaActual() = runTest {
        val slowClinica = CompletableDeferred<CatalogResult<List<Professional>>>()
        repository.pendingProfessionals[1] = slowClinica
        repository.professionalsResults[2] = CatalogResult.Success(listOf(luis))
        val viewModel = ProfessionalsViewModel(repository)
        advanceUntilIdle()

        viewModel.onCategorySelected(1)
        advanceUntilIdle()
        viewModel.onCategorySelected(2)
        advanceUntilIdle()
        slowClinica.complete(CatalogResult.Success(listOf(ana)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2L, state.selectedCategoryId)
        assertEquals(LoadState.Ready(listOf(luis)), state.professionals)
    }

    /** Una sesión vencida se muestra como un error más: la vuelta al login no es de esta pantalla. */
    @Test
    fun sesionVencidaSeMuestraComoError() = runTest {
        repository.categoriesResult = CatalogResult.Failure(CatalogError.SessionExpired)
        val viewModel = ProfessionalsViewModel(repository)

        advanceUntilIdle()

        assertEquals(
            LoadState.Error(CatalogErrorMessages.messageFor(CatalogError.SessionExpired)),
            viewModel.uiState.value.categories,
        )
    }
}
