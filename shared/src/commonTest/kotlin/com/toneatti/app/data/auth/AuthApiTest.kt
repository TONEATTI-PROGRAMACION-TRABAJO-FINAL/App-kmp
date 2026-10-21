package com.toneatti.app.data.auth

import com.toneatti.app.data.auth.dto.TokenResponse
import com.toneatti.app.data.auth.dto.UserResponse
import com.toneatti.app.data.remote.TurnosHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pruebas de [AuthApi] contra un servidor simulado (`MockEngine`): forma de las
 * peticiones y traducción de cada respuesta de Servicio-turnos a [AuthResult].
 */
class AuthApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val problemHeaders = headersOf(HttpHeaders.ContentType, "application/problem+json")

    /** Peticiones recibidas por el servidor simulado, para inspeccionarlas. */
    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): AuthApi {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return AuthApi(TurnosHttpClient.create(engine))
    }

    /** Login envía POST /users/login con email y contraseña en JSON. */
    @Test
    fun loginEnviaElCuerpoEsperado() = runTest {
        val api = api { respond(TOKEN_JSON, HttpStatusCode.OK, jsonHeaders) }

        api.login("ana@example.com", "secreta123")

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("http://10.0.2.2:8080/users/login", request.url.toString())
        val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
        assertEquals("ana@example.com", body.getValue("email").jsonPrimitive.content)
        assertEquals("secreta123", body.getValue("password").jsonPrimitive.content)
    }

    /** 200 de login se lee como [TokenResponse], ignorando campos desconocidos. */
    @Test
    fun loginExitosoDevuelveElToken() = runTest {
        val api = api {
            respond(
                """{"accessToken":"jwt","tokenType":"Bearer","expiresIn":86400,"extra":true}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = api.login("ana@example.com", "secreta123")

        assertEquals(AuthResult.Success(TokenResponse("jwt", "Bearer", 86_400)), result)
    }

    /** 401 de login son credenciales inválidas. */
    @Test
    fun login401SonCredencialesInvalidas() = runTest {
        val api = api { respond(problem(401, "Invalid email or password"), HttpStatusCode.Unauthorized, problemHeaders) }

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), api.login("ana@example.com", "mala"))
    }

    /** 400 conserva el `detail` del problem+json. */
    @Test
    fun login400SonDatosInvalidosConDetalle() = runTest {
        val api = api { respond(problem(400, "Invalid request content."), HttpStatusCode.BadRequest, problemHeaders) }

        assertEquals(
            AuthResult.Failure(AuthError.InvalidData("Invalid request content.")),
            api.login("ana", "x"),
        )
    }

    /** Un 5xx es un error inesperado con su código. */
    @Test
    fun login500EsUnErrorInesperado() = runTest {
        val api = api { respond(problem(500, "Boom"), HttpStatusCode.InternalServerError, problemHeaders) }

        assertEquals(
            AuthResult.Failure(AuthError.Unexpected(500, "Boom")),
            api.login("ana@example.com", "secreta123"),
        )
    }

    /** Un 200 con un cuerpo que no es un token no rompe la app. */
    @Test
    fun loginConCuerpoIlegibleEsUnErrorInesperado() = runTest {
        val api = api { respond("""{"foo":1}""", HttpStatusCode.OK, jsonHeaders) }

        assertEquals(
            AuthResult.Failure(AuthError.Unexpected(200, null)),
            api.login("ana@example.com", "secreta123"),
        )
    }

    /** Sin conexión con el servidor el resultado es un error de red, no una excepción. */
    @Test
    fun sinConexionEsErrorDeRed() = runTest {
        val api = api { throw IOException("Connection refused") }

        assertEquals(AuthResult.Failure(AuthError.Network), api.login("ana@example.com", "secreta123"))
    }

    /** `/users/me` envía el token como Bearer y lee el usuario. */
    @Test
    fun meEnviaElTokenYDevuelveElUsuario() = runTest {
        val api = api {
            respond(
                """{"externalId":"id-1","email":"ana@example.com","firstName":"Ana","lastName":"Pérez"}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = api.me("jwt")

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://10.0.2.2:8080/users/me", request.url.toString())
        assertEquals("Bearer jwt", request.headers[HttpHeaders.Authorization])
        assertEquals(AuthResult.Success(UserResponse("id-1", "ana@example.com", "Ana", "Pérez")), result)
    }

    /** 401 de `/users/me` (sin cuerpo, como lo responde Spring Security) es token rechazado. */
    @Test
    fun me401EsNoAutorizado() = runTest {
        val api = api { respond("", HttpStatusCode.Unauthorized) }

        assertEquals(AuthResult.Failure(AuthError.Unauthorized), api.me("vencido"))
    }

    /** 404 de `/users/me`: el usuario ya no existe (por ejemplo, tras reiniciar H2). */
    @Test
    fun me404EsUsuarioInexistente() = runTest {
        val api = api { respond(problem(404, "User not found"), HttpStatusCode.NotFound, problemHeaders) }

        assertEquals(AuthResult.Failure(AuthError.UserNotFound), api.me("jwt"))
    }

    private fun problem(status: Int, detail: String): String =
        """{"type":"about:blank","title":"Error","status":$status,"detail":"$detail","instance":"/users"}"""

    private companion object {
        const val TOKEN_JSON = """{"accessToken":"jwt","tokenType":"Bearer","expiresIn":86400}"""
    }
}
