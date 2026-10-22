package com.example.kotlinapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlinapp.data.model.SubaSector
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.model.WoodWasteItem
import com.example.kotlinapp.data.repository.WoodWasteRepository

data class EcologicalZone(
    val name: String,
    val type: String,
    val description: String,
    val centerLng: Double,
    val centerLat: Double,
    val radiusWidthKm: Double,
    val radiusHeightKm: Double,
    val color: Color
)

object SubaEcologicalData {
    val ZONES = listOf(
        EcologicalZone(
            name = "Reserva Thomas van der Hammen",
            type = "Reserva Forestal Productora",
            description = "Principal corredor biológico del norte de Bogotá conectando los Cerros Orientales con el Río Bogotá.",
            centerLng = -74.0750,
            centerLat = 4.7850,
            radiusWidthKm = 0.038,
            radiusHeightKm = 0.024,
            color = Color(0xFF2E7D32)
        ),
        EcologicalZone(
            name = "Humedal La Conejera",
            type = "Humedal y Reserva de Biodiversidad",
            description = "Humedal emblemático de Suba, hábitat de la tingua bogotana y curí sabanero.",
            centerLng = -74.0920,
            centerLat = 4.7560,
            radiusWidthKm = 0.024,
            radiusHeightKm = 0.009,
            color = Color(0xFF00796B)
        ),
        EcologicalZone(
            name = "Cerros de Suba",
            type = "Reserva Forestal y Mirador",
            description = "Ecosistema de bosque andino y colinas que dividen el valle de Suba con el Río Bogotá.",
            centerLng = -74.0850,
            centerLat = 4.7450,
            radiusWidthKm = 0.020,
            radiusHeightKm = 0.022,
            color = Color(0xFF388E3C)
        ),
        EcologicalZone(
            name = "Humedal Juan Amarillo (Tibabuyes)",
            type = "Humedal Ramsar Protegido",
            description = "El humedal más extenso de Bogotá, amortiguador hídrico en el límite entre Suba y Engativá.",
            centerLng = -74.1020,
            centerLat = 4.7180,
            radiusWidthKm = 0.035,
            radiusHeightKm = 0.008,
            color = Color(0xFF0288D1)
        ),
        EcologicalZone(
            name = "Humedal de Córdoba",
            type = "Humedal Urbano y Avifauna",
            description = "Corredor hídrico en Niza con más de 120 especies de aves registradas.",
            centerLng = -74.0670,
            centerLat = 4.7080,
            radiusWidthKm = 0.015,
            radiusHeightKm = 0.008,
            color = Color(0xFF0097A7)
        ),
        EcologicalZone(
            name = "Humedal Torca y Guaymaral",
            type = "Reserva Hídrica del Norte",
            description = "Cuerpo hídrico en la cuenca alta limítrofe con Chía y Usaquén.",
            centerLng = -74.0380,
            centerLat = 4.7950,
            radiusWidthKm = 0.018,
            radiusHeightKm = 0.012,
            color = Color(0xFF00897B)
        ),
        EcologicalZone(
            name = "Parque Mirador de los Nevados",
            type = "Parque Ecológico Distrital",
            description = "Antigua cantera restaurada como santuario de flora nativa y cosmovisión Muisca.",
            centerLng = -74.0815,
            centerLat = 4.7480,
            radiusWidthKm = 0.009,
            radiusHeightKm = 0.007,
            color = Color(0xFF43A047)
        ),
        EcologicalZone(
            name = "Bosques San José de Bavaria",
            type = "Zona Verde y Arbolado de Protección",
            description = "Sector de alta cobertura arbórea de acacias, cipreses y sauces en Suba Norte.",
            centerLng = -74.0620,
            centerLat = 4.7600,
            radiusWidthKm = 0.015,
            radiusHeightKm = 0.014,
            color = Color(0xFF4CAF50)
        )
    )
}

enum class MapEngineView(val displayName: String, val subtitle: String) {
    OPEN_STREET_MAP(
        "Mapa de calles",
        "Calles, barrios y puntos de retiro"
    ),
    VECTOR_CANVAS(
        "Vista sin conexión",
        "Ubicaciones guardadas en esta app"
    )
}

@Composable
fun TreeMapScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val wasteItems by WoodWasteRepository.items.collectAsState()
    var selectedSectorName by remember { mutableStateOf("TODOS") }
    var selectedItem by remember { mutableStateOf<WoodWasteItem?>(null) }
    var selectedReserve by remember { mutableStateOf<EcologicalZone?>(null) }

    // El modo local abre primero para que el prototipo sea navegable sin credenciales ni red.
    // Los visores conectados permanecen disponibles desde los chips superiores.
    var currentEngine by remember { mutableStateOf(MapEngineView.VECTOR_CANVAS) }

    // Coordenadas dinámicas de enfoque en Suba
    var mapCenterLat by remember { mutableStateOf(4.7450) }
    var mapCenterLng by remember { mutableStateOf(-74.0850) }
    var mapZoomLevel by remember { mutableIntStateOf(13) }

    // Capas ecológicas activas
    var showGreenReserves by remember { mutableStateOf(true) }

    // Controles de zoom y paneo para motor vectorial de respaldo
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val filteredItems = remember(wasteItems, selectedSectorName) {
        if (selectedSectorName == "TODOS") wasteItems
        else wasteItems.filter { it.subaSector.equals(selectedSectorName, ignoreCase = true) }
    }

    // Abrir el visor web oficial de OpenStreetMap en Suba
    fun openOpenStreetMapOfficial(lat: Double = 4.7450, lng: Double = -74.0850) {
        try {
            val uri = Uri.parse("https://www.openstreetmap.org/#map=15/$lat/$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackUri = Uri.parse("https://www.openstreetmap.org")
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tree_map_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Adaptable con protección contra desbordamiento
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (currentEngine) {
                            MapEngineView.OPEN_STREET_MAP -> "Mapa de Suba"
                            MapEngineView.VECTOR_CANVAS -> "Mapa de Suba"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentEngine.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Botón selector de motor de mapas
                IconButton(
                    onClick = {
                        currentEngine = when (currentEngine) {
                            MapEngineView.OPEN_STREET_MAP -> MapEngineView.VECTOR_CANVAS
                            MapEngineView.VECTOR_CANVAS -> MapEngineView.OPEN_STREET_MAP
                        }
                    }
                ) {
                    Icon(
                        imageVector = when (currentEngine) {
                            MapEngineView.OPEN_STREET_MAP -> Icons.Filled.Public
                            MapEngineView.VECTOR_CANVAS -> Icons.Filled.Map
                        },
                        contentDescription = "Cambiar vista del mapa",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Barra 1: Selector de Motores de Mapa (Horizontalmente scrolleable)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(listOf(MapEngineView.OPEN_STREET_MAP, MapEngineView.VECTOR_CANVAS)) { engine ->
                    FilterChip(
                        selected = currentEngine == engine,
                        onClick = { currentEngine = engine },
                        label = { Text(engine.displayName, maxLines = 1) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Toggle para mostrar/ocultar reservas
                item {
                    FilterChip(
                        selected = showGreenReserves,
                        onClick = { showGreenReserves = !showGreenReserves },
                        label = { Text("Zonas verdes", maxLines = 1) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF1B5E20)
                        )
                    )
                }

            }

            // Barra 2: Filtro de Sectores de Suba
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSectorName == "TODOS",
                        onClick = {
                            selectedSectorName = "TODOS"
                            selectedItem = null
                            selectedReserve = null
                            mapCenterLat = 4.7450
                            mapCenterLng = -74.0850
                            mapZoomLevel = 13
                        },
                        label = { Text("Toda Suba (${wasteItems.size})", maxLines = 1) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                items(SubaSectors.ALL) { sec ->
                    val count = wasteItems.count { it.subaSector.contains(sec.name, ignoreCase = true) }
                    FilterChip(
                        selected = selectedSectorName == sec.name,
                        onClick = {
                            selectedSectorName = sec.name
                            selectedItem = null
                            selectedReserve = null
                            mapCenterLat = sec.referenceLatitude
                            mapCenterLng = sec.referenceLongitude
                            mapZoomLevel = 14
                        },
                        label = { Text("${sec.name} ($count)", maxLines = 1) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Contenedor Principal del Mapa
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            ) {
                when (currentEngine) {
                    MapEngineView.OPEN_STREET_MAP -> {
                        // 1. COMPONENTE MAPWEBVIEW: Visor OpenStreetMap Suba (100% abierto y gratuito)
                        // Con capas OSM Estándar, Zonas Verdes (Eco), Satelital Libre y Topo/Relieve
                        MapWebView(
                            wasteItems = filteredItems,
                            ecologicalZones = SubaEcologicalData.ZONES,
                            showReserves = showGreenReserves,
                            centerLat = mapCenterLat,
                            centerLng = mapCenterLng,
                            zoomLevel = mapZoomLevel,
                            onWasteSelected = { item ->
                                selectedItem = item
                                selectedReserve = null
                            },
                            onReserveSelected = { res ->
                                selectedReserve = res
                                selectedItem = null
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    MapEngineView.VECTOR_CANVAS -> {
                        // 3. MOTOR VECTORIAL LOCAL NATIVO (100% Offline)
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val surfaceColor = MaterialTheme.colorScheme.surface
                        val tertiaryColor = MaterialTheme.colorScheme.tertiary

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(filteredItems, zoomScale, panOffsetX, panOffsetY, showGreenReserves) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(0.85f, 3.0f)
                                        panOffsetX = (panOffsetX + pan.x).coerceIn(-250f * zoomScale, 250f * zoomScale)
                                        panOffsetY = (panOffsetY + pan.y).coerceIn(-250f * zoomScale, 250f * zoomScale)
                                    }
                                }
                                .pointerInput(filteredItems, zoomScale, panOffsetX, panOffsetY, showGreenReserves) {
                                    detectTapGestures { tapOffset ->
                                        val w = size.width
                                        val h = size.height

                                        val minLat = 4.7050
                                        val maxLat = 4.8150
                                        val minLng = -74.1280
                                        val maxLng = -74.0250

                                        val clickedWaste = filteredItems.find { item ->
                                            val nx = ((item.exactLongitude - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f)
                                            val ny = (1f - ((item.exactLatitude - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.05f, 0.95f)
                                            val px = (nx * w - w / 2) * zoomScale + w / 2 + panOffsetX
                                            val py = (ny * h - h / 2) * zoomScale + h / 2 + panOffsetY
                                            Math.hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble()) < 44
                                        }

                                        if (clickedWaste != null) {
                                            selectedItem = clickedWaste
                                            selectedReserve = null
                                            return@detectTapGestures
                                        }

                                        if (showGreenReserves) {
                                            val clickedReserve = SubaEcologicalData.ZONES.find { z ->
                                                val nx = ((z.centerLng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f)
                                                val ny = (1f - ((z.centerLat - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.05f, 0.95f)
                                                val px = (nx * w - w / 2) * zoomScale + w / 2 + panOffsetX
                                                val py = (ny * h - h / 2) * zoomScale + h / 2 + panOffsetY
                                                val rx = (z.radiusWidthKm / (maxLng - minLng)).toFloat() * w * zoomScale
                                                val ry = (z.radiusHeightKm / (maxLat - minLat)).toFloat() * h * zoomScale
                                                Math.hypot((tapOffset.x - px).toDouble() / rx, (tapOffset.y - py).toDouble() / ry) <= 1.2
                                            }
                                            if (clickedReserve != null) {
                                                selectedReserve = clickedReserve
                                                selectedItem = null
                                            }
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            val minLat = 4.7050
                            val maxLat = 4.8150
                            val minLng = -74.1280
                            val maxLng = -74.0250

                            fun projectPoint(lng: Double, lat: Double): Offset {
                                val nx = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.04f, 0.96f)
                                val ny = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.04f, 0.96f)
                                val px = (nx * w - w / 2) * zoomScale + w / 2 + panOffsetX
                                val py = (ny * h - h / 2) * zoomScale + h / 2 + panOffsetY
                                return Offset(px, py)
                            }

                            // Reservas
                            if (showGreenReserves) {
                                SubaEcologicalData.ZONES.forEach { zone ->
                                    val center = projectPoint(zone.centerLng, zone.centerLat)
                                    val rx = (zone.radiusWidthKm / (maxLng - minLng)).toFloat() * w * zoomScale
                                    val ry = (zone.radiusHeightKm / (maxLat - minLat)).toFloat() * h * zoomScale
                                    val isSelectedReserve = selectedReserve?.name == zone.name

                                    drawOval(
                                        color = zone.color.copy(alpha = if (isSelectedReserve) 0.40f else 0.22f),
                                        topLeft = Offset(center.x - rx, center.y - ry),
                                        size = Size(rx * 2, ry * 2)
                                    )
                                    drawOval(
                                        color = if (isSelectedReserve) Color.White else zone.color.copy(alpha = 0.75f),
                                        topLeft = Offset(center.x - rx, center.y - ry),
                                        size = Size(rx * 2, ry * 2),
                                        style = Stroke(width = (if (isSelectedReserve) 2.5f else 1.2f).dp.toPx() * zoomScale)
                                    )
                                }
                            }

                            // Río Bogotá
                            val riverPath = Path().apply {
                                val p1 = projectPoint(-74.1260, 4.7150)
                                val p2 = projectPoint(-74.1210, 4.7450)
                                val p3 = projectPoint(-74.1160, 4.7700)
                                val p4 = projectPoint(-74.0940, 4.8100)
                                moveTo(p1.x, p1.y)
                                cubicTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)
                            }
                            drawPath(
                                path = riverPath,
                                color = Color(0xFF64B5F6).copy(alpha = 0.55f),
                                style = Stroke(width = 6.dp.toPx() * zoomScale)
                            )

                            // Centros de Sectores
                            SubaSectors.ALL.forEach { sec ->
                                val pos = projectPoint(sec.referenceLongitude, sec.referenceLatitude)
                                val isFilterActive = selectedSectorName == sec.name

                                drawCircle(
                                    color = if (isFilterActive) primaryColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.08f),
                                    radius = (if (isFilterActive) 34.dp.toPx() else 22.dp.toPx()) * zoomScale,
                                    center = pos
                                )
                                drawCircle(
                                    color = if (isFilterActive) primaryColor else primaryColor.copy(alpha = 0.45f),
                                    radius = 3.5f.dp.toPx() * zoomScale,
                                    center = pos
                                )
                            }

                            // Pines de Residuos
                            filteredItems.forEach { item ->
                                val pos = projectPoint(item.exactLongitude, item.exactLatitude)
                                val isSel = selectedItem?.id == item.id

                                val markerColor = when (item.typedStatus) {
                                    WasteStatus.DISPONIBLE -> secondaryColor
                                    WasteStatus.RESERVADO -> tertiaryColor
                                    WasteStatus.APROVECHADO -> primaryColor
                                    WasteStatus.INACTIVO -> Color.Gray
                                }

                                drawCircle(
                                    color = markerColor.copy(alpha = if (isSel) 0.5f else 0.22f),
                                    radius = (if (isSel) 20.dp.toPx() else 12.dp.toPx()) * zoomScale,
                                    center = pos
                                )
                                drawCircle(
                                    color = markerColor,
                                    radius = (if (isSel) 9.dp.toPx() else 6.5f.dp.toPx()) * zoomScale,
                                    center = pos
                                )
                                drawCircle(
                                    color = surfaceColor,
                                    radius = (if (isSel) 9.dp.toPx() else 6.5f.dp.toPx()) * zoomScale,
                                    center = pos,
                                    style = Stroke(width = 2.dp.toPx() * zoomScale)
                                )
                            }
                        }

                        // Botones de zoom para el modo vectorial
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(3.0f) },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Acercar", modifier = Modifier.size(18.dp))
                            }
                            SmallFloatingActionButton(
                                onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.85f) },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Alejar", modifier = Modifier.size(18.dp))
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    zoomScale = 1f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = "Centrar en Suba", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Banner distintivo del motor activo (solo para motores no-web)
                if (currentEngine != MapEngineView.OPEN_STREET_MAP) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (currentEngine) {
                                MapEngineView.OPEN_STREET_MAP -> ""
                                MapEngineView.VECTOR_CANVAS -> "Vista sin conexión"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // TARJETA 1: Detalle de Reserva Natural Seleccionada (Botones responsivos sin desbordamiento)
            selectedReserve?.let { res ->
                Card(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Forest, contentDescription = null, tint = res.color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = res.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { selectedReserve = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                            }
                        }

                        Text(
                            text = res.type,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = res.color
                        )

                        Text(
                            text = res.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Botones Adaptables en Row equilibrado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openOpenStreetMapOfficial(res.centerLat, res.centerLng) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Abrir mapa",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // TARJETA 2: Detalle del Residuo Seleccionado (Botones responsivos sin desbordamiento)
            selectedItem?.let { item ->
                Card(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selectedItem = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                            }
                        }

                        // Badges Informativos
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Sector: ${item.subaSector}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.formattedQuantity,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.typedStatus.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (item.suggestedUses.isNotBlank()) {
                            Text(
                                text = "Aprovechamiento: ${item.suggestedUses}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Botones Adaptables para el residuo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { openOpenStreetMapOfficial(item.exactLatitude, item.exactLongitude) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Abrir mapa",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
