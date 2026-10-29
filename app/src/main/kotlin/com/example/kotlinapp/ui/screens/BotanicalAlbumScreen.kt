package com.example.kotlinapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.kotlinapp.data.network.ForestAiService
import com.example.kotlinapp.data.network.PlantNetCandidate
import com.example.kotlinapp.data.network.PlantNetService
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.BotanicalAlbum
import com.example.kotlinapp.data.repository.BotanicalRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SpeciesScreen(onNavigateBack: () -> Unit) {
    val user by AuthRepository.currentUser.collectAsState()
    key(user?.uid.orEmpty()) { AccountBotanicalAlbum(onNavigateBack) }
}

@Composable
private fun AccountBotanicalAlbum(onNavigateBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val user by AuthRepository.currentUser.collectAsState(); val uid = user?.uid.orEmpty()
    var rows by remember(uid) { mutableStateOf<List<BotanicalRecord>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }; var tag by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }; var scanner by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<BotanicalRecord?>(null) }; var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    suspend fun reload() { rows = withContext(Dispatchers.IO) { BotanicalAlbum.read(context, uid) } }
    LaunchedEffect(uid) { if (uid.isNotBlank()) runCatching { reload() }.onFailure { message = "No se pudo abrir la colección local." } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) scope.launch {
        busy = true; runCatching { withContext(Dispatchers.IO) { BotanicalAlbum.export(context, uid, uri) } }.onSuccess { message = "Respaldo guardado con fotos y cuidados." }.onFailure { message = it.message }; busy = false
    } }
    val importAlbumLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch {
        busy = true; runCatching { val count = withContext(Dispatchers.IO) { BotanicalAlbum.importBackup(context, uid, uri) }; reload(); message = "$count registros añadidos, sin duplicar los existentes." }.onFailure { message = it.message }; busy = false
    } }
    val visible = rows.filter { (tag.isBlank() || tag in it.tags) && (query.isBlank() || "${it.common} ${it.scientific} ${it.tags.joinToString()}".contains(query, true)) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text("Mi álbum botánico", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Box { IconButton(onClick = { menu = true }, enabled = !busy && uid.isNotBlank()) { Icon(Icons.Default.MoreVert, "Gestionar colección") }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Exportar colección con fotos") }, onClick = { menu = false; export.launch("WoodMatch-album-botanico.zip") })
                    DropdownMenuItem(text = { Text("Importar respaldo") }, onClick = { menu = false; importAlbumLauncher.launch(arrayOf("application/zip", "application/octet-stream")) })
                }
            }
        } }
        item { Card { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.LocalFlorist, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Las plantas que encontraste", style = MaterialTheme.typography.headlineSmall)
            Text("Fotografía, identifica y elige qué conservar. El álbum es local y está separado por cuenta.")
            Button(enabled = AuthRepository.isAuthenticated && !busy, onClick = { scanner = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Escanear planta") }
        } } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Árbol" to Icons.Default.Forest, "Arbusto" to Icons.Default.Nature, "Otras plantas" to Icons.Default.Spa).forEach { (label, icon) ->
                Card(onClick = { tag = if (tag == label) "" else label }, modifier = Modifier.width(145.dp)) {
                    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.tertiaryContainer))).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text("${rows.count { label in it.tags }} registros" + if (tag == label) " · Seleccionado" else "", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } }
        item { OutlinedTextField(query, { query = it }, label = { Text("Buscar nombre común o científico") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tag.isBlank(), onClick = { tag = "" }, label = { Text("Todas (${rows.size})") })
            BotanicalAlbum.tags.forEach { option -> FilterChip(selected = tag == option, onClick = { tag = if (tag == option) "" else option }, label = { Text("$option (${rows.count { option in it.tags }})") }) }
        } }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        if (visible.isEmpty()) item { OutlinedCard { Column(Modifier.padding(20.dp)) {
            Text(if (rows.isEmpty()) "Tu próxima caminata puede iniciar esta colección" else "No hay coincidencias con estos filtros", style = MaterialTheme.typography.titleMedium)
            Text(if (rows.isEmpty()) "Guarda los resultados que quieras conservar. Los cuidados son opcionales." else "Prueba otro nombre o selecciona Todas.")
        } } }
        items(visible, key = { it.id }) { row -> Card(onClick = { selected = row }, modifier = Modifier.fillMaxWidth()) {
            AsyncImage(model = File(row.photo), contentDescription = "Tu fotografía de ${row.common}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(190.dp))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.common, style = MaterialTheme.typography.titleLarge); Text(row.scientific)
                Text(row.tags.joinToString(" · ").ifBlank { "Sin etiquetas" }, color = MaterialTheme.colorScheme.primary)
                Text(if (row.care.isBlank()) "Sin cuidados consultados" else "Cuidados guardados · Sin conexión", style = MaterialTheme.typography.labelMedium)
            }
        } }
        item { Text("Exporta un respaldo antes de cambiar de teléfono o desinstalar la app.", style = MaterialTheme.typography.bodySmall) }
    }
    if (scanner) BotanicalScanner(onDismiss = { scanner = false }, onSaved = { scope.launch { reload() }; message = "Registro guardado en tu colección." })
    selected?.let { record -> BotanicalRecordDetail(record, uid, onDismiss = { selected = null }, onUpdated = { updated -> selected = updated; scope.launch { reload() } }) }
}

@Composable
private fun BotanicalScanner(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val user by AuthRepository.currentUser.collectAsState(); val uid = user?.uid.orEmpty()
    var photo by rememberSaveable { mutableStateOf<String?>(null) }; var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<PlantNetCandidate>>(emptyList()) }; var candidate by remember { mutableStateOf<PlantNetCandidate?>(null) }
    var tags by remember { mutableStateOf(setOf<String>()) }; var organ by rememberSaveable { mutableStateOf("auto") }
    var care by remember { mutableStateOf("") }; var saved by remember { mutableStateOf<BotanicalRecord?>(null) }
    var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    fun reset(uri: Uri?) { photo = uri?.toString(); candidates = emptyList(); candidate = null; care = ""; saved = null; tags = emptySet(); message = null }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { reset(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success) reset(cameraPath?.let(Uri::parse)) }
    fun takePhoto() { runCatching {
        val folder = File(context.cacheDir, "camera").apply { mkdirs() }; val file = File.createTempFile("plant_", ".jpg", folder)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file); cameraPath = uri.toString(); camera.launch(uri)
    }.onFailure { message = "No se pudo abrir la cámara. Puedes elegir una imagen." } }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Escáner botánico") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1 · Fotografía una planta", style = MaterialTheme.typography.titleMedium)
            Text("Acerca la cámara a hojas, flores o corteza, con buena luz y sin otros objetos ocupando la imagen.")
            Row { OutlinedButton(enabled = !busy, onClick = { takePhoto() }) { Text("Cámara") }; Spacer(Modifier.width(8.dp)); OutlinedButton(enabled = !busy, onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Elegir imagen") } }
            photo?.let { AsyncImage(model = Uri.parse(it), contentDescription = "Fotografía seleccionada", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(170.dp)) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("auto" to "Automático", "leaf" to "Hoja", "flower" to "Flor", "fruit" to "Fruto", "bark" to "Corteza").forEach { (value, label) -> FilterChip(selected = organ == value, enabled = !busy && candidate == null, onClick = { organ = value }, label = { Text(label) }) }
            }
            if (photo != null && candidate == null) Button(enabled = !busy, onClick = { busy = true; message = null; scope.launch {
                PlantNetService.identifyFromUri(context, Uri.parse(photo), organ).onSuccess { result ->
                    candidates = result.candidates.filter { it.scientificName.isNotBlank() && it.score > 0 }; candidate = candidates.firstOrNull()
                    tags = mapOf("leaf" to "Hoja", "flower" to "Flor", "fruit" to "Fruto", "bark" to "Corteza")[organ]?.let { setOf(it) } ?: emptySet()
                    if (candidate == null) message = "No pudimos identificar una planta en esta imagen. Repite una foto cercana y enfocada de hojas, flores o corteza. No se guardó ningún registro."
                }.onFailure { message = it.message }; busy = false
            } }, modifier = Modifier.fillMaxWidth()) { Text("Identificar con Pl@ntNet") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            candidate?.let { current ->
                Text("2 · Revisa el resultado", style = MaterialTheme.typography.titleMedium)
                Text(current.primaryCommonName, style = MaterialTheme.typography.titleLarge); Text(current.scientificName)
                Text("Coincidencia Pl@ntNet: ${current.scorePercentage} %. Es una propuesta, no una certeza.")
                if (candidates.size > 1) candidates.take(3).forEach { option -> TextButton(enabled = !busy && saved == null, onClick = { candidate = option; care = "" }) { Text("${option.scientificName} · ${option.scorePercentage}%") } }
                Text("Organiza tu álbum", style = MaterialTheme.typography.titleMedium)
                Text("Combina etiquetas como Arbusto y Flor. Elige la forma de la planta; no la deducimos de una foto parcial.", style = MaterialTheme.typography.bodySmall)
                BotanicalAlbum.tags.chunked(3).forEach { group -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { group.forEach { label -> FilterChip(selected = label in tags, enabled = saved == null && !busy, onClick = { tags = if (label in tags) tags - label else tags + label }, label = { Text(label) }) } } }
                Text("3 · Decide qué conservar", style = MaterialTheme.typography.titleMedium)
                if (care.isNotBlank()) Text(care)
                OutlinedButton(enabled = !busy && care.isBlank(), onClick = { busy = true; scope.launch {
                    ForestAiService.getCandidateCareGuide(current.scientificName, current.primaryCommonName, current.score).onSuccess { text ->
                        care = text
                        saved?.let { record -> runCatching { withContext(Dispatchers.IO) { BotanicalAlbum.updateCare(context, uid, record.id, text) }; onSaved() }.onFailure { message = "Los cuidados se generaron, pero no se pudieron guardar. " + it.message } }
                    }.onFailure { message = it.message }; busy = false
                } }, modifier = Modifier.fillMaxWidth()) { Text(if (care.isBlank()) "Consultar cuidados (Gemini)" else "Cuidados guardados") }
                Button(enabled = !busy && saved == null, onClick = { busy = true; scope.launch {
                    runCatching { saved = withContext(Dispatchers.IO) { BotanicalAlbum.save(context, uid, Uri.parse(photo), current, tags.toList(), care) }; onSaved(); message = "Guardado. Puedes volver a leerlo sin repetir consultas." }.onFailure { message = it.message }; busy = false
                } }, modifier = Modifier.fillMaxWidth()) { Text(if (saved == null) "Guardar en mi colección" else "Guardado en mi colección") }
            }
            if (photo != null) OutlinedButton(enabled = !busy, onClick = { reset(null); takePhoto() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Text("Repetir foto") }
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
private fun BotanicalRecordDetail(record: BotanicalRecord, uid: String, onDismiss: () -> Unit, onUpdated: (BotanicalRecord) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(record.common) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AsyncImage(model = File(record.photo), contentDescription = "Tu fotografía de ${record.common}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(200.dp))
        Text(record.scientific, style = MaterialTheme.typography.titleMedium); Text(record.tags.joinToString(" · "))
        Text("Coincidencia Pl@ntNet: ${(record.score * 100).toInt()} % · Identificación orientativa")
        Text("Encontrada el " + java.text.DateFormat.getDateInstance().format(java.util.Date(record.createdAt)), style = MaterialTheme.typography.labelMedium)
        if (record.care.isNotBlank()) { Text("Cuidados guardados", style = MaterialTheme.typography.titleMedium); Text(record.care) }
        else { Text("Tu foto e identificación están guardadas. Los cuidados son opcionales.")
            Button(enabled = !busy, onClick = { busy = true; scope.launch {
                ForestAiService.getCandidateCareGuide(record.scientific, record.common, record.score).onSuccess { care ->
                    runCatching { withContext(Dispatchers.IO) { BotanicalAlbum.updateCare(context, uid, record.id, care) }; onUpdated(record.copy(care = care)) }
                        .onFailure { message = "No se pudieron guardar los cuidados. " + it.message }
                }.onFailure { message = it.message }; busy = false
            } }) { Text(if (busy) "Consultando…" else "Consultar cuidados (Gemini)") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cerrar") } })
}
