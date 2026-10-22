package com.toneatti.app.viewmodel

/**
 * Validación de los formularios de autenticación, con las mismas reglas que
 * Servicio-turnos (`LoginRequest`, `PasswordWithinBcryptLimit`).
 *
 * Validar antes de enviar es necesario, no solo cómodo: el 400 del backend trae un
 * mensaje genérico, sin indicar qué campo falló.
 *
 * Cada función devuelve el mensaje de error para mostrar debajo del campo, o `null`
 * si el valor es válido. Los textos están en español porque son para el usuario.
 */
object AuthFormValidator {

    /** Límite de BCrypt: las contraseñas más largas se rechazan en el backend. */
    const val PASSWORD_MAX_BYTES: Int = 72

    /**
     * Formato de email razonable: algo@dominio.tld. Es un poco más estricto que el
     * `@Email` del backend (exige un punto en el dominio), así que todo lo que pasa
     * aquí también pasa allá.
     */
    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Email del login: obligatorio y con formato válido.
     *
     * @param email valor ya sin espacios alrededor.
     */
    fun loginEmailError(email: String): String? = when {
        email.isBlank() -> "Ingresá tu email"
        !emailPattern.matches(email) -> "Ingresá un email válido"
        else -> null
    }

    /**
     * Contraseña del login: obligatoria y de como máximo [PASSWORD_MAX_BYTES] bytes
     * en UTF-8. A diferencia del registro, no hay largo mínimo: esa política aplica
     * al crear la cuenta.
     *
     * @param password valor tal como lo escribió el usuario (no se recorta).
     */
    fun loginPasswordError(password: String): String? = when {
        password.isBlank() -> "Ingresá tu contraseña"
        exceedsBcryptLimit(password) -> "La contraseña es demasiado larga"
        else -> null
    }

    /** Las letras con tilde, la ñ o los emojis ocupan más de un byte en UTF-8. */
    private fun exceedsBcryptLimit(password: String): Boolean =
        password.encodeToByteArray().size > PASSWORD_MAX_BYTES
}
