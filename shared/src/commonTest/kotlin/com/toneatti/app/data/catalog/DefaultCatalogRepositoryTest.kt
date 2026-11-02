package com.toneatti.app.data.catalog

import com.toneatti.app.data.catalog.model.Professional
import com.toneatti.app.data.catalog.model.ProfessionalCategory
import com.toneatti.app.data.remote.TurnosHttpClient
import com.toneatti.app.data.session.FakeTokenStorage
import com.toneatti.app.data.session.SessionManager
import com.toneatti.app.data.session.UserSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de [DefaultCatalogRepository]: conversión a modelos y qué pasa con la sesión
 * cuando falta o el backend la rechaza.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCatalogRepositoryTest {

    private val now = 1_000_000_000L
    private val validSession = UserSession("jwt", expiresAtMillis = now + 3_600_000)
    private val storage = FakeTokenStorage(validSession)
    private val sessionManager = SessionManager(storage, nowMillis = { now })

    /** Peticiones recibidas por el servidor simulado. */
    private val requests = mutableListOf<HttpRequestData>()

    /** Cantidad de avisos de sesión vencida. */
    private var expirations = 0

    private fun repository(status: HttpStatusCode, body: String = ""): DefaultCatalogRepository {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        return DefaultCatalogRepository(CatalogApi(TurnosHttpClient.create(engine)), sessionManager)
    }

    private fun TestScope.listenExpirations() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sessionManager.expirations.collect { expirations++ }
        }
    }

    /** Las categorías se convierten al modelo de la app, con el token de la sesión. */
    @Test
    fun categoriesDevuelveLosModelos() = runTest {
        val repository = repository(
            HttpStatusCode.OK,
            """[{"id":1,"name":"Clinica medica","description":"Atencion clinica general"}]""",
        )

        assertEquals(
            CatalogResult.Success(listOf(ProfessionalCategory(1, "Clinica medica", "Atencion clinica general"))),
            repository.categories(),
        )
        assertEquals("Bearer jwt", requests.single().headers[HttpHeaders.Authorization])
    }

    /** Los profesionales se convierten al modelo y se descartan los deshabilitados. */
    @Test
    fun professionalsDescartaLosDeshabilitados() = runTest {
        val repository = repository(
            HttpStatusCode.OK,
            """[{"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true},
               {"id":18,"categoryId":1,"firstName":"Luis","lastName":"Diaz","enabled":false}]""",
        )

        assertEquals(
            CatalogResult.Success(listOf(Professional(15, 1, "Ana", "Gomez"))),
            repository.professionals(categoryId = 1),
        )
    }

    /** Sin sesión no se hace la petición: la sesión vencida se informa y se avisa. */
    @Test
    fun sinSesionDevuelveSesionVencidaSinLlamarAlBackend() = runTest {
        listenExpirations()
        storage.session = null
        val repository = repository(HttpStatusCode.OK, "[]")

        assertEquals(CatalogResult.Failure(CatalogError.SessionExpired), repository.categories())
        assertTrue(requests.isEmpty())
        assertEquals(1, expirations)
    }

    /** Un 401 descarta la sesión, avisa y se informa como sesión vencida. */
    @Test
    fun respuesta401BorraLaSesionYAvisa() = runTest {
        listenExpirations()
        val repository = repository(HttpStatusCode.Unauthorized)

        assertEquals(CatalogResult.Failure(CatalogError.SessionExpired), repository.professionals(categoryId = 1))
        assertNull(storage.session)
        assertEquals(1, expirations)
    }

    /** Otros errores no tocan la sesión: Servicio-catalogo caído no es motivo para salir. */
    @Test
    fun respuesta503ConservaLaSesion() = runTest {
        listenExpirations()
        val repository = repository(HttpStatusCode.ServiceUnavailable)

        assertEquals(CatalogResult.Failure(CatalogError.ServiceUnavailable), repository.categories())
        assertEquals(validSession, storage.session)
        assertEquals(0, expirations)
    }
}
