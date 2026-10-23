package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kotlinapp.data.model.TreeReport
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.ReportRepository
import com.example.kotlinapp.data.repository.WoodWasteRepository
import com.example.kotlinapp.ui.components.WithdrawalPassActions
import com.example.kotlinapp.ui.components.WoodMatchTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewReportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateReport: () -> Unit
) {
    val currentUser by AuthRepository.currentUser.collectAsState()
    val wasteItems by WoodWasteRepository.items.collectAsState()
    val wasteRequests by WoodWasteRepository.requests.collectAsState()
    val wasteError by WoodWasteRepository.error.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var reports by remember { mutableStateOf<List<TreeReport>>(emptyList()) }
    var isLoadingReports by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<TreeReport?>(null) }
    var activityMessage by remember { mutableStateOf<String?>(null) }
    var isDeactivating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun reportStatusText(report: TreeReport): String {
        val expires = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(report.expiresAt))
        return if (report.expiresAt <= System.currentTimeMillis()) {
            "Vencido: requiere verificación"
        } else {
            "Vigente hasta $expires"
        }
    }

    fun loadReports() {
        isLoadingReports = true
        coroutineScope.launch {
            reports = ReportRepository.getAllReports()
            isLoadingReports = false
        }
    }

    LaunchedEffect(currentUser) {
        loadReports()
    }

    // Modal de detalle de reporte
    if (selectedReport != null) {
        val rep = selectedReport!!
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Forest,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = rep.species.ifBlank { "Árbol no identificado" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rep.imageUri?.let { photo ->
                        AsyncImage(
                            model = photo,
                            contentDescription = "Foto del reporte",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Text(
                        text = "Condición: ${rep.condition}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rep.condition == "Caído" || rep.condition == "En riesgo")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = reportStatusText(rep),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rep.expiresAt <= System.currentTimeMillis())
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Descripción: ${rep.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Coordenadas: ${String.format("%.4f", rep.latitude)}, ${String.format("%.4f", rep.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Registrado: ${rep.formattedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedReport = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("view_reports_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WoodMatchTopAppBar(
                title = "Reportes y Actividad",
                showBackButton = true,
                onBackClick = onNavigateBack,
                trailingContent = {
                    IconButton(onClick = { loadReports() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Recargar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Pestaña Dual: Reportes de la comunidad / Mi actividad
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Reportes (${reports.size})") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_community_reports")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Mi actividad") },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.NaturePeople,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_my_activity")
                )
            }

            // CONTENIDO SEGÚN LA PESTAÑA SELECCIONADA
            when (selectedTabIndex) {
                0 -> {
                    // PESTAÑA 1: Reportes Comunitarios
                    if (isLoadingReports) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (reports.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Forest,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No hay reportes registrados",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Genera un nuevo reporte para mapear árboles caídos o podas en Suba.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Incidentes forestales y material disponible en Suba",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(reports, key = { it.id }) { report ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedReport = report }
                                        .testTag("report_card_${report.id}"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = report.species.ifBlank { "Árbol sin clasificar" },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (report.condition == "Caído" || report.condition == "En riesgo")
                                                            MaterialTheme.colorScheme.errorContainer
                                                        else
                                                            MaterialTheme.colorScheme.secondaryContainer
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = report.condition,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (report.condition == "Caído" || report.condition == "En riesgo")
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = report.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = reportStatusText(report),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (report.expiresAt <= System.currentTimeMillis())
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${String.format("%.4f", report.latitude)}, ${String.format("%.4f", report.longitude)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Schedule,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = report.formattedDate,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }

                1 -> {
                    // PESTAÑA 2: Mi Actividad (Publicaciones, solicitudes y pases de retiro)
                    val myItems = wasteItems.filter { it.authorId == currentUser?.uid }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("my_activity_content"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            if (!AuthRepository.isAuthenticated) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Sesión local / invitado",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Para sincronizar tu actividad con la nube y recibir notificaciones de solicitudes, inicia sesión con tu cuenta de WoodMatch.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            wasteError?.let { err ->
                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            activityMessage?.let { msg ->
                                Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Mis Publicaciones
                        item {
                            Text(
                                text = "Mis publicaciones de madera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (myItems.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No has publicado material aún.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = onNavigateToCreateReport) {
                                            Text("Crear publicación o reporte")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(myItems, key = { "item_${it.id}" }) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = item.typedStatus.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${item.formattedQuantity} · ${item.subaSector}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.isAvailable) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(
                                                enabled = !isDeactivating,
                                                onClick = {
                                                    isDeactivating = true
                                                    coroutineScope.launch {
                                                        WoodWasteRepository.updateWasteStatus(item.id, WasteStatus.INACTIVO)
                                                            .onSuccess { activityMessage = "Publicación desactivada correctamente." }
                                                            .onFailure { activityMessage = "No se pudo desactivar. Comprueba tu conexión." }
                                                        isDeactivating = false
                                                    }
                                                }
                                            ) {
                                                Text("Desactivar publicación")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Solicitudes e Historial
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Solicitudes e historial de retiros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (wasteRequests.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No tienes solicitudes registradas.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(wasteRequests, key = { "request_${it.id}" }) { req ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = req.wasteTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${req.typedStatus.displayName} · Fecha propuesta: ${req.proposedDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        WithdrawalPassActions(req, req.authorId == currentUser?.uid)
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }

        // Floating Action Button exclusivo para crear reportes
        ExtendedFloatingActionButton(
            onClick = onNavigateToCreateReport,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuevo reporte") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_report_fab")
        )
    }
}
