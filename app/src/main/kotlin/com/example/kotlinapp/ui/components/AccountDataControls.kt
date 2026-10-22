package com.example.kotlinapp.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.kotlinapp.data.repository.AuthRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File

@Composable
fun AccountDataControls(onDeleted: () -> Unit) {
    if (!AuthRepository.isAuthenticated) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column {
        Text("Tus datos", style = MaterialTheme.typography.titleLarge)
        TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    val result = FirebaseFunctions.getInstance().getHttpsCallable("exportAccount").call().await()
                    val data = result.data as Map<*, *>
                    val file = File(context.cacheDir, "woodmatch-mis-datos.json")
                    file.writeText(JSONObject(data).toString(2))
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).setType("application/json")
                        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(intent, "Exportar mis datos"))
                } catch (_: Exception) { message = "No se pudo exportar. Comprueba tu conexión y el servicio." }
                finally { busy = false }
            }
        }) { Text("Exportar mis datos") }
        TextButton(enabled = !busy, onClick = { confirm = true }) { Text("Eliminar mi cuenta y datos") }
        message?.let { Text(it) }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false },
        title = { Text("¿Eliminar tu cuenta?") },
        text = { Text("Se eliminarán tu perfil, publicaciones, fotos y solicitudes del servidor. Esta acción no se puede deshacer. Si llevas más de cinco minutos conectado, vuelve a iniciar sesión primero.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    FirebaseFunctions.getInstance().getHttpsCallable("deleteAccount").call().await()
                    AuthRepository.currentUser.value?.uid?.let { com.example.kotlinapp.data.repository.ReportRepository.deleteUserReports(it) }
                    File(context.filesDir, "publication_photos").deleteRecursively()
                    AuthRepository.logout(); confirm = false; onDeleted()
                } catch (_: Exception) { confirm = false; message = "No se pudo confirmar la eliminación completa. Vuelve a iniciar sesión; si tu cuenta quedó bloqueada, el responsable deberá completar la solicitud." }
                finally { busy = false }
            }
        }) { Text("Eliminar definitivamente") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Cancelar") } })
}
