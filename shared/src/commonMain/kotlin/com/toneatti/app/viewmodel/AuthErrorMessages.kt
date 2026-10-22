package com.toneatti.app.viewmodel

import com.toneatti.app.data.auth.AuthError

/**
 * Traduce un [AuthError] al mensaje en español que ve el usuario.
 *
 * Los textos del backend (`detail` del problem+json) están en inglés y pensados para
 * diagnóstico, por eso no se muestran.
 */
object AuthErrorMessages {

    /**
     * @param error error devuelto por el repositorio.
     * @return mensaje para mostrar en la pantalla.
     */
    fun messageFor(error: AuthError): String = when (error) {
        AuthError.InvalidCredentials -> "Email o contraseña incorrectos."
        AuthError.EmailAlreadyRegistered -> "Ese email ya está registrado."
        is AuthError.InvalidData -> "Revisá los datos ingresados."
        AuthError.Network -> "No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo."
        AuthError.Unauthorized, AuthError.UserNotFound -> "Tu sesión no es válida. Iniciá sesión de nuevo."
        is AuthError.Unexpected -> "Ocurrió un error en el servidor. Intentá más tarde."
    }
}
