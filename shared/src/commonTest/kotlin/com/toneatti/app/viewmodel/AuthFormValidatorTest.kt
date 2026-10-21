package com.toneatti.app.viewmodel

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pruebas de [AuthFormValidator] para el login: deben coincidir con las reglas de
 * `LoginRequest` de Servicio-turnos.
 */
class AuthFormValidatorTest {

    @Test
    fun emailVacioEsInvalido() {
        assertNotNull(AuthFormValidator.loginEmailError(""))
        assertNotNull(AuthFormValidator.loginEmailError("   "))
    }

    @Test
    fun emailSinFormatoEsInvalido() {
        assertNotNull(AuthFormValidator.loginEmailError("ana"))
        assertNotNull(AuthFormValidator.loginEmailError("ana@"))
        assertNotNull(AuthFormValidator.loginEmailError("ana@example"))
        assertNotNull(AuthFormValidator.loginEmailError("ana example@mail.com"))
    }

    @Test
    fun emailConFormatoEsValido() {
        assertNull(AuthFormValidator.loginEmailError("ana@example.com"))
        assertNull(AuthFormValidator.loginEmailError("ana.perez+turnos@mail.example.com.ar"))
    }

    @Test
    fun contrasenaVaciaEsInvalida() {
        assertNotNull(AuthFormValidator.loginPasswordError(""))
        assertNotNull(AuthFormValidator.loginPasswordError("   "))
    }

    /** En el login no hay largo mínimo: esa regla es del registro. */
    @Test
    fun contrasenaCortaEsValidaEnElLogin() {
        assertNull(AuthFormValidator.loginPasswordError("a"))
    }

    @Test
    fun contrasenaDe72BytesEsValida() {
        assertNull(AuthFormValidator.loginPasswordError("a".repeat(72)))
    }

    @Test
    fun contrasenaDe73BytesEsInvalida() {
        assertNotNull(AuthFormValidator.loginPasswordError("a".repeat(73)))
    }

    /** "ñ" ocupa 2 bytes en UTF-8: 37 caracteres son 74 bytes aunque sean menos de 72 caracteres. */
    @Test
    fun elLimiteSeCuentaEnBytesNoEnCaracteres() {
        assertNull(AuthFormValidator.loginPasswordError("ñ".repeat(36)))
        assertNotNull(AuthFormValidator.loginPasswordError("ñ".repeat(37)))
    }
}
