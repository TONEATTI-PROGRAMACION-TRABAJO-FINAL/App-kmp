package com.toneatti.app.data.remote

/**
 * Configuración de la conexión con Servicio-turnos.
 *
 * No contiene secretos: la URL base es pública y el JWT del usuario se obtiene en
 * tiempo de ejecución.
 */
object ApiConfig {

    /**
     * URL base de Servicio-turnos.
     *
     * `10.0.2.2` es la dirección con la que el emulador de Android llega al `localhost`
     * de la máquina, y `8080` el puerto por defecto de Spring Boot. Para un dispositivo
     * físico hay que reemplazarla por la IP de la máquina en la red local (y permitirla
     * en `network_security_config.xml`).
     */
    const val BASE_URL: String = "http://10.0.2.2:8080/"

    /** Tiempo máximo para establecer la conexión. */
    const val CONNECT_TIMEOUT_MILLIS: Long = 10_000

    /** Tiempo máximo para una petición completa. */
    const val REQUEST_TIMEOUT_MILLIS: Long = 15_000
}
