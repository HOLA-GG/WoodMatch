package com.example.kotlinapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
    LaunchedEffect(importing) {
        val source = importing ?: return@LaunchedEffect
        try {
            val result =
                withContext(Dispatchers.IO) { PhotoStorage.importPhoto(context, Uri.parse(source)) }
            selected(result)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("La foto acompañará tu publicación. Evita rostros, placas y datos personales.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
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
                Text("Tomar foto")
            }
            OutlinedButton(
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
                Text("Elegir foto")
            }
        }
        if (importing != null) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
