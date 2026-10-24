package com.example.kotlinapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kotlinapp.data.model.WithdrawalRequest
import com.example.kotlinapp.data.repository.WoodWasteRepository
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WithdrawalPassActions(request: WithdrawalRequest, isOwner: Boolean) {
    if (!request.isAccepted) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf<String?>(null) }
    var expiry by remember { mutableLongStateOf(0) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var code by rememberSaveable(request.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun validate(value: String) {
        if (!value.startsWith("${request.id}:")) {
            message = "El pase corresponde a otra solicitud."
            busy = false
            return
        }
        scope.launch {
            WoodWasteRepository.redeemPass(value)
                .onSuccess {
                    message = "Pase validado. Ahora ambas personas deben confirmar la entrega."
                }
                .onFailure { message = it.message ?: "No se pudo validar el pase." }
            busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (request.passValidatedAt == null) {
            if (!isOwner)
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            WoodWasteRepository.issuePass(request.id)
                                .onSuccess {
                                    token = it
                                    expiry = System.currentTimeMillis() + 600000
                                }
                                .onFailure { message = it.message }
                            busy = false
                        }
                    },
                ) {
                    Text("Mostrar QR o código de entrega")
                }
            else {
                Text(
                    "Pide a quien recoge que muestre su pase. Puedes escanearlo o escribir sus ocho dígitos."
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(8) },
                    label = { Text("Código de 8 dígitos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !busy && code.length == 8,
                        onClick = {
                            busy = true
                            validate("${request.id}:$code")
                        },
                    ) {
                        Text("Validar código")
                    }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            try {
                                GmsBarcodeScanning.getClient(context)
                                    .startScan()
                                    .addOnSuccessListener { validate(it.rawValue.orEmpty()) }
                                    .addOnCanceledListener { busy = false }
                                    .addOnFailureListener {
                                        busy = false
                                        message =
                                            "No se pudo abrir el escáner. Usa el código de ocho dígitos."
                                    }
                            } catch (_: Exception) {
                                busy = false
                                message = "No se pudo abrir el escáner. Usa el código."
                            }
                        },
                    ) {
                        Text("Escanear QR")
                    }
                }
            }
        } else
            Text("✓ Pase validado. Confirma únicamente después de entregar o recibir el material.")
        message?.let { Text(it) }
    }
    LaunchedEffect(token) {
        while (token != null) {
            clock = System.currentTimeMillis()
            delay(1000)
        }
    }
    if (token != null) {
        val current = token!!
        val bitmap =
            remember(current) {
                val matrix = MultiFormatWriter().encode(current, BarcodeFormat.QR_CODE, 480, 480)
                Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888).apply {
                    setPixels(
                        IntArray(480 * 480) { i ->
                            if (matrix[i % 480, i / 480]) android.graphics.Color.BLACK
                            else android.graphics.Color.WHITE
                        },
                        0,
                        480,
                        0,
                        0,
                        480,
                        480,
                    )
                }
            }
        DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
        AlertDialog(
            onDismissRequest = { token = null },
            title = { Text("Pase de entrega") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (clock < expiry) {
                        Image(
                            bitmap.asImageBitmap(),
                            "Código QR del pase de entrega",
                            Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                        Text(
                            current.substringAfter(':'),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            "Vence en ${((expiry-clock)/1000).coerceAtLeast(0)} segundos. Muéstralo a quien entrega el material."
                        )
                    } else Text("Este pase venció. Cierra y genera uno nuevo.")
                    Text(
                        "Emitir otro pase invalida el anterior. La validación no reemplaza la confirmación de ambas personas."
                    )
                }
            },
            confirmButton = { TextButton(onClick = { token = null }) { Text("Cerrar") } },
        )
    }
}
