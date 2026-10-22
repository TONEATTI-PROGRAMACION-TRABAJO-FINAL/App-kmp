package com.toneatti.app.data.auth

import com.toneatti.app.data.remote.TurnosHttpClient
import com.toneatti.app.data.session.FakeTokenStorage
import com.toneatti.app.data.session.UserSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de [DefaultAuthRepository]: qué se guarda después de cada login y cuándo se
 * conserva o se borra la sesión al abrir la app.
 */
class DefaultAuthRepositoryTest {

    private val now = 1_000_000_000L
    private val storage = FakeTokenStorage()
    private val validSession = UserSession("jwt", expiresAtMillis = now + 3_600_000)

    /** Peticiones recibidas por el servidor simulado. */
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): DefaultAuthRepository {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return DefaultAuthRepository(AuthApi(TurnosHttpClient.create(engine)), storage, nowMillis = { now })
    }

    private fun repository(status: HttpStatusCode, body: String = ""): DefaultAuthRepository =
        repository { respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }

    /** Un login exitoso guarda el token con su vencimiento absoluto. */
    @Test
    fun loginExitosoGuardaLaSesion() = runTest {
        val repository = repository(
            HttpStatusCode.OK,
            """{"accessToken":"jwt","tokenType":"Bearer","expiresIn":3600}""",
        )

        val result = repository.login("ana@example.com", "secreta123")

        assertEquals(AuthResult.Success(Unit), result)
        assertEquals(UserSession("jwt", expiresAtMillis = now + 3_600_000), storage.session)
    }

    /** Un login fallido devuelve el error y no guarda nada. */
    @Test
    fun loginFallidoNoGuardaNada() = runTest {
        val repository = repository(HttpStatusCode.Unauthorized)

        val result = repository.login("ana@example.com", "mala")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
        assertNull(storage.session)
    }

    /** Sin sesión guardada no hay nada que validar ni petición que hacer. */
    @Test
    fun restoreSinSesionDevuelveFalse() = runTest {
        val repository = repository(HttpStatusCode.OK)

        assertFalse(repository.restoreSession())
        assertTrue(requests.isEmpty())
    }

    /** Una sesión vencida se borra sin consultar al backend. */
    @Test
    fun restoreConSesionVencidaLaBorra() = runTest {
        storage.session = UserSession("jwt", expiresAtMillis = now - 1)
        val repository = repository(HttpStatusCode.OK)

        assertFalse(repository.restoreSession())
        assertNull(storage.session)
        assertTrue(requests.isEmpty())
    }

    /** Si `/users/me` responde 200, la sesión sigue y se envió el token guardado. */
    @Test
    fun restoreConSesionAceptadaDevuelveTrue() = runTest {
        storage.session = validSession
        val repository = repository(
            HttpStatusCode.OK,
            """{"externalId":"id-1","email":"ana@example.com","firstName":"Ana","lastName":"Pérez"}""",
        )

        assertTrue(repository.restoreSession())
        assertEquals("Bearer jwt", requests.single().headers[HttpHeaders.Authorization])
        assertEquals(validSession, storage.session)
    }

    /** 401 de `/users/me` (token rechazado, p. ej. otro `USER_JWT_SECRET`): se borra. */
    @Test
    fun restoreConTokenRechazadoLaBorra() = runTest {
        storage.session = validSession
        val repository = repository(HttpStatusCode.Unauthorized)

        assertFalse(repository.restoreSession())
        assertNull(storage.session)
    }

    /** 404 de `/users/me` (usuario borrado, p. ej. al reiniciar H2): se borra. */
    @Test
    fun restoreConUsuarioInexistenteLaBorra() = runTest {
        storage.session = validSession
        val repository = repository(HttpStatusCode.NotFound)

        assertFalse(repository.restoreSession())
        assertNull(storage.session)
    }

    /** Sin conexión no se cierra la sesión. */
    @Test
    fun restoreSinConexionConservaLaSesion() = runTest {
        storage.session = validSession
        val repository = repository { throw IOException("Connection refused") }

        assertTrue(repository.restoreSession())
        assertEquals(validSession, storage.session)
    }

    /** Un error del servidor tampoco cierra la sesión. */
    @Test
    fun restoreConErrorDelServidorConservaLaSesion() = runTest {
        storage.session = validSession
        val repository = repository(HttpStatusCode.InternalServerError)

        assertTrue(repository.restoreSession())
        assertEquals(validSession, storage.session)
    }
}
