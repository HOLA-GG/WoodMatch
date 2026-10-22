package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.WoodWasteRepository
import com.example.kotlinapp.ui.components.WithdrawalPassActions
import kotlinx.coroutines.launch

@Composable
fun MyActivityScreen(onExplore: () -> Unit) {
    val user by AuthRepository.currentUser.collectAsState()
    val items by WoodWasteRepository.items.collectAsState()
    val requests by WoodWasteRepository.requests.collectAsState()
    val error by WoodWasteRepository.error.collectAsState()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Mi actividad", style = MaterialTheme.typography.headlineMedium)
            Text("Tus publicaciones, solicitudes e historial de retiros.")
            if (!AuthRepository.isAuthenticated) Text("Inicia sesión para recuperar tu actividad. El modo invitado no guarda publicaciones ni solicitudes.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it) }
            Text("Mis publicaciones", style = MaterialTheme.typography.titleLarge)
        }
        val mine = items.filter { it.authorId == user?.uid }
        if (mine.isEmpty()) item { Text("Aún no tienes publicaciones."); TextButton(onClick = onExplore) { Text("Explorar material") } }
        items(mine, key = { "item_${it.id}" }) { item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text("${item.formattedQuantity} · ${item.typedStatus.displayName}")
                if (item.isAvailable) TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        WoodWasteRepository.updateWasteStatus(item.id, WasteStatus.INACTIVO)
                            .onSuccess { message = "Publicación desactivada." }
                            .onFailure { message = "No se pudo desactivar. Comprueba tu conexión." }
                        busy = false
                    }
                }) { Text("Desactivar publicación") }
            } }
        }
        item { Text("Solicitudes e historial", style = MaterialTheme.typography.titleLarge) }
        if (requests.isEmpty()) item { Text("No tienes solicitudes registradas.") }
        items(requests, key = { "request_${it.id}" }) { request ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(request.wasteTitle, style = MaterialTheme.typography.titleMedium)
                Text("${request.typedStatus.displayName} · Fecha propuesta: ${request.proposedDate}")
                WithdrawalPassActions(request, request.authorId == user?.uid)
                TextButton(onClick = onExplore) { Text("Gestionar solicitudes") }
            } }
        }
    }
}
