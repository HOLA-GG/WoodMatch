package com.example.kotlinapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(onNavigateBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = loading) { /* Wait for Firebase to finish this request. */ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
            .testTag("forgot_password_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !loading, onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
            }
            Text("Recuperar acceso", style = MaterialTheme.typography.headlineSmall)
        }
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ))).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    if (requested) Icons.Outlined.MarkEmailRead else Icons.Outlined.Email,
                    null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (requested) "Revisa tu correo" else "Volvamos a conectar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Recupera tu contraseña mediante un enlace de Firebase. Solo necesitas el correo con el que te registraste.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (!requested) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(254); message = null },
                enabled = !loading,
                label = { Text("Correo de tu cuenta") },
                leadingIcon = { Icon(Icons.Outlined.Email, null) },
                modifier = Modifier.fillMaxWidth().testTag("recovery_email"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
            )
            Button(
                enabled = !loading && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("recovery_submit"),
                onClick = {
                    loading = true
                    message = null
                    scope.launch {
                        try {
                            AuthRepository.forgotPassword(email)
                                .onSuccess { message = it; requested = true }
                                .onFailure { message = it.message }
                        } finally { loading = false }
                    }
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (loading) "Solicitando enlace…" else "Enviar enlace de recuperación")
            }
        }
        message?.let { text ->
            Surface(
                Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                shape = MaterialTheme.shapes.large,
                color = if (requested) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(text, Modifier.padding(18.dp),
                    color = if (requested) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (requested) {
            OutlinedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sigue estos pasos", style = MaterialTheme.typography.titleMedium)
                    Text("1. Busca el mensaje de recuperación en tu buzón y en Spam.")
                    Text("2. Abre el enlace y elige una contraseña nueva.")
                    Text("3. Regresa a WoodMatch e inicia sesión con la nueva contraseña.")
                    Text("Si no llega, comprueba que escribiste el correo usado en el registro. El envío no confirma que la cuenta exista ni que el buzón haya recibido el mensaje.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Volver a WoodMatch")
            }
            TextButton(onClick = { requested = false; message = null }, modifier = Modifier.fillMaxWidth()) {
                Text("Revisar el correo ingresado")
            }
        }
    }
}
