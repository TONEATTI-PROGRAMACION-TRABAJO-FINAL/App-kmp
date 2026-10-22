package com.toneatti.app.data.session

/**
 * [TokenStorage] en memoria para los tests: sin cifrado ni disco.
 *
 * @property session sesión guardada; los tests pueden fijarla o leerla directamente.
 */
class FakeTokenStorage(var session: UserSession? = null) : TokenStorage {

    override suspend fun save(session: UserSession) {
        this.session = session
    }

    override suspend fun load(): UserSession? = session

    override suspend fun clear() {
        session = null
    }
}
