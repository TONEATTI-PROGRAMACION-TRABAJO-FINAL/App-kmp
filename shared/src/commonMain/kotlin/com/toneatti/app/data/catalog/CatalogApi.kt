package com.toneatti.app.data.catalog

import com.toneatti.app.data.auth.dto.ProblemDetail
import com.toneatti.app.data.catalog.dto.CategoryResponse
import com.toneatti.app.data.catalog.dto.ProfessionalResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * Cliente de los endpoints de catálogo de Servicio-turnos.
 *
 * Todos requieren el JWT del usuario, que se envía como `Authorization: Bearer` en cada
 * método y no como cabecera global del `HttpClient`: el mismo cliente hace el login y el
 * registro, que van sin token.
 *
 * Cada método devuelve un [CatalogResult] y nunca lanza excepciones por respuestas de
 * error o falta de conexión.
 *
 * @param client cliente creado con `TurnosHttpClient`.
 */
class CatalogApi(private val client: HttpClient) {

    /**
     * `GET /professional-categories`: las categorías habilitadas (`[]` si no hay).
     *
     * @param accessToken JWT del usuario.
     */
    suspend fun categories(accessToken: String): CatalogResult<List<CategoryResponse>> =
        request { client.get("professional-categories") { bearerAuth(accessToken) } }

    /**
     * `GET /professionals?categoryId=`: los profesionales habilitados de la categoría
     * (`[]` si no tiene o no existe).
     *
     * @param accessToken JWT del usuario.
     * @param categoryId categoría elegida.
     */
    suspend fun professionals(accessToken: String, categoryId: Long): CatalogResult<List<ProfessionalResponse>> =
        request {
            client.get("professionals") {
                bearerAuth(accessToken)
                parameter("categoryId", categoryId)
            }
        }

    /**
     * Ejecuta una petición y traduce la respuesta a [CatalogResult].
     *
     * El éxito es siempre 200. 401 → [CatalogError.Unauthorized] (lo responde Spring
     * Security, sin cuerpo); 503 → [CatalogError.ServiceUnavailable]; el resto →
     * [CatalogError.Unexpected].
     *
     * @param send realiza la petición.
     */
    private suspend inline fun <reified T> request(send: () -> HttpResponse): CatalogResult<T> {
        val response = try {
            send()
        } catch (e: IOException) {
            // Sin conexión, conexión rechazada o tiempo agotado (los timeouts de Ktor son IOException).
            return CatalogResult.Failure(CatalogError.Network)
        }

        return when (val status = response.status) {
            HttpStatusCode.OK -> try {
                CatalogResult.Success(response.body<T>())
            } catch (e: CancellationException) {
                // La cancelación de la corrutina (p. ej. al cambiar de categoría) debe propagarse.
                throw e
            } catch (e: IOException) {
                // La conexión se cortó mientras se leía el cuerpo.
                CatalogResult.Failure(CatalogError.Network)
            } catch (e: Exception) {
                // Cuerpo que no coincide con el DTO o con un tipo de contenido inesperado.
                CatalogResult.Failure(CatalogError.Unexpected(status.value, detail = null))
            }
            HttpStatusCode.Unauthorized -> CatalogResult.Failure(CatalogError.Unauthorized)
            HttpStatusCode.ServiceUnavailable -> CatalogResult.Failure(CatalogError.ServiceUnavailable)
            else -> CatalogResult.Failure(CatalogError.Unexpected(status.value, problemDetailOf(response)))
        }
    }

    /** Lee el `detail` de un cuerpo problem+json, o `null` si no hay cuerpo o no se puede leer. */
    private suspend fun problemDetailOf(response: HttpResponse): String? =
        try {
            response.body<ProblemDetail>().detail
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // El cuerpo de error es solo para diagnóstico: si no se entiende, se ignora.
            null
        }
}
