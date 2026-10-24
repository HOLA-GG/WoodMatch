package com.example.kotlinapp.ui.pilot

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.network.ForestAiService
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.components.PhotoAttachment
import com.example.kotlinapp.ui.screens.OsmNativeMapView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

val MaterialKinds =
    listOf(
        "Árbol",
        "Podas y ramas",
        "Troncos",
        "Estibas",
        "Tablas",
        "Muebles",
        "Madera recuperable",
    )

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PublicationWizard(
    existing: WoodWasteItem? = null,
    imported: TreeReport? = null,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val user by AuthRepository.currentUser.collectAsState()
    if (!AuthRepository.isAuthenticated) {
        AccessMessage("Inicia sesión para publicar hallazgos y ofrecer material.", onBack)
        return
    }
    val scope = rememberCoroutineScope()
    val prefs = remember { PilotPreferences.store(context) }
    val draftKey = "draft_${user?.uid}_${existing?.id ?: imported?.id ?: "new"}"
    val saved =
        remember(draftKey) {
            runCatching { JSONObject(prefs.getString(draftKey, "{}")!!) }.getOrDefault(JSONObject())
        }
    var id by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("id").ifBlank { existing?.id ?: PilotApi.operationId() })
        }
    var step by rememberSaveable(draftKey) { mutableIntStateOf(saved.optInt("step", 0)) }
    var kind by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("kind", existing?.entryType ?: "HALLAZGO"))
        }
    var title by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("title", existing?.title ?: ""))
        }
    var description by
        rememberSaveable(draftKey) {
            mutableStateOf(
                saved.optString("description", existing?.description ?: imported?.description ?: "")
            )
        }
    var material by
        rememberSaveable(draftKey) {
            mutableStateOf(
                saved.optString(
                    "material",
                    existing?.wasteType?.takeIf { it in MaterialKinds } ?: "Madera recuperable",
                )
            )
        }
    var condition by
        rememberSaveable(draftKey) {
            mutableStateOf(
                saved.optString(
                    "condition",
                    existing?.materialState ?: imported?.condition ?: "Por revisar",
                )
            )
        }
    var category by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("category", existing?.category ?: "ESTANDAR"))
        }
    var quantity by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("quantity", existing?.quantityValue?.toString() ?: ""))
        }
    var unit by
        rememberSaveable(draftKey) {
            mutableStateOf(
                saved.optString(
                    "unit",
                    existing?.quantityUnit?.takeIf {
                        it in listOf("unidades", "bultos", "kg", "m³")
                    } ?: "unidades",
                )
            )
        }
    var species by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("species", existing?.speciesName ?: ""))
        }
    var photo by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optString("photo").ifBlank { imported?.imageUri.orEmpty() })
        }
    var latitude by
        rememberSaveable(draftKey) {
            mutableDoubleStateOf(
                saved.optDouble("latitude", existing?.exactLatitude ?: imported?.latitude ?: 4.745)
            )
        }
    var longitude by
        rememberSaveable(draftKey) {
            mutableDoubleStateOf(
                saved.optDouble(
                    "longitude",
                    existing?.exactLongitude ?: imported?.longitude ?: -74.085,
                )
            )
        }
    var selectedPoint by
        rememberSaveable(draftKey) {
            mutableStateOf(saved.optBoolean("selectedPoint", existing?.locationConfirmed == true))
        }
    var consent by rememberSaveable(draftKey) { mutableStateOf(false) }
    var authorized by rememberSaveable(draftKey) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var leave by remember { mutableStateOf(false) }
    var remotePhoto by remember(existing?.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(existing?.imagePath) {
        if (existing != null)
            runCatching { WoodWasteRepository.photoUrl(existing) }.onSuccess { remotePhoto = it }
    }
    val sector =
        SubaSectors.ALL.minByOrNull {
            (it.referenceLatitude - latitude) * (it.referenceLatitude - latitude) +
                (it.referenceLongitude - longitude) * (it.referenceLongitude - longitude)
        }!!
    val draft =
        JSONObject()
            .put("id", id)
            .put("step", step)
            .put("kind", kind)
            .put("title", title)
            .put("description", description)
            .put("material", material)
            .put("condition", condition)
            .put("category", category)
            .put("quantity", quantity)
            .put("unit", unit)
            .put("species", species)
            .put("photo", photo)
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("selectedPoint", selectedPoint)
            .toString()
    SideEffect { prefs.edit().putString(draftKey, draft).apply() }
    val operation = rememberSaveable(draft, consent, authorized) { PilotApi.operationId() }
    BackHandler(enabled = !busy) { leave = true }
    fun locate() {
        val permitted =
            listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
                .any {
                    ContextCompat.checkSelfPermission(context, it) ==
                        PackageManager.PERMISSION_GRANTED
                }
        if (!permitted) {
            message = "Puedes elegir el punto tocando el mapa."
            return
        }
        locating = true
        scope.launch {
            val cancellation = CancellationTokenSource()
            try {
                val location =
                    withTimeoutOrNull(15000) {
                        LocationServices.getFusedLocationProviderClient(context)
                            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                            .await()
                    }
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    selectedPoint = true
                    consent = false
                    message =
                        "Ubicación obtenida (precisión aproximada: ${location.accuracy.toInt()} m). Revisa el punto antes de publicar."
                } else message = "No se obtuvo tu ubicación. Elige el punto en el mapa."
            } catch (_: SecurityException) {
                message = "No hay permiso de ubicación. Puedes elegir el punto en el mapa."
            } catch (_: Exception) {
                message = "No se pudo obtener tu ubicación. Elige el punto en el mapa."
            } finally {
                cancellation.cancel()
                locating = false
            }
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            locate()
        }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row {
            IconButton(enabled = !busy, onClick = { leave = true }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver y conservar borrador")
            }
            Column {
                Text(
                    if (existing != null) "Editar publicación"
                    else "Comparte una nueva oportunidad",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Paso ${step+1} de 3 · Borrador guardado",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())
        message?.let { StatusMessage(it) }
        when (step) {
            0 -> {
                Text("Foto y ubicación", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Un hallazgo puede ser un árbol, un mueble o madera recuperable. Ofrece únicamente material que puedas entregar."
                )
                ChoiceRow(
                    listOf("HALLAZGO" to "Reportar hallazgo", "OFERTA" to "Ofrecer material"),
                    kind,
                ) {
                    if (it == "OFERTA" && user?.canPublish != true)
                        message =
                            "Para ofrecer material, cambia tu perfil a generador o mixto en Ajustes."
                    else {
                        kind = it
                        authorized = false
                    }
                }
                if (photo.isNotBlank() || remotePhoto != null)
                    AsyncImage(
                        model = photo.ifBlank { remotePhoto.orEmpty() },
                        contentDescription = "Fotografía de la publicación",
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                    )
                PhotoAttachment { uri ->
                    photo = uri.toString()
                    message = null
                }
                Text("La foto se asociará al punto que confirmes, aunque provenga de tu galería.")
                var guiding by remember { mutableStateOf(false) }
                OutlinedButton(
                    enabled = !guiding,
                    onClick = {
                        guiding = true
                        scope.launch {
                            ForestAiService.guideNewUserReport(material, sector.name, description)
                                .onSuccess { message = it }
                                .onFailure {
                                    message = it.message ?: "No se pudo consultar la guía. Intenta otra vez."
                                }
                            guiding = false
                        }
                    },
                ) {
                    Text(if (guiding) "Consultando Gemini…" else "Guía asistida del reporte")
                }
                OutlinedButton(
                    enabled = !locating,
                    onClick = {
                        permission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    },
                ) {
                    Text(if (locating) "Buscando ubicación…" else "Usar mi ubicación")
                }
                Text(
                    if (selectedPoint) "Punto elegido · ${sector.name}"
                    else
                        "Toca el mapa para elegir el punto. El centro inicial es solo una referencia."
                )
                OsmNativeMapView(
                    wasteItems =
                        if (selectedPoint)
                            listOf(
                                WoodWasteItem(
                                    id = id,
                                    title = "Punto elegido",
                                    exactLatitude = latitude,
                                    exactLongitude = longitude,
                                )
                            )
                        else emptyList(),
                    centerLat = latitude,
                    centerLng = longitude,
                    zoomLevel = 15,
                    onWasteSelected = {},
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    onPointSelected = { lat, lng ->
                        latitude = lat
                        longitude = lng
                        selectedPoint = true
                        consent = false
                    },
                )
                var latInput by
                    rememberSaveable(latitude) {
                        mutableStateOf("%.6f".format(java.util.Locale.US, latitude))
                    }
                var lngInput by
                    rememberSaveable(longitude) {
                        mutableStateOf("%.6f".format(java.util.Locale.US, longitude))
                    }
                Text("También puedes ajustar las coordenadas sin usar gestos en el mapa.")
                OutlinedTextField(
                    value = latInput,
                    onValueChange = { latInput = it },
                    label = { Text("Latitud") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lngInput,
                    onValueChange = { lngInput = it },
                    label = { Text("Longitud") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TextButton(
                    onClick = {
                        val lat = latInput.replace(',', '.').toDoubleOrNull()
                        val lng = lngInput.replace(',', '.').toDoubleOrNull()
                        if (
                            lat != null && lng != null && lat in 4.66..4.86 && lng in -74.18..-74.01
                        ) {
                            latitude = lat
                            longitude = lng
                            selectedPoint = true
                            consent = false
                            message = null
                        } else message = "Elige un punto en el área de atención de Suba."
                    }
                ) {
                    Text("Aplicar coordenadas")
                }
            }
            1 -> {
                Text("Cuéntanos qué encontraste", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Título") },
                    placeholder = { Text("Ej. Estibas para un nuevo proyecto") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Tipo de material", style = MaterialTheme.typography.titleMedium)
                ChoiceRow(MaterialKinds.map { it to it }, material) { material = it }
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it.take(120) },
                    label = { Text("Condición física") },
                    supportingText = { Text("Ej. Caído, limpio, pintado, requiere revisión") },
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceRow(WasteCategory.entries.map { it.name to it.displayName }, category) {
                    category = it
                }
                if (kind == "OFERTA") {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.take(15) },
                        label = { Text("Cantidad aproximada") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    ChoiceRow(listOf("unidades", "bultos", "kg", "m³").map { it to it }, unit) {
                        unit = it
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(3000) },
                    label = { Text("Descripción o referencia (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it.take(160) },
                    label = { Text("Especie, si la conoces (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                var asking by remember { mutableStateOf(false) }
                OutlinedButton(
                    enabled = !asking,
                    onClick = {
                        asking = true
                        scope.launch {
                            ForestAiService.guideNewUserReport(material, sector.name, description)
                                .onSuccess { message = it }
                                .onFailure { message = it.message }
                            asking = false
                        }
                    },
                ) {
                    Text(if (asking) "Consultando…" else "Ayúdame a describirlo")
                }
            }
            else -> {
                Text("Revisa antes de compartir", style = MaterialTheme.typography.headlineMedium)
                Card {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${if(kind=="OFERTA") "Material ofrecido" else "Hallazgo"} · $material"
                        )
                        Text("$condition · ${sector.name}")
                        if (kind == "OFERTA") Text("$quantity $unit (aprox.)")
                        if (description.isNotBlank()) Text(description)
                    }
                }
                CheckLine(
                    consent,
                    "Confirmo el punto y acepto que su ubicación exacta y la foto sean visibles para la comunidad con cuenta.",
                ) {
                    consent = it
                }
                if (kind == "OFERTA")
                    CheckLine(
                        authorized,
                        "Tengo autorización para ofrecer y entregar este material.",
                    ) {
                        authorized = it
                    }
                Text(
                    "Si es un hallazgo, no se podrá reservar hasta que lo conviertas en material ofrecido con la autorización correspondiente."
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) OutlinedButton(enabled = !busy, onClick = { step-- }) { Text("Anterior") }
            Button(
                enabled = !busy,
                onClick = {
                    message = null
                    val amount = quantity.replace(',', '.').toDoubleOrNull()
                    if (step == 0 && (photo.isBlank() && existing?.imagePath == null))
                        message = "Adjunta una foto."
                    else if (
                        step == 0 &&
                            (!selectedPoint ||
                                latitude !in 4.66..4.86 ||
                                longitude !in -74.18..-74.01)
                    )
                        message = "Confirma un punto en el área de atención de Suba."
                    else if (step == 1 && (title.trim().length < 3 || condition.isBlank()))
                        message = "Escribe un título de al menos tres caracteres y la condición."
                    else if (
                        step == 1 &&
                            kind == "OFERTA" &&
                            (amount == null ||
                                !amount.isFinite() ||
                                amount <= 0 ||
                                amount > 1000000)
                    )
                        message = "Revisa la cantidad."
                    else if (step < 2) step++
                    else if (!consent || (kind == "OFERTA" && !authorized))
                        message = "Confirma la ubicación y, si corresponde, tu autorización."
                    else {
                        busy = true
                        val item =
                            WoodWasteItem(
                                id = id,
                                schemaVersion = 3,
                                revision = existing?.revision ?: 0,
                                entryType = kind,
                                title = title.trim(),
                                description = description.trim(),
                                wasteType = material,
                                materialState = condition,
                                category = category,
                                quantityValue = amount ?: 0.0,
                                quantityUnit = unit,
                                subaSector = sector.name,
                                exactLatitude = latitude,
                                exactLongitude = longitude,
                                locationConfirmed = selectedPoint,
                                exactLocationConsent = consent,
                                authorizedToOffer = authorized,
                                speciesName = species,
                                imagePath = existing?.imagePath,
                            )
                        scope.launch {
                            WoodWasteRepository.saveEntry(
                                    item,
                                    photo.takeIf { it.isNotBlank() },
                                    operation,
                                    editing = existing != null,
                                )
                                .onSuccess {
                                    prefs.edit().remove(draftKey).apply()
                                    onDone()
                                }
                                .onFailure {
                                    message =
                                        it.message
                                            ?: "No se pudo publicar. Tu borrador sigue guardado."
                                }
                            busy = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (busy) "Guardando…" else if (step < 2) "Continuar" else "Publicar")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (leave)
        AlertDialog(
            onDismissRequest = { leave = false },
            title = { Text("Tu borrador está guardado") },
            text = { Text("Puedes retomarlo al volver a Crear.") },
            confirmButton = { TextButton(onClick = onBack) { Text("Guardar y salir") } },
            dismissButton = {
                TextButton(onClick = { leave = false }) { Text("Continuar editando") }
            },
        )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(options: List<Pair<String, String>>, value: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (id, label) ->
            FilterChip(selected = value == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
fun CheckLine(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, modifier = Modifier.padding(top = 12.dp).weight(1f))
    }
}

@Composable
fun AccessMessage(message: String, onBack: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack) { Text("Volver") }
    }
}

@Composable
fun StatusMessage(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            message,
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
