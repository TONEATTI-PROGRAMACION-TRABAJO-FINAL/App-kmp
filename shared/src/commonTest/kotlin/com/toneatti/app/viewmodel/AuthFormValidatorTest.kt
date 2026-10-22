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

    // ----- Registro (reglas de UserRequest) -----

    /** Arma un email válido de exactamente [length] caracteres. */
    private fun emailOfLength(length: Int): String {
        val domain = "@example.com"
        return "a".repeat(length - domain.length) + domain
    }

    @Test
    fun registroAceptaEmailDe254Caracteres() {
        assertNull(AuthFormValidator.registerEmailError(emailOfLength(254)))
    }

    @Test
    fun registroRechazaEmailDe255Caracteres() {
        assertNotNull(AuthFormValidator.registerEmailError(emailOfLength(255)))
    }

    @Test
    fun registroRechazaEmailVacioOSinFormato() {
        assertNotNull(AuthFormValidator.registerEmailError(""))
        assertNotNull(AuthFormValidator.registerEmailError("ana@example"))
    }

    @Test
    fun registroRechazaContrasenaVacia() {
        assertNotNull(AuthFormValidator.registerPasswordError(""))
        assertNotNull(AuthFormValidator.registerPasswordError("        "))
    }

    @Test
    fun registroRechazaContrasenaDe7Caracteres() {
        assertNotNull(AuthFormValidator.registerPasswordError("a".repeat(7)))
    }

    @Test
    fun registroAceptaContrasenaDe8Caracteres() {
        assertNull(AuthFormValidator.registerPasswordError("a".repeat(8)))
    }

    @Test
    fun registroAceptaContrasenaDe72BytesYRechazaLaDe73() {
        assertNull(AuthFormValidator.registerPasswordError("a".repeat(72)))
        assertNotNull(AuthFormValidator.registerPasswordError("a".repeat(73)))
    }

    /** 37 "ñ" son 37 caracteres (pasa el mínimo) pero 74 bytes (supera el máximo). */
    @Test
    fun registroCuentaElMaximoEnBytes() {
        assertNull(AuthFormValidator.registerPasswordError("ñ".repeat(36)))
        assertNotNull(AuthFormValidator.registerPasswordError("ñ".repeat(37)))
    }

    @Test
    fun nombreYApellidoVaciosSonInvalidos() {
        assertNotNull(AuthFormValidator.firstNameError(""))
        assertNotNull(AuthFormValidator.lastNameError("  "))
    }

    @Test
    fun nombreYApellidoDe1CaracterSonInvalidos() {
        assertNotNull(AuthFormValidator.firstNameError("A"))
        assertNotNull(AuthFormValidator.lastNameError("P"))
    }

    @Test
    fun nombreYApellidoDe2Y100CaracteresSonValidos() {
        assertNull(AuthFormValidator.firstNameError("Al"))
        assertNull(AuthFormValidator.firstNameError("a".repeat(100)))
        assertNull(AuthFormValidator.lastNameError("Li"))
        assertNull(AuthFormValidator.lastNameError("a".repeat(100)))
    }

    @Test
    fun nombreYApellidoDe101CaracteresSonInvalidos() {
        assertNotNull(AuthFormValidator.firstNameError("a".repeat(101)))
        assertNotNull(AuthFormValidator.lastNameError("a".repeat(101)))
    }
}
