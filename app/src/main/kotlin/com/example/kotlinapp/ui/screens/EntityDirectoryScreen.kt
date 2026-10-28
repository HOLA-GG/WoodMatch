package com.example.kotlinapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Public business references, not an inventory or a list of WoodMatch partners. */
private data class SubaEntity(
    val id: String,
    val name: String,
    val activity: String,
    val address: String,
    val phone: String,
    val displayPhone: String,
    val website: String,
    val source: String,
    val sourceName: String,
)

private val subaEntities = listOf(
    SubaEntity(
        id = "carpincentro_suba",
        name = "Carpincentro Ardisa · Suba",
        activity = "Comercio de tableros, maderas y materiales de carpintería.",
        address = "Carrera 103C # 138-40, Suba, Bogotá",
        phone = "+573042143553",
        displayPhone = "304 214 3553",
        website = "https://www.carpincentro.com/",
        source = "https://duratex.com.co/location/carpincentro-ardisa-suba/",
        sourceName = "Directorio de distribuidores de Duratex",
    ),
    SubaEntity(
        id = "monomerados_suba",
        name = "Monomerados · Sede principal Suba",
        activity = "Materiales para carpintería, tableros y servicios de corte y canteo.",
        address = "Calle 139 # 103-56, Suba, Bogotá",
        phone = "+573204989451",
        displayPhone = "320 498 9451",
        website = "https://monomerados.com/",
        source = "https://monomerados.com/",
        sourceName = "Sitio oficial · Nuestras sedes",
    ),
)

@Composable
fun EntityDirectoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(query) {
        subaEntities.filter {
            query.isBlank() || "${it.name} ${it.activity} ${it.address}".contains(query, true)
        }
    }
    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "No se pudo abrir el enlace. Intenta con otra aplicación.", Toast.LENGTH_LONG).show()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                }
                Text("Conexiones en Suba", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ))).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.Forest, null, Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("La circularidad empieza cerca", style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                    Text("Explora entidades locales relacionadas con madera y carpintería. Puedes consultarles si desean participar en WoodMatch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                }
            }
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Directorio de referencia", style = MaterialTheme.typography.titleMedium)
                        Text("La vinculación y los excedentes de estas entidades no están confirmados en WoodMatch. Consulta disponibilidad y condiciones directamente; no retires materiales sin autorización.",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Datos públicos revisados: 26 de octubre de 2026.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar en el directorio") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
            )
        }
        items(visible, key = { it.id }) { entity ->
            Card(
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraLarge) {
                        Text("Suba · Referencia pública", Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(entity.name, style = MaterialTheme.typography.titleLarge)
                    Text(entity.activity, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(entity.address, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Phone, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(entity.displayPhone, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = {
                            val url = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(entity.name + " " + entity.address)}"
                            open(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    ) {
                        Icon(Icons.Default.Map, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver ubicación")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = { open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entity.phone}"))) },
                    ) {
                        Icon(Icons.Default.Phone, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Contactar")
                    }
                    TextButton(modifier = Modifier.fillMaxWidth(),
                        onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(entity.website))) }) {
                        Text("Visitar sitio oficial")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp))
                    }
                    HorizontalDivider()
                    Text("Fuente: ${entity.sourceName}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(modifier = Modifier.fillMaxWidth(),
                        onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(entity.source))) }) {
                        Text("Consultar fuente pública")
                    }
                }
            }
        }
        if (visible.isEmpty()) item {
            Text("No hay entidades con esa búsqueda en este directorio de Suba.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
    }
}
