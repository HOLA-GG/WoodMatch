package com.example.kotlinapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.model.WasteCategory
import com.example.kotlinapp.data.model.WoodWasteItem
import com.example.kotlinapp.data.network.CircularEconomyAiService
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.WoodWasteRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishWoodWasteScreen(
    onNavigateBack: () -> Unit,
    onPublishSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentUser by AuthRepository.currentUser.collectAsState()

    // Form fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var wasteType by remember { mutableStateOf("Ramas y podas de arbolado") }
    var materialState by remember { mutableStateOf("Verde recién cortado") }
    var category by remember { mutableStateOf(WasteCategory.ESTANDAR.name) }

    // Cantidades estructuradas (sin métricas engañosas)
    var quantityValueStr by remember { mutableStateOf("1.0") }
    var quantityUnit by remember { mutableStateOf("m³") }
    var quantityEstimated by remember { mutableStateOf(true) }

    // Ubicación protegida en Suba
    var selectedSector by remember { mutableStateOf(currentUser?.sectorSuba ?: "Suba Centro") }
    var sectorExpanded by remember { mutableStateOf(false) }
    var referenceAddress by remember { mutableStateOf("") }

    // Especie botánica y soporte
    var speciesName by remember { mutableStateOf("Especie no identificada") }
    var isSpeciesKnown by remember { mutableStateOf(false) }

    // Sugerencias orientativas de IA
    var suggestedUses by remember { mutableStateOf("") }
    var technicalNotes by remember { mutableStateOf("") }
    var isAiAssisted by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }

    // Imagen
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerAiAssistance() {
        isAiLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val suggestion = CircularEconomyAiService.suggestValorization(
                    wasteType = wasteType,
                    materialState = materialState,
                    speciesName = speciesName
                )
                category = suggestion.recommendedCategory.name
                suggestedUses = suggestion.suggestedUses
                technicalNotes = suggestion.technicalNotes
                isAiAssisted = true
            } catch (e: Exception) {
                errorMessage = "No fue posible conectar con el asistente de IA. Puedes ingresar las sugerencias manualmente."
            } finally {
                isAiLoading = false
            }
        }
    }

    fun submitPublication() {
        val qVal = quantityValueStr.toDoubleOrNull()
        if (title.isBlank()) {
            errorMessage = "El título de la publicación es obligatorio."
            return
        }
        if (qVal == null || qVal <= 0.0) {
            errorMessage = "Ingresa una cantidad numérica válida mayor a 0."
            return
        }

        val sectorObj = SubaSectors.findByName(selectedSector)
            ?: SubaSectors.ALL.first()

        val author = currentUser

        isSubmitting = true
        errorMessage = null

        coroutineScope.launch {
            val volumeM3 = if (quantityUnit == "m³") qVal else null

            val item = WoodWasteItem(
                title = title.trim(),
                description = description.trim().ifBlank { "Material leñoso disponible en Suba." },
                wasteType = wasteType,
                materialState = materialState,
                category = category,
                quantityValue = qVal,
                quantityUnit = quantityUnit,
                volumeM3 = volumeM3,
                quantityEstimated = quantityEstimated,
                subaSector = selectedSector,
                exactLatitude = sectorObj.referenceLatitude,
                exactLongitude = sectorObj.referenceLongitude,
                referenceAddress = referenceAddress.trim(),
                speciesName = if (isSpeciesKnown && speciesName.isNotBlank()) speciesName.trim() else "Especie no identificada",
                suggestedUses = suggestedUses.trim(),
                isAiAssisted = isAiAssisted,
                authorId = author?.uid ?: "gen_carlos",
                authorName = author?.name ?: "Generador Suba",
                authorPhone = author?.phone ?: "+573001234567",
                imageUri = imageUri?.toString()
            )

            val result = WoodWasteRepository.publishWasteItem(item)
            isSubmitting = false
            if (result.isSuccess) {
                onPublishSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Error al publicar residuo."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("publish_waste_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Publicar Residuo Leñoso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Economía Circular • Localidad de Suba",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Recycling,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Al registrar este material leñoso, facilitas su rescate por carpinterías, artesanos y composteras locales, evitando su llegada a rellenos sanitarios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Card Principal Formulario
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título de la Publicación") },
                        placeholder = { Text("Ej. Ramas secas de poda o Trozas de pino") },
                        leadingIcon = { Icon(Icons.Filled.Nature, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_title_input")
                    )

                    // Tipo de Residuo Leñoso
                    Text(
                        text = "Tipo de Material Leñoso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val wasteTypes = listOf(
                        "Ramas y podas de arbolado",
                        "Troncos y rollizos",
                        "Pallets y embalajes",
                        "Astillas / Viruta / Aserrín",
                        "Retazos de carpintería",
                        "Biomasa mixta"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        wasteTypes.chunked(2).forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { wt ->
                                    FilterChip(
                                        selected = wasteType == wt,
                                        onClick = { wasteType = wt },
                                        label = { Text(wt, style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Estado del Material
                    Text(
                        text = "Estado del Material",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val states = listOf(
                        "Verde recién cortado",
                        "Seco / Estacionado",
                        "Curado / Tratado",
                        "Degradado / Compost"
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(states) { st ->
                            FilterChip(
                                selected = materialState == st,
                                onClick = { materialState = st },
                                label = { Text(st, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Cantidades Estructuradas
                    Text(
                        text = "Cantidad y Volumen Declarado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = quantityValueStr,
                        onValueChange = { quantityValueStr = it },
                        label = { Text("Valor Numérico") },
                        placeholder = { Text("Ej. 2.5") },
                        leadingIcon = { Icon(Icons.Filled.Numbers, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_quantity_value_input")
                    )

                    // Selector de Unidad
                    Text(
                        text = "Unidad de Medida",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val units = listOf("m³" to "m³ (Metros cúbicos)", "kg" to "kg (Kilogramos)", "bultos" to "Bultos / Sacos", "unidades" to "Unidades")
                        items(units) { (unitVal, unitLabel) ->
                            val isSelected = quantityUnit == unitVal
                            FilterChip(
                                selected = isSelected,
                                onClick = { quantityUnit = unitVal },
                                label = { Text(unitLabel, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Toggle Estimado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Es una estimación aproximada?",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (quantityEstimated) "Dato visual preliminar" else "Dato pesado o medido con precisión",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = quantityEstimated,
                            onCheckedChange = { quantityEstimated = it },
                            modifier = Modifier.testTag("publish_estimated_switch")
                        )
                    }

                    // Sector de Suba
                    Text(
                        text = "Sector en Suba (Ubicación aproximada pública)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = sectorExpanded,
                        onExpandedChange = { sectorExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedSector,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sector / Barrio de Suba") },
                            leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectorExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("publish_sector_input")
                        )
                        ExposedDropdownMenu(
                            expanded = sectorExpanded,
                            onDismissRequest = { sectorExpanded = false }
                        ) {
                            SubaSectors.ALL.forEach { sec ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sec.name, fontWeight = FontWeight.Bold)
                                            Text(sec.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        selectedSector = sec.name
                                        sectorExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = referenceAddress,
                        onValueChange = { referenceAddress = it },
                        label = { Text("Punto de Referencia (Opcional)") },
                        placeholder = { Text("Ej. Cerca a estación Suba Rincón, parque barrial") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Especie Botánica
                    Text(
                        text = "Especie del Árbol (Opcional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !isSpeciesKnown,
                            onClick = { isSpeciesKnown = false; speciesName = "Especie no identificada" },
                            label = { Text("Desconocida / Variada") },
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = isSpeciesKnown,
                            onClick = { isSpeciesKnown = true },
                            label = { Text("Especificar especie") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (isSpeciesKnown) {
                        OutlinedTextField(
                            value = speciesName,
                            onValueChange = { speciesName = it },
                            label = { Text("Nombre o género botánico") },
                            placeholder = { Text("Ej. Acacia, Pino pátula, Eucalipto, Sauce") },
                            leadingIcon = { Icon(Icons.Filled.Forest, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ASISTENCIA IA PARA VALORIZACIÓN CIRCULAR (ORIENTATIVA)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Asistente IA",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Button(
                                    onClick = { triggerAiAssistance() },
                                    enabled = !isAiLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.testTag("publish_trigger_ai_button")
                                ) {
                                    if (isAiLoading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Sugerir Usos", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                }
                            }

                            Text(
                                text = "El asistente de IA sugiere categorías y posibles destinos circulares basándose en el tipo y estado de la madera. Todas las sugerencias son editables.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )

                            // Categoría de aprovechamiento
                            Text(
                                text = "Categoría recomendada:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val categories = listOf(
                                    WasteCategory.BIOMASA.name to "Biomasa / Suelo",
                                    WasteCategory.ESTANDAR.name to "Madera Estándar",
                                    WasteCategory.FINA.name to "Madera Fina / Torno"
                                )
                                items(categories) { (catKey, label) ->
                                    FilterChip(
                                        selected = category == catKey,
                                        onClick = { category = catKey },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            // Usos recomendados (editable por el usuario)
                            OutlinedTextField(
                                value = suggestedUses,
                                onValueChange = { suggestedUses = it },
                                label = { Text("Usos Circulares Recomendados (Editable)") },
                                placeholder = { Text("Ej. Construcción ligera, compostaje, artesanías, tutores...") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (technicalNotes.isNotBlank()) {
                                Text(
                                    text = "Nota técnica: $technicalNotes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Disclaimer obligatorio
                            Text(
                                text = "Aviso: Sugerencia orientativa generada por IA. No constituye certificación estructural ni resistencia mecánica.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Foto del residuo
                    Text(
                        text = "Fotografía del Residuo (Opcional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("publish_select_photo_button")
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (imageUri != null) "Cambiar Foto" else "Seleccionar Foto")
                        }

                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Foto del residuo leñoso",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    // Descripción General
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción Adicional y Condiciones de Retiro") },
                        placeholder = { Text("Ej. Fácil acceso para camioneta, ramas trozadas en tramos de 1 metro...") },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error Message
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_error_message")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botón de Publicación
                    Button(
                        onClick = { submitPublication() },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Publicar Residuo en Suba",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
