package com.example.kotlinapp.ui.pilot

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kotlinapp.data.network.PilotApi
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MaterialNotifications() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun register(enabled: Boolean) {
        busy = true
        scope.launch {
            runCatching {
                val messaging = FirebaseMessaging.getInstance()
                val token = messaging.token.await()
                PilotApi.call("registerNotificationDevice", mapOf("token" to token, "enabled" to enabled))
                context.getSharedPreferences("material_alerts", 0).edit()
                    .putString("uid", if (enabled) FirebaseAuth.getInstance().currentUser?.uid else "")
                    .putBoolean("enabled", enabled).apply()
                messaging.isAutoInitEnabled = enabled
                if (!enabled) messaging.deleteToken().await()
            }.onSuccess { message = if (enabled) "Avisos activados para este dispositivo." else "Avisos desactivados." }
                .onFailure { message = "No se pudo actualizar: " + it.message }
            busy = false
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) register(true) else message = "Puedes seguir buscando materiales y activar los avisos cuando quieras."
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("material_alerts", 0)
        if (prefs.getBoolean("enabled", false) && prefs.getString("uid", "") == FirebaseAuth.getInstance().currentUser?.uid) register(true)
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
            Text("Entérate cuando llegue material", style = MaterialTheme.typography.titleMedium)
            Text("Activa avisos de nuevas ofertas para receptores y perfiles mixtos. Puedes desactivarlos aquí.")
            Row {
                TextButton(enabled = !busy, onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else register(true)
                }) { Text("Activar avisos") }
                TextButton(enabled = !busy, onClick = { register(false) }) { Text("Desactivar") }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
