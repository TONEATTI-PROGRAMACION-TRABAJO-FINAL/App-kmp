package com.toneatti.app.data.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Punto único de la sesión para los endpoints que requieren el JWT del usuario.
 *
 * - Entrega el token a los repositorios con [accessToken].
 * - Cuando la sesión deja de servir (vencida en el dispositivo o rechazada por el
 *   backend con un 401), la borra y lo avisa por [expirations]. Quien conoce la
 *   navegación escucha ese aviso y vuelve al login, así ninguna pantalla repite esa lógica.
 *
 * Debe haber una sola instancia por proceso (la crea el contenedor de dependencias),
 * porque el aviso solo llega a quien escucha esa misma instancia.
 *
 * @param tokenStorage dónde está guardada la sesión.
 * @param nowMillis reloj; se inyecta para poder fijar el tiempo en los tests.
 */
class SessionManager(
    private val tokenStorage: TokenStorage,
    private val nowMillis: () -> Long,
) {

    // Sin repetición para los que se suscriben después, y con lugar para un solo aviso:
    // varios 401 casi simultáneos (dos requests en paralelo) se reducen a uno, y emitir
    // nunca suspende a quien llama.
    private val _expirations = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emite cada vez que la sesión se descarta por vencida o rechazada. */
    val expirations: SharedFlow<Unit> = _expirations.asSharedFlow()

    /**
     * Devuelve el JWT del usuario para enviarlo como `Authorization: Bearer`.
     *
     * Si no hay sesión, o ya venció según el reloj del dispositivo, la descarta con
     * [expire] y devuelve `null`: no tiene sentido enviar un request que el backend
     * va a rechazar.
     *
     * @return el token, o `null` si hay que volver a iniciar sesión.
     */
    suspend fun accessToken(): String? {
        val session = tokenStorage.load()
        if (session == null || session.isExpired(nowMillis())) {
            expire()
            return null
        }
        return session.accessToken
    }

    /**
     * Borra la sesión guardada y avisa por [expirations].
     *
     * La llaman los repositorios cuando el backend responde 401 (token vencido, con
     * firma inválida o de un usuario que ya no existe).
     */
    suspend fun expire() {
        tokenStorage.clear()
        _expirations.tryEmit(Unit)
    }
}
