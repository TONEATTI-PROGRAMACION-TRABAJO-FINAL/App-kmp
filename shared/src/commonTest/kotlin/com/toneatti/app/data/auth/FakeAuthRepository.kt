package com.toneatti.app.data.auth

/**
 * [AuthRepository] programable para los tests de los ViewModels.
 *
 * @property loginResult resultado que devolverá [login].
 * @property restoreSessionResult resultado que devolverá [restoreSession].
 */
class FakeAuthRepository(
    var loginResult: AuthResult<Unit> = AuthResult.Success(Unit),
    var restoreSessionResult: Boolean = false,
) : AuthRepository {

    /** Credenciales recibidas en cada llamada a [login], en orden. */
    val loginCalls = mutableListOf<Pair<String, String>>()

    override suspend fun login(email: String, password: String): AuthResult<Unit> {
        loginCalls += email to password
        return loginResult
    }

    override suspend fun restoreSession(): Boolean = restoreSessionResult
}
