package com.toneatti.app.data.catalog

import com.toneatti.app.data.catalog.dto.CategoryResponse
import com.toneatti.app.data.catalog.dto.ProfessionalResponse
import com.toneatti.app.data.catalog.model.Professional
import com.toneatti.app.data.catalog.model.ProfessionalCategory
import com.toneatti.app.data.session.SessionManager

/**
 * Consultas del catálogo (categorías y profesionales) que usan los ViewModels.
 *
 * Es una interfaz para que los tests de los ViewModels usen un fake, sin red ni sesión.
 */
interface CatalogRepository {

    /**
     * Categorías habilitadas.
     *
     * @return la lista (vacía si no hay); [CatalogError.SessionExpired] si hay que volver
     *   a iniciar sesión, u otro [CatalogError].
     */
    suspend fun categories(): CatalogResult<List<ProfessionalCategory>>

    /**
     * Profesionales habilitados de una categoría.
     *
     * @param categoryId categoría elegida.
     * @return la lista (vacía si no tiene); [CatalogError.SessionExpired] si hay que volver
     *   a iniciar sesión, u otro [CatalogError].
     */
    suspend fun professionals(categoryId: Long): CatalogResult<List<Professional>>
}

/**
 * Implementación de [CatalogRepository] sobre Servicio-turnos.
 *
 * El token lo pide a [SessionManager] en cada consulta; si no hay sesión o el backend la
 * rechaza (401), se la descarta con [SessionManager.expire], que avisa para volver al
 * login. Así ninguna pantalla tiene que ocuparse del 401.
 *
 * @param catalogApi cliente de los endpoints de catálogo.
 * @param sessionManager sesión del usuario.
 */
class DefaultCatalogRepository(
    private val catalogApi: CatalogApi,
    private val sessionManager: SessionManager,
) : CatalogRepository {

    override suspend fun categories(): CatalogResult<List<ProfessionalCategory>> =
        authorized { token -> catalogApi.categories(token) }
            .map { categories -> categories.map { it.toModel() } }

    override suspend fun professionals(categoryId: Long): CatalogResult<List<Professional>> =
        authorized { token -> catalogApi.professionals(token, categoryId) }
            // El backend ya descarta los deshabilitados; se filtra igual para no ofrecer
            // nunca un profesional que no atiende.
            .map { professionals -> professionals.filter { it.enabled }.map { it.toModel() } }

    /**
     * Ejecuta [call] con el token del usuario.
     *
     * Sin token, devuelve [CatalogError.SessionExpired] sin hacer la petición
     * ([SessionManager.accessToken] ya avisó). Con un 401, descarta la sesión y devuelve
     * también [CatalogError.SessionExpired].
     *
     * @param call petición que recibe el token.
     */
    private suspend fun <T> authorized(call: suspend (String) -> CatalogResult<T>): CatalogResult<T> {
        val token = sessionManager.accessToken() ?: return CatalogResult.Failure(CatalogError.SessionExpired)
        val result = call(token)
        if (result is CatalogResult.Failure && result.error == CatalogError.Unauthorized) {
            sessionManager.expire()
            return CatalogResult.Failure(CatalogError.SessionExpired)
        }
        return result
    }

    /** Convierte el valor de un éxito y deja pasar el fallo tal cual. */
    private inline fun <T, R> CatalogResult<T>.map(transform: (T) -> R): CatalogResult<R> =
        when (this) {
            is CatalogResult.Success -> CatalogResult.Success(transform(value))
            is CatalogResult.Failure -> this
        }

    private fun CategoryResponse.toModel(): ProfessionalCategory =
        ProfessionalCategory(id = id, name = name, description = description)

    private fun ProfessionalResponse.toModel(): Professional =
        Professional(id = id, categoryId = categoryId, firstName = firstName, lastName = lastName)
}
