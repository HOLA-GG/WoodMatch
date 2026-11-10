package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.WoodWasteRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf("FEEDBACK") }
    var subject by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var operationId by rememberSaveable { mutableStateOf(PilotApi.operationId()) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    suspend fun load() {
        val response = PilotApi.call("mySupportCases").getJSONArray("cases")
        rows = (0 until response.length()).map { response.getJSONObject(it) }
    }
    fun refresh() {
        if (busy) return
        busy = true
        scope.launch { runCatching { load() }.onFailure { message = it.message }; busy = false }
    }
    LaunchedEffect(Unit) { busy = true; runCatching { load() }.onFailure { message = it.message }; busy = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text("Ayuda y participación", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { refresh() }, enabled = !busy) { Icon(Icons.Default.Refresh, "Actualizar casos") }
        }
        Text("Tu experiencia mejora WoodMatch. Consulta aquí las respuestas y el avance de tus casos.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SupportAgent, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Estamos para escucharte", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("Comparte una incidencia, una idea o tu experiencia. Aquí puedes seguir cada respuesta.", textAlign = TextAlign.Center)
            }
        }
        run {
            SupportChoice("Tipo", listOf("FEEDBACK","INCIDENCIA","OPINION","SATISFACCION"), type) { type = it }
            if (type == "SATISFACCION") {
                Text("¿Cómo fue tu experiencia? 1: muy insatisfecho · 5: muy satisfecho")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (1..5).forEach { n -> FilterChip(selected = rating == n, onClick = { rating = n }, label = { Text("$n ★") }); Spacer(Modifier.width(4.dp)) } }
            }
            OutlinedTextField(subject, { subject = it.take(120) }, label = { Text("Asunto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(detail, { detail = it.take(1500) }, label = { Text("¿Qué ocurrió o qué mejorarías?") }, supportingText = { Text("Incluye pasos y resultado esperado. Evita contraseñas y datos privados.") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && subject.isNotBlank() && detail.isNotBlank(), onClick = {
                if (busy) return@Button
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
        Text("Mi seguimiento", style = MaterialTheme.typography.titleLarge)
        if (rows.isEmpty() && !busy) Text("Aún no hay casos para mostrar.")
        rows.forEach { row ->
            key(row.getString("id"), row.optLong("updatedAt")) {
                SupportCaseCard(row, busy) { operation, data ->
                    if (busy) return@SupportCaseCard
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
private fun SupportCaseCard(row: JSONObject, busy: Boolean, action: (String, Map<String, Any?>) -> Unit) {
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
            run {
                if (response.isNotBlank()) { Text("Respuesta de soporte", style = MaterialTheme.typography.labelLarge); Text(response) }
                if (row.optString("status") in listOf("RESUELTO", "CERRADO")) {
                    OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("¿Por qué necesitas reabrir el caso?") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(enabled = !busy && reason.isNotBlank(), onClick = { action("reopenSupportCase", mapOf("id" to row.getString("id"), "reason" to reason)) }) { Text("Reabrir caso") }
                }
            }
            val history = row.optJSONArray("history")
            if (history != null && history.length() > 0) Text("Último movimiento: " + history.getJSONObject(history.length()-1).optString("action").replace('_',' '), style = MaterialTheme.typography.labelSmall)
        }
    }
}
