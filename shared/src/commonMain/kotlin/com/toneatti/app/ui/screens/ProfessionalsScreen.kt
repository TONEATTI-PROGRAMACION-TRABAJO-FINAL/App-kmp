package com.toneatti.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de la pestaña "Profesionales".
 *
 * Está modelada como una **clase** cuyo método composable [Content] dibuja la UI.
 * La instancia la crea [com.toneatti.app.navigation.MainScreen] y se conserva
 * con `remember`, por lo que no se recrea en cada recomposición.
 *
 * Por ahora solo muestra un texto de ejemplo; la búsqueda de profesionales
 * llega en K3.
 */
class ProfessionalsScreen {

    /**
     * Dibuja el contenido de la pantalla.
     *
     * @param modifier modificador aplicado al contenedor raíz de la pantalla.
     */
    @Composable
    fun Content(modifier: Modifier = Modifier) {
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
            Text(
                text = "Acá vas a poder buscar profesionales.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
