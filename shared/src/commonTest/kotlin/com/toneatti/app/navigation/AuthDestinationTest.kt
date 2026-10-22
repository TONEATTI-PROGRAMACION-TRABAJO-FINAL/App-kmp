package com.toneatti.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de [AuthDestination]: sus rutas conviven en el mismo `NavHost` que las de
 * [AppDestination], así que no pueden repetirse, y no deben ser pestañas.
 */
class AuthDestinationTest {

    /** Ninguna ruta puede repetirse entre los dos enums. */
    @Test
    fun lasRutasSonUnicasEntreTodosLosDestinos() {
        val routes = AuthDestination.entries.map { it.route } + AppDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    /** Las pantallas de autenticación no son pestañas: la BottomBar se oculta en ellas. */
    @Test
    fun noSonPestanas() {
        AuthDestination.entries.forEach { destination ->
            assertNull(AppDestination.fromRoute(destination.route))
        }
    }

    /** Cada ruta tiene texto. */
    @Test
    fun lasRutasNoEstanVacias() {
        assertTrue(AuthDestination.entries.all { it.route.isNotBlank() })
    }
}
