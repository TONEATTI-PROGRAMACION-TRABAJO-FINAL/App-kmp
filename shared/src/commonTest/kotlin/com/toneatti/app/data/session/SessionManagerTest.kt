package com.toneatti.app.data.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pruebas de [SessionManager]: cuándo entrega el token y cuándo descarta la sesión y avisa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val now = 1_000_000_000L
    private val storage = FakeTokenStorage()
    private val manager = SessionManager(storage, nowMillis = { now })

    /** Cantidad de avisos recibidos por [SessionManager.expirations]. */
    private var expirations = 0

    /** Escucha los avisos durante todo el test (se cancela solo al terminar). */
    private fun TestScope.listenExpirations() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.expirations.collect { expirations++ }
        }
    }

    /** Con una sesión vigente entrega su token y no la toca. */
    @Test
    fun conSesionVigenteDevuelveElToken() = runTest {
        listenExpirations()
        val session = UserSession("jwt", expiresAtMillis = now + 3_600_000)
        storage.session = session

        assertEquals("jwt", manager.accessToken())
        assertEquals(session, storage.session)
        assertEquals(0, expirations)
    }

    /** Sin sesión guardada no hay token y se avisa para volver al login. */
    @Test
    fun sinSesionDevuelveNullYAvisa() = runTest {
        listenExpirations()

        assertNull(manager.accessToken())
        assertEquals(1, expirations)
    }

    /** Una sesión vencida según el reloj del dispositivo se borra y se avisa. */
    @Test
    fun conSesionVencidaLaBorraYAvisa() = runTest {
        listenExpirations()
        storage.session = UserSession("jwt", expiresAtMillis = now)

        assertNull(manager.accessToken())
        assertNull(storage.session)
        assertEquals(1, expirations)
    }

    /** [SessionManager.expire] borra la sesión y avisa. */
    @Test
    fun expireBorraLaSesionYAvisa() = runTest {
        listenExpirations()
        storage.session = UserSession("jwt", expiresAtMillis = now + 3_600_000)

        manager.expire()

        assertNull(storage.session)
        assertEquals(1, expirations)
    }

    /** Cada descarte produce su aviso. */
    @Test
    fun cadaExpireAvisaDeNuevo() = runTest {
        listenExpirations()

        manager.expire()
        manager.expire()

        assertEquals(2, expirations)
    }
}
