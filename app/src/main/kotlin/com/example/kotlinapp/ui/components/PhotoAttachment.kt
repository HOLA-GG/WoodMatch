package com.example.kotlinapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.kotlinapp.data.image.PhotoStorage
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** External camera + Android photo picker. The draft owner persists the selected URI. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoAttachment(onSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected by rememberUpdatedState(onSelected)
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedFile by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attached by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(importing) {
        val source = importing ?: return@LaunchedEffect
        try {
            val result =
                withContext(Dispatchers.IO) { PhotoStorage.importPhoto(context, Uri.parse(source)) }
            selected(result)
            attached = true
            capturedFile?.let { File(it).delete() }
            capturedFile = null
            importing = null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = "No se pudo abrir la foto. Elige una imagen de hasta 15 MB."
            capturedFile?.let { File(it).delete() }
            capturedFile = null
            importing = null
        }
    }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                error = null
                importing = uri.toString()
            }
        }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = pending
            try {
                if (success && path != null) {
                    capturedFile = path
                    importing =
                        FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                File(path),
                            )
                            .toString()
                } else path?.let { File(it).delete() }
            } catch (_: Exception) {
                error = "No se pudo recuperar la foto. Intenta otra vez."
                path?.let { File(it).delete() }
            } finally {
                pending = null
            }
        }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Evidencia fotográfica", style = MaterialTheme.typography.titleMedium)
        Text("Una imagen clara ayuda a entender el hallazgo. Evita rostros, placas y datos personales.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = importing == null && pending == null,
                onClick = {
                    try {
                        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
                        val file = File.createTempFile("capture_", ".jpg", directory)
                        pending = file.absolutePath
                        camera.launch(
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                        )
                    } catch (_: Exception) {
                        pending?.let { File(it).delete() }
                        pending = null
                        error = "No se pudo abrir la cámara. Puedes elegir una foto."
                    }
                },
            ) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tomar foto")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = importing == null && pending == null,
                onClick = {
                    try {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    } catch (_: Exception) {
                        error = "No se pudo abrir la galería."
                    }
                },
            ) {
                Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elegir foto")
            }
        }
        if (importing != null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Preparando tu foto…", style = MaterialTheme.typography.labelMedium)
        } else if (attached) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Text("Foto preparada. Puedes continuar o reemplazarla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      }
    }
}

