package com.toneatti.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.toneatti.app.viewmodel.LoginUiState

/**
 * Pantalla de inicio de sesión contra Servicio-turnos.
 *
 * Como el resto de las pantallas, es una **clase** con un método composable [Content],
 * creada una sola vez con `remember` por [com.toneatti.app.navigation.MainScreen].
 *
 * Es una pantalla **sin estado**: no conoce al ViewModel ni al `NavController`.
 * Recibe el [LoginUiState] y avisa cada acción del usuario con un callback; quien la
 * usa conecta esos callbacks con el ViewModel y decide adónde navegar.
 */
class LoginScreen {

    /**
     * Dibuja el formulario de inicio de sesión.
     *
     * @param state estado actual del formulario.
     * @param onEmailChange el usuario editó el email.
     * @param onPasswordChange el usuario editó la contraseña.
     * @param onSubmit el usuario pidió ingresar (botón o tecla "Listo" del teclado).
     * @param onGoToRegister el usuario quiere crear una cuenta.
     * @param modifier modificador aplicado al contenedor raíz de la pantalla.
     */
    @Composable
    fun Content(
        state: LoginUiState,
        onEmailChange: (String) -> Unit,
        onPasswordChange: (String) -> Unit,
        onSubmit: () -> Unit,
        onGoToRegister: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Iniciar sesión",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                // Mientras se envía la petición no se puede editar lo que se está enviando.
                enabled = !state.isLoading,
                label = { Text("Email") },
                isError = state.emailError != null,
                supportingText = state.emailError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                label = { Text("Contraseña") },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { error -> { Text(error) } },
                singleLine = true,
                // Muestra puntos en lugar del texto escrito.
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                // "Listo" en el teclado equivale a tocar el botón.
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

            // Error del servidor o de conexión (los de validación van debajo de cada campo).
            state.errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Ingresar")
                }
            }
            TextButton(
                onClick = onGoToRegister,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text("Crear cuenta")
            }
        }
    }
}
