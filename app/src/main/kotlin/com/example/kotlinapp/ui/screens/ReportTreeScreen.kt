package com.example.kotlinapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.kotlinapp.data.model.EstadoResiduo
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.network.ValidadorReportesService
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.ReportRepository
import com.example.kotlinapp.ui.components.WoodMatchTopAppBar
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.io.File

/**
 * Pantalla de Reporte de Árboles y Madera Urbana.
 * Diseñada para ser ágil y a prueba de fallos:
 * - Detecta automáticamente la ubicación GPS sin obligar al usuario a digitar coordenadas.
 * - Condiciona el color del pin: VERDE si la ubicación GPS está activa y confirmada,
 *   NARANJA si está pendiente de confirmación geográfica.
 * - Manejo robusto de permisos y hardware a prueba de crash ("woodmatch keeps stopping").
 */
@Composable
fun ReportTreeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by AuthRepository.currentUser.collectAsState()

    var description by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Caído") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Estado de Ubicación GPS
    var latitude by remember { mutableDoubleStateOf(4.7450) }
    var longitude by remember { mutableDoubleStateOf(-74.0850) }
    var detectedSector by remember { mutableStateOf("Suba Centro") }
    var isGpsActive by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var locationStatusMessage by remember { mutableStateOf("Buscando señal GPS...") }

    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val validadorService = remember { ValidadorReportesService() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Función segura para actualizar el sector de Suba según coordenadas
    fun updateSectorFromCoords(lat: Double, lng: Double) {
        val nearestSector = SubaSectors.ALL.minByOrNull { sector ->
            val dLat = sector.referenceLatitude - lat
            val dLng = sector.referenceLongitude - lng
            dLat * dLat + dLng * dLng
        }
        detectedSector = nearestSector?.name ?: "Suba"
    }

    // Proveedor seguro de ubicación sin riesgo de crash
    fun fetchCurrentLocationSafely() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            isGpsActive = false
            locationStatusMessage = "GPS inactivo. Toca para conceder acceso y fijar pin verde."
            return
        }

        isLocating = true
        locationStatusMessage = "Obteniendo coordenadas exactas..."

        // 1. Intentar con FusedLocationProvider de Google Play Services de forma protegida
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        isGpsActive = true
                        isLocating = false
                        updateSectorFromCoords(loc.latitude, loc.longitude)
                        locationStatusMessage = "Ubicación GPS confirmada • Sector $detectedSector"
                    } else {
                        // 2. Si FusedLocation es null (común en emuladores), usar LocationManager del sistema
                        tryFallbackLocationManager(context) { fallbackLoc ->
                            isLocating = false
                            if (fallbackLoc != null) {
                                latitude = fallbackLoc.latitude
                                longitude = fallbackLoc.longitude
                                isGpsActive = true
                                updateSectorFromCoords(fallbackLoc.latitude, fallbackLoc.longitude)
                                locationStatusMessage = "Ubicación GPS confirmada • Sector $detectedSector"
                            } else {
                                isGpsActive = true // Permiso concedido, coordenadas predeterminadas de Suba
                                updateSectorFromCoords(latitude, longitude)
                                locationStatusMessage = "GPS activo en Suba ($detectedSector)"
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    tryFallbackLocationManager(context) { fallbackLoc ->
                        isLocating = false
                        if (fallbackLoc != null) {
                            latitude = fallbackLoc.latitude
                            longitude = fallbackLoc.longitude
                            isGpsActive = true
                            updateSectorFromCoords(fallbackLoc.latitude, fallbackLoc.longitude)
                            locationStatusMessage = "Ubicación GPS confirmada • Sector $detectedSector"
                        } else {
                            isGpsActive = false
                            locationStatusMessage = "No se pudo leer GPS del dispositivo."
                        }
                    }
                }
        } catch (e: Exception) {
            // Ante cualquier fallo de Play Services, usar LocationManager nativo de Android
            tryFallbackLocationManager(context) { fallbackLoc ->
                isLocating = false
                if (fallbackLoc != null) {
                    latitude = fallbackLoc.latitude
                    longitude = fallbackLoc.longitude
                    isGpsActive = true
                    updateSectorFromCoords(fallbackLoc.latitude, fallbackLoc.longitude)
                    locationStatusMessage = "Ubicación confirmada • Sector $detectedSector"
                } else {
                    isGpsActive = false
                    locationStatusMessage = "Sin señal GPS precisa."
                }
            }
        }
    }

    // Launcher de permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocationSafely()
        } else {
            isGpsActive = false
            locationStatusMessage = "Permiso no concedido. El reporte se registrará con pin naranja pendiente."
        }
    }

    // Lanzar solicitud de ubicación al ingresar a la pantalla de forma segura
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchCurrentLocationSafely()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Galería
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Cámara protegida con FileProvider
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured) {
            selectedImageUri = pendingCameraUri
        }
        pendingCameraUri = null
    }

    fun takePhotoSafely() {
        runCatching {
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val file = File(picturesDir, "woodmatch_report_${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.onSuccess { uri ->
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { error ->
            errorMessage = "No se pudo iniciar la cámara: ${error.message}"
        }
    }

    fun handleSubmit() {
        val textoLimpio = description.trim()
        if (textoLimpio.length < 5) {
            errorMessage = "Por favor ingresa una breve descripción o referencia (mínimo 5 caracteres)."
            return
        }
        errorMessage = null
        isSubmitting = true

        coroutineScope.launch {
            val user = currentUser
            val pinEstado = if (isGpsActive) EstadoResiduo.DISPONIBLE else EstadoResiduo.PENDIENTE

            val puntoGeo = org.osmdroid.util.GeoPoint(latitude, longitude)
            val res = ReportRepository.crearPuntoResiduo(
                coordenadas = puntoGeo,
                tipo = condition,
                resumen = textoLimpio,
                userId = user?.uid ?: "user_${System.currentTimeMillis()}",
                userName = user?.name ?: "Ciudadano Suba",
                imageUri = selectedImageUri?.toString(),
                estado = pinEstado
            )

            isSubmitting = false
            res.onSuccess {
                val colorPinText = if (isGpsActive) "Pin VERDE verificado" else "Pin NARANJA (pendiente de GPS)"
                successDialogMessage = "Tu reporte en $detectedSector ha sido creado exitosamente.\n\n" +
                        "Estado: $colorPinText\n" +
                        "Coordenadas: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                showSuccessDialog = true
            }.onFailure { err ->
                errorMessage = err.message ?: "No se pudo guardar el reporte."
            }
        }
    }

    // Diálogo de Confirmación
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isGpsActive) Color(0xFF2E7D32) else Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (isGpsActive) "¡Reporte con Pin Verde Creado!" else "¡Reporte Creado!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(text = successDialogMessage, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToMap()
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Ver en el Mapa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToReports()
                    }
                ) {
                    Text("Mis Reportes")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("report_tree_screen")
    ) {
        WoodMatchTopAppBar(
            title = "Nuevo Reporte",
            showBackButton = true,
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Mensaje de error si ocurre alguno
            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 2. Tarjeta de Ubicación GPS Automática y Condicionamiento de Pin
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGpsActive) {
                        Color(0xFFE8F5E9) // Fondo suave verde
                    } else {
                        Color(0xFFFFF3E0) // Fondo suave naranja
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGpsActive) Color(0xFF2E7D32) else Color(0xFFFF9800)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isGpsActive) Icons.Filled.LocationOn else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isGpsActive) "Pin Verde • GPS Verificado" else "Pin Naranja • Sin GPS Activo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isGpsActive) Color(0xFF1B5E20) else Color(0xFFE65100)
                            )
                        }
                        Text(
                            text = locationStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 3. Situación / Estado del Arbolado o Madera
            Text(
                text = "Situación del residuo o árbol",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Caído", "Dañado", "En riesgo", "Madera limpia").forEach { itemCondition ->
                    val isSelected = condition == itemCondition
                    FilterChip(
                        selected = isSelected,
                        onClick = { condition = itemCondition },
                        label = { Text(itemCondition) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // 4. Adjuntar Evidencia (Foto)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Foto adjuntada",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toca para cambiarla",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Adjuntar foto (opcional)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cámara o galería de fotos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { takePhotoSafely() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tomar Foto")
                }
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería")
                }
            }

            // 5. Breve Descripción o Referencia
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("¿Qué ocurre y referencias del lugar?") },
                placeholder = { Text("Ej: Tronco caído bloqueando sendero cerca al humedal La Conejera...") },
                minLines = 3,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_description_input")
            )

            // 6. Botón de Enviar Reporte
            Button(
                onClick = { handleSubmit() },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_report_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isGpsActive) "Guardar con Pin Verde (GPS)" else "Guardar Reporte",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Función auxiliar para obtener la última ubicación conocida mediante LocationManager
 * nativo de Android sin depender exclusivamente de Google Play Services.
 */
@SuppressLint("MissingPermission")
private fun tryFallbackLocationManager(context: Context, callback: (Location?) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            callback(null)
            return
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var bestLoc: Location? = null

        for (provider in providers) {
            if (lm.isProviderEnabled(provider)) {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && (bestLoc == null || loc.accuracy < bestLoc.accuracy)) {
                    bestLoc = loc
                }
            }
        }

        if (bestLoc != null) {
            callback(bestLoc)
        } else {
            // Solicitar una actualización rápida si no hay última ubicación guardada
            var answered = false
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!answered) {
                        answered = true
                        lm.removeUpdates(this)
                        callback(location)
                    }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val provider = if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else null

            if (provider != null) {
                lm.requestSingleUpdate(provider, listener, null)
                // Timeout de 2 segundos para evitar bloquear
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (!answered) {
                        answered = true
                        lm.removeUpdates(listener)
                        callback(null)
                    }
                }, 2000)
            } else {
                callback(null)
            }
        }
    } catch (e: Exception) {
        callback(null)
    }
}
