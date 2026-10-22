package com.toneatti.app.data.auth

import com.toneatti.app.data.auth.dto.LoginRequest
import com.toneatti.app.data.auth.dto.ProblemDetail
import com.toneatti.app.data.auth.dto.RegisterRequest
import com.toneatti.app.data.auth.dto.TokenResponse
import com.toneatti.app.data.auth.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * Cliente de los endpoints de usuario de Servicio-turnos.
 *
 * Cada método devuelve un [AuthResult] y nunca lanza excepciones por respuestas de
 * error o falta de conexión. El significado de cada código de estado depende del
 * endpoint (un 401 en login son credenciales inválidas; en `/users/me`, un token rechazado).
 *
 * @param client cliente creado con `TurnosHttpClient`.
 */
class AuthApi(private val client: HttpClient) {

    /**
     * `POST /users`: 201 con el usuario creado; 409 si el email ya está registrado.
     *
     * No devuelve token: para usar la cuenta hay que iniciar sesión después.
     *
     * @param email email del usuario.
     * @param password contraseña.
     * @param firstName nombre.
     * @param lastName apellido.
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): AuthResult<UserResponse> =
        request(
            send = {
                client.post("users") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RegisterRequest(email = email, password = password, firstName = firstName, lastName = lastName),
                    )
                }
            },
            successStatus = HttpStatusCode.Created,
            errorFor = { status ->
                if (status == HttpStatusCode.Conflict) AuthError.EmailAlreadyRegistered else null
            },
        )

    /**
     * `POST /users/login`: 200 con el token; 401 si las credenciales no son válidas.
     *
     * @param email email del usuario.
     * @param password contraseña.
     */
    suspend fun login(email: String, password: String): AuthResult<TokenResponse> =
        request(
            send = {
                client.post("users/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email = email, password = password))
                }
            },
            successStatus = HttpStatusCode.OK,
            errorFor = { status ->
                if (status == HttpStatusCode.Unauthorized) AuthError.InvalidCredentials else null
            },
        )

    /**
     * `GET /users/me`: datos del usuario dueño del token. 401 si el token no es válido
     * (lo responde Spring Security, sin cuerpo); 404 si el usuario ya no existe.
     *
     * @param accessToken JWT del usuario.
     */
    suspend fun me(accessToken: String): AuthResult<UserResponse> =
        request(
            send = { client.get("users/me") { bearerAuth(accessToken) } },
            successStatus = HttpStatusCode.OK,
            errorFor = { status ->
                when (status) {
                    HttpStatusCode.Unauthorized -> AuthError.Unauthorized
                    HttpStatusCode.NotFound -> AuthError.UserNotFound
                    else -> null
                }
            },
        )

    /**
     * Ejecuta una petición y traduce la respuesta a [AuthResult].
     *
     * @param send realiza la petición.
     * @param successStatus código que indica éxito; su cuerpo se lee como [T].
     * @param errorFor errores propios del endpoint; `null` deja el manejo general
     *   (400 → [AuthError.InvalidData], resto → [AuthError.Unexpected]).
     */
    private suspend inline fun <reified T> request(
        send: () -> HttpResponse,
        successStatus: HttpStatusCode,
        errorFor: (HttpStatusCode) -> AuthError?,
    ): AuthResult<T> {
        val response = try {
            send()
        } catch (e: IOException) {
            // Sin conexión, conexión rechazada o tiempo agotado (los timeouts de Ktor son IOException).
            return AuthResult.Failure(AuthError.Network)
        }

        val status = response.status
        if (status == successStatus) {
            return try {
                AuthResult.Success(response.body<T>())
            } catch (e: CancellationException) {
                // La cancelación de la corrutina (p. ej. al cerrar la pantalla) debe propagarse.
                throw e
            } catch (e: IOException) {
                // La conexión se cortó mientras se leía el cuerpo.
                AuthResult.Failure(AuthError.Network)
            } catch (e: Exception) {
                // Cuerpo que no coincide con el DTO o con un tipo de contenido inesperado.
                AuthResult.Failure(AuthError.Unexpected(status.value, detail = null))
            }
        }

        errorFor(status)?.let { return AuthResult.Failure(it) }
        val detail = problemDetailOf(response)
        return AuthResult.Failure(
            if (status == HttpStatusCode.BadRequest) {
                AuthError.InvalidData(detail)
            } else {
                AuthError.Unexpected(status.value, detail)
            },
        )
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
