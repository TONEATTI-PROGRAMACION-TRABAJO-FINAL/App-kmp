package com.toneatti.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toneatti.app.AppContainer
import com.toneatti.app.ui.components.AppBottomBar
import com.toneatti.app.ui.screens.BookingsScreen
import com.toneatti.app.ui.screens.ProfessionalsScreen
import com.toneatti.app.ui.screens.SettingsScreen
import com.toneatti.app.ui.screens.auth.LoginScreen
import com.toneatti.app.viewmodel.LoginViewModel
import com.toneatti.app.viewmodel.SessionState
import com.toneatti.app.viewmodel.SessionViewModel

/**
 * Contenedor principal de la aplicación, modelado como una **clase** con un método
 * composable [Content]. Es el **único** punto donde se conoce el `NavController`;
 * aquí se unen la [AppBottomBar] y el `NavHost`.
 *
 * La composición es:
 *
 * 1. [SessionViewModel] decide si hay sesión; mientras lo comprueba se muestra un
 *    indicador de carga, porque el destino inicial del `NavHost` no puede cambiar después.
 * 2. `Scaffold` reserva el espacio de la barra inferior y aplica los insets.
 * 3. [AppBottomBar] recibe los destinos, la ruta activa y el callback de selección;
 *    solo se muestra en las pestañas, no en las pantallas de [AuthDestination].
 * 4. `NavHost` resuelve cada destino a su pantalla.
 *
 * Las pantallas son clases cuyas instancias se crean aquí una sola vez con `remember`.
 * Ninguna recibe el `NavController` ni su ViewModel: solo datos y callbacks. Los
 * ViewModels se crean aquí con `viewModel { }`: el de cada pantalla queda ligado a su
 * entrada del back stack (sobrevive a la rotación y se descarta al salir de ella);
 * el de la sesión, a la `Activity`.
 *
 * La instancia la crea [com.toneatti.app.App] con `remember`.
 */
class MainScreen {

    /**
     * Dibuja el contenedor principal (BottomBar + contenido navegable).
     *
     * @param darkTheme estado actual del modo oscuro; se reenvía a la pantalla de Ajustes.
     * @param onDarkThemeChange callback para cambiar el modo oscuro desde Ajustes.
     * @param modifier modificador opcional para el contenedor raíz.
     */
    @Composable
    fun Content(
        darkTheme: Boolean,
        onDarkThemeChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        // Controlador de navegación que gestiona el back stack entre pestañas.
        val navController = rememberNavController()

        // Observa el back stack para conocer la ruta activa y poder resaltar la pestaña.
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        // Comprobación de la sesión guardada; se hace una sola vez por Activity.
        val sessionViewModel = viewModel { SessionViewModel(AppContainer.authRepository) }
        val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()

        // Instancias creadas una sola vez por composición y reutilizadas en cada
        // recomposición (no se recrean los objetos en cada pasada).
        val bottomBar = remember { AppBottomBar() }
        val loginScreen = remember { LoginScreen() }
        val professionalsScreen = remember { ProfessionalsScreen() }
        val bookingsScreen = remember { BookingsScreen() }
        val settingsScreen = remember { SettingsScreen() }

        Scaffold(
            modifier = modifier,
            bottomBar = {
                // Solo las pestañas muestran la barra; el login no.
                if (AppDestination.fromRoute(currentRoute) != null) {
                    bottomBar.Content(
                        // `entries` devuelve todas las constantes del enum en orden de declaración.
                        destinations = AppDestination.entries,
                        currentRoute = currentRoute,
                        onSelect = { destination -> navigateToTab(navController, destination) },
                    )
                }
            },
        ) { innerPadding ->
            if (sessionState == SessionState.Checking) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            // El padding interno evita que el contenido quede debajo de la BottomBar
            // o de las barras del sistema (estado/navegación).
            NavHost(
                navController = navController,
                // Con sesión válida se entra directo a la primera pestaña; si no, al login.
                startDestination = if (sessionState == SessionState.LoggedIn) {
                    AppDestination.Professionals.route
                } else {
                    AuthDestination.Login.route
                },
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(AuthDestination.Login.route) {
                    val loginViewModel = viewModel { LoginViewModel(AppContainer.authRepository) }
                    val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()

                    // El ViewModel avisa el éxito en el estado; la navegación se decide aquí.
                    LaunchedEffect(loginState.loggedIn) {
                        if (loginState.loggedIn) navigateToMain(navController)
                    }

                    loginScreen.Content(
                        state = loginState,
                        onEmailChange = loginViewModel::onEmailChange,
                        onPasswordChange = loginViewModel::onPasswordChange,
                        onSubmit = loginViewModel::submit,
                    )
                }
                composable(AppDestination.Professionals.route) {
                    professionalsScreen.Content()
                }
                composable(AppDestination.Bookings.route) {
                    bookingsScreen.Content()
                }
                composable(AppDestination.Settings.route) {
                    // Ajustes necesita conocer y poder modificar el tema global.
                    settingsScreen.Content(
                        darkTheme = darkTheme,
                        onDarkThemeChange = onDarkThemeChange,
                    )
                }
            }
        }
    }

    /**
     * Navega a una pestaña aplicando el comportamiento **estándar de una BottomBar**.
     *
     * Opciones usadas:
     *
     * - `popUpTo(Professionals)`: deja la pila en un solo nivel sobre la primera pestaña,
     *   de modo que pulsar atrás sale de la app en lugar de recorrer las pestañas
     *   visitadas. Se usa la primera pestaña y no `graph.startDestinationId` porque,
     *   si la app arrancó en el login, ese destino ya no está en la pila.
     * - `launchSingleTop = true`: evita crear copias del mismo destino si el usuario
     *   pulsa dos veces la misma pestaña.
     * - `saveState` / `restoreState`: conservan el estado de cada pestaña (scroll,
     *   texto escrito, etc.) al alternar entre ellas.
     *
     * @param navController controlador de navegación sobre el que se actúa.
     * @param destination pestaña a la que se desea navegar.
     */
    private fun navigateToTab(
        navController: NavHostController,
        destination: AppDestination,
    ) {
        navController.navigate(destination.route) {
            popUpTo(AppDestination.Professionals.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * Tras un login exitoso, va a la primera pestaña y quita el login de la pila,
     * para que pulsar atrás salga de la app en lugar de volver al formulario.
     *
     * @param navController controlador de navegación sobre el que se actúa.
     */
    private fun navigateToMain(navController: NavHostController) {
        navController.navigate(AppDestination.Professionals.route) {
            popUpTo(AuthDestination.Login.route) { inclusive = true }
        }
    }
}
