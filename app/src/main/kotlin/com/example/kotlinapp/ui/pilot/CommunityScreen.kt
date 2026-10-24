package com.example.kotlinapp.ui.pilot

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kotlinapp.R
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.screens.OsmNativeMapView
import kotlinx.coroutines.launch

@Composable
fun PilotHome(
    onMap: () -> Unit,
    onCreate: () -> Unit,
    onSpecies: () -> Unit,
    onCatalog: () -> Unit,
) {
    val user by AuthRepository.currentUser.collectAsState()
    val all by WoodWasteRepository.items.collectAsState()
    val available = all.count { it.isAvailable }
    val found = all.count { it.entryType == "HALLAZGO" && it.status == "HALLAZGO" }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.logo),
                    contentDescription = "WoodMatch",
                    modifier = Modifier.size(72.dp),
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "Hola, ${user?.name?.substringBefore(' ') ?: "explorador"}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Suba · Comunidad circular", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        Icons.Default.Eco,
                        null,
                        Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "La madera merece\notra oportunidad.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Conecta hallazgos, materiales y personas. Desde una poda hasta una estiba o un mueble recuperable.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(onClick = onCreate) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Crear publicación")
                    }
                }
            }
        }
        item {
            Text("Cerca de ti", style = MaterialTheme.typography.headlineMedium)
            Text(
                "$available materiales disponibles · $found hallazgos en la vista actual",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            ActionCard(
                "Explorar el mapa",
                "Encuentra madera y hallazgos por sector",
                Icons.Default.Map,
                onMap,
            )
        }
        item {
            ActionCard(
                "Materiales y hallazgos",
                "Estibas, tablas, muebles, podas y más",
                Icons.Default.Inventory2,
                onCatalog,
            )
        }
        item {
            ActionCard(
                "Guía botánica",
                "Explora especies, identifica plantas y consulta consejos",
                Icons.Default.LocalFlorist,
                onSpecies,
            )
        }
        if (!PilotApi.configured)
            item {
                StatusMessage(
                    "Esta versión aún necesita la conexión de la comunidad. Puedes explorar la guía botánica; las publicaciones estarán disponibles cuando se complete la configuración."
                )
            }
        item {
            Text("Recuperar empieza por observar", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Un hallazgo informa a la comunidad. Ofrecer material indica que tienes autorización para entregarlo."
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(
    mapFirst: Boolean = false,
    onCreate: () -> Unit,
    onEdit: (WoodWasteItem) -> Unit,
) {
    val all by WoodWasteRepository.items.collectAsState()
    val error by WoodWasteRepository.error.collectAsState()
    val cached by WoodWasteRepository.fromCache.collectAsState()
    var showMap by rememberSaveable(mapFirst) { mutableStateOf(mapFirst) }
    var filters by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("TODOS") }
    var category by rememberSaveable { mutableStateOf("TODOS") }
    var status by rememberSaveable { mutableStateOf("TODOS") }
    var sector by rememberSaveable { mutableStateOf("TODOS") }
    var material by rememberSaveable { mutableStateOf("TODOS") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val active = all.filter { it.status in listOf("HALLAZGO", "DISPONIBLE", "RESERVADO") }
    val visible =
        active.filter {
            (query.isBlank() ||
                "${it.title} ${it.description} ${it.wasteType}"
                    .contains(query, ignoreCase = true)) &&
                (kind == "TODOS" || it.entryType == kind) &&
                (category == "TODOS" || it.category == category) &&
                (status == "TODOS" || it.status == status) &&
                (sector == "TODOS" || it.subaSector == sector) &&
                (material == "TODOS" || it.wasteType == material)
        }
    val selected = all.find { it.id == selectedId }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (showMap) "Mapa de Suba" else "La comunidad comparte",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showMap = !showMap }) {
                Icon(
                    if (showMap) Icons.Default.ViewList else Icons.Default.Map,
                    if (showMap) "Ver lista accesible" else "Ver mapa",
                )
            }
            IconButton(onClick = { filters = !filters }) {
                Icon(Icons.Default.Tune, "Mostrar u ocultar filtros")
            }
        }
        if (!showMap || filters) OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar madera, muebles, estibas…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (filters)
            Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                ChoiceRow(
                    listOf("TODOS" to "Todos", "HALLAZGO" to "Hallazgos", "OFERTA" to "Ofertas"),
                    kind,
                ) {
                    kind = it
                }
                ChoiceRow(
                    listOf(
                        "TODOS" to "Cualquier estado",
                        "DISPONIBLE" to "Disponible",
                        "RESERVADO" to "Reservado",
                        "HALLAZGO" to "Hallazgo",
                    ),
                    status,
                ) {
                    status = it
                }
                ChoiceRow(
                    listOf("TODOS" to "Todo material") + MaterialKinds.map { it to it },
                    material,
                ) {
                    material = it
                }
                ChoiceRow(
                    listOf("TODOS" to "Todas las categorías") +
                        WasteCategory.entries.map { it.name to it.displayName },
                    category,
                ) {
                    category = it
                }
                ChoiceRow(
                    listOf("TODOS" to "Todos los sectores") + SubaSectors.names.map { it to it },
                    sector,
                ) {
                    sector = it
                }
            }
        if (!AuthRepository.isAuthenticated)
            Text("Inicia sesión para ver la comunidad y compartir material.")
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { WoodWasteRepository.refreshSession() }) { Text("Actualizar") }
        }
        if (cached)
            Text(
                "Mostrando datos guardados. Las acciones necesitan conexión.",
                style = MaterialTheme.typography.labelMedium,
            )
        if (!showMap || filters) Text(
            "${visible.size} publicaciones · Hasta 100 publicaciones comunitarias por vista",
            style = MaterialTheme.typography.labelMedium,
        )
        if (showMap) {
            OsmNativeMapView(
                wasteItems = visible,
                centerLat = 4.745,
                centerLng = -74.085,
                zoomLevel = 13,
                onWasteSelected = { selectedId = it.id },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            if (filters) Text(
                "Verde: disponible · Naranja: hallazgo o reservado. Toca un punto para ver el estado.",
                style = MaterialTheme.typography.labelSmall,
            )
        } else
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (visible.isEmpty())
                    item {
                        Text("Aún no hay publicaciones con estos filtros.")
                        TextButton(onClick = onCreate) { Text("Compartir un hallazgo o material") }
                    }
                items(visible, key = { it.id }) { item -> EntryCard(item) { selectedId = item.id } }
            }
    }
    if (selected != null)
        EntryDetail(
            selected,
            onDismiss = { selectedId = null },
            onEdit = {
                selectedId = null
                onEdit(it)
            },
        )
    else if (selectedId != null)
        AlertDialog(
            onDismissRequest = { selectedId = null },
            title = { Text("La publicación cambió") },
            text = {
                Text(
                    "Puede haber sido retirada o recolectada. Actualiza la comunidad para ver los materiales disponibles."
                )
            },
            confirmButton = { TextButton(onClick = { selectedId = null }) { Text("Entendido") } },
        )
}

@Composable
fun EntryCard(item: WoodWasteItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.typedStatus.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            Text("${item.wasteType} · ${item.subaSector}")
            Text(
                if (item.entryType == "HALLAZGO") "Hallazgo comunitario · No reservable"
                else item.formattedQuantity,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetail(
    item: WoodWasteItem,
    onDismiss: () -> Unit,
    onEdit: (WoodWasteItem) -> Unit,
) {
    val user by AuthRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var image by remember(item.id, item.imagePath) { mutableStateOf<String?>(null) }
    var message by remember(item.id) { mutableStateOf<String?>(null) }
    var requesting by rememberSaveable(item.id) { mutableStateOf(false) }
    var use by rememberSaveable(item.id) { mutableStateOf("") }
    var date by rememberSaveable(item.id) { mutableStateOf("") }
    var quantity by rememberSaveable(item.id) { mutableStateOf("") }
    var note by rememberSaveable(item.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val op = rememberSaveable(item.id, use, date, quantity, note) { PilotApi.operationId() }
    LaunchedEffect(item.id, item.imagePath) {
        runCatching { WoodWasteRepository.photoUrl(item) }.onSuccess { image = it }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                item.typedStatus.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(item.title, style = MaterialTheme.typography.headlineMedium)
            image?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Foto de ${item.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
            Text("${item.wasteType} · ${item.materialState}")
            Text(item.subaSector)
            if (item.entryType != "HALLAZGO") Text(item.formattedQuantity)
            if (item.description.isNotBlank()) Text(item.description)
            Text("Compartido por ${item.authorName}", style = MaterialTheme.typography.bodySmall)
            if (item.authorId == user?.uid && item.status in listOf("HALLAZGO", "DISPONIBLE"))
                Button(onClick = { onEdit(item) }) { Text("Editar o convertir publicación") }
            if (
                item.authorId != user?.uid &&
                    item.isAvailable &&
                    item.entryType != "HALLAZGO" &&
                    user?.canRequest == true
            ) {
                if (!requesting)
                    Button(onClick = { requesting = true }) { Text("Solicitar recolección") }
                else {
                    OutlinedTextField(
                        value = use,
                        onValueChange = { use = it.take(500) },
                        label = { Text("¿Para qué lo usarás?") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it.take(10) },
                        label = { Text("Fecha propuesta (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.take(100) },
                        label = { Text("Cantidad que necesitas") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(1000) },
                        label = { Text("Mensaje (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                WoodWasteRepository.request(item.id, use, date, quantity, note, op)
                                    .onSuccess {
                                        message =
                                            "Solicitud enviada. Sigue su estado en Mi actividad."
                                        requesting = false
                                    }
                                    .onFailure { message = it.message }
                                busy = false
                            }
                        },
                    ) {
                        Text(if (busy) "Enviando…" else "Enviar solicitud")
                    }
                }
            } else if (item.authorId != user?.uid && item.isAvailable && user?.canRequest != true)
                Text("Para solicitar material, elige perfil receptor o mixto en Ajustes.")
            message?.let { StatusMessage(it) }
            TextButton(onClick = onDismiss) { Text("Cerrar") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
