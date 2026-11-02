package com.toneatti.app.viewmodel

import com.toneatti.app.data.catalog.CatalogError

/**
 * Traduce un [CatalogError] al mensaje en español que ve el usuario.
 *
 * Los textos del backend (`detail` del problem+json) están en inglés y pensados para
 * diagnóstico, por eso no se muestran.
 */
object CatalogErrorMessages {

    /**
     * @param error error devuelto por el repositorio.
     * @return mensaje para mostrar en la pantalla.
     */
    fun messageFor(error: CatalogError): String = when (error) {
        // La app ya está volviendo al login; el mensaje solo se ve durante la transición.
        CatalogError.Unauthorized, CatalogError.SessionExpired -> "Tu sesión venció. Iniciá sesión de nuevo."
        CatalogError.ServiceUnavailable -> "El catálogo no está disponible en este momento. Intentá más tarde."
        CatalogError.Network -> "No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo."
        is CatalogError.Unexpected -> "Ocurrió un error en el servidor. Intentá más tarde."
    }
}
