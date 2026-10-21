package com.toneatti.app.data.auth

import com.toneatti.app.data.remote.TurnosHttpClient
import com.toneatti.app.data.session.FakeTokenStorage
import com.toneatti.app.data.session.UserSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pruebas de [DefaultAuthRepository]: qué se guarda (y qué no) después de cada login.
 */
class DefaultAuthRepositoryTest {

    private val now = 1_000_000_000L
    private val storage = FakeTokenStorage()

    private fun repository(status: HttpStatusCode, body: String): DefaultAuthRepository {
        val engine = MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        return DefaultAuthRepository(AuthApi(TurnosHttpClient.create(engine)), storage, nowMillis = { now })
    }

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
        val repository = repository(HttpStatusCode.Unauthorized, "")

        val result = repository.login("ana@example.com", "mala")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
        assertNull(storage.session)
    }
}
