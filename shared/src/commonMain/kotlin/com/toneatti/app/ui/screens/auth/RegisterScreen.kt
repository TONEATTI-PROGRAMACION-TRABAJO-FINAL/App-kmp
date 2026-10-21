package com.toneatti.app.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.toneatti.app.viewmodel.AuthFormValidator
import com.toneatti.app.viewmodel.RegisterUiState

/**
 * Pantalla de registro de usuario contra Servicio-turnos.
 *
 * Como el resto de las pantallas, es una **clase** con un método composable [Content],
 * creada una sola vez con `remember` por [com.toneatti.app.navigation.MainScreen].
 *
 * Es una pantalla **sin estado**: no conoce al ViewModel ni al `NavController`.
 * Recibe el [RegisterUiState] y avisa cada acción del usuario con un callback.
 */
class RegisterScreen {

    /**
     * Dibuja el formulario de registro.
     *
     * @param state estado actual del formulario.
     * @param onFirstNameChange el usuario editó el nombre.
     * @param onLastNameChange el usuario editó el apellido.
     * @param onEmailChange el usuario editó el email.
     * @param onPasswordChange el usuario editó la contraseña.
     * @param onSubmit el usuario pidió crear la cuenta (botón o tecla "Listo").
     * @param onGoToLogin el usuario quiere volver al inicio de sesión.
     * @param modifier modificador aplicado al contenedor raíz de la pantalla.
     */
    @Composable
    fun Content(
        state: RegisterUiState,
        onFirstNameChange: (String) -> Unit,
        onLastNameChange: (String) -> Unit,
        onEmailChange: (String) -> Unit,
        onPasswordChange: (String) -> Unit,
        onSubmit: () -> Unit,
        onGoToLogin: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        // Mientras se envía, o si la cuenta ya se creó, el formulario no se puede editar.
        val formEnabled = !state.isLoading && !state.accountCreated

        Column(
            modifier = modifier
                .fillMaxSize()
                // Con cuatro campos y el teclado abierto puede no entrar en pantalla.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))

            FormField(
                value = state.firstName,
                onValueChange = onFirstNameChange,
                label = "Nombre",
                error = state.firstNameError,
                enabled = formEnabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            FormField(
                value = state.lastName,
                onValueChange = onLastNameChange,
                label = "Apellido",
                error = state.lastNameError,
                enabled = formEnabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            FormField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                error = state.emailError,
                enabled = formEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            FormField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                error = state.passwordError,
                // Sin error, se recuerda la regla de largo mínimo.
                hint = "Al menos ${AuthFormValidator.PASSWORD_MIN_LENGTH} caracteres",
                enabled = formEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                // "Listo" en el teclado equivale a tocar el botón.
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                visualTransformation = PasswordVisualTransformation(),
            )

            // Error del servidor, de conexión o del login automático.
            state.errorMessage?.let { message ->
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
                enabled = formEnabled,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Crear cuenta")
                }
            }
            TextButton(
                onClick = onGoToLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text("Ya tengo cuenta")
            }
        }
    }

    /**
     * Campo del formulario: [OutlinedTextField] de una línea con su error (o una ayuda)
     * debajo.
     *
     * @param value texto actual.
     * @param onValueChange callback con el texto nuevo.
     * @param label etiqueta del campo.
     * @param error mensaje de error; si no es `null`, el campo se marca en rojo.
     * @param enabled si el campo se puede editar.
     * @param keyboardOptions tipo de teclado y acción de la tecla final.
     * @param modifier modificador opcional aplicado al campo.
     * @param hint texto de ayuda cuando no hay error.
     * @param keyboardActions qué hacer con la tecla final del teclado.
     * @param visualTransformation transformación visual (por ejemplo, ocultar la contraseña).
     */
    @Composable
    private fun FormField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        error: String?,
        enabled: Boolean,
        keyboardOptions: KeyboardOptions,
        modifier: Modifier = Modifier,
        hint: String? = null,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        visualTransformation: VisualTransformation = VisualTransformation.None,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = enabled,
            label = { Text(label) },
            isError = error != null,
            supportingText = (error ?: hint)?.let { text -> { Text(text) } },
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}
