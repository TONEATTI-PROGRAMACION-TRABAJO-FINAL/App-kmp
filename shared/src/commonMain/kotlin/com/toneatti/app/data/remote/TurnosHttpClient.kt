package com.toneatti.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Crea el `HttpClient` de Ktor configurado para Servicio-turnos.
 *
 * - **URL base** de [ApiConfig]: las peticiones usan rutas relativas (`"users/login"`).
 * - **JSON** con kotlinx-serialization, tanto `application/json` como
 *   `application/problem+json` (RFC 9457), el formato de error del backend.
 * - **`expectSuccess = false`**: los códigos 4xx/5xx no lanzan excepción; quien llama
 *   decide qué significa cada uno.
 * - **Timeouts** para no dejar al usuario esperando indefinidamente.
 *
 * No se instala el plugin `Logging` a propósito: escribiría el token y las contraseñas en el log.
 */
object TurnosHttpClient {

    /**
     * JSON tolerante a campos nuevos: si el backend agrega propiedades a una respuesta,
     * la app sigue funcionando.
     */
    val json: Json = Json { ignoreUnknownKeys = true }

    /** Cliente con el motor de la plataforma (OkHttp en Android). */
    fun create(): HttpClient = HttpClient { configure() }

    /**
     * Cliente con un motor dado. Lo usan los tests con `MockEngine`.
     *
     * @param engine motor HTTP a usar.
     */
    fun create(engine: HttpClientEngine): HttpClient = HttpClient(engine) { configure() }

    private fun HttpClientConfig<*>.configure() {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
            json(json, contentType = ContentType("application", "problem+json"))
        }
        install(HttpTimeout) {
            connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MILLIS
        }
        defaultRequest {
            url(ApiConfig.BASE_URL)
        }
    }
}
