package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.ui.pilot.MaterialNotifications
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpen: (String, String) -> Unit) {
    val scope=rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    suspend fun load() {
        loading=true
        runCatching { val list=PilotApi.call("myNotifications").getJSONArray("notifications"); rows=(0 until list.length()).map { list.getJSONObject(it) } }
            .onSuccess { error=null }.onFailure { error=it.message }
        loading=false
    }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Row {
            IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver") }
            Text("Notificaciones",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f))
            IconButton(enabled=!loading,onClick={scope.launch { load() }}) { Icon(Icons.Default.Refresh,"Actualizar avisos") }
        } }
        item { MaterialNotifications() }
        if(loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
        if(rows.isEmpty() && !loading) item { Text("Aquí aparecerán las novedades de tus solicitudes y las respuestas de soporte.") }
        items(rows,key={it.getString("id")}) { row ->
            Card(Modifier.fillMaxWidth().clickable { scope.launch {
                runCatching { PilotApi.call("markNotificationRead",mapOf("id" to row.getString("id"))) }
                    .onSuccess { onOpen(row.optString("requestId"),row.optString("caseId")) }.onFailure { error=it.message }
            } },colors=CardDefaults.cardColors(containerColor=if(row.optBoolean("read")) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(row.optString("title"),style=MaterialTheme.typography.titleMedium)
                    Text(row.optString("body"))
                    Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(row.optLong("createdAt"))),style=MaterialTheme.typography.labelSmall)
                    if(!row.optBoolean("read")) Text("Nuevo · Toca para ver",style=MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
