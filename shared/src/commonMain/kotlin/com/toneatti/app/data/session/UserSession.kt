package com.toneatti.app.data.session

import kotlinx.serialization.Serializable

/**
 * Sesión del usuario final: el JWT que emite `POST /users/login` de Servicio-turnos
 * y el instante en que vence.
 *
 * Es siempre el token **del usuario**; el JWT técnico de la cátedra nunca llega a la app.
 *
 * El vencimiento se guarda como instante absoluto (y no como los `expiresIn` segundos
 * que devuelve el backend) para poder comprobarlo al reabrir la app sin decodificar el JWT.
 *
 * @property accessToken JWT que se envía como `Authorization: Bearer <token>`.
 * @property expiresAtMillis instante de vencimiento, en milisegundos desde la época Unix.
 */
@Serializable
data class UserSession(
    val accessToken: String,
    val expiresAtMillis: Long,
) {

    /**
     * Indica si la sesión ya venció en [nowMillis].
     *
     * Se la da por vencida [EXPIRY_MARGIN_MILLIS] antes del instante real, para no
     * usar un token que vencería en pleno viaje de la petición o por un reloj del
     * dispositivo levemente adelantado.
     *
     * @param nowMillis instante actual, en milisegundos desde la época Unix.
     */
    fun isExpired(nowMillis: Long): Boolean =
        nowMillis >= expiresAtMillis - EXPIRY_MARGIN_MILLIS

    /** El `toString` generado expondría el token en los logs. */
    override fun toString(): String = "UserSession(accessToken=****, expiresAtMillis=$expiresAtMillis)"

    companion object {
        /** Margen de seguridad antes del vencimiento real: un minuto. */
        const val EXPIRY_MARGIN_MILLIS: Long = 60_000

        /**
         * Crea la sesión a partir de la respuesta de login.
         *
         * @param accessToken JWT recibido.
         * @param expiresInSeconds vida del token en segundos (`expiresIn` del backend).
         * @param nowMillis instante en que se recibió la respuesta.
         */
        fun fromExpiresIn(accessToken: String, expiresInSeconds: Long, nowMillis: Long): UserSession =
            UserSession(
                accessToken = accessToken,
                expiresAtMillis = nowMillis + expiresInSeconds * 1_000,
            )
    }
}
