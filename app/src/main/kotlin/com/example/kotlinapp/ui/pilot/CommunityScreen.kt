package com.example.kotlinapp.ui.pilot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
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
    onEntities: () -> Unit = {},
) {
    val user by AuthRepository.currentUser.collectAsState()
    val receiver = user?.typedRole == UserRole.RECEPTOR
    val mixed = user?.typedRole == UserRole.MIXTO
    val all by WoodWasteRepository.items.collectAsState()
    val latest = all.filter { it.status in listOf("DISPONIBLE", "HALLAZGO") && !it.imagePath.isNullOrBlank() }.maxByOrNull { it.createdAt }
    var heroImage by remember(latest?.id, latest?.imagePath) { mutableStateOf<String?>(null) }
    LaunchedEffect(latest?.id, latest?.imagePath) { heroImage = latest?.let { runCatching { WoodWasteRepository.photoUrl(it) }.getOrNull() } }
    val available = all.count { it.isAvailable }
    val found = all.count { it.entryType == "HALLAZGO" && it.status == "HALLAZGO" }
    val myContributions = all.count { it.authorId == user?.uid }
    val myCompleted = all.count { it.authorId == user?.uid && it.status == "APROVECHADO" }
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
            Card(onClick = if (receiver || mixed) onCatalog else onEntities, shape = MaterialTheme.shapes.extraLarge) {
                Box(Modifier.fillMaxWidth().height(210.dp).background(MaterialTheme.colorScheme.secondaryContainer)) {
                    if ((receiver || mixed) && heroImage != null) AsyncImage(model = heroImage, contentDescription = latest?.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(if (receiver || mixed) Icons.Default.Forest else Icons.Default.Business, null, Modifier.align(Alignment.Center).size(88.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Surface(Modifier.align(Alignment.BottomStart).padding(12.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (receiver || mixed) "Lo último de la comunidad" else "Conecta con entidades de Suba", style = MaterialTheme.typography.labelLarge)
                            Text(if (receiver || mixed) latest?.title ?: "Cada material abre una posibilidad" else "Madera, carpintería y nuevas oportunidades", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(62.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("$myContributions", style = MaterialTheme.typography.titleLarge)
                                Text("aportes", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text("Tu huella circular", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                if (myContributions == 0) "Tu primer hallazgo puede abrir una nueva ruta de aprovechamiento."
                                else "${myCompleted} aportes ya cerraron su ciclo en la comunidad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Text(
                        "La madera merece\notra oportunidad.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        if (receiver) "Encuentra materiales en Suba, solicita lo que puedes aprovechar y coordina su recolección. No necesitas publicar para recibir."
                        else "Conecta hallazgos, materiales y personas. Desde una poda hasta una estiba o un mueble recuperable.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    SeasonalAction(
                        onClick = if (receiver) onCatalog else onCreate,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(if (receiver) Icons.Default.Search else Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (receiver) "Buscar materiales" else "Crear publicación")
                    }
                    if (mixed) {
                        OutlinedButton(
                            onClick = onCatalog,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Default.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Buscar materiales")
                        }
                    }
                    if (receiver) {
                        TextButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                            Text("Reportar un hallazgo (opcional)")
                        }
                    }
                }
            }
        }
        if (receiver || mixed) item { MaterialNotifications() }
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
                "Entidades de Suba",
                "Conoce negocios locales y consulta sus condiciones directamente",
                Icons.Default.Business,
                onEntities,
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
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(25.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SeasonalAction(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Row(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)))
            .padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(
    mapFirst: Boolean = false,
    onCreate: () -> Unit,
    onEdit: (WoodWasteItem) -> Unit,
    onEntities: () -> Unit = {},
) {
    val all by WoodWasteRepository.items.collectAsState()
    val error by WoodWasteRepository.error.collectAsState()
    val cached by WoodWasteRepository.fromCache.collectAsState()
    val loading by WoodWasteRepository.loading.collectAsState()
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val filterMaxHeight = (maxHeight * 0.30f).coerceIn(96.dp, 220.dp)
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
        if (!showMap) {
            OutlinedButton(onClick = onEntities, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.Business, null)
                Spacer(Modifier.width(8.dp))
                Text("Entidades de Suba")
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
        AnimatedVisibility(visible = filters) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = filterMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
        }
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Sincronizando la comunidad…", style = MaterialTheme.typography.labelMedium)
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
                modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(),
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
                if (visible.isEmpty() && !loading)
                    item {
                        Text("Aún no hay publicaciones con estos filtros.")
                        TextButton(onClick = onCreate) { Text("Compartir un hallazgo o material") }
                    }
                items(visible, key = { it.id }) { item -> EntryCard(item) { selectedId = item.id } }
            }
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
    var photoLoading by remember(item.id, item.imagePath) { mutableStateOf(false) }
    var photoError by remember(item.id, item.imagePath) { mutableStateOf(false) }
    var photoRetry by remember(item.id, item.imagePath) { mutableIntStateOf(0) }
    var message by remember(item.id) { mutableStateOf<String?>(null) }
    var reporting by rememberSaveable(item.id) { mutableStateOf(false) }
    var reportReason by rememberSaveable(item.id) { mutableStateOf("Contenido ajeno a WoodMatch") }
    var reportDetail by rememberSaveable(item.id) { mutableStateOf("") }
    var reportId by rememberSaveable(item.id) { mutableStateOf(PilotApi.operationId()) }
    var requesting by rememberSaveable(item.id) { mutableStateOf(false) }
    var use by rememberSaveable(item.id) { mutableStateOf("") }
    var date by rememberSaveable(item.id) { mutableStateOf("") }
    var quantity by rememberSaveable(item.id) { mutableStateOf("") }
    var note by rememberSaveable(item.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val op = rememberSaveable(item.id, use, date, quantity, note) { PilotApi.operationId() }
    LaunchedEffect(item.id, item.imagePath, photoRetry) {
        photoLoading = true
        photoError = false
        try {
            runCatching { WoodWasteRepository.photoUrl(item) }
                .onSuccess { image = it }
                .onFailure { photoError = true }
        } finally {
            photoLoading = false
        }
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
            if (photoLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Preparando fotografía…", style = MaterialTheme.typography.labelMedium)
            }
            if (photoError) {
                Text("No se pudo cargar la fotografía.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { photoRetry++ }) { Text("Reintentar fotografía") }
            }
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
            if (user != null && item.authorId != user?.uid) {
                TextButton(onClick = { reporting = !reporting }) { Icon(Icons.Default.Flag, null); Spacer(Modifier.width(8.dp)); Text("Reportar publicación") }
                if (reporting) {
                    Text("Ayúdanos a cuidar la comunidad", style = MaterialTheme.typography.titleMedium)
                    listOf("Contenido ajeno a WoodMatch", "Fotografía incorrecta", "Spam o publicidad", "Contenido inapropiado", "Otro motivo").forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = reportReason == reason, onClick = { reportReason = reason })
                            Text(reason)
                        }
                    }
                    OutlinedTextField(reportDetail, { reportDetail = it.take(1000) }, label = { Text("Información adicional (opcional)") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            runCatching { PilotApi.call("createSupportCase", mapOf("id" to reportId, "type" to "MODERACION", "subject" to reportReason, "detail" to reportDetail.ifBlank { reportReason }, "publicationId" to item.id)) }
                                .onSuccess { message = "Reporte recibido. Sigue la respuesta en Perfil → Ayuda y participación."; reporting = false; reportId = PilotApi.operationId() }
                                .onFailure { message = it.message }
                            busy = false
                        }
                    }) { Text("Enviar reporte") }
                }
            }
            message?.let { StatusMessage(it) }
            TextButton(onClick = onDismiss) { Text("Cerrar") }
            Spacer(Modifier.height(24.dp))
        }
    }
}


