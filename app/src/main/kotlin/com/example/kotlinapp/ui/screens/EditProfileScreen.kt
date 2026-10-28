package com.example.kotlinapp.ui.pilot

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val user by AuthRepository.currentUser.collectAsState()
    val current = user
    if (!AuthRepository.isAuthenticated || current == null) {
        AccessMessage("Inicia sesión para editar tus datos.", onBack)
        return
    }
    var name by rememberSaveable(current.uid) { mutableStateOf(current.name) }
    var phone by rememberSaveable(current.uid) { mutableStateOf(current.phone) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dirty = name.trim() != current.name || phone.trim() != current.phone
    BackHandler(enabled = busy) { /* Wait for the save to complete. */ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !busy, onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al perfil")
            }
            Text("Editar mi perfil", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f))
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu nombre en la comunidad", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Mantén tus datos al día para coordinar intercambios con claridad.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(120); saved = false; error = null },
            enabled = !busy,
            label = { Text("Nombre o empresa") },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.take(20); saved = false; error = null },
            enabled = !busy,
            label = { Text("Teléfono") },
            supportingText = { Text("Formato colombiano: +57 seguido de 10 dígitos.") },
            leadingIcon = { Icon(Icons.Outlined.Phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tu participación", style = MaterialTheme.typography.labelLarge)
                Text(current.typedRole.displayName, style = MaterialTheme.typography.titleMedium)
                Text("El rol elegido al registrarte se conserva. Las cuentas mixtas pueden ofrecer y buscar material.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (saved) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Text("Tus datos quedaron guardados.", color = MaterialTheme.colorScheme.primary)
            }
        }
        Button(
            enabled = !busy && dirty,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        AuthRepository.updateProfile(name, phone)
                            .onSuccess { saved = true }
                            .onFailure { error = it.message ?: "No se pudieron guardar tus datos." }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } finally {
                        busy = false
                    }
                }
            },
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(10.dp))
            }
            Text(if (busy) "Guardando…" else "Guardar cambios")
        }
    }
}
