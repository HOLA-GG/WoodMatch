package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.network.PilotApi
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SupportHomeScreen(onCases: () -> Unit, onModeration: () -> Unit, onNotifications: () -> Unit = {}) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true
        runCatching {
            check(PilotApi.call("supportAccess").optBoolean("operator")) { "Esta cuenta no tiene acceso a soporte." }
            val array = PilotApi.call("adminSupportCases").getJSONArray("cases")
            rows = (0 until array.length()).map { array.getJSONObject(it) }
        }.onSuccess { error = null }.onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    val open = rows.filter { it.optString("status") !in listOf("RESUELTO", "CERRADO") }
    val ratings = rows.filter { it.optString("type") == "SATISFACCION" }.map { it.optInt("rating") }.filter { it in 1..5 }
    val responseHours = rows.mapNotNull { row ->
        val history = row.optJSONArray("history") ?: return@mapNotNull null
        (0 until history.length()).map { history.getJSONObject(it) }
            .firstOrNull { it.optString("actorId") != row.optString("reporterId") }
            ?.let { (it.optLong("at") - row.optLong("createdAt")).coerceAtLeast(0) / 3600000.0 }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.SupportAgent, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Row {
                    Text("Centro de soporte", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onNotifications) { Icon(Icons.Default.Notifications, "Notificaciones de soporte") }
                }
                Text("Cookiestech · Atención y moderación")
                Text("Escucha a la comunidad, sigue cada incidencia y registra las decisiones sobre publicaciones.")
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Tu bandeja de hoy", style = MaterialTheme.typography.titleLarge)
        SupportMetric("Casos abiertos", open.size.toString())
        SupportMetric("Prioridad alta o crítica", open.count { it.optString("priority") in listOf("ALTA","CRITICA") }.toString())
        SupportMetric("Publicaciones por revisar", open.count { it.optString("type") == "MODERACION" }.toString())
        SupportMetric("Satisfacción recibida", if (ratings.isEmpty()) "Sin valoraciones" else String.format(java.util.Locale.getDefault(), "%.1f / 5 · %d respuestas", ratings.average(), ratings.size))
        SupportMetric("Primera atención registrada", if (responseHours.isEmpty()) "Sin datos suficientes" else String.format(java.util.Locale.getDefault(), "%.1f h de media", responseHours.average()))
        Text("Indicadores calculados sobre los casos cargados en la bandeja.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onCases, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Default.Inbox, null); Spacer(Modifier.width(8.dp)); Text("Atender casos") }
        OutlinedButton(onClick = onModeration, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Default.Gavel, null); Spacer(Modifier.width(8.dp)); Text("Revisar publicaciones reportadas") }
        TextButton(onClick = { scope.launch { load() } }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Actualizar resumen") }
    }
}

@Composable
private fun SupportMetric(label: String, value: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
