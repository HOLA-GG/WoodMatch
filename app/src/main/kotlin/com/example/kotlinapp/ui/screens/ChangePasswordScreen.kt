package com.example.kotlinapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinapp.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    onRecoverPassword: (() -> Unit)? = null,
) {
    // Deliberately not rememberSaveable: credentials never enter saved state or preferences.
    var current by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val operation: PasswordChangeViewModel = viewModel()
    val result by operation.state.collectAsState()
    val loading = result.loading
    val changed = result.changed
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(result) {
        message = result.message
        if (result.changed) { current = ""; replacement = ""; confirmation = "" }
    }
    BackHandler(enabled = loading) { /* Avoid leaving a password change in an ambiguous state. */ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
            .testTag("change_password_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !loading, onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al perfil")
            }
            Text("Cambiar contraseña", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ))).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(if (changed) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                    null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(if (changed) "Tu acceso está renovado" else "Protege tu cuenta",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Firebase confirma tu identidad antes de cambiar la contraseña. WoodMatch no guarda tus contraseñas en el perfil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        if (!AuthRepository.isAuthenticated) {
            Text("Inicia sesión para cambiar tu contraseña.")
        } else if (!changed) {
            CredentialField("Contraseña actual", current, !loading, "current_password") {
                current = it; message = null
            }
            CredentialField("Nueva contraseña", replacement, !loading, "new_password") {
                replacement = it; message = null
            }
            Text("Al menos 8 caracteres, una mayúscula, una minúscula y un número.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            CredentialField("Confirmar nueva contraseña", confirmation, !loading, "confirm_new_password") {
                confirmation = it; message = null
            }
            Button(
                enabled = !loading && current.isNotBlank() && replacement.isNotBlank() && confirmation.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("change_password_submit"),
                onClick = {
                    if (replacement != confirmation) {
                        message = "Las contraseñas nuevas no coinciden."
                    } else {
                        message = null
                        operation.submit(current, replacement)
                    }
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (loading) "Confirmando y guardando…" else "Actualizar contraseña")
            }
            onRecoverPassword?.let { recover ->
                TextButton(enabled = !loading, onClick = recover, modifier = Modifier.fillMaxWidth()) {
                    Text("Olvidé mi contraseña actual")
                }
            }
        }
        message?.let { text ->
            Surface(
                Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                shape = MaterialTheme.shapes.large,
                color = if (changed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(text, Modifier.padding(18.dp),
                    color = if (changed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (changed || !AuthRepository.isAuthenticated) {
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Volver al perfil")
            }
        }
    }
}

data class PasswordChangeState(
    val loading: Boolean = false,
    val changed: Boolean = false,
    val message: String? = null,
)

/** Keeps an in-flight Firebase request across rotation, without saving credentials to disk. */
class PasswordChangeViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(PasswordChangeState())
    val state = mutableState.asStateFlow()

    fun submit(currentPassword: String, newPassword: String) {
        if (mutableState.value.loading || mutableState.value.changed) return
        mutableState.value = PasswordChangeState(loading = true)
        viewModelScope.launch {
            try {
                AuthRepository.changePassword(currentPassword, newPassword)
                    .onSuccess {
                        mutableState.value = PasswordChangeState(
                            changed = true,
                            message = "Contraseña actualizada. La próxima vez que inicies sesión, utiliza la nueva.",
                        )
                    }
                    .onFailure {
                        mutableState.value = PasswordChangeState(message = it.message ?: "No se pudo cambiar la contraseña.")
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = PasswordChangeState(message = "No se pudo cambiar la contraseña. Inténtalo de nuevo.")
            }
        }
    }
}

@Composable
private fun CredentialField(
    label: String,
    value: String,
    enabled: Boolean,
    tag: String,
    onChange: (String) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    if (visible) "Ocultar contraseña" else "Mostrar contraseña")
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
