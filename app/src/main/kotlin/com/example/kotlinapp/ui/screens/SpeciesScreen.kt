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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    var showScannerDialog by remember { mutableStateOf(false) }

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
        var selectedImagePath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
        val selectedImageUri = selectedImagePath?.let(Uri::parse)
        var imageUrlInput by remember { mutableStateOf("") }
        var selectedOrgan by remember { mutableStateOf("auto") }
        var isIdentifying by remember { mutableStateOf(false) }
        var identificationResult by remember { mutableStateOf<PlantNetIdentificationResult?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }


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

                    com.example.kotlinapp.ui.components.PhotoAttachment { uri ->
                        selectedImagePath = uri.toString()
                        imageUrlInput = ""
                        identificationResult = null
                        errorMessage = null
                    }

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
                            Text("Buscar coincidencias")
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
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Candidato de Pl@ntNet",
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
                                                text = "Coincidencia: ${it.scorePercentage}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (bestCandidate != null) {
                                    Text(
                                        text = if (bestCandidate.score < 0.5)
                                            "La coincidencia es baja. Prueba una foto más clara de una hoja, flor o corteza y compara los candidatos; no confirma la especie."
                                        else "La puntuación ordena coincidencias del servicio; no certifica la identidad ni es una probabilidad de acierto.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        text = "Parte consultada: " + when (selectedOrgan) {
                                            "leaf" -> "Hoja"
                                            "flower" -> "Flor"
                                            "fruit" -> "Fruto"
                                            "bark" -> "Corteza"
                                            else -> "Selección automática"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Colores, tallo, apariencia y cuidados no vienen incluidos en esta identificación. Puedes pedir orientación botánica en la ficha del candidato.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            selectedSpecies = bestCandidate.toTreeSpecies("Sin verificar")
                                            showScannerDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.LocalFlorist, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ver ficha del candidato")
                                    }
                                    if (res.candidates.size > 1) {
                                        Text("Otros candidatos", style = MaterialTheme.typography.titleSmall)
                                        res.candidates.drop(1).forEach { candidate ->
                                            OutlinedButton(
                                                onClick = {
                                                    selectedSpecies = candidate.toTreeSpecies("Sin verificar")
                                                    showScannerDialog = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(Modifier.fillMaxWidth()) {
                                                    Text(candidate.primaryCommonName)
                                                    Text(
                                                        "${candidate.scientificName} · Coincidencia ${candidate.scorePercentage}%",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
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
    // 3. MODAL DE DETALLE BOTÁNICO
    if (selectedSpecies != null) {
        val sp = selectedSpecies!!
        var isGeneratingCareGuide by remember(sp.id) { mutableStateOf(false) }
        var careGuideText by remember(sp.id) { mutableStateOf<String?>(null) }
        var careGuideError by remember(sp.id) { mutableStateOf<String?>(null) }

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
                        if (sp.id.startsWith("pn_")) {
                            Text(
                                "Imagen de referencia de Pl@ntNet; no es tu fotografía.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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

                    Text(
                        "Apariencia, colores, tallo y cuidados",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Estos datos no están incluidos en la respuesta de Pl@ntNet. El asistente botánico puede ofrecer una descripción general del candidato; no observa ni verifica la planta de tu foto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    careGuideError?.let { problem ->
                        Text(problem, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    // La orientación permanece separada from the provider's identification data.
                    if (careGuideText == null) {
                        OutlinedButton(
                            onClick = {
                                isGeneratingCareGuide = true
                                careGuideError = null
                                coroutineScope.launch {
                                    try {
                                        ForestAiService.getCareGuide(sp.scientificName, "Suba, Bogotá")
                                            .onSuccess { careGuideText = it }
                                            .onFailure { careGuideError = it.message ?: "No se pudo consultar la orientación. Intenta otra vez." }
                                    } finally {
                                        isGeneratingCareGuide = false
                                    }
                                }
                            },
                            enabled = !isGeneratingCareGuide,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingCareGuide) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Consultando orientación…")
                            } else {
                                Icon(Icons.Filled.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (careGuideError != null) "Reintentar orientación botánica" else "Consultar ficha orientativa")
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
                                    text = "Orientación generada · Requiere revisión",
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

    Scaffold(
        topBar = {
            WoodMatchTopAppBar(
                title = "Guía botánica",
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
                    onClick = { showScannerDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("btn_scan_flora")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Escanear Especie")
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
                                    text = "¿Consultar '$searchQuery' en Pl@ntNet?",
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

                Text(
                    "Filtros del catálogo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (isQueryingRegion) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Actualizando especies…", style = MaterialTheme.typography.labelSmall)
                    }
                }
                SpeciesFilterMenu(
                    label = "Región",
                    selected = selectedRegion,
                    options = regions,
                    onSelect = onSelectRegion,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpeciesFilterMenu(
                        label = "Categoría",
                        selected = selectedCategory,
                        options = categories,
                        onSelect = { selectedCategory = it },
                        modifier = Modifier.weight(1f),
                    )
                    SpeciesFilterMenu(
                        label = "Temporada",
                        selected = selectedSeason,
                        options = seasons,
                        onSelect = { selectedSeason = it },
                        modifier = Modifier.weight(1f),
                    )
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
                                        Text("Consultando Pl@ntNet...")
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



@Composable
private fun SpeciesFilterMenu(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    selected,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
