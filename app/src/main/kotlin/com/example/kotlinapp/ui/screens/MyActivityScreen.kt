package com.example.kotlinapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.components.ActivityRequestCard
import com.example.kotlinapp.ui.pilot.StatusMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyActivityScreen(
    onExplore: () -> Unit,
    onEdit: (WoodWasteItem) -> Unit = {},
    onImport: (TreeReport) -> Unit = {},
) {
    val user by AuthRepository.currentUser.collectAsState()
    val all by WoodWasteRepository.items.collectAsState()
    val requests by WoodWasteRepository.requests.collectAsState()
    val error by WoodWasteRepository.error.collectAsState()
    val loading by WoodWasteRepository.loading.collectAsState()
    val cached by WoodWasteRepository.fromCache.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importedJson by rememberSaveable { mutableStateOf("[]") }
    val imported =
        remember(importedJson) {
            val rows = org.json.JSONArray(importedJson)
            (0 until rows.length()).map { LegacyReports.decode(rows.getJSONObject(it).toString()) }
        }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    importing = true
                    try {
                        importedJson =
                            withContext(Dispatchers.IO) { LegacyReports.read(context, uri) }
                        message = "Archivo leído. Revisa cada reporte antes de compartirlo."
                    } catch (_: Exception) {
                        message =
                            "No se pudo importar. Elige un JSON con hasta cien reportes y menor de 1 MB."
                    } finally {
                        importing = false
                    }
                }
        }
    var local by remember { mutableStateOf<List<TreeReport>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pair<WithdrawalRequest, String>?>(null) }
    var transitionReason by rememberSaveable { mutableStateOf("") }
    var requestFilter by rememberSaveable { mutableStateOf("TODAS") }
    var showImport by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(user?.uid) { local = ReportRepository.getUserReports(user?.uid.orEmpty()) }
    fun transition(request: WithdrawalRequest, action: String) {
        transitionReason = ""
        pending = request to action
    }
    val mine = all.filter { it.authorId == user?.uid }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            ActivityHero(
                publications = mine.size,
                active = requests.count { it.isPending || it.isAccepted },
                completed = requests.count { it.isCompleted },
            )
        }
        if (loading || importing || busy) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(
                            when {
                                importing -> "Leyendo tus reportes…"
                                busy -> "Guardando el cambio…"
                                else -> "Cargando tu actividad…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (!AuthRepository.isAuthenticated) {
            item {
                ActivityEmptyCard(
                    Icons.Default.Lock,
                    "Tu actividad es privada",
                    "Inicia sesión para recuperar tus publicaciones y coordinar entregas.",
                )
            }
        }
        if (cached) {
            item { StatusMessage("Datos guardados en el teléfono. Las acciones necesitan conexión.") }
        }
        error?.let { problem ->
            item {
                StatusMessage(problem)
                TextButton(
                    onClick = { WoodWasteRepository.refreshSession() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver a intentar")
                }
            }
        }
        message?.let { text -> item { StatusMessage(text) } }
        item {
            ActivitySection(
                Icons.Default.Forest, "Mis publicaciones",
                "Hallazgos y materiales que compartes", mine.size,
            )
        }
        if (mine.isEmpty() && !loading && AuthRepository.isAuthenticated)
            item {
                ActivityEmptyCard(
                    Icons.Default.Spa,
                    "Aquí empieza una nueva oportunidad",
                    "Cuando publiques un hallazgo o material, podrás seguirlo y editarlo aquí.",
                ) {
                    OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Explore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Explorar comunidad")
                    }
                }
            }
        items(mine, key = { "item_${it.id}" }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActivityBadge(item.typedStatus.displayName)
                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${item.wasteType} · ${item.subaSector}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (item.entryType == "HALLAZGO") "Hallazgo comunitario · No reservable"
                        else item.formattedQuantity,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (item.status in listOf("HALLAZGO", "DISPONIBLE")) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = { onEdit(item) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Editar publicación")
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    WoodWasteRepository.updateWasteStatus(
                                            item.id,
                                            WasteStatus.INACTIVO,
                                        )
                                        .onSuccess {
                                            message = "Publicación retirada de la comunidad."
                                        }
                                        .onFailure { message = it.message }
                                    busy = false
                                }
                            },
                        ) {
                            Text("Retirar publicación", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    }
                }
            }
        }
        item {
            TextButton(onClick = { showImport = !showImport }) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Gestionar reportes antiguos") }
        }
        if (showImport) item {
            OutlinedCard(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Recupera tus reportes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Importa un archivo JSON. Podrás revisar cada registro antes de compartirlo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        enabled = !importing && !busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try { picker.launch(arrayOf("application/json", "text/plain")) }
                            catch (_: Exception) { message = "No se pudo abrir el selector de archivos." }
                        },
                    ) {
                        Icon(Icons.Default.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (importing) "Leyendo archivo…" else "Importar reportes")
                    }
                }
            }
        }
        if ((local + imported).isNotEmpty())
            item {
                ActivitySection(
                    Icons.Default.Inventory2, "Reportes de este teléfono",
                    "Revísalos antes de compartirlos",
                    (local + imported).distinctBy { it.id }.size,
                )
            }
        items((local + imported).distinctBy { it.id }, key = { "local_${it.id}" }) { report ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActivityBadge("Pendiente de revisión")
                    Text(
                        report.description.ifBlank { "Reporte guardado en el teléfono" },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = { onImport(report) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Revisar e importar")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null)
                    }
                }
            }
        }
        item {
            ActivitySection(
                Icons.Default.Handshake, "Solicitudes e historial",
                "Sigue cada paso de tus entregas", requests.size,
            )
        }
        if (requests.isEmpty() && !loading && AuthRepository.isAuthenticated)
            item {
                ActivityEmptyCard(
                    Icons.Default.Handshake,
                    "Las conexiones crecen aquí",
                    "Al solicitar un material o recibir una solicitud, podrás coordinar y confirmar la entrega desde este espacio.",
                )
            }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TODAS" to "Todas", "ACTIVAS" to "En proceso", "COMPLETADA" to "Completadas", "CANCELADA" to "Canceladas", "RECHAZADA" to "Rechazadas", "VENCIDA" to "Vencidas").forEach { (value,label) ->
                    FilterChip(selected = requestFilter == value, onClick = { requestFilter = value }, label = { Text(label) })
                }
            }
        }
        val visibleRequests = requests.filter { requestFilter == "TODAS" || (requestFilter == "ACTIVAS" && (it.isPending || it.isAccepted)) || it.status == requestFilter }
        if (visibleRequests.isEmpty() && requests.isNotEmpty()) item { Text("No hay solicitudes en este estado.") }
        items(visibleRequests, key = { "request_${it.id}" }) { request ->
            ActivityRequestCard(request, all.find { it.id == request.wasteId }, request.authorId == user?.uid, busy,
                transition = { transition(request, it) }, notify = { message = it })
        }
    }

    pending?.let { (request, action) ->
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = {
                Text(
                    when (action) {
                        "accept" -> "¿Reservar este material?"
                        "confirm" -> "¿Confirmar la entrega?"
                        "cancel" -> "¿Cancelar la solicitud?"
                        else -> "¿Rechazar la solicitud?"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (action == "confirm")
                            "Confirma solo si la entrega ocurrió. Cuando ambas personas confirmen se cerrará el retiro y quedará su comprobante privado."
                        else "La otra persona recibirá el aviso y verá el cambio en su actividad. Al cancelar una reserva, el material volverá a estar disponible."
                    )
                    if (action in listOf("cancel", "reject")) OutlinedTextField(transitionReason, { transitionReason = it.take(500) }, label = { Text("Motivo obligatorio") }, modifier = Modifier.fillMaxWidth())
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy && (action !in listOf("cancel", "reject") || transitionReason.isNotBlank()),
                    onClick = {
                        busy = true
                        scope.launch {
                            WoodWasteRepository.transition(request.id, action, reason = transitionReason)
                                .onSuccess { message = "Estado actualizado." }
                                .onFailure { message = it.message }
                            busy = false
                            pending = null
                        }
                    },
                ) {
                    Text(if (busy) "Guardando…" else "Confirmar")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pending = null }) { Text("Volver") }
            },
        )
    }
}


@Composable
private fun ActivityHero(publications: Int, active: Int, completed: Int) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                )))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ) {
                    Icon(
                        Icons.Default.Eco, null,
                        Modifier.padding(12.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Mi actividad", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Cada intercambio deja una nueva oportunidad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                "Tus publicaciones, solicitudes y comprobantes privados, en un mismo lugar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityStat(publications, "Publicaciones", Modifier.weight(1f))
                ActivityStat(active, "En proceso", Modifier.weight(1f))
                ActivityStat(completed, "Entregas", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value.toString(), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ActivitySection(icon: ImageVector, title: String, subtitle: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                icon, null, Modifier.padding(10.dp).size(22.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            count.toString(), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ActivityBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ActivityEmptyCard(
    icon: ImageVector,
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon, null, Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                title, style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.invoke()
        }
    }
}

