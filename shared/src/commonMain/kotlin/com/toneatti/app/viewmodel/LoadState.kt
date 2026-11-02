package com.toneatti.app.viewmodel

/**
 * Estado de un dato que se carga del backend, tal como lo ve una pantalla.
 *
 * Una lista vacía es [Ready] con la lista vacía, no un error: la pantalla decide qué
 * texto mostrar en ese caso.
 *
 * @param T tipo del dato cargado.
 */
sealed interface LoadState<out T> {

    /** La petición está en curso. */
    data object Loading : LoadState<Nothing>

    /**
     * La petición falló.
     *
     * @property message mensaje en español para mostrar junto al botón de reintento.
     */
    data class Error(val message: String) : LoadState<Nothing>

    /**
     * El dato llegó.
     *
     * @property value dato cargado.
     */
    data class Ready<out T>(val value: T) : LoadState<T>
}
