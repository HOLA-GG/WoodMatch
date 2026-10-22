package com.example.kotlinapp.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Uses a camera application; no broad camera or storage permission required. */
@Composable
fun PhotoAttachment(onSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun importPhoto(uri: Uri) {
        busy = true; error = null
        scope.launch {
            try { onSelected(withContext(Dispatchers.IO) { com.example.kotlinapp.data.image.PhotoStorage.importPhoto(context, uri) }) }
            catch (_: Exception) { error = "No se pudo abrir la imagen. Elige un archivo JPG, PNG o WebP de hasta 15 MB." }
            finally { busy = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importPhoto(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pending?.let { File(it) }
        if (success && file != null) {
            importPhoto(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        } else { file?.delete() }
        pending = null
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("La foto será visible con tu publicación. Evita rostros, placas y datos personales.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = {
                try {
                    val file = File.createTempFile("capture_", ".jpg", context.cacheDir)
                    pending = file.absolutePath
                    camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                } catch (_: Exception) { error = "No hay una cámara disponible. Puedes seleccionar una imagen." }
            }) { Text("Tomar foto") }
            OutlinedButton(enabled = !busy, onClick = {
                try { picker.launch("image/*") } catch (_: Exception) { error = "No hay selector de imágenes disponible." }
            }) { Text("Elegir imagen") }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
