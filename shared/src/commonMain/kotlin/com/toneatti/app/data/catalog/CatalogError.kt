package com.toneatti.app.data.catalog

/**
 * Errores posibles de los endpoints de catálogo de Servicio-turnos, ya interpretados.
 *
 * La capa de datos no arma textos para el usuario: el ViewModel traduce estos casos
 * a mensajes en español.
 */
sealed interface CatalogError {

    /**
     * 401: el backend rechazó el token (vencido, con firma inválida o ausente).
     *
     * Lo produce [CatalogApi]; el repositorio lo convierte en [SessionExpired] después
     * de descartar la sesión.
     */
    data object Unauthorized : CatalogError

    /**
     * No hay sesión que sirva: no había token, venció en el dispositivo o el backend lo
     * rechazó. La sesión ya se descartó y la app está volviendo al login.
     */
    data object SessionExpired : CatalogError

    /** 503 `SERVICE_UNAVAILABLE`: Servicio-turnos no pudo consultar a Servicio-catalogo. */
    data object ServiceUnavailable : CatalogError

    /** No hubo respuesta: sin conexión, servidor caído o tiempo agotado. */
    data object Network : CatalogError

    /**
     * Cualquier otra respuesta: códigos no previstos o un cuerpo ilegible.
     *
     * @property status código HTTP recibido.
     * @property detail `detail` del problem+json, si lo había.
     */
    data class Unexpected(val status: Int, val detail: String?) : CatalogError
}
