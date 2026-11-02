package com.toneatti.app.data.catalog

/**
 * Resultado de una operación del catálogo: éxito con un valor o un [CatalogError].
 *
 * Igual que `AuthResult`, se usa en lugar de excepciones para que quien llama esté
 * obligado (por el `when` exhaustivo) a contemplar los errores esperables.
 *
 * @param T tipo del valor en caso de éxito.
 */
sealed interface CatalogResult<out T> {

    /** Operación exitosa. @property value valor obtenido. */
    data class Success<out T>(val value: T) : CatalogResult<T>

    /** Operación fallida. @property error causa del fallo. */
    data class Failure(val error: CatalogError) : CatalogResult<Nothing>
}
