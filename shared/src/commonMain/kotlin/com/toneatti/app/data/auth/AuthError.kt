package com.toneatti.app.data.auth

/**
 * Errores posibles de los endpoints de usuario de Servicio-turnos, ya interpretados
 * según el endpoint y el código de estado.
 *
 * La capa de datos no arma textos para el usuario: cada ViewModel traduce estos
 * casos a mensajes en español.
 */
sealed interface AuthError {

    /** 401 de `POST /users/login`: email o contraseña incorrectos (el backend no distingue cuál). */
    data object InvalidCredentials : AuthError

    /**
     * 400: el backend rechazó los datos. No debería ocurrir si la validación de la app
     * coincide con la del backend.
     *
     * @property detail `detail` del problem+json, para diagnóstico.
     */
    data class InvalidData(val detail: String?) : AuthError

    /** 401 de un endpoint autenticado: token ausente, vencido o con firma inválida. */
    data object Unauthorized : AuthError

    /** 404 de `GET /users/me`: el token es válido pero el usuario ya no existe. */
    data object UserNotFound : AuthError

    /** No hubo respuesta: sin conexión, servidor caído o tiempo agotado. */
    data object Network : AuthError

    /**
     * Cualquier otra respuesta: errores 5xx, códigos no previstos o un cuerpo ilegible.
     *
     * @property status código HTTP recibido.
     * @property detail `detail` del problem+json, si lo había.
     */
    data class Unexpected(val status: Int, val detail: String?) : AuthError
}
