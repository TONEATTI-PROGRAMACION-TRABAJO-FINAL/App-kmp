package com.toneatti.app.viewmodel

import com.toneatti.app.data.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas de [SessionViewModel]: arranca comprobando y termina en el estado que
 * indique el repositorio, y marca la sesión descartada cuando llega el aviso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    /** Avisos de sesión descartada, como los de `SessionManager.expirations`. */
    private val expirations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Con sesión válida pasa de comprobando a con sesión. */
    @Test
    fun conSesionValidaTerminaLoggedIn() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = true), expirations)

        assertEquals(SessionState.Checking, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(SessionState.LoggedIn, viewModel.state.value)
    }

    /** Sin sesión válida termina sin sesión. */
    @Test
    fun sinSesionValidaTerminaLoggedOut() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = false), expirations)

        advanceUntilIdle()

        assertEquals(SessionState.LoggedOut, viewModel.state.value)
    }

    /** Sin avisos, la sesión no figura como descartada. */
    @Test
    fun sinAvisosLaSesionNoEstaDescartada() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = true), expirations)

        advanceUntilIdle()

        assertFalse(viewModel.sessionExpired.value)
    }

    /** Un aviso marca la sesión como descartada hasta que la UI confirma que volvió al login. */
    @Test
    fun unAvisoMarcaLaSesionDescartadaHastaQueSeAtiende() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = true), expirations)
        advanceUntilIdle()

        expirations.tryEmit(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.sessionExpired.value)

        viewModel.onSessionExpiredHandled()
        assertFalse(viewModel.sessionExpired.value)
    }

    /** Después de atendido, un nuevo aviso vuelve a marcarla. */
    @Test
    fun unSegundoAvisoVuelveAMarcarla() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = true), expirations)
        advanceUntilIdle()
        expirations.tryEmit(Unit)
        advanceUntilIdle()
        viewModel.onSessionExpiredHandled()

        expirations.tryEmit(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.sessionExpired.value)
    }
}
