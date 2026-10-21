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
}
