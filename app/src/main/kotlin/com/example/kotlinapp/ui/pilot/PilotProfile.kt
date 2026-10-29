package com.example.kotlinapp.ui.pilot

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.components.PrivacyNotice
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PilotProfile(
    seasonMode: String,
    appearance: String,
    textScale: Float,
    onSeason: (String) -> Unit,
    onAppearance: (String) -> Unit,
    onTextScale: (Float) -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val user by AuthRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val prefs = remember { PilotPreferences.store(context) }
    var diagnostics by remember { mutableStateOf(prefs.getBoolean("diagnostics", false)) }

    fun openExternal(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { message = "No se pudo abrir el enlace. Comprueba que tengas un navegador." }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Tu espacio", style = MaterialTheme.typography.headlineMedium)
        Text("Cuida tu perfil y haz WoodMatch más cómodo para ti.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ))).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
                        Icon(Icons.Outlined.Spa, null, Modifier.padding(12.dp).size(30.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(user?.name ?: "Explorador", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(user?.typedRole?.displayName ?: "Invitado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                if (AuthRepository.isAuthenticated) {
                    Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    OutlinedButton(onClick = onEditProfile, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Editar mi perfil")
                    }
                } else Text("Inicia sesión para compartir y coordinar materiales.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        message?.let { StatusMessage(it) }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Preparando tu solicitud…", style = MaterialTheme.typography.labelMedium)
        }

        if (AuthRepository.isAuthenticated) SettingsGroup(
            Icons.Outlined.Person, "Mi cuenta", "Los datos de tu participación en la comunidad",
        ) {
            ProfileDetail("Nombre", user?.name.orEmpty())
            ProfileDetail("Teléfono", user?.phone.orEmpty())
            ProfileDetail("Participación", user?.typedRole?.displayName.orEmpty())
            Text("El rol se define al registrarte. Las cuentas mixtas pueden crear y buscar materiales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onChangePassword, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cambiar contraseña")
            }
        }

        SettingsGroup(Icons.Outlined.Palette, "Apariencia", "Elige cómo quieres ver WoodMatch") {
            SettingOption("Sistema", "Sigue la apariencia de tu teléfono", Icons.Outlined.Settings,
                appearance == "SYSTEM") { onAppearance("SYSTEM") }
            SettingOption("Claro", "Superficies cálidas y detalles botánicos", Icons.Outlined.LightMode,
                appearance == "LIGHT") { onAppearance("LIGHT") }
            SettingOption("Oscuro", "Fondos suaves para ambientes con poca luz", Icons.Outlined.DarkMode,
                appearance == "DARK") { onAppearance("DARK") }
        }

        SettingsGroup(Icons.Outlined.Eco, "Una paleta para cada estación", "Colores que acompañan tu recorrido") {
            SettingOption("Automático", "Cambia según el mes del año", Icons.Outlined.AutoMode,
                seasonMode == "AUTO") { onSeason("AUTO") }
            SettingOption("Primavera", "Verdes frescos y brotes", Icons.Outlined.LocalFlorist,
                seasonMode == "SPRING") { onSeason("SPRING") }
            SettingOption("Verano", "Bosque y madera cálida", Icons.Outlined.WbSunny,
                seasonMode == "SUMMER") { onSeason("SUMMER") }
            SettingOption("Otoño", "Verdes profundos y acentos tierra", Icons.Outlined.Park,
                seasonMode == "AUTUMN") { onSeason("AUTUMN") }
            SettingOption("Invierno", "Tonos frescos y calma", Icons.Outlined.AcUnit,
                seasonMode == "SNOW") { onSeason("SNOW") }
            Text("Las estaciones son decorativas; no representan el clima de Bogotá.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsGroup(Icons.Outlined.AccessibilityNew, "Accesibilidad", "Un tamaño de lectura cómodo para ti") {
            SettingOption("Tamaño del sistema", "Respeta tu configuración de Android", Icons.Outlined.TextFields,
                textScale == 1f) { onTextScale(1f) }
            SettingOption("Ampliado · +15 %", "Más espacio para cada palabra", Icons.Outlined.TextIncrease,
                textScale == 1.15f) { onTextScale(1.15f) }
            SettingOption("Grande · +30 %", "Lectura más cómoda en toda la app", Icons.Outlined.FormatSize,
                textScale == 1.3f) { onTextScale(1.3f) }
            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Una nueva vida para la madera", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Así se verá el texto. Puedes cambiarlo cuando quieras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text("El aumento se suma al tamaño elegido en Android. También se respetan TalkBack y la escala de animaciones del sistema.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsGroup(Icons.Outlined.Shield, "Privacidad y datos", "Tú decides qué compartir") {
            CheckLine(diagnostics, "Compartir diagnósticos de fallos (opcional)") {
                diagnostics = it
                com.example.kotlinapp.data.network.PilotTelemetry.enabled = it
                prefs.edit().putBoolean("diagnostics", it).apply()
                if (FirebaseApp.getApps(context).isNotEmpty())
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(it)
            }
            Text("Incluyen versión y tipo de fallo. No añadimos fotos, coordenadas, códigos ni contenido de tus consultas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrivacyNotice()
            if (AuthRepository.isAuthenticated) OutlinedButton(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val result = PilotApi.call("exportAccount")
                            val file = withContext(Dispatchers.IO) {
                                File(File(context.cacheDir, "exports").apply { mkdirs() }, "mis-datos.json")
                                    .apply { writeText(result.toString(2)) }
                            }
                            val uri = FileProvider.getUriForFile(context,
                                "${context.packageName}.fileprovider", file)
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("application/json")
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                "Guardar mis datos",
                            ))
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            message = "No se pudieron exportar tus datos. Comprueba tu conexión."
                        } finally { busy = false }
                    }
                },
            ) {
                Icon(Icons.Outlined.Download, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportar mis datos")
            }
        }

        SettingsGroup(Icons.Outlined.Info, "Información y contacto", "WoodMatch · Comunidad circular de Suba") {
            Text("¿Necesitas ayuda?", style = MaterialTheme.typography.titleMedium)
            Text("Si algo falla, conserva el mensaje que aparece y compártelo con el responsable del piloto. Puedes añadir una captura sin datos personales.",
                style = MaterialTheme.typography.bodyMedium)
            if (AuthRepository.isAuthenticated) OutlinedButton(onClick = onSupport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.SupportAgent, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Feedback, incidencias y satisfacción")
            }
            ProfileDetail("Responsable del piloto", "cookiestech@woodmatch.org")
            HorizontalDivider()
            Text("Un pequeño desvío curioso", style = MaterialTheme.typography.titleMedium)
            Text("Una sorpresa fuera de WoodMatch. Es entretenimiento, no la página de soporte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { openExternal("https://theuselessweb.com/") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Explore, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rincón curioso", Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, "Abre un sitio externo", Modifier.size(18.dp))
            }
            Text("WoodMatch ${BuildConfig.VERSION_NAME} · Piloto académico",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            onClick = { AuthRepository.logout(); onLogout() },
        ) { Text(if (AuthRepository.isAuthenticated) "Cerrar sesión" else "Iniciar sesión") }
        if (AuthRepository.isAuthenticated) TextButton(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = { deleting = true },
        ) { Text("Eliminar mi cuenta", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
    }

    if (deleting) AlertDialog(
        onDismissRequest = { if (!busy) deleting = false },
        icon = { Icon(Icons.Outlined.DeleteOutline, null) },
        title = { Text("¿Eliminar tu cuenta?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Se eliminarán tu perfil, fotos y publicaciones. Los comprobantes de otras personas conservarán únicamente los datos necesarios del movimiento sin tu nombre. Es irreversible. Debes haber iniciado sesión en los últimos cinco minutos.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        AuthRepository.deleteCurrentAccount()
                            .onSuccess { onLogout() }
                            .onFailure { message = it.message }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } finally { busy = false; deleting = false }
                }
            }) { Text(if (busy) "Eliminando…" else "Eliminar definitivamente") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { deleting = false }) { Text("Conservar mi cuenta") }
        },
    )
}

@Composable
private fun SettingsGroup(icon: ImageVector, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium) {
                    Icon(icon, null, Modifier.padding(10.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingOption(title: String, detail: String, icon: ImageVector, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "Sin registrar" }, style = MaterialTheme.typography.bodyLarge)
    }
}
