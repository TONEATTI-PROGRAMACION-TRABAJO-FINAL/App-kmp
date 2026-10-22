package com.toneatti.app.viewmodel

import com.toneatti.app.data.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pruebas de [SessionViewModel]: arranca comprobando y termina en el estado que
 * indique el repositorio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

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
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = true))

        assertEquals(SessionState.Checking, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(SessionState.LoggedIn, viewModel.state.value)
    }

    /** Sin sesión válida termina sin sesión. */
    @Test
    fun sinSesionValidaTerminaLoggedOut() = runTest {
        val viewModel = SessionViewModel(FakeAuthRepository(restoreSessionResult = false))

        advanceUntilIdle()

        assertEquals(SessionState.LoggedOut, viewModel.state.value)
    }
}
