package com.toneatti.app.data.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas de [UserSession]: cálculo del vencimiento a partir de `expiresIn`,
 * margen de seguridad y que el token no se filtre por `toString`.
 */
class UserSessionTest {

    private val now = 1_000_000_000L

    /** `expiresIn` llega en segundos y se convierte a un instante absoluto en milisegundos. */
    @Test
    fun fromExpiresInCalculaElVencimientoEnMilisegundos() {
        val session = UserSession.fromExpiresIn("token", expiresInSeconds = 86_400, nowMillis = now)

        assertEquals(now + 86_400_000, session.expiresAtMillis)
    }

    /** Con tiempo de sobra antes del vencimiento, la sesión sigue vigente. */
    @Test
    fun noEstaVencidaAntesDelMargen() {
        val session = UserSession("token", expiresAtMillis = now + UserSession.EXPIRY_MARGIN_MILLIS + 1)

        assertFalse(session.isExpired(now))
    }

    /** Dentro del margen de un minuto ya se la considera vencida. */
    @Test
    fun estaVencidaDentroDelMargen() {
        val session = UserSession("token", expiresAtMillis = now + UserSession.EXPIRY_MARGIN_MILLIS)

        assertTrue(session.isExpired(now))
    }

    /** Pasado el instante de vencimiento, está vencida. */
    @Test
    fun estaVencidaDespuesDelVencimiento() {
        val session = UserSession("token", expiresAtMillis = now - 1)

        assertTrue(session.isExpired(now))
    }

    /** El token es una credencial: no debe aparecer en logs. */
    @Test
    fun toStringNoExponeElToken() {
        val session = UserSession("secreto.jwt.firma", expiresAtMillis = now)

        assertFalse(session.toString().contains("secreto.jwt.firma"))
    }
}
