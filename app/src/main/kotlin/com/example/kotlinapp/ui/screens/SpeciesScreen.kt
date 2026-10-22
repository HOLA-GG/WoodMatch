package com.example.kotlinapp.ui.screens

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kotlinapp.data.model.TreeSpecies
import com.example.kotlinapp.data.network.ForestAiService
import com.example.kotlinapp.data.network.PlantNetIdentificationResult
import com.example.kotlinapp.data.network.PlantNetService
import com.example.kotlinapp.data.repository.SpeciesRepository
import com.example.kotlinapp.ui.components.WoodMatchTopAppBar
import kotlinx.coroutines.launch
import java.util.UUID
import java.io.File

@Composable
fun SpeciesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allSpecies by SpeciesRepository.speciesList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("Todas") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var selectedSeason by remember { mutableStateOf("Todos") }

    var isQueryingRegion by remember { mutableStateOf(false) }
    var selectedSpecies by remember { mutableStateOf<TreeSpecies?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showAiConsultantDialog by remember { mutableStateOf(false) }

    var isSearchingOnline by remember { mutableStateOf(false) }
    var onlineSearchMessage by remember { mutableStateOf<String?>(null) }

    fun performOnlineSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            Toast.makeText(context, "Ingresa al menos 2 letras para consultar", Toast.LENGTH_SHORT).show()
            return
        }
        isSearchingOnline = true
        onlineSearchMessage = null
        coroutineScope.launch {
            val result = SpeciesRepository.searchOnlinePlantNet(trimmed)
            isSearchingOnline = false
            result.onSuccess { found ->
                if (found.isNotEmpty()) {
                    Toast.makeText(context, "¡Se encontraron ${found.size} especies en línea (Pl@ntNet)!", Toast.LENGTH_SHORT).show()
                } else {
                    onlineSearchMessage = "No se encontraron registros botánicos para '$trimmed' en el servicio de Pl@ntNet."
                }
            }.onFailure { err ->
                onlineSearchMessage = "Error en consulta en línea: ${err.message}"
            }
        }
    }

    // Regiones biogeográficas para consulta selectiva
    val regions = listOf("Todas", "Andina", "Páramo", "Amazonía", "Caribe", "Orinoquía", "Pacífico")
    val categories = listOf("Todos", "Árbol", "Arbusto", "Flor / Planta")
    val seasons = listOf("Todos", "Primavera", "Verano", "Otoño", "Invierno")

    // Carga inicial
    LaunchedEffect(Unit) {
        SpeciesRepository.fetchSpeciesByRegion("Todas", limit = 15)
    }

    // Filtrado por región
    fun onSelectRegion(region: String) {
        selectedRegion = region
        isQueryingRegion = true
        coroutineScope.launch {
            try {
                SpeciesRepository.fetchSpeciesByRegion(region, limit = 15)
            } finally {
                isQueryingRegion = false
            }
        }
    }

    // Filtrado en memoria
    val filteredSpecies = remember(allSpecies, searchQuery, selectedRegion, selectedCategory, selectedSeason) {
        allSpecies.filter { species ->
            val matchesQuery = searchQuery.isBlank() ||
                    species.commonName.contains(searchQuery, ignoreCase = true) ||
                    species.scientificName.contains(searchQuery, ignoreCase = true) ||
                    species.description.contains(searchQuery, ignoreCase = true) ||
                    species.family.contains(searchQuery, ignoreCase = true)

            val matchesRegion = selectedRegion == "Todas" ||
                    species.regionGroup.equals(selectedRegion, ignoreCase = true) ||
                    species.nativeRegion.contains(selectedRegion, ignoreCase = true)

            val matchesCategory = selectedCategory == "Todos" ||
                    species.category.equals(selectedCategory, ignoreCase = true)

            val matchesSeason = selectedSeason == "Todos" ||
                    species.season.equals(selectedSeason, ignoreCase = true)

            matchesQuery && matchesRegion && matchesCategory && matchesSeason
        }
    }

    // 1. MODAL ESCÁNER BOTÁNICO
    if (showScannerDialog) {
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var imageUrlInput by remember { mutableStateOf("") }
        var selectedOrgan by remember { mutableStateOf("auto") }
        var isIdentifying by remember { mutableStateOf(false) }
        var identificationResult by remember { mutableStateOf<PlantNetIdentificationResult?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var assignRegion by remember { mutableStateOf("Andina") }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                imageUrlInput = ""
                identificationResult = null
                errorMessage = null
            }
        }
        var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { captured ->
            if (captured) {
                selectedImageUri = pendingCameraUri
                imageUrlInput = ""
                identificationResult = null
                errorMessage = null
            }
            pendingCameraUri = null
        }

        fun takeBotanicalPhoto() {
            runCatching {
                val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: error("No se pudo preparar el almacenamiento de la cámara")
                val file = File(picturesDir, "plant_${System.currentTimeMillis()}.jpg")
                file.parentFile?.mkdirs()
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.onSuccess { uri ->
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }.onFailure { error ->
                errorMessage = error.message ?: "No se pudo abrir la cámara."
            }
        }

        AlertDialog(
            onDismissRequest = { showScannerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Escáner Botánico", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Selecciona o toma una fotografía de la planta:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galería")
                    }
                    OutlinedButton(
                        onClick = { takeBotanicalPhoto() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cámara")
                    }
                    }

                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = {
                            imageUrlInput = it
                            if (it.isNotBlank()) selectedImageUri = null
                        },
                        label = { Text("O ingresa enlace directo de la imagen") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Vista previa de imagen seleccionada
                    if (selectedImageUri != null || imageUrlInput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedImageUri ?: imageUrlInput,
                                contentDescription = "Foto de espécimen",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Selector de órgano vegetal
                    Text(
                        text = "2. Parte del espécimen a analizar:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val organs = listOf(
                            "auto" to "Automático",
                            "leaf" to "Hoja",
                            "flower" to "Flor",
                            "fruit" to "Fruto",
                            "bark" to "Corteza"
                        )
                        organs.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedOrgan == key,
                                onClick = { selectedOrgan = key },
                                label = { Text(label) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    // Botón para identificar
                    Button(
                        onClick = {
                            isIdentifying = true
                            errorMessage = null
                            identificationResult = null

                            coroutineScope.launch {
                                val result = if (selectedImageUri != null) {
                                    PlantNetService.identifyFromUri(context, selectedImageUri!!, selectedOrgan)
                                } else if (imageUrlInput.isNotBlank()) {
                                    PlantNetService.identifyByUrl(imageUrlInput.trim(), selectedOrgan)
                                } else {
                                    Result.failure(Exception("Por favor selecciona una fotografía primero."))
                                }

                                isIdentifying = false
                                result.onSuccess {
                                    identificationResult = it
                                }
                                result.onFailure {
                                    errorMessage = it.localizedMessage ?: "No se pudo identificar la especie. Intenta con otra imagen."
                                }
                            }
                        },
                        enabled = !isIdentifying && (selectedImageUri != null || imageUrlInput.isNotBlank()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isIdentifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analizando espécimen vegetal...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Identificar Especie")
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // RESULTADOS
                    if (identificationResult != null) {
                        val res = identificationResult!!
                        val bestCandidate = res.candidates.firstOrNull()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Especie Reconocida",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    bestCandidate?.let {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${it.scorePercentage}% Precisión",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (bestCandidate != null) {
                                    Text(
                                        text = bestCandidate.primaryCommonName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${bestCandidate.scientificName} ${bestCandidate.authorship}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (bestCandidate.family.isNotBlank()) {
                                        Text(
                                            text = "Familia: ${bestCandidate.family}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = "Región de registro:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        regions.filter { it != "Todas" }.forEach { reg ->
                                            FilterChip(
                                                selected = assignRegion == reg,
                                                onClick = { assignRegion = reg },
                                                label = { Text(reg) },
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val newTree = bestCandidate.toTreeSpecies(assignRegion)
                                            coroutineScope.launch {
                                                SpeciesRepository.addSpecies(newTree)
                                                Toast.makeText(context, "¡${newTree.commonName} guardada en tu catálogo!", Toast.LENGTH_LONG).show()
                                                showScannerDialog = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Guardar en Catálogo Forestal")
                                    }
                                } else {
                                    Text("No se obtuvieron coincidencias concluyentes. Intenta con otra foto.")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScannerDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // 2. MODAL: GUÍA INTERACTIVA DE REPORTE BOTÁNICO PARA NUEVOS USUARIOS
    if (showAiConsultantDialog) {
        var selectedWasteType by remember { mutableStateOf("Poda / Ramas") }
        var customSpeciesInput by remember { mutableStateOf("") }
        var selectedSector by remember { mutableStateOf("Suba Centro") }
        var isAskingAi by remember { mutableStateOf(false) }
        var aiAnswer by remember { mutableStateOf<String?>(null) }

        val wasteTypes = listOf(
            "🪵 Poda / Ramas",
            "🌲 Tronco macizo",
            "📦 Estibas / Tablas",
            "🌾 Aserrín / Viruta",
            "🌱 Árbol urbano"
        )

        val subaSectorsList = listOf(
            "Suba Centro",
            "Niza / La Colina",
            "El Rincón",
            "Tibabuyes",
            "San José de Bavaria",
            "Britalia",
            "Lombardía"
        )

        AlertDialog(
            onDismissRequest = { showAiConsultantDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guía Asistida de Reporte",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "El Asistente Botánico orienta a nuevos usuarios para clasificar su residuo y publicarlo paso a paso en Suba.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Paso 1: Tipo de material
                    Text(
                        text = "1. Selecciona o escribe el tipo de madera:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wasteTypes.forEach { typeLabel ->
                            FilterChip(
                                selected = selectedWasteType == typeLabel,
                                onClick = {
                                    selectedWasteType = typeLabel
                                    aiAnswer = null
                                },
                                label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customSpeciesInput,
                        onValueChange = { customSpeciesInput = it },
                        placeholder = { Text("Opcional: Especie (ej. Eucalipto, Pino, Sauce)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Paso 2: Sector de Suba
                    Text(
                        text = "2. Sector de ubicación en Suba:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subaSectorsList.forEach { sec ->
                            FilterChip(
                                selected = selectedSector == sec,
                                onClick = {
                                    selectedSector = sec
                                    aiAnswer = null
                                },
                                label = { Text(sec, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Botón para consultar a Gemini (síntesis en 3 puntos)
                    Button(
                        onClick = {
                            if (!isAskingAi) {
                                isAskingAi = true
                                aiAnswer = null
                                coroutineScope.launch {
                                    val materialDesc = if (customSpeciesInput.isNotBlank()) {
                                        "$selectedWasteType ($customSpeciesInput)"
                                    } else selectedWasteType

                                    val res = ForestAiService.guideNewUserReport(
                                        wasteType = materialDesc,
                                        sector = selectedSector,
                                        details = "Usuario nuevo registrando en Suba"
                                    )
                                    aiAnswer = res.getOrNull() ?: "1. Diagnóstico: Material leñoso registrado.\n2. Aprovechamiento: Se evalúa para compostaje o bioconstrucción.\n3. Reporte: Presiona 'Completar Reporte' a continuación."
                                    isAskingAi = false
                                }
                            }
                        },
                        enabled = !isAskingAi,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAskingAi) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analizando con Asistente Botánico...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generar Guía de Reporte (3 Puntos)", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Resultado en 3 Puntos Clave
                    if (aiAnswer != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Spa,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3 Puntos Clave del Asistente:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = aiAnswer!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Botón de Acción Directo para Usuarios Nuevos
                        Button(
                            onClick = {
                                showAiConsultantDialog = false
                                onNavigateToCreateReport()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Completar Reporte de Madera", maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiConsultantDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // 3. MODAL DE DETALLE BOTÁNICO
    if (selectedSpecies != null) {
        val sp = selectedSpecies!!
        var isGeneratingCareGuide by remember { mutableStateOf(false) }
        var careGuideText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { selectedSpecies = null },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sp.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(sp.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = sp.commonName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (sp.category) {
                                    "Arbusto" -> Icons.Filled.Nature
                                    "Flor / Planta" -> Icons.Filled.LocalFlorist
                                    else -> Icons.Filled.Forest
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = sp.commonName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = sp.scientificName,
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (sp.family.isNotBlank()) {
                            Text(
                                text = "Familia botánica: ${sp.family}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = sp.description,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Región: ${sp.regionGroup}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sp.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hábitat: ${sp.nativeRegion}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Co2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (sp.co2AbsorptionPerYearKg < 0) "Captura de CO₂: sin datos verificados" else "Estimación de referencia: ${sp.co2AbsorptionPerYearKg} kg CO₂ / año",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estado: ${sp.conservationStatus}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // BOTÓN PARA CONSULTAR GUÍA DE CUIDADOS INTELIGENTE
                    if (careGuideText == null) {
                        OutlinedButton(
                            onClick = {
                                isGeneratingCareGuide = true
                                coroutineScope.launch {
                                    val guide = ForestAiService.getCareGuide(sp.commonName, sp.regionGroup)
                                    careGuideText = guide.getOrNull() ?: "Guía disponible próximamente."
                                    isGeneratingCareGuide = false
                                }
                            },
                            enabled = !isGeneratingCareGuide,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingCareGuide) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generando guía de cuidados...")
                            } else {
                                Icon(Icons.Filled.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guía de Siembra y Cuidados")
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Recomendaciones de Siembra y Cuidado:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = careGuideText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSpecies = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // 4. REGISTRO MANUAL DE ESPECIE
    if (showAddDialog) {
        var commonName by remember { mutableStateOf("") }
        var scientificName by remember { mutableStateOf("") }
        var family by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Árbol") }
        var regionGroup by remember { mutableStateOf("Andina") }
        var season by remember { mutableStateOf("Primavera") }
        var co2Absorption by remember { mutableStateOf("25") }
        var nativeRegion by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("Registrar Especie Forestal", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Región Biogeográfica:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        regions.filter { it != "Todas" }.forEach { reg ->
                            FilterChip(
                                selected = regionGroup == reg,
                                onClick = { regionGroup = reg },
                                label = { Text(reg) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Categoría:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Árbol", "Arbusto", "Flor / Planta").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = commonName,
                        onValueChange = { commonName = it },
                        label = { Text("Nombre Común (ej. Guayacán)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scientificName,
                        onValueChange = { scientificName = it },
                        label = { Text("Nombre Científico (ej. Handroanthus)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = family,
                        onValueChange = { family = it },
                        label = { Text("Familia Botánica (ej. Bignoniaceae)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Enlace de Fotografía") },
                        leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = co2Absorption,
                        onValueChange = { co2Absorption = it },
                        label = { Text("Fijación CO₂ (kg/año)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nativeRegion,
                        onValueChange = { nativeRegion = it },
                        label = { Text("Hábitat Detallado (ej. Bosque Seco Tropical)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción botánica y ecológica") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (commonName.isNotBlank() && scientificName.isNotBlank()) {
                            val newSpecies = TreeSpecies(
                                id = "sp_${UUID.randomUUID().toString().take(8)}",
                                commonName = commonName.trim(),
                                scientificName = scientificName.trim(),
                                season = season,
                                co2AbsorptionPerYearKg = co2Absorption.toIntOrNull() ?: 20,
                                description = description.ifBlank { "Espécimen vegetal registrado por la comunidad." },
                                nativeRegion = nativeRegion.ifBlank { "$regionGroup, Colombia" },
                                conservationStatus = "Evaluación comunitaria",
                                tag = category,
                                category = category,
                                imageUrl = imageUrl.trim(),
                                family = family.trim(),
                                regionGroup = regionGroup
                            )
                            coroutineScope.launch {
                                SpeciesRepository.addSpecies(newSpecies)
                                Toast.makeText(context, "Especie guardada en tu catálogo", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Por favor completa el nombre común y científico", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar Especie")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            WoodMatchTopAppBar(
                title = "Catálogo Forestal",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = { showAiConsultantDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.testTag("btn_ai_consultant")
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = "Consultor Forestal")
                }

                FloatingActionButton(
                    onClick = { showScannerDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("btn_scan_flora")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Escanear Especie")
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("btn_add_species")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Registrar Especie")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .testTag("species_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // BANNER ESCÁNER BOTÁNICO INTELIGENTE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScannerDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Escáner Botánico",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Fotografía una hoja, flor o corteza para reconocer al instante la especie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Búsqueda rápida
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("species_search_input"),
                    placeholder = {
                        Text(
                            text = "Buscar ej: rosa, tulipán, girasol, Quercus...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSearchingOnline) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { performOnlineSearch(searchQuery) }) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDownload,
                                        contentDescription = "Buscar en línea",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    onlineSearchMessage = null
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performOnlineSearch(searchQuery) }),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSearchingOnline) { performOnlineSearch(searchQuery) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "¿Consultar '$searchQuery' en Pl@ntNet API?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Buscar en línea",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FILTRO DE REGIÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FILTRAR POR REGIÓN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isQueryingRegion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    regions.forEach { reg ->
                        val isSelected = selectedRegion == reg
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectRegion(reg) },
                            label = { Text(reg) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filtros secundarios
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }

                    seasons.filter { it != "Todos" }.forEach { season ->
                        FilterChip(
                            selected = selectedSeason == season,
                            onClick = { selectedSeason = if (selectedSeason == season) "Todos" else season },
                            label = { Text(season) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Conteo de especies
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedRegion == "Todas") {
                            "${filteredSpecies.size} especies en catálogo"
                        } else {
                            "${filteredSpecies.size} especies en $selectedRegion"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Flora Neotropical",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredSpecies.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Sin resultados locales para \"$searchQuery\"" else "No hay especies con estos filtros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "Puedes consultar en vivo la base botánica global de Pl@ntNet (más de 300.000 especies catalogadas) para extraer este taxón e incorporarlo a la app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { performOnlineSearch(searchQuery) },
                                    enabled = !isSearchingOnline,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSearchingOnline) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Consultando Pl@ntNet API...")
                                    } else {
                                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Buscar \"$searchQuery\" en Pl@ntNet")
                                    }
                                }

                                if (onlineSearchMessage != null) {
                                    Text(
                                        text = onlineSearchMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lista de Tarjetas Botánicas
            items(filteredSpecies) { species ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSpecies = species }
                        .testTag("species_card_${species.id}"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (species.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(species.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = species.commonName,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (species.category) {
                                        "Arbusto" -> Icons.Filled.Nature
                                        "Flor / Planta" -> Icons.Filled.LocalFlorist
                                        else -> Icons.Filled.Forest
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = species.commonName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = species.regionGroup,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = species.scientificName,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (species.family.isNotBlank()) {
                                Text(
                                    text = "Fam: ${species.family}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = species.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (species.co2AbsorptionPerYearKg < 0) "CO₂: sin datos" else "~${species.co2AbsorptionPerYearKg} kg CO₂/año (referencia)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = species.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}
