package com.toneatti.app.data.auth

import com.toneatti.app.data.session.TokenStorage
import com.toneatti.app.data.session.UserSession

/**
 * Operaciones de autenticación que usan los ViewModels.
 *
 * Es una interfaz para que los tests de los ViewModels usen un fake, sin red ni
 * almacenamiento real.
 */
interface AuthRepository {

    /**
     * Inicia sesión y, si las credenciales son válidas, guarda el JWT del usuario.
     *
     * @param email email del usuario, ya sin espacios alrededor.
     * @param password contraseña tal como la escribió el usuario.
     * @return éxito si el token quedó guardado; el [AuthError] en caso contrario.
     */
    suspend fun login(email: String, password: String): AuthResult<Unit>

    /**
     * Decide, al abrir la app, si la sesión guardada sigue sirviendo.
     *
     * - Sin sesión, o vencida según el reloj del dispositivo: `false` (y se borra).
     * - Vigente: se confirma con `GET /users/me`. Si el backend la rechaza (401) o el
     *   usuario ya no existe (404, por ejemplo al reiniciar la base H2 en memoria),
     *   `false` y se borra.
     * - Si el backend no responde o falla, `true`: estar sin conexión no es motivo para
     *   cerrar la sesión.
     *
     * @return `true` si se puede entrar directo a la pantalla principal.
     */
    suspend fun restoreSession(): Boolean
}

/**
 * Implementación de [AuthRepository] sobre Servicio-turnos y el almacenamiento seguro.
 *
 * @param authApi cliente de los endpoints de usuario.
 * @param tokenStorage dónde se guarda la sesión.
 * @param nowMillis reloj; se inyecta para poder fijar el tiempo en los tests.
 */
class DefaultAuthRepository(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val nowMillis: () -> Long,
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult<Unit> =
        when (val result = authApi.login(email, password)) {
            is AuthResult.Success -> {
                val token = result.value
                // El vencimiento se fija con el reloj del dispositivo al recibir la respuesta.
                tokenStorage.save(UserSession.fromExpiresIn(token.accessToken, token.expiresIn, nowMillis()))
                AuthResult.Success(Unit)
            }
            is AuthResult.Failure -> result
        }

    override suspend fun restoreSession(): Boolean {
        val session = tokenStorage.load() ?: return false
        if (session.isExpired(nowMillis())) {
            tokenStorage.clear()
            return false
        }
        return when (val result = authApi.me(session.accessToken)) {
            is AuthResult.Success -> true
            is AuthResult.Failure -> when (result.error) {
                AuthError.Unauthorized, AuthError.UserNotFound -> {
                    tokenStorage.clear()
                    false
                }
                // Sin conexión o error del servidor: se conserva la sesión.
                else -> true
            }
        }
    }
}
