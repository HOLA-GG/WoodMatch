package com.example.kotlinapp.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.ReportRepository
import kotlinx.coroutines.launch
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
    val currentUser by AuthRepository.currentUser.collectAsState()

    Column {
        Text("Tus datos", style = MaterialTheme.typography.titleLarge)
        TextButton(enabled = !busy, onClick = {
            val user = currentUser ?: return@TextButton
            busy = true
            try {
                val data = JSONObject().apply {
                    put("id_usuario", user.uid)
                    put("nombre", user.name)
                    put("correo", user.email)
                    put("telefono", user.phone)
                    put("sector_suba", user.sectorSuba)
                    put("rol", user.role)
                    put("ciudad", user.city)
                }
                val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, "woodmatch-mis-datos.json")
                file.writeText(data.toString(2))
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).setType("application/json")
                    .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, "Exportar mis datos"))
            } catch (e: Exception) {
                message = "No se pudo exportar: ${e.message}"
            } finally {
                busy = false
            }
        }) { Text("Exportar mis datos") }

        TextButton(enabled = !busy, onClick = { confirm = true }) { Text("Eliminar mi cuenta y datos") }
        message?.let { Text(it) }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { if (!busy) confirm = false },
        title = { Text("¿Eliminar tu cuenta?") },
        text = { Text("Se eliminarán permanentemente tu perfil, publicaciones y solicitudes. Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val res = AuthRepository.deleteCurrentAccount()
                        busy = false
                        confirm = false
                        if (res.isSuccess) {
                            onDeleted()
                        } else {
                            message = res.exceptionOrNull()?.message ?: "Error al eliminar la cuenta."
                        }
                    }
                }
            ) {
                Text("Eliminar definitivamente")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { confirm = false }) {
                Text("Cancelar")
            }
        }
    )
}
