package com.toneatti.app.data.catalog.model

/**
 * Profesional habilitado tal como lo usa la app.
 *
 * No tiene `enabled`: el repositorio solo entrega profesionales habilitados.
 *
 * @property id identificador del profesional.
 * @property categoryId categoría a la que pertenece.
 * @property firstName nombre.
 * @property lastName apellido.
 */
data class Professional(
    val id: Long,
    val categoryId: Long,
    val firstName: String,
    val lastName: String,
) {
    /** Nombre y apellido, como se muestra en las listas. */
    val fullName: String
        get() = "$firstName $lastName"
}
