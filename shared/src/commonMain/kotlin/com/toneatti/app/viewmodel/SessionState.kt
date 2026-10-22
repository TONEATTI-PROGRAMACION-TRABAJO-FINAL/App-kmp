package com.toneatti.app.viewmodel

/**
 * Estado de la sesión al abrir la app, publicado por [SessionViewModel].
 */
enum class SessionState {
    /** Todavía se está comprobando la sesión guardada. */
    Checking,

    /** Hay una sesión válida: se entra a la pantalla principal. */
    LoggedIn,

    /** No hay sesión válida: se muestra el inicio de sesión. */
    LoggedOut,
}
