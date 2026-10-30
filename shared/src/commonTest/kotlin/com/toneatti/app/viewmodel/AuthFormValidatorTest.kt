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
    fun contrasenaDe128CaracteresEsValida() {
        assertNull(AuthFormValidator.loginPasswordError("a".repeat(128)))
    }

    @Test
    fun contrasenaDe129CaracteresEsInvalida() {
        assertNotNull(AuthFormValidator.loginPasswordError("a".repeat(129)))
    }

    /**
     * El límite se cuenta en unidades UTF-16, como `@Size` en el backend: "ñ" es una
     * unidad aunque ocupe 2 bytes en UTF-8, y un emoji son dos.
     */
    @Test
    fun elLimiteSeCuentaEnUnidadesUtf16() {
        assertNull(AuthFormValidator.loginPasswordError("ñ".repeat(128)))
        assertNotNull(AuthFormValidator.loginPasswordError("ñ".repeat(129)))
        assertNull(AuthFormValidator.loginPasswordError("😀".repeat(64)))
        assertNotNull(AuthFormValidator.loginPasswordError("😀".repeat(64) + "a"))
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
    fun registroAceptaContrasenaDe128CaracteresYRechazaLaDe129() {
        assertNull(AuthFormValidator.registerPasswordError("a".repeat(128)))
        assertNotNull(AuthFormValidator.registerPasswordError("a".repeat(129)))
    }

    /** 128 "ñ" son 256 bytes en UTF-8, pero 128 unidades UTF-16: es válida, como en el backend. */
    @Test
    fun registroCuentaElMaximoEnUnidadesUtf16() {
        assertNull(AuthFormValidator.registerPasswordError("ñ".repeat(128)))
        assertNotNull(AuthFormValidator.registerPasswordError("ñ".repeat(129)))
        assertNull(AuthFormValidator.registerPasswordError("😀".repeat(64)))
        assertNotNull(AuthFormValidator.registerPasswordError("😀".repeat(64) + "a"))
    }

    /** 4 emojis son 8 unidades UTF-16: alcanzan el mínimo, como con `@Size(min = 8)`. */
    @Test
    fun registroCuentaElMinimoEnUnidadesUtf16() {
        assertNull(AuthFormValidator.registerPasswordError("😀".repeat(4)))
        assertNotNull(AuthFormValidator.registerPasswordError("😀".repeat(3) + "a"))
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
