package com.toneatti.app.viewmodel

/**
 * Validación de los formularios de autenticación, con las mismas reglas que
 * Servicio-turnos (`LoginRequest`, `UserRequest`, `PasswordWithinBcryptLimit`).
 *
 * Validar antes de enviar es necesario, no solo cómodo: el 400 del backend trae un
 * mensaje genérico, sin indicar qué campo falló.
 *
 * Cada función devuelve el mensaje de error para mostrar debajo del campo, o `null`
 * si el valor es válido. Los textos están en español porque son para el usuario.
 *
 * Los largos se cuentan con `String.length` (unidades UTF-16), igual que `@Size` en Java.
 */
object AuthFormValidator {

    /** Límite de BCrypt: las contraseñas más largas se rechazan en el backend. */
    const val PASSWORD_MAX_BYTES: Int = 72

    /** Largo mínimo de la contraseña al registrarse. */
    const val PASSWORD_MIN_LENGTH: Int = 8

    /** Largo máximo del email al registrarse. */
    const val EMAIL_MAX_LENGTH: Int = 254

    /** Largo mínimo de nombre y apellido (el mismo que aplica la cátedra al paciente). */
    const val NAME_MIN_LENGTH: Int = 2

    /** Largo máximo de nombre y apellido. */
    const val NAME_MAX_LENGTH: Int = 100

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

    /**
     * Email del registro: las reglas del login más un largo máximo de [EMAIL_MAX_LENGTH].
     *
     * @param email valor ya sin espacios alrededor.
     */
    fun registerEmailError(email: String): String? = when {
        email.length > EMAIL_MAX_LENGTH -> "El email puede tener como máximo $EMAIL_MAX_LENGTH caracteres"
        else -> loginEmailError(email)
    }

    /**
     * Contraseña del registro: obligatoria, de al menos [PASSWORD_MIN_LENGTH] caracteres
     * y de como máximo [PASSWORD_MAX_BYTES] bytes en UTF-8.
     *
     * @param password valor tal como lo escribió el usuario (no se recorta).
     */
    fun registerPasswordError(password: String): String? = when {
        password.isBlank() -> "Ingresá una contraseña"
        password.length < PASSWORD_MIN_LENGTH ->
            "La contraseña debe tener al menos $PASSWORD_MIN_LENGTH caracteres"
        exceedsBcryptLimit(password) -> "La contraseña es demasiado larga"
        else -> null
    }

    /**
     * Nombre: obligatorio, de [NAME_MIN_LENGTH] a [NAME_MAX_LENGTH] caracteres.
     *
     * @param firstName valor ya sin espacios alrededor.
     */
    fun firstNameError(firstName: String): String? = nameError(firstName, field = "nombre")

    /**
     * Apellido: obligatorio, de [NAME_MIN_LENGTH] a [NAME_MAX_LENGTH] caracteres.
     *
     * @param lastName valor ya sin espacios alrededor.
     */
    fun lastNameError(lastName: String): String? = nameError(lastName, field = "apellido")

    private fun nameError(value: String, field: String): String? = when {
        value.isBlank() -> "Ingresá tu $field"
        value.length !in NAME_MIN_LENGTH..NAME_MAX_LENGTH ->
            "El $field debe tener entre $NAME_MIN_LENGTH y $NAME_MAX_LENGTH caracteres"
        else -> null
    }

    /** Las letras con tilde, la ñ o los emojis ocupan más de un byte en UTF-8. */
    private fun exceedsBcryptLimit(password: String): Boolean =
        password.encodeToByteArray().size > PASSWORD_MAX_BYTES
}
