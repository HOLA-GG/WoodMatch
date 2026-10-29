package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.WoodWasteRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import coil.compose.AsyncImage

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf("FEEDBACK") }
    var subject by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var operationId by rememberSaveable { mutableStateOf(PilotApi.operationId()) }
    var operator by remember { mutableStateOf(false) }
    var inbox by rememberSaveable { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    suspend fun load() {
        val access = PilotApi.call("supportAccess")
        operator = access.optBoolean("operator")
        val response = PilotApi.call(if (inbox && operator) "adminSupportCases" else "mySupportCases").getJSONArray("cases")
        rows = (0 until response.length()).map { response.getJSONObject(it) }
    }
    fun refresh() { scope.launch { busy = true; runCatching { load() }.onFailure { message = it.message }; busy = false } }
    LaunchedEffect(inbox) { busy = true; runCatching { load() }.onFailure { message = it.message }; busy = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text("Ayuda y participación", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { refresh() }, enabled = !busy) { Icon(Icons.Default.Refresh, "Actualizar casos") }
        }
        Text("Tu experiencia mejora WoodMatch. Consulta aquí las respuestas y el avance de tus casos.")
        if (operator) {
            Text("Soporte y moderación", style = MaterialTheme.typography.titleMedium)
            Text("Prioriza, investiga, responde y confirma la solución antes de cerrar. Los reportes de contenido se revisan con un motivo registrado.")
            Row {
                FilterChip(selected = !inbox, onClick = { inbox = false }, label = { Text("Mis casos") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = inbox, onClick = { inbox = true }, label = { Text("Bandeja de soporte") })
            }
        }
        if (!inbox) {
            SupportChoice("Tipo", listOf("FEEDBACK","INCIDENCIA","OPINION","SATISFACCION"), type) { type = it }
            if (type == "SATISFACCION") {
                Text("¿Cómo fue tu experiencia? 1: muy insatisfecho · 5: muy satisfecho")
                Row { (1..5).forEach { n -> FilterChip(selected = rating == n, onClick = { rating = n }, label = { Text("$n ★") }); Spacer(Modifier.width(4.dp)) } }
            }
            OutlinedTextField(subject, { subject = it.take(120) }, label = { Text("Asunto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(detail, { detail = it.take(1500) }, label = { Text("¿Qué ocurrió o qué mejorarías?") }, supportingText = { Text("Incluye pasos y resultado esperado. Evita contraseñas y datos privados.") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && subject.isNotBlank() && detail.isNotBlank(), onClick = {
                busy = true
                scope.launch {
                    runCatching {
                        val result = PilotApi.call("createSupportCase", mapOf("id" to operationId, "type" to type, "subject" to subject, "detail" to detail, "rating" to rating))
                        subject = ""; detail = ""; operationId = PilotApi.operationId()
                        message = "Caso registrado: " + result.getString("id").take(8)
                        load()
                    }.onFailure { message = it.message }
                    busy = false
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Enviar caso") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Text(if (inbox) "Casos de la comunidad" else "Mi seguimiento", style = MaterialTheme.typography.titleLarge)
        if (rows.isEmpty() && !busy) Text("Aún no hay casos para mostrar.")
        rows.forEach { row ->
            key(row.getString("id"), row.optLong("updatedAt")) {
                SupportCaseCard(row, operator && inbox, busy) { operation, data ->
                    busy = true
                    scope.launch {
                        runCatching { PilotApi.call(operation, data); WoodWasteRepository.refreshSession(); load() }
                            .onSuccess { message = "Cambio guardado." }.onFailure { message = it.message }
                        busy = false
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SupportChoice(label: String, values: List<String>, selected: String, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("$label: " + selected.replace('_', ' ')); Icon(Icons.Default.ExpandMore, null) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value -> DropdownMenuItem(text = { Text(value.replace('_', ' ')) }, onClick = { change(value); expanded = false }) }
        }
    }
}

@Composable
private fun SupportCaseCard(row: JSONObject, admin: Boolean, busy: Boolean, action: (String, Map<String, Any?>) -> Unit) {
    val scope = rememberCoroutineScope()
    var publication by remember { mutableStateOf<JSONObject?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(row.optString("status")) }
    var priority by remember { mutableStateOf(row.optString("priority")) }
    var response by remember { mutableStateOf(row.optString("response")) }
    var reason by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.optString("subject"), style = MaterialTheme.typography.titleMedium)
            Text(row.optString("type") + " · " + row.optString("status").replace('_',' ') + " · " + row.optString("priority"))
            Text("Caso " + row.optString("id").take(8), style = MaterialTheme.typography.labelSmall)
            Text(row.optString("detail"))
            if (row.optString("type") == "SATISFACCION") Text("Satisfacción: " + row.optInt("rating") + "/5")
            if (row.optString("publicationId").isNotBlank()) Text("Publicación: " + row.optString("publicationTitle"))
            if (admin) {
                SupportChoice("Estado", listOf("NUEVO","EN_REVISION","EN_PROGRESO","RESUELTO","CERRADO"), status) { status = it }
                SupportChoice("Prioridad", listOf("BAJA","MEDIA","ALTA","CRITICA"), priority) { priority = it }
                OutlinedTextField(response, { response = it.take(1500) }, label = { Text("Respuesta o solución para el usuario") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy, onClick = { action("updateSupportCase", mapOf("id" to row.getString("id"), "status" to status, "priority" to priority, "response" to response, "expectedUpdatedAt" to row.optLong("updatedAt"))) }) { Text("Guardar seguimiento") }
                if (row.optString("publicationId").isNotBlank()) {
                    TextButton(onClick = { scope.launch {
                        runCatching { PilotApi.call("supportPublication", mapOf("id" to row.optString("publicationId"))) }
                            .onSuccess { publication = it; previewError = null }.onFailure { previewError = it.message }
                    } }) { Text("Revisar publicación y fotografía") }
                    publication?.let { entry ->
                        Text(entry.optString("title") + " · " + entry.optString("status"))
                        Text(entry.optString("description"))
                        if (!entry.isNull("url")) AsyncImage(model = entry.optString("url"), contentDescription = "Fotografía de la publicación reportada", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                    previewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("Motivo de moderación") }, modifier = Modifier.fillMaxWidth())
                    Row {
                        listOf("OCULTAR","RESTAURAR").forEach { decision ->
                            TextButton(enabled = !busy && reason.isNotBlank(), onClick = { action("moderateEntry", mapOf("id" to row.optString("publicationId"), "caseId" to row.getString("id"), "reason" to reason, "action" to decision)) }) { Text(decision.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                    Text("Las publicaciones reservadas o recolectadas requieren revisar su flujo antes de moderar.", style = MaterialTheme.typography.bodySmall)
                }
            } else if (response.isNotBlank()) { Text("Respuesta de soporte", style = MaterialTheme.typography.labelLarge); Text(response) }
            val history = row.optJSONArray("history")
            if (history != null && history.length() > 0) Text("Último movimiento: " + history.getJSONObject(history.length()-1).optString("action").replace('_',' '), style = MaterialTheme.typography.labelSmall)
        }
    }
}
