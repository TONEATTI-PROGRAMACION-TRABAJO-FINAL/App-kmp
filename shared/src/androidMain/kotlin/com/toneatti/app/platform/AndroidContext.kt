package com.toneatti.app.platform

import android.content.Context

/**
 * Guarda el `Context` de la aplicación para las implementaciones `actual` de Android
 * que lo necesitan (por ejemplo, el almacenamiento del token).
 *
 * El código común no puede recibir un `Context`, y las clases de UI no reciben
 * parámetros en el constructor; por eso `MainActivity` lo registra una vez con
 * [init] antes de dibujar la interfaz.
 *
 * Solo se guarda el `Context` de la **aplicación**, que vive lo mismo que el proceso:
 * guardar el de una `Activity` la retendría en memoria después de destruida.
 */
object AndroidContext {

    private var applicationContext: Context? = null

    /**
     * Registra el `Context` de la aplicación. Puede llamarse más de una vez
     * (por ejemplo, al recrearse la `Activity`): siempre es el mismo.
     *
     * @param context cualquier `Context`; se conserva su `applicationContext`.
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Devuelve el `Context` de la aplicación.
     *
     * @throws IllegalStateException si todavía no se llamó a [init].
     */
    fun get(): Context =
        checkNotNull(applicationContext) { "AndroidContext.init() must be called from MainActivity first" }
}
