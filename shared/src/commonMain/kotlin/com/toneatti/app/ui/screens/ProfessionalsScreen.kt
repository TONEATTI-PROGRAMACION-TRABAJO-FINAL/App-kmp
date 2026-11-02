package com.toneatti.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toneatti.app.data.catalog.model.Professional
import com.toneatti.app.data.catalog.model.ProfessionalCategory
import com.toneatti.app.viewmodel.LoadState
import com.toneatti.app.viewmodel.ProfessionalsUiState

/**
 * Pantalla de la pestaña "Profesionales": búsqueda de profesionales por categoría.
 *
 * Está modelada como una **clase** cuyo método composable [Content] dibuja la UI.
 * La instancia la crea [com.toneatti.app.navigation.MainScreen] y se conserva
 * con `remember`, por lo que no se recrea en cada recomposición.
 *
 * Es una pantalla **sin estado**: no conoce al ViewModel ni al `NavController`.
 * Recibe el [ProfessionalsUiState] y avisa cada acción con un callback.
 *
 * Arriba muestra las categorías como chips; al elegir una, debajo aparecen sus
 * profesionales habilitados. Cada carga tiene su indicador, su error con "Reintentar"
 * y su texto para la lista vacía. Los profesionales todavía no se pueden tocar: la
 * elección de horario llega en la K4.
 */
class ProfessionalsScreen {

    /**
     * Dibuja la pantalla.
     *
     * @param state estado actual de la búsqueda.
     * @param onCategorySelected el usuario eligió una categoría (recibe su id).
     * @param onRetryCategories el usuario pidió reintentar la carga de categorías.
     * @param onRetryProfessionals el usuario pidió reintentar la carga de profesionales.
     * @param modifier modificador aplicado al contenedor raíz de la pantalla.
     */
    @Composable
    fun Content(
        state: ProfessionalsUiState,
        onCategorySelected: (Long) -> Unit,
        onRetryCategories: () -> Unit,
        onRetryProfessionals: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "Profesionales",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))

            when (val categories = state.categories) {
                LoadState.Loading -> Loading(Modifier.fillMaxSize())
                is LoadState.Error -> ErrorWithRetry(categories.message, onRetryCategories, Modifier.fillMaxSize())
                is LoadState.Ready -> if (categories.value.isEmpty()) {
                    Message("No hay categorías disponibles.", Modifier.fillMaxSize())
                } else {
                    CategoryChips(
                        categories = categories.value,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = onCategorySelected,
                    )
                    Spacer(Modifier.height(8.dp))
                    // `weight` deja a la lista el espacio que sobra debajo de los chips.
                    ProfessionalsSection(
                        professionals = state.professionals,
                        onRetry = onRetryProfessionals,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
    }

    /**
     * Fila con scroll horizontal de chips, uno por categoría; el elegido aparece marcado.
     *
     * @param categories categorías a mostrar.
     * @param selectedCategoryId categoría elegida, o `null`.
     * @param onCategorySelected el usuario tocó un chip.
     * @param modifier modificador de la fila.
     */
    @Composable
    private fun CategoryChips(
        categories: List<ProfessionalCategory>,
        selectedCategoryId: Long?,
        onCategorySelected: (Long) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // La clave estable (id) conserva cada chip si la lista cambia de orden.
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) },
                )
            }
        }
    }

    /**
     * Zona de los profesionales de la categoría elegida.
     *
     * @param professionals carga de profesionales, o `null` si no hay categoría elegida.
     * @param onRetry el usuario pidió reintentar la carga.
     * @param modifier modificador de la zona.
     */
    @Composable
    private fun ProfessionalsSection(
        professionals: LoadState<List<Professional>>?,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        when (professionals) {
            null -> Message("Elegí una categoría para ver sus profesionales.", modifier)
            LoadState.Loading -> Loading(modifier)
            is LoadState.Error -> ErrorWithRetry(professionals.message, onRetry, modifier)
            is LoadState.Ready -> if (professionals.value.isEmpty()) {
                Message("No hay profesionales habilitados en esta categoría.", modifier)
            } else {
                ProfessionalList(professionals.value, modifier)
            }
        }
    }

    /**
     * Lista de profesionales. Los ítems no son clickeables hasta que exista la elección
     * de horario (K4).
     *
     * @param professionals profesionales a mostrar.
     * @param modifier modificador de la lista.
     */
    @Composable
    private fun ProfessionalList(
        professionals: List<Professional>,
        modifier: Modifier = Modifier,
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(professionals, key = { it.id }) { professional ->
                ListItem(headlineContent = { Text(professional.fullName) })
                HorizontalDivider()
            }
        }
    }

    /**
     * Indicador de carga centrado.
     *
     * @param modifier modificador del contenedor.
     */
    @Composable
    private fun Loading(modifier: Modifier = Modifier) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    /**
     * Mensaje de error centrado con un botón para reintentar.
     *
     * @param message mensaje en español.
     * @param onRetry el usuario tocó "Reintentar".
     * @param modifier modificador del contenedor.
     */
    @Composable
    private fun ErrorWithRetry(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }

    /**
     * Texto informativo centrado (lista vacía o falta elegir categoría).
     *
     * @param text texto a mostrar.
     * @param modifier modificador del contenedor.
     */
    @Composable
    private fun Message(text: String, modifier: Modifier = Modifier) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
