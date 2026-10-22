package com.toneatti.app.data.auth

/**
 * [AuthRepository] programable para los tests de los ViewModels.
 *
 * @property registerResult resultado que devolverá [register].
 * @property loginResult resultado que devolverá [login].
 * @property restoreSessionResult resultado que devolverá [restoreSession].
 */
class FakeAuthRepository(
    var registerResult: AuthResult<Unit> = AuthResult.Success(Unit),
    var loginResult: AuthResult<Unit> = AuthResult.Success(Unit),
    var restoreSessionResult: Boolean = false,
) : AuthRepository {

    /** Datos recibidos en cada llamada a [register], en orden. */
    val registerCalls = mutableListOf<RegisterCall>()

    /** Argumentos de una llamada a [register]. */
    data class RegisterCall(val email: String, val password: String, val firstName: String, val lastName: String)

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): AuthResult<Unit> {
        registerCalls += RegisterCall(email, password, firstName, lastName)
        return registerResult
    }

    /** Credenciales recibidas en cada llamada a [login], en orden. */
    val loginCalls = mutableListOf<Pair<String, String>>()

    override suspend fun login(email: String, password: String): AuthResult<Unit> {
        loginCalls += email to password
        return loginResult
    }

    override suspend fun restoreSession(): Boolean = restoreSessionResult
}
