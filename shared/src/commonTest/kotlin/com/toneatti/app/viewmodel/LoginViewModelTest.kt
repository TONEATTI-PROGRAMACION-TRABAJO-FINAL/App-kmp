package com.toneatti.app.viewmodel

import com.toneatti.app.data.auth.AuthError
import com.toneatti.app.data.auth.AuthResult
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de [LoginViewModel] con un repositorio falso.
 *
 * `viewModelScope` usa `Dispatchers.Main`, que no existe en la JVM de los tests: se
 * reemplaza por un despachador de prueba que solo avanza cuando el test lo pide,
 * para poder observar el estado intermedio (`isLoading`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(email: String, password: String) = LoginViewModel(repository).apply {
        onEmailChange(email)
        onPasswordChange(password)
    }

    /** Con datos inválidos se marcan los campos y no se llama al backend. */
    @Test
    fun formularioInvalidoMarcaLosCamposYNoEnvia() = runTest {
        val viewModel = viewModelWith(email = "ana", password = "")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertTrue(repository.loginCalls.isEmpty())
    }

    /** Editar un campo borra su error. */
    @Test
    fun editarUnCampoBorraSuError() = runTest {
        val viewModel = viewModelWith(email = "ana", password = "")
        viewModel.submit()

        viewModel.onEmailChange("ana@example.com")

        assertNull(viewModel.uiState.value.emailError)
    }

    /** Login exitoso: muestra carga mientras espera y termina con `loggedIn`. */
    @Test
    fun loginExitosoTerminaConLoggedIn() = runTest {
        val viewModel = viewModelWith(email = "  ana@example.com ", password = "secreta123")

        viewModel.submit()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.loggedIn)
        assertNull(state.errorMessage)
        // El email se envía recortado; la contraseña, tal cual.
        assertEquals(listOf("ana@example.com" to "secreta123"), repository.loginCalls)
    }

    /** Credenciales inválidas: mensaje de error y sigue en la pantalla. */
    @Test
    fun credencialesInvalidasMuestranUnMensaje() = runTest {
        repository.loginResult = AuthResult.Failure(AuthError.InvalidCredentials)
        val viewModel = viewModelWith(email = "ana@example.com", password = "mala")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Email o contraseña incorrectos.", state.errorMessage)
        assertFalse(state.loggedIn)
        assertFalse(state.isLoading)
    }

    /** Sin conexión: mensaje de error de red. */
    @Test
    fun sinConexionMuestraUnMensaje() = runTest {
        repository.loginResult = AuthResult.Failure(AuthError.Network)
        val viewModel = viewModelWith(email = "ana@example.com", password = "secreta123")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            "No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo.",
            viewModel.uiState.value.errorMessage,
        )
    }

    /** Un doble toque mientras carga no envía dos peticiones. */
    @Test
    fun dobleEnvioSeIgnoraMientrasCarga() = runTest {
        val viewModel = viewModelWith(email = "ana@example.com", password = "secreta123")

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.loginCalls.size)
    }

    /** La contraseña no aparece en el `toString` del estado. */
    @Test
    fun elEstadoNoExponeLaContrasena() {
        val viewModel = viewModelWith(email = "ana@example.com", password = "secreta123")

        assertFalse(viewModel.uiState.value.toString().contains("secreta123"))
    }
}
