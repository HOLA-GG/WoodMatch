package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.network.PilotApi
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); var type by remember { mutableStateOf("FEEDBACK") }
    var subject by remember { mutableStateOf("") }; var detail by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }; var sending by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ayuda y participación", style = MaterialTheme.typography.headlineMedium)
        Text("Cuéntanos una incidencia, una propuesta, tu opinión o tu nivel de satisfacción.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
            OutlinedTextField(value = type.lowercase().replaceFirstChar { it.uppercase() }, onValueChange = {}, readOnly = true, label = { Text("Tipo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded, { expanded = false }) { listOf("INCIDENCIA", "FEEDBACK", "OPINION", "SATISFACCION").forEach { option -> DropdownMenuItem({ Text(option.lowercase().replaceFirstChar { it.uppercase() }) }, { type = option; expanded = false }) } }
        }
        OutlinedTextField(subject, { subject = it.take(120) }, label = { Text("Asunto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(detail, { detail = it.take(1500) }, label = { Text("Detalle") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(enabled = !sending && subject.isNotBlank() && detail.isNotBlank(), onClick = { sending = true; scope.launch { runCatching { PilotApi.call("createSupportCase", mapOf("id" to PilotApi.operationId(), "type" to type, "subject" to subject, "detail" to detail)) }.onSuccess { message = "Gracias. Tu caso fue registrado para seguimiento."; subject = ""; detail = "" }.onFailure { message = it.message ?: "No se pudo enviar el caso." }; sending = false } }, modifier = Modifier.fillMaxWidth()) { Text(if (sending) "Enviando…" else "Enviar") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
