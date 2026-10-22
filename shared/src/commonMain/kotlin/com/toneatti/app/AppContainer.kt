package com.toneatti.app

import com.toneatti.app.data.auth.AuthApi
import com.toneatti.app.data.auth.AuthRepository
import com.toneatti.app.data.auth.DefaultAuthRepository
import com.toneatti.app.data.remote.TurnosHttpClient
import com.toneatti.app.data.session.TokenStorage
import com.toneatti.app.data.session.getTokenStorage
import io.ktor.client.HttpClient
import kotlin.time.Clock

/**
 * Contenedor de dependencias de la app (inyección manual, sin librerías).
 *
 * Es un `object` y no una clase recordada con `remember` porque estas dependencias
 * deben vivir lo mismo que el proceso, igual que los ViewModels que las usan: si
 * se recreara al rotar la pantalla, los ViewModels retenidos seguirían usando el
 * `HttpClient` anterior y quedarían dos clientes abiertos.
 *
 * Todo se crea de forma perezosa (`by lazy`), la primera vez que se usa: en Android,
 * después de que `MainActivity` registró el `Context`.
 */
object AppContainer {

    private val httpClient: HttpClient by lazy { TurnosHttpClient.create() }

    private val authApi: AuthApi by lazy { AuthApi(httpClient) }

    private val tokenStorage: TokenStorage by lazy { getTokenStorage() }

    /** Repositorio de autenticación que reciben los ViewModels. */
    val authRepository: AuthRepository by lazy {
        DefaultAuthRepository(
            authApi = authApi,
            tokenStorage = tokenStorage,
            nowMillis = { Clock.System.now().toEpochMilliseconds() },
        )
    }
}
