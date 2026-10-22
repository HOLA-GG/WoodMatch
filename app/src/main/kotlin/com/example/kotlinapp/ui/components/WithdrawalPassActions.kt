package com.example.kotlinapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.model.WithdrawalRequest
import com.example.kotlinapp.data.repository.WoodWasteRepository
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.launch

@Composable
fun WithdrawalPassActions(request: WithdrawalRequest, isOwner: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    if (!request.isAccepted) return
    Column {
        if (!isOwner && request.passValidatedAt == null) {
            OutlinedButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    WoodWasteRepository.issuePass(request.id).onSuccess { token ->
                        val matrix = MultiFormatWriter().encode(token, BarcodeFormat.QR_CODE, 512, 512)
                        bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
                            setPixels(IntArray(512 * 512) { i -> if (matrix[i % 512, i / 512]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }, 0, 512, 0, 0, 512, 512)
                        }
                    }.onFailure { message = "No se pudo emitir el pase. Comprueba tu conexión y la reserva." }
                    busy = false
                }
            }) { Text("Mostrar pase QR") }
        }
        if (isOwner && request.passValidatedAt == null) {
            OutlinedButton(enabled = !busy, onClick = {
                busy = true; message = null
                try {
                    GmsBarcodeScanning.getClient(context).startScan()
                        .addOnSuccessListener { result ->
                            scope.launch {
                                WoodWasteRepository.redeemPass(result.rawValue.orEmpty())
                                    .onSuccess { message = "Pase validado. Confirma el retiro después de entregar el material." }
                                    .onFailure { message = "Pase inválido, vencido, utilizado o sin conexión." }
                                busy = false
                            }
                        }.addOnCanceledListener { busy = false }
                        .addOnFailureListener { busy = false; message = "No se pudo abrir el escáner. Comprueba Google Play Services y vuelve a intentar." }
                } catch (_: Exception) { busy = false; message = "El escáner no está disponible en este dispositivo." }
            }) { Text("Validar pase QR") }
        }
        message?.let { Text(it) }
    }
    bitmap?.let { image ->
        AlertDialog(onDismissRequest = { bitmap = null }, title = { Text("Pase de retiro · 10 minutos") },
            text = { Column { Image(image.asImageBitmap(), "Pase QR de retiro", Modifier.fillMaxWidth().aspectRatio(1f)); Text("Muéstralo al generador. No lo compartas. Emitir otro pase invalida el anterior. El pase autoriza el retiro; no certifica identidad física.") } },
            confirmButton = { TextButton(onClick = { bitmap = null }) { Text("Cerrar") } })
    }
}
