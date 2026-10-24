package com.example.kotlinapp.ui.pilot

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.components.PrivacyNotice
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PilotProfile(
    seasonMode: String,
    appearance: String,
    onSeason: (String) -> Unit,
    onAppearance: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val user by AuthRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember(user) { mutableStateOf(user?.name.orEmpty()) }
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var role by remember(user) { mutableStateOf(user?.role ?: "GENERADOR") }
    var sector by remember(user) { mutableStateOf(user?.sectorSuba ?: "Suba Centro") }
    var busy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val prefs = remember { PilotPreferences.store(context) }
    var diagnostics by remember { mutableStateOf(prefs.getBoolean("diagnostics", false)) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tu espacio en WoodMatch", style = MaterialTheme.typography.headlineMedium)
        Text(user?.name ?: "Explorador", style = MaterialTheme.typography.titleLarge)
        message?.let { StatusMessage(it) }
        if (AuthRepository.isAuthenticated) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(120) },
                label = { Text("Nombre o empresa") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.take(20) },
                label = { Text("Teléfono (+57…)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("¿Cómo participas?")
            ChoiceRow(
                listOf(
                    "GENERADOR" to "Ofrezco material",
                    "RECEPTOR" to "Recojo material",
                    "MIXTO" to "Ambas actividades",
                ),
                role,
            ) {
                role = it
            }
            Text("Tu sector")
            ChoiceRow(SubaSectors.names.map { it to it }, sector) { sector = it }
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        AuthRepository.updateCurrentUserProfile(name, phone, sector, role)
                            .onSuccess { message = "Perfil actualizado." }
                            .onFailure {
                                message =
                                    "No se pudo actualizar tu perfil. Revisa los datos y tu conexión."
                            }
                        busy = false
                    }
                },
            ) {
                Text("Guardar perfil")
            }
        }
        HorizontalDivider()
        Text("Una paleta para cada estación", style = MaterialTheme.typography.headlineSmall)
        Text("El modo automático sigue un calendario decorativo; no representa el clima de Bogotá.")
        ChoiceRow(
            listOf(
                "AUTO" to "◷ Automático",
                "SPRING" to "🌱 Primavera",
                "SUMMER" to "🌿 Verano",
                "AUTUMN" to "🍁 Otoño",
                "SNOW" to "❄ Invierno",
            ),
            seasonMode,
            onSeason,
        )
        Text("Apariencia", style = MaterialTheme.typography.titleLarge)
        ChoiceRow(
            listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Oscuro"),
            appearance,
            onAppearance,
        )
        Text("Accesibilidad", style = MaterialTheme.typography.titleLarge)
        Text(
            "WoodMatch respeta el tamaño de texto, TalkBack y la escala de animaciones de Android. El mapa también ofrece una lista accesible."
        )
        CheckLine(diagnostics, "Compartir diagnósticos de fallos para mejorar la app (opcional)") {
            diagnostics = it
            com.example.kotlinapp.data.network.PilotTelemetry.enabled = it
            prefs.edit().putBoolean("diagnostics", it).apply()
            if (FirebaseApp.getApps(context).isNotEmpty())
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(it)
        }
        Text(
            "Los diagnósticos incluyen versión y tipo de fallo. No añadimos fotos, coordenadas, códigos ni contenido de tus consultas.",
            style = MaterialTheme.typography.bodySmall,
        )
        PrivacyNotice()
        if (AuthRepository.isAuthenticated) {
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val result = PilotApi.call("exportAccount")
                            val file =
                                withContext(Dispatchers.IO) {
                                    File(
                                            File(context.cacheDir, "exports").apply { mkdirs() },
                                            "mis-datos.json",
                                        )
                                        .apply { writeText(result.toString(2)) }
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
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    "Guardar mis datos",
                                )
                            )
                        } catch (_: Exception) {
                            message = "No se pudieron exportar tus datos. Comprueba tu conexión."
                        } finally {
                            busy = false
                        }
                    }
                },
            ) {
                Text("Exportar mis datos")
            }
            TextButton(enabled = !busy, onClick = { deleting = true }) {
                Text("Eliminar mi cuenta")
            }
        }
        Button(
            enabled = !busy,
            onClick = {
                AuthRepository.logout()
                onLogout()
            },
        ) {
            Text(if (AuthRepository.isAuthenticated) "Cerrar sesión" else "Iniciar sesión")
        }
        Text(
            "WoodMatch ${BuildConfig.VERSION_NAME} · Piloto académico",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(24.dp))
    }
    if (deleting)
        AlertDialog(
            onDismissRequest = { if (!busy) deleting = false },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Text(
                    "Se eliminarán tu perfil, fotos y publicaciones. Los comprobantes de otras personas conservarán únicamente los datos necesarios del movimiento sin tu nombre. Es irreversible. Debes haber iniciado sesión en los últimos cinco minutos."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            AuthRepository.deleteCurrentAccount()
                                .onSuccess { onLogout() }
                                .onFailure { message = it.message }
                            busy = false
                            deleting = false
                        }
                    },
                ) {
                    Text("Eliminar definitivamente")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { deleting = false }) {
                    Text("Conservar mi cuenta")
                }
            },
        )
}
