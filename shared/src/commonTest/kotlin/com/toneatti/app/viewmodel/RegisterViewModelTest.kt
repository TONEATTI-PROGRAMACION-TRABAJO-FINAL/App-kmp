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
 * Pruebas de [RegisterViewModel] con un repositorio falso: validación, datos enviados,
 * login automático y manejo de errores.
 *
 * Como en [LoginViewModelTest], `Dispatchers.Main` se reemplaza por un despachador de
 * prueba que solo avanza cuando el test lo pide.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(
        firstName: String = " Ana ",
        lastName: String = " Pérez ",
        email: String = " ana@example.com ",
        password: String = " secreta123 ",
    ) = RegisterViewModel(repository).apply {
        onFirstNameChange(firstName)
        onLastNameChange(lastName)
        onEmailChange(email)
        onPasswordChange(password)
    }

    /** Con datos inválidos se marcan los cuatro campos y no se llama al backend. */
    @Test
    fun formularioInvalidoMarcaLosCamposYNoEnvia() = runTest {
        val viewModel = viewModelWith(firstName = "A", lastName = "", email = "ana", password = "corta")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.firstNameError)
        assertNotNull(state.lastNameError)
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertTrue(repository.registerCalls.isEmpty())
        assertTrue(repository.loginCalls.isEmpty())
    }

    /** Un nombre de un carácter rodeado de espacios no pasa: se valida ya recortado. */
    @Test
    fun seValidaElNombreRecortado() = runTest {
        val viewModel = viewModelWith(firstName = "  A  ")

        viewModel.submit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.firstNameError)
        assertTrue(repository.registerCalls.isEmpty())
    }

    /** Registro exitoso: envía los datos recortados (salvo la contraseña) e inicia sesión. */
    @Test
    fun registroExitosoIniciaSesionAutomaticamente() = runTest {
        val viewModel = viewModelWith()

        viewModel.submit()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(
            listOf(FakeAuthRepository.RegisterCall("ana@example.com", " secreta123 ", "Ana", "Pérez")),
            repository.registerCalls,
        )
        assertEquals(listOf("ana@example.com" to " secreta123 "), repository.loginCalls)
        val state = viewModel.uiState.value
        assertTrue(state.loggedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    /** 409: el aviso va debajo del email, no se intenta el login y se puede corregir. */
    @Test
    fun emailYaRegistradoSeMuestraEnElCampoEmail() = runTest {
        repository.registerResult = AuthResult.Failure(AuthError.EmailAlreadyRegistered)
        val viewModel = viewModelWith()

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Ese email ya está registrado.", state.emailError)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertTrue(repository.loginCalls.isEmpty())

        viewModel.onEmailChange("otra@example.com")
        assertNull(viewModel.uiState.value.emailError)
    }

    /** Sin conexión al registrar: mensaje debajo del formulario. */
    @Test
    fun sinConexionMuestraUnMensaje() = runTest {
        repository.registerResult = AuthResult.Failure(AuthError.Network)
        val viewModel = viewModelWith()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            "No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo.",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.accountCreated)
    }

    /** Cuenta creada pero login automático fallido: aviso y formulario bloqueado. */
    @Test
    fun siFallaElLoginAutomaticoSeBloqueaElFormulario() = runTest {
        repository.loginResult = AuthResult.Failure(AuthError.Network)
        val viewModel = viewModelWith()

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.accountCreated)
        assertFalse(state.loggedIn)
        assertFalse(state.isLoading)
        assertEquals(RegisterViewModel.ACCOUNT_CREATED_LOGIN_FAILED, state.errorMessage)

        // Reenviar daría 409: se ignora, y las ediciones tampoco borran el aviso.
        viewModel.onEmailChange("otra@example.com")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(1, repository.registerCalls.size)
        assertEquals(" ana@example.com ", viewModel.uiState.value.email)
        assertEquals(RegisterViewModel.ACCOUNT_CREATED_LOGIN_FAILED, viewModel.uiState.value.errorMessage)
    }

    /** Un doble toque mientras carga no crea dos cuentas. */
    @Test
    fun dobleEnvioSeIgnoraMientrasCarga() = runTest {
        val viewModel = viewModelWith()

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.registerCalls.size)
        assertEquals(1, repository.loginCalls.size)
    }

    /** La contraseña no aparece en el `toString` del estado. */
    @Test
    fun elEstadoNoExponeLaContrasena() {
        val viewModel = viewModelWith(password = "secreta123")

        assertFalse(viewModel.uiState.value.toString().contains("secreta123"))
    }
}
