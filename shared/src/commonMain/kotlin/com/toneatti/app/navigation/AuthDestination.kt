package com.toneatti.app.navigation

/**
 * Destinos de autenticación: pantallas a las que se llega **sin** sesión y que se
 * muestran sin la BottomBar.
 *
 * Van separados de [AppDestination] porque aquel enum es el único punto de verdad de
 * las pestañas: si estuvieran ahí, la BottomBar los mostraría.
 *
 * @property route identificador de navegación. Debe ser único, también respecto de
 *   las rutas de [AppDestination].
 */
enum class AuthDestination(
    val route: String,
) {
    /** Inicio de sesión. */
    Login(route = "login"),

    /** Registro de usuario; se llega desde el login. */
    Register(route = "register"),
}
