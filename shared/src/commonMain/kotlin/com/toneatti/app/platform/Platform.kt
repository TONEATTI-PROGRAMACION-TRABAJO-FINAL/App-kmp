package com.toneatti.app.platform

/**
 * Información básica de la plataforma donde se ejecuta la aplicación.
 *
 * Es el contrato del patrón `expect`/`actual`: se declara aquí, en `commonMain`,
 * y cada target (en este proyecto, solo `androidMain`) aporta su propia
 * implementación. El código común consume el resultado sin saber en qué
 * plataforma corre.
 */
interface Platform {
    /** Nombre legible de la plataforma (por ejemplo, `"Android 35"`). */
    val name: String
}

/**
 * Devuelve la implementación de [Platform] correspondiente a la plataforma actual.
 *
 * Cada target debe proporcionar su `actual fun getPlatform(): Platform`.
 */
expect fun getPlatform(): Platform
