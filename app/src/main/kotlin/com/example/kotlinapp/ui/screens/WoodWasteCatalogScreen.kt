package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.kotlinapp.ui.components.WithdrawalPassActions
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kotlinapp.data.model.RequestStatus
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.model.WasteCategory
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.model.WithdrawalRequest
import com.example.kotlinapp.data.model.WoodWasteItem
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.WoodWasteRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WoodWasteCatalogScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateReport: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allWasteItems by WoodWasteRepository.items.collectAsState()
    val allRequests by WoodWasteRepository.requests.collectAsState()
    val catalogError by WoodWasteRepository.error.collectAsState()
    val currentUser by AuthRepository.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Muro de Residuos, 1: Solicitudes y Retiros

    // Filtros
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("TODAS") }
    var selectedSectorFilter by remember { mutableStateOf<String?>("TODOS") }
    var onlyAvailableFilter by remember { mutableStateOf(false) }

    // Dialogo de Solicitud de Retiro
    var requestTargetItem by remember { mutableStateOf<WoodWasteItem?>(null) }
    var intendedUseInput by remember { mutableStateOf("") }
    var proposedDateInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var isSubmittingRequest by remember { mutableStateOf(false) }
    var requestSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Filtrado del Muro de Residuos
    val filteredWasteItems = allWasteItems.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.speciesName.contains(searchQuery, ignoreCase = true) ||
                item.subaSector.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategoryFilter == null || selectedCategoryFilter == "TODAS" ||
                item.category.equals(selectedCategoryFilter, ignoreCase = true)

        val matchesSector = selectedSectorFilter == null || selectedSectorFilter == "TODOS" ||
                item.subaSector.equals(selectedSectorFilter, ignoreCase = true)

        val matchesAvailability = !onlyAvailableFilter || item.isAvailable

        matchesSearch && matchesCategory && matchesSector && matchesAvailability
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            catalogError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            if (!AuthRepository.isAuthenticated) Text("Inicia sesión para ver publicaciones compartidas. Puedes consultar Especies como invitado.", modifier = Modifier.padding(16.dp))
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("catalog_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Muro de Residuos Leñosos",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Oferta y Solicitudes en Suba • Piloto Circular",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Residuos (${filteredWasteItems.size})", maxLines = 1, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Solicitudes (${allRequests.size})", maxLines = 1, fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: MURO DE RESIDUOS LEÑOSOS
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra de búsqueda
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por tipo, especie o barrio en Suba...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("catalog_search_input")
                    )

                    // Filtros horizontales por categoría y sectores de Suba
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = onlyAvailableFilter,
                                onClick = { onlyAvailableFilter = !onlyAvailableFilter },
                                label = { Text("Solo Disponibles") },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == "TODAS",
                                onClick = { selectedCategoryFilter = "TODAS" },
                                label = { Text("Todas Categorías") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        items(WasteCategory.values()) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.name,
                                onClick = { selectedCategoryFilter = cat.name },
                                label = { Text(cat.displayName.substringBefore(" ")) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Selector de Sector en Suba
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSectorFilter == "TODOS",
                                onClick = { selectedSectorFilter = "TODOS" },
                                label = { Text("Toda Suba") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(SubaSectors.ALL) { sec ->
                            FilterChip(
                                selected = selectedSectorFilter == sec.name,
                                onClick = { selectedSectorFilter = sec.name },
                                label = { Text(sec.name, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Lista de Publicaciones
                    if (filteredWasteItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Recycling,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No se encontraron residuos con los filtros seleccionados.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredWasteItems, key = { it.id }) { item ->
                                WoodWasteCard(
                                    item = item,
                                    onRequestClick = {
                                        requestTargetItem = item
                                        intendedUseInput = ""
                                        proposedDateInput = ""
                                        messageInput = ""
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            } else {
                // TAB 1: GESTIÓN DE SOLICITUDES Y RETIROS
                RequestsManagementView(
                    requests = allRequests,
                    currentUser = currentUser,
                    onAcceptRequest = { wasteId, reqId ->
                        coroutineScope.launch {
                            WoodWasteRepository.acceptWithdrawalRequest(wasteId, reqId).onFailure { Toast.makeText(context, "No se pudo aceptar. Revisa la disponibilidad y tu conexión.", Toast.LENGTH_LONG).show() }
                        }
                    },
                    onRejectRequest = { wasteId, reqId, reason ->
                        coroutineScope.launch {
                            WoodWasteRepository.rejectWithdrawalRequest(wasteId, reqId, reason).onFailure { Toast.makeText(context, "No se pudo rechazar la solicitud.", Toast.LENGTH_LONG).show() }
                        }
                    },
                    onCompleteWithdrawal = { wasteId, reqId ->
                        coroutineScope.launch {
                            WoodWasteRepository.completeWithdrawal(wasteId, reqId, currentUser?.uid ?: "").onFailure { Toast.makeText(context, "No se pudo cerrar el retiro. Valida el pase QR y vuelve a intentar.", Toast.LENGTH_LONG).show() }
                        }
                    }
                )
            }
        }

        // FAB Publicar Residuo
        if (currentUser?.canPublish != false) {
            FloatingActionButton(
                onClick = onNavigateToCreateReport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("catalog_fab_publish")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Publicar Residuo")
            }
        }

        // Modal para Solicitar Retiro de Material
        requestTargetItem?.let { target ->
            AlertDialog(
                onDismissRequest = { requestTargetItem = null },
                title = {
                    Text(
                        text = "Solicitar Retiro de Residuo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Material: ${target.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cantidad: ${target.formattedQuantity} • Sector: ${target.subaSector}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = intendedUseInput,
                            onValueChange = { intendedUseInput = it },
                            label = { Text("Uso proyectado para el material") },
                            placeholder = { Text("Ej. Fabricación de bancos, compostaje...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_intended_use_input")
                        )

                        OutlinedTextField(
                            value = proposedDateInput,
                            onValueChange = { proposedDateInput = it },
                            label = { Text("Fecha estimada de retiro") },
                            placeholder = { Text("Ej. Sábado 20 de Octubre - Mañana") },
                            leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_date_input")
                        )

                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            label = { Text("Mensaje o condiciones de retiro") },
                            placeholder = { Text("Ej. Contamos con furgón y herramientas de carga.") },
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (intendedUseInput.isNotBlank()) {
                                isSubmittingRequest = true
                                coroutineScope.launch {
                                    val user = currentUser ?: com.example.kotlinapp.data.model.User(
                                        uid = "guest_req",
                                        name = "Receptor Suba",
                                        phone = "+573000000000"
                                    )
                                    WoodWasteRepository.submitWithdrawalRequest(
                                        wasteId = target.id,
                                        requester = user,
                                        intendedUse = intendedUseInput.trim(),
                                        proposedDate = proposedDateInput.trim().ifBlank { "A convenir" },
                                        requestedQuantity = target.formattedQuantity,
                                        message = messageInput.trim()
                                    )
                                    isSubmittingRequest = false
                                    requestTargetItem = null
                                    selectedTab = 1 // Cambiar a pestaña de solicitudes
                                }
                            }
                        },
                        enabled = !isSubmittingRequest && intendedUseInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_request_button")
                    ) {
                        if (isSubmittingRequest) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Enviar Solicitud")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { requestTargetItem = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun WoodWasteCard(
    item: WoodWasteItem,
    onRequestClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("waste_item_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Imagen si existe
            if (!item.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Badges de Estado y Categoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (item.typedStatus) {
                        WasteStatus.HALLAZGO -> MaterialTheme.colorScheme.tertiary
                    WasteStatus.DISPONIBLE -> MaterialTheme.colorScheme.primary
                        WasteStatus.RESERVADO -> MaterialTheme.colorScheme.tertiary
                        WasteStatus.APROVECHADO -> MaterialTheme.colorScheme.secondary
                        WasteStatus.INACTIVO -> MaterialTheme.colorScheme.error
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.typedStatus.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }

                    // Sector de Suba
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.subaSector,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Título
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Detalles Clave: Cantidad, Tipo y Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Scale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.formattedQuantity,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Recycling,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.wasteType.substringBefore(" "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Descripción
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                // Especie o Usos Recomendados
                if (item.suggestedUses.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Nature,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aprovechamiento sugerido: ${item.suggestedUses}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Botón Solicitar Retiro (solo si está disponible)
                if (item.isAvailable) {
                    Button(
                        onClick = onRequestClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("request_waste_button_${item.id}")
                    ) {
                        Icon(Icons.Filled.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Solicitar Material", maxLines = 1)
                    }
                } else if (item.isReserved) {
                    Text(
                        text = "Material en proceso de coordinación de retiro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RequestsManagementView(
    requests: List<WithdrawalRequest>,
    currentUser: com.example.kotlinapp.data.model.User?,
    onAcceptRequest: (String, String) -> Unit,
    onRejectRequest: (String, String, String?) -> Unit,
    onCompleteWithdrawal: (String, String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No hay solicitudes de retiro registradas aún.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(requests.filter { it.authorId == currentUser?.uid || it.requesterId == currentUser?.uid }, key = { it.id }) { req ->
                RequestItemCard(
                    req = req,
                    isCurrentUserAuthor = req.authorId == currentUser?.uid,
                    onAccept = { onAcceptRequest(req.wasteId, req.id) },
                    onReject = { onRejectRequest(req.wasteId, req.id, null) },
                    onComplete = { onCompleteWithdrawal(req.wasteId, req.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RequestItemCard(
    req: WithdrawalRequest,
    isCurrentUserAuthor: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Encabezado Estado y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (req.typedStatus) {
                    RequestStatus.PENDIENTE -> MaterialTheme.colorScheme.tertiary
                    RequestStatus.ACEPTADA -> MaterialTheme.colorScheme.primary
                    RequestStatus.COMPLETADA -> MaterialTheme.colorScheme.secondary
                    RequestStatus.RECHAZADA -> MaterialTheme.colorScheme.error
                    RequestStatus.CANCELADA -> MaterialTheme.colorScheme.onSurfaceVariant
                    RequestStatus.VENCIDA -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = req.typedStatus.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")).format(Date(req.createdAt))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = req.wasteTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Datos del Solicitante
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${req.requesterName} (${req.requesterRole})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Uso proyectado
            Text(
                text = "Uso proyectado: ${req.intendedUse}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (req.proposedDate.isNotBlank()) {
                Text(
                    text = "Fecha de retiro propuesta: ${req.proposedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (req.message.isNotBlank()) {
                Text(
                    text = "\"${req.message}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Datos de contacto visibles al aceptar
            if (req.isAccepted || req.isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contacto de coordinación: ${req.requesterPhone.ifBlank { "No compartido" }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            WithdrawalPassActions(req, isCurrentUserAuthor)

            // Botones de Acción según estado
            if (req.isPending && isCurrentUserAuthor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Text("Rechazar", maxLines = 1)
                    }
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Text("Aceptar", maxLines = 1)
                    }
                }
            } else if (req.isAccepted && isCurrentUserAuthor && req.passValidatedAt != null) {
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Retiro", maxLines = 1)
                }
            }
        }
    }
}
