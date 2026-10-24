package com.example.kotlinapp.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.SeasonalTheme
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.model.UserRole
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.ui.components.PrivacyNotice
import com.example.kotlinapp.ui.components.WoodMatchTopAppBar
import com.example.kotlinapp.ui.theme.AutumnPrimary
import com.example.kotlinapp.ui.theme.SnowPrimary
import com.example.kotlinapp.ui.theme.SpringPrimary
import com.example.kotlinapp.ui.theme.SummerPrimary
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

private data class SeasonOption(
    val theme: SeasonalTheme,
    val name: String,
    val toneName: String,
    val icon: ImageVector,
    val swatchColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSeason: SeasonalTheme,
    onSeasonChange: (SeasonalTheme) -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by AuthRepository.currentUser.collectAsState()

    // Configuración de notificaciones
    var notifyIncidents by remember { mutableStateOf(true) }
    var notifySeasonalAdvice by remember { mutableStateOf(true) }
    var notifyMonthlySummary by remember { mutableStateOf(false) }

    // Diálogos del sistema
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPasswordResetConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    // Estados para edición de perfil (CRUD)
    var editName by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var editPhone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var editSector by remember(currentUser) { mutableStateOf(currentUser?.sectorSuba ?: "Suba Centro") }
    var sectorDropdownExpanded by remember { mutableStateOf(false) }
    var editRole by remember(currentUser) { mutableStateOf(currentUser?.role ?: UserRole.GENERADOR.name) }

    var isSavingProfile by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<String?>(null) }
    var profileIsError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val seasonOptions = listOf(
        SeasonOption(SeasonalTheme.SPRING, "Primavera", "Verde Esmeralda", Icons.Filled.LocalFlorist, SpringPrimary),
        SeasonOption(SeasonalTheme.SUMMER, "Verano", "Ámbar Dorado", Icons.Filled.WbSunny, SummerPrimary),
        SeasonOption(SeasonalTheme.AUTUMN, "Otoño", "Terracota Cálido", Icons.Filled.Eco, AutumnPrimary),
        SeasonOption(SeasonalTheme.SNOW, "Invierno", "Azul Glaciar", Icons.Filled.AcUnit, SnowPrimary)
    )

    fun exportUserData() {
        val user = currentUser ?: return
        try {
            val json = JSONObject().apply {
                put("id_usuario", user.uid)
                put("nombre", user.name)
                put("correo", user.email)
                put("telefono", user.phone)
                put("sector_suba", user.sectorSuba)
                put("rol", user.role)
                put("ciudad", user.city)
                put("fecha_exportacion", System.currentTimeMillis())
                put("plataforma", "WoodMatch Android - Suba")
            }
            val cacheFile = File(File(context.cacheDir, "exports").apply { mkdirs() }, "woodmatch_datos_usuario.json")
            cacheFile.writeText(json.toString(2))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Descargar y compartir mis datos"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Modal: Acerca de WoodMatch
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("WoodMatch ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Plataforma ciudadana y comunitaria para la gestión, georreferenciación y aprovechamiento circular de residuos leñosos en Suba.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Mapeo georreferenciado sobre OpenStreetMap\n• Red de generadores de podas y talleres transformadores\n• Compromiso ecológico con los humedales y reservas de Suba\n• Bogotá D.C., Colombia",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Modal: Confirmación de Restablecimiento de Contraseña
    if (showPasswordResetConfirm) {
        AlertDialog(
            onDismissRequest = { showPasswordResetConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Restablecer Contraseña")
            },
            text = {
                Text("Se enviará un correo seguro con el enlace para actualizar tu contraseña a: ${currentUser?.email ?: ""}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasswordResetConfirm = false
                        coroutineScope.launch {
                            val userEmail = currentUser?.email ?: ""
                            if (userEmail.isNotBlank()) {
                                AuthRepository.forgotPassword(userEmail, currentUser?.phone ?: "", "")
                                Toast.makeText(context, "Correo de restablecimiento enviado.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Confirmación de Eliminación Definitiva de Cuenta
    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text("¿Eliminar tu cuenta y todos tus datos?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta acción es definitiva e irreversible. Se eliminarán permanentemente tu perfil, publicaciones, solicitudes y credenciales de acceso de WoodMatch.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Conforme a las leyes de protección de datos personales (Habeas Data Ley 1581) y las políticas de Google Play, no se conservará ninguna información privada tuya tras la eliminación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingAccount,
                    onClick = {
                        isDeletingAccount = true
                        coroutineScope.launch {
                            val res = AuthRepository.deleteCurrentAccount()
                            isDeletingAccount = false
                            if (res.isSuccess) {
                                showDeleteAccountConfirm = false
                                Toast.makeText(context, "Tu cuenta ha sido eliminada exitosamente.", Toast.LENGTH_LONG).show()
                                onLogout()
                            } else {
                                val err = res.exceptionOrNull()?.message ?: "Error al eliminar la cuenta."
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                    } else {
                        Text("Eliminar definitivamente")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = { showDeleteAccountConfirm = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        WoodMatchTopAppBar(
            title = "Ajustes y Cuenta",
            showBackButton = true,
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // SECCIÓN 1: GESTIÓN DE PERFIL Y DATOS PERSONALES (CRUD)
            if (AuthRepository.isAuthenticated && currentUser != null) {
                Text(
                    text = "MI PERFIL Y DATOS PERSONALES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mensaje de éxito / error al actualizar
                        profileMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (profileIsError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (profileIsError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Campo: Nombre
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it; profileMessage = null },
                            label = { Text("Nombre o Razón Social") },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Campo: Teléfono
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it; profileMessage = null },
                            label = { Text("Número de Teléfono (+57...)") },
                            leadingIcon = {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Selector de Sector en Suba
                        ExposedDropdownMenuBox(
                            expanded = sectorDropdownExpanded,
                            onExpandedChange = { sectorDropdownExpanded = !sectorDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = editSector,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sector en Suba") },
                                leadingIcon = {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectorDropdownExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("settings_sector_dropdown"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = sectorDropdownExpanded,
                                onDismissRequest = { sectorDropdownExpanded = false }
                            ) {
                                SubaSectors.ALL.forEach { sector ->
                                    DropdownMenuItem(
                                        text = { Text(sector.name) },
                                        onClick = {
                                            editSector = sector.name
                                            sectorDropdownExpanded = false
                                            profileMessage = null
                                        }
                                    )
                                }
                            }
                        }

                        // Selector de Rol
                        Text(
                            text = "Rol en la plataforma:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                UserRole.GENERADOR to "Generador",
                                UserRole.RECEPTOR to "Receptor",
                                UserRole.MIXTO to "Mixto"
                            ).forEach { (roleEnum, label) ->
                                val isSelected = editRole.equals(roleEnum.name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        editRole = roleEnum.name
                                        profileMessage = null
                                    },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        // Botón: Guardar Cambios en Perfil
                        Button(
                            onClick = {
                                if (isSavingProfile) return@Button
                                isSavingProfile = true
                                profileMessage = null
                                coroutineScope.launch {
                                    val result = AuthRepository.updateCurrentUserProfile(
                                        name = editName,
                                        phone = editPhone,
                                        sectorSuba = editSector,
                                        role = editRole
                                    )
                                    isSavingProfile = false
                                    result.onSuccess {
                                        profileIsError = false
                                        profileMessage = "Tus datos se actualizaron correctamente."
                                    }.onFailure { err ->
                                        profileIsError = true
                                        profileMessage = err.message ?: "Error al actualizar perfil."
                                    }
                                }
                            },
                            enabled = !isSavingProfile,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_profile_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSavingProfile) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Guardar cambios del perfil", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECCIÓN 2: APARIENCIA Y ESTACIONES
            Text(
                text = "APARIENCIA Y ESTACIONES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Paleta Estacional",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        seasonOptions.forEach { option ->
                            val isSelected = currentSeason == option.theme
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSeasonChange(option.theme) }
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("season_chip_${option.name.lowercase()}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) option.swatchColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(option.swatchColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN 3: NOTIFICACIONES
            Text(
                text = "NOTIFICACIONES Y ALERTAS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alertas de senderos y madera", style = MaterialTheme.typography.titleMedium)
                            Text("Árboles caídos o podas disponibles en tu sector de Suba", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notifyIncidents,
                            onCheckedChange = { notifyIncidents = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Consejos botánicos", style = MaterialTheme.typography.titleMedium)
                            Text("Épocas de poda, siembra y floración en Bogotá", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notifySeasonalAdvice,
                            onCheckedChange = { notifySeasonalAdvice = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // SECCIÓN 4: SEGURIDAD, HABEAS DATA Y PRIVACIDAD
            Text(
                text = "SEGURIDAD, HABEAS DATA Y CUENTA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Cambiar Contraseña
                    if (AuthRepository.isAuthenticated) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showPasswordResetConfirm = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Cambiar Contraseña", style = MaterialTheme.typography.bodyMedium)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Exportar datos
                    if (AuthRepository.isAuthenticated) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { exportUserData() }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Exportar mis datos (JSON)", style = MaterialTheme.typography.bodyMedium)
                                    Text("Descarga una copia de tu información personal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Políticas de Privacidad
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            PrivacyNotice(buttonText = "Políticas de Privacidad y Tratamiento de Datos")
                        }
                    }

                    // Acerca de WoodMatch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Acerca de WoodMatch", style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    }

                    // Botón de Eliminación de Cuenta
                    if (AuthRepository.isAuthenticated) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showDeleteAccountConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_account_button")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar mi cuenta y datos definitivamente")
                        }
                    }
                }
            }

            // SECCIÓN 5: CERRAR SESIÓN
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_logout_button"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar Sesión")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
