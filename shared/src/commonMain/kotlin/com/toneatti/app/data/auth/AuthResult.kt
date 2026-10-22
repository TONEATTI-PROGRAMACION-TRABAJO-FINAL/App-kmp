package com.toneatti.app.data.auth

/**
 * Resultado de una operación de autenticación: éxito con un valor o un [AuthError].
 *
 * Se usa en lugar de excepciones para que quien llama esté obligado (por el `when`
 * exhaustivo) a contemplar los errores esperables, como credenciales inválidas o
 * falta de conexión.
 *
 * @param T tipo del valor en caso de éxito.
 */
sealed interface AuthResult<out T> {

    /** Operación exitosa. @property value valor obtenido. */
    data class Success<out T>(val value: T) : AuthResult<T>

    /** Operación fallida. @property error causa del fallo. */
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}
