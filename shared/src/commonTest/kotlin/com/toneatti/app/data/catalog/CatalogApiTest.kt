package com.toneatti.app.data.catalog

import com.toneatti.app.data.catalog.dto.CategoryResponse
import com.toneatti.app.data.catalog.dto.ProfessionalResponse
import com.toneatti.app.data.remote.TurnosHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pruebas de [CatalogApi] contra un servidor simulado (`MockEngine`): forma de las
 * peticiones y traducción de cada respuesta de Servicio-turnos a [CatalogResult].
 */
class CatalogApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val problemHeaders = headersOf(HttpHeaders.ContentType, "application/problem+json")

    /** Peticiones recibidas por el servidor simulado, para inspeccionarlas. */
    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): CatalogApi {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return CatalogApi(TurnosHttpClient.create(engine))
    }

    /** Las categorías se piden con GET y el token del usuario como Bearer. */
    @Test
    fun categoriesEnviaElTokenComoBearer() = runTest {
        val api = api { respond("[]", HttpStatusCode.OK, jsonHeaders) }

        api.categories("jwt")

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://10.0.2.2:8080/professional-categories", request.url.toString())
        assertEquals("Bearer jwt", request.headers[HttpHeaders.Authorization])
    }

    /** 200 se lee como lista de categorías, ignorando campos desconocidos y sin descripción. */
    @Test
    fun categoriesExitosoDevuelveLaLista() = runTest {
        val api = api {
            respond(
                """[{"id":1,"name":"Clinica medica","description":"Atencion clinica general","extra":true},
                   {"id":2,"name":"Pediatria"}]""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        assertEquals(
            CatalogResult.Success(
                listOf(
                    CategoryResponse(1, "Clinica medica", "Atencion clinica general"),
                    CategoryResponse(2, "Pediatria", null),
                ),
            ),
            api.categories("jwt"),
        )
    }

    /** Los profesionales se piden con el `categoryId` como query y el token como Bearer. */
    @Test
    fun professionalsEnviaLaCategoriaYElToken() = runTest {
        val api = api { respond("[]", HttpStatusCode.OK, jsonHeaders) }

        api.professionals("jwt", categoryId = 7)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://10.0.2.2:8080/professionals?categoryId=7", request.url.toString())
        assertEquals("Bearer jwt", request.headers[HttpHeaders.Authorization])
    }

    /** 200 se lee como lista de profesionales. */
    @Test
    fun professionalsExitosoDevuelveLaLista() = runTest {
        val api = api {
            respond(
                """[{"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true}]""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        assertEquals(
            CatalogResult.Success(listOf(ProfessionalResponse(15, 1, "Ana", "Gomez", enabled = true))),
            api.professionals("jwt", categoryId = 1),
        )
    }

    /** Una categoría sin profesionales devuelve una lista vacía, no un error. */
    @Test
    fun professionalsSinResultadosDevuelveListaVacia() = runTest {
        val api = api { respond("[]", HttpStatusCode.OK, jsonHeaders) }

        assertEquals(CatalogResult.Success(emptyList()), api.professionals("jwt", categoryId = 99))
    }

    /** 401 (de Spring Security, sin cuerpo) es token rechazado. */
    @Test
    fun respuesta401EsNoAutorizado() = runTest {
        val api = api { respond("", HttpStatusCode.Unauthorized) }

        assertEquals(CatalogResult.Failure(CatalogError.Unauthorized), api.categories("vencido"))
    }

    /** 503 es Servicio-catalogo no disponible. */
    @Test
    fun respuesta503EsServicioNoDisponible() = runTest {
        val api = api {
            respond(problem(503, "Catalog service unavailable"), HttpStatusCode.ServiceUnavailable, problemHeaders)
        }

        assertEquals(CatalogResult.Failure(CatalogError.ServiceUnavailable), api.professionals("jwt", 1))
    }

    /** Cualquier otro código es inesperado y conserva el `detail` del problem+json. */
    @Test
    fun otroCodigoEsInesperadoConDetalle() = runTest {
        val api = api { respond(problem(500, "Boom"), HttpStatusCode.InternalServerError, problemHeaders) }

        assertEquals(CatalogResult.Failure(CatalogError.Unexpected(500, "Boom")), api.categories("jwt"))
    }

    /** Un 200 con un cuerpo que no es una lista no rompe la app. */
    @Test
    fun cuerpoIlegibleEsInesperado() = runTest {
        val api = api { respond("""{"foo":1}""", HttpStatusCode.OK, jsonHeaders) }

        assertEquals(CatalogResult.Failure(CatalogError.Unexpected(200, null)), api.categories("jwt"))
    }

    /** Sin conexión con el servidor el resultado es un error de red, no una excepción. */
    @Test
    fun sinConexionEsErrorDeRed() = runTest {
        val api = api { throw IOException("Connection refused") }

        assertEquals(CatalogResult.Failure(CatalogError.Network), api.professionals("jwt", 1))
    }

    private fun problem(status: Int, detail: String): String =
        """{"type":"about:blank","title":"Error","status":$status,"detail":"$detail","instance":"/professionals"}"""
}
