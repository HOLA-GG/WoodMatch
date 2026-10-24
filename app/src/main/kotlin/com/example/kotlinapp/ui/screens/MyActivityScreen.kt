package com.example.kotlinapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.components.WithdrawalPassActions
import com.example.kotlinapp.ui.pilot.EntryCard
import com.example.kotlinapp.ui.pilot.StatusMessage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
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
                    try {
                        importedJson =
                            withContext(Dispatchers.IO) { LegacyReports.read(context, uri) }
                    } catch (_: Exception) {
                        message =
                            "No se pudo importar. Elige un JSON con hasta cien reportes y menor de 1 MB."
                    }
                }
        }
    var local by remember { mutableStateOf<List<TreeReport>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pair<WithdrawalRequest, String>?>(null) }
    LaunchedEffect(user?.uid) { local = ReportRepository.getUserReports(user?.uid.orEmpty()) }
    fun transition(request: WithdrawalRequest, action: String) {
        pending = request to action
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text("Mi actividad", style = MaterialTheme.typography.headlineMedium)
            Text("Publicaciones, solicitudes y comprobantes privados.")
            if (!AuthRepository.isAuthenticated) Text("Inicia sesión para recuperar tu actividad.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { StatusMessage(it) }
            Text("Mis publicaciones", style = MaterialTheme.typography.titleLarge)
        }
        val mine = all.filter { it.authorId == user?.uid }
        if (mine.isEmpty())
            item {
                Text("Tu próxima publicación puede darle otra vida a la madera.")
                TextButton(onClick = onExplore) { Text("Explorar comunidad") }
            }
        items(mine, key = { "item_${it.id}" }) { item ->
            Column {
                EntryCard(item) {
                    if (item.status in listOf("HALLAZGO", "DISPONIBLE")) onEdit(item)
                }
                if (item.status in listOf("HALLAZGO", "DISPONIBLE"))
                    FlowRow {
                        TextButton(onClick = { onEdit(item) }) { Text("Editar o convertir") }
                        TextButton(
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
                            Text("Retirar publicación")
                        }
                    }
            }
        }
        item {
            TextButton(onClick = { picker.launch(arrayOf("application/json", "text/plain")) }) {
                Text("Importar reportes desde un archivo")
            }
        }
        if (local.isNotEmpty())
            item {
                Text("Reportes de este teléfono", style = MaterialTheme.typography.titleLarge)
                Text("Revísalos antes de compartirlos. No se publicarán automáticamente.")
            }
        items((local + imported).distinctBy { it.id }, key = { "local_${it.id}" }) { report ->
            OutlinedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(report.description)
                    TextButton(onClick = { onImport(report) }) { Text("Revisar e importar") }
                }
            }
        }
        item { Text("Solicitudes e historial", style = MaterialTheme.typography.titleLarge) }
        if (requests.isEmpty())
            item {
                Text(
                    "Todavía no hay solicitudes. Cuando coordines una entrega, podrás seguirla aquí."
                )
            }
        items(requests, key = { "request_${it.id}" }) { request ->
            val owner = request.authorId == user?.uid
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        all.find { it.id == request.wasteId }?.title ?: request.wasteTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(request.typedStatus.displayName, color = MaterialTheme.colorScheme.primary)
                    if (request.proposedDate.isNotBlank())
                        Text("Fecha propuesta: ${request.proposedDate}")
                    if (request.requestedQuantity.isNotBlank())
                        Text("Cantidad solicitada: ${request.requestedQuantity}")
                    if (owner && request.intendedUse.isNotBlank())
                        Text("Uso previsto: ${request.intendedUse}")
                    if (request.message.isNotBlank()) Text(request.message)
                    if (request.isPending && owner)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !busy, onClick = { transition(request, "accept") }) {
                                Text("Aceptar")
                            }
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { transition(request, "reject") },
                            ) {
                                Text("Rechazar")
                            }
                        }
                    WithdrawalPassActions(request, owner)
                    if (request.isAccepted && request.passValidatedAt != null) {
                        val confirmed =
                            if (owner) request.authorConfirmedAt != null
                            else request.requesterConfirmedAt != null
                        if (confirmed)
                            Text("Tu confirmación quedó registrada. Esperando a la otra persona.")
                        else
                            Button(enabled = !busy, onClick = { transition(request, "confirm") }) {
                                Text(
                                    if (owner) "Confirmar que entregué" else "Confirmar que recibí"
                                )
                            }
                    }
                    if (request.isPending || request.isAccepted)
                        TextButton(enabled = !busy, onClick = { transition(request, "cancel") }) {
                            Text("Cancelar solicitud o reserva")
                        }
                    if (request.isCompleted) {
                        Text(
                            "Entrega confirmada por ambas personas. Ya no aparece entre los materiales disponibles."
                        )
                        request.receipt?.let { receipt ->
                            Text(
                                "Comprobante ${request.id.take(8)} · ${receipt["quantity"] ?: ""} ${receipt["unit"] ?: ""}"
                            )
                            TextButton(
                                onClick = {
                                    try {
                                        val folder =
                                            File(context.cacheDir, "exports").apply { mkdirs() }
                                        val file =
                                            File(folder, "comprobante-${request.id}.json").apply {
                                                writeText(JSONObject(receipt).toString(2))
                                            }
                                        val uri =
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file,
                                            )
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND)
                                                    .setType("application/json")
                                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                                    .addFlags(
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    ),
                                                "Guardar comprobante",
                                            )
                                        )
                                    } catch (_: Exception) {
                                        message = "No se pudo compartir el comprobante."
                                    }
                                }
                            ) {
                                Text("Guardar o compartir comprobante")
                            }
                            Text(
                                "Comprobante de entrega; no es una factura fiscal.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
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
                Text(
                    if (action == "confirm")
                        "Confirma solo si la entrega ocurrió. Cuando ambas personas confirmen se cerrará el retiro y quedará su comprobante privado."
                    else "La otra persona verá el cambio de estado en su actividad."
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            WoodWasteRepository.transition(request.id, action)
                                .onSuccess { message = "Estado actualizado." }
                                .onFailure { message = it.message }
                            busy = false
                            pending = null
                        }
                    },
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pending = null }) { Text("Volver") }
            },
        )
    }
}
