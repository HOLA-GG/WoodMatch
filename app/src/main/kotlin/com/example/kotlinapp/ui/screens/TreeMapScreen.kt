package com.example.kotlinapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.model.TreeReport
import com.example.kotlinapp.data.repository.ReportRepository
import com.example.kotlinapp.ui.components.WoodMatchTopAppBar

@Composable
fun TreeMapScreen(
    onNavigateBack: () -> Unit
) {
    var reports by remember { mutableStateOf<List<TreeReport>>(emptyList()) }
    var selectedReport by remember { mutableStateOf<TreeReport?>(null) }
    var conditionFilter by remember { mutableStateOf("Todos") }

    LaunchedEffect(Unit) {
        reports = ReportRepository.getAllReports()
    }

    val conditions = listOf("Todos", "Caído", "Dañado", "Sano", "En riesgo")
    val filteredReports = remember(reports, conditionFilter) {
        if (conditionFilter == "Todos") reports
        else reports.filter { it.condition.equals(conditionFilter, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tree_map_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WoodMatchTopAppBar(
                title = "Mapa de Incidencias GPS",
                showBackButton = true,
                onBackClick = onNavigateBack
            )

            // Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                conditions.forEach { cond ->
                    FilterChip(
                        selected = conditionFilter == cond,
                        onClick = { conditionFilter = cond },
                        label = { Text(cond) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Interactive Map Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFFE8ECE4))
            ) {
                // Topo / Contour lines and Grid Canvas
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredReports) {
                            detectTapGestures { tapOffset ->
                                val w = size.width
                                val h = size.height
                                val tapped = filteredReports.find { rep ->
                                    // Map coordinates to 0..1 bounding box
                                    val relX = ((rep.longitude - (-74.12)) / ((-74.05) - (-74.12))).coerceIn(0.15, 0.85)
                                    val relY = (1.0 - ((rep.latitude - 4.58) / (4.65 - 4.58))).coerceIn(0.15, 0.85)
                                    val pinX = (relX * w).toFloat()
                                    val pinY = (relY * h).toFloat()
                                    val dist = (tapOffset.x - pinX) * (tapOffset.x - pinX) + (tapOffset.y - pinY) * (tapOffset.y - pinY)
                                    dist < 1600f // 40px radius
                                }
                                if (tapped != null) {
                                    selectedReport = tapped
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw topographic contour curves
                    for (i in 1..5) {
                        val path = Path().apply {
                            moveTo(0f, h * 0.18f * i)
                            cubicTo(
                                w * 0.25f, h * 0.14f * i + 30f,
                                w * 0.75f, h * 0.22f * i - 20f,
                                w, h * 0.17f * i
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFFD3DDD0),
                            style = Stroke(width = 2.5f)
                        )
                    }

                    // Draw hiking trail path
                    val trailPath = Path().apply {
                        moveTo(w * 0.2f, h * 0.85f)
                        lineTo(w * 0.35f, h * 0.6f)
                        lineTo(w * 0.65f, h * 0.5f)
                        lineTo(w * 0.55f, h * 0.25f)
                        lineTo(w * 0.8f, h * 0.15f)
                    }
                    drawPath(
                        path = trailPath,
                        color = secondaryColor.copy(alpha = 0.5f),
                        style = Stroke(width = 3.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f))
                    )

                    // Draw report pins
                    filteredReports.forEach { rep ->
                        val relX = ((rep.longitude - (-74.12)) / ((-74.05) - (-74.12))).coerceIn(0.15, 0.85)
                        val relY = (1.0 - ((rep.latitude - 4.58) / (4.65 - 4.58))).coerceIn(0.15, 0.85)
                        val pinX = (relX * w).toFloat()
                        val pinY = (relY * h).toFloat()

                        val pinColor = when (rep.condition) {
                            "Caído" -> Color(0xFFD32F2F)
                            "En riesgo" -> Color(0xFFE65100)
                            "Dañado" -> Color(0xFFF57C00)
                            else -> Color(0xFF2E7D32)
                        }

                        // Pin shadow & pulse ring
                        drawCircle(
                            color = pinColor.copy(alpha = 0.25f),
                            radius = 22f,
                            center = Offset(pinX, pinY)
                        )
                        // Pin core
                        drawCircle(
                            color = pinColor,
                            radius = 12f,
                            center = Offset(pinX, pinY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(pinX, pinY)
                        )
                    }
                }

                // Map Legend Overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Caído", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE65100)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("En riesgo", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sano", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Selected Pin Floating Card
                if (selectedReport != null) {
                    val rep = selectedReport!!
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rep.species,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Condición: ${rep.condition} · ${rep.formattedDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedReport = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = rep.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GPS: ${String.format("%.4f", rep.latitude)}, ${String.format("%.4f", rep.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
