package com.toneatti.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de la pestaña "Ajustes".
 *
 * Está modelada como una **clase** cuyo método composable [Content] dibuja la UI.
 * La instancia la crea [com.toneatti.app.navigation.MainScreen] y se conserva
 * con `remember`; los valores que pueden cambiar (tema) se reciben como parámetros
 * de [Content], no del constructor, para que la instancia siga siendo estable.
 *
 * El interruptor de "Modo oscuro" usa **estado elevado**: no guarda su propio
 * valor; recibe [darkTheme] y notifica cambios con [onDarkThemeChange]. El estado
 * real vive en `App`, de modo que el tema de toda la app reacciona al cambio.
 */
class SettingsScreen {

    /**
     * Dibuja el contenido de la pantalla.
     *
     * @param darkTheme estado actual del modo oscuro (controlado desde arriba).
     * @param onDarkThemeChange callback que solicita cambiar el modo oscuro.
     * @param modifier modificador aplicado al contenedor raíz de la pantalla.
     */
    @Composable
    fun Content(
        darkTheme: Boolean,
        onDarkThemeChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))

            // Estado elevado: cambiarlo altera el tema global de la aplicación.
            ToggleRow(
                title = "Modo oscuro",
                checked = darkTheme,
                onCheckedChange = onDarkThemeChange,
            )
            HorizontalDivider()
        }
    }

    /**
     * Fila reutilizable interna de esta pantalla: etiqueta a la izquierda (ocupa
     * el espacio disponible) y un [Switch] a la derecha.
     *
     * @param title texto descriptivo de la preferencia.
     * @param checked estado actual del interruptor.
     * @param onCheckedChange callback invocado con el nuevo valor al alternar.
     * @param modifier modificador opcional aplicado a la fila.
     */
    @Composable
    private fun ToggleRow(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                // `weight(1f)` empuja el Switch al extremo derecho.
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
