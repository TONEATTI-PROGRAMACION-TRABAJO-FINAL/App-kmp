package com.toneatti.app.data.session

/**
 * Almacenamiento seguro y persistente de la [UserSession] en el dispositivo.
 *
 * Es una interfaz para que el código común (repositorios, ViewModels y sus tests)
 * no dependa de la implementación de cada plataforma: en Android cifra con el
 * Android Keystore; en los tests se reemplaza por un fake en memoria.
 *
 * Las operaciones son `suspend` porque tocan disco y criptografía: la implementación
 * las saca del hilo principal.
 */
interface TokenStorage {

    /** Guarda [session], reemplazando la anterior si la había. */
    suspend fun save(session: UserSession)

    /**
     * Devuelve la sesión guardada, o `null` si no hay ninguna o no se pudo leer
     * (por ejemplo, si la clave de cifrado ya no existe). En ese último caso la
     * implementación borra lo guardado.
     */
    suspend fun load(): UserSession?

    /** Borra la sesión guardada. */
    suspend fun clear()
}

/**
 * Devuelve la implementación de [TokenStorage] de la plataforma actual.
 *
 * Sigue el mismo patrón `expect`/`actual` que `getPlatform()`. Cada llamada crea una
 * instancia nueva: quien la use debe conservarla (lo hará el contenedor de dependencias).
 */
expect fun getTokenStorage(): TokenStorage
