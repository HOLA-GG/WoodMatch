package com.example.kotlinapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.network.PilotApi
import com.example.kotlinapp.data.repository.WoodWasteRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun activityDate(value: Long?): String = value?.let {
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-CO")).apply { timeZone = TimeZone.getTimeZone("America/Bogota") }.format(Date(it))
} ?: "Sin fecha"

fun activityAction(value: Any?): String = when (value?.toString()?.uppercase()) {
    "ACCEPT", "ACEPTADA" -> "Solicitud aceptada"
    "REJECT", "RECHAZADA" -> "Solicitud rechazada"
    "CANCEL", "CANCELADA" -> "Reserva cancelada"
    "CONFIRM", "COMPLETADA" -> "Entrega confirmada"
    "VENCIDA", "EXPIRED" -> "Plazo vencido"
    "SOLICITADA" -> "Solicitud enviada"
    else -> (value?.toString() ?: "Movimiento").replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActivityRequestCard(request: WithdrawalRequest, item: WoodWasteItem?, owner: Boolean, busy: Boolean,
    transition: (String) -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var image by remember(item?.id, item?.imagePath) { mutableStateOf<String?>(null) }
    var proposing by rememberSaveable(request.id) { mutableStateOf(false) }
    var proposedDate by rememberSaveable(request.id) { mutableStateOf("") }
    var reason by rememberSaveable(request.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showHistory by rememberSaveable(request.id) { mutableStateOf(false) }
    var proposalAction by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val proposal = request.reschedule
    val waiting = proposal?.get("state") == "PENDIENTE"
    val mustRespond = waiting && proposal?.get("requires") == (if (owner) "OWNER" else "REQUESTER")
    LaunchedEffect(item?.id, item?.imagePath) { image = item?.let { runCatching { WoodWasteRepository.photoUrl(it) }.getOrNull() } }
    fun updateSchedule(action: String? = null) {
        sending = true
        scope.launch {
            runCatching {
                PilotApi.call(if (action == null) "proposeReschedule" else "respondReschedule",
                    mapOf("requestId" to request.id, "operationId" to PilotApi.operationId(), "proposedDate" to proposedDate, "reason" to reason, "action" to action))
            }.onSuccess { notify("Cambio registrado. La otra persona recibirá el aviso."); proposing = false; proposalAction = null; reason = ""; WoodWasteRepository.refreshSession() }
                .onFailure { notify(it.message ?: "No se pudo guardar el cambio.") }
            sending = false
        }
    }
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        image?.let { AsyncImage(it, "Material de la solicitud", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp)) }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.secondaryContainer))).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(request.wasteTitle.ifBlank { item?.title ?: "Material compartido" }, style = MaterialTheme.typography.titleLarge)
            SuggestionChip(onClick = {}, label = { Text(request.typedStatus.displayName) }, icon = { Icon(if (request.isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule, null) })
            Text(if (owner) "Solicitud recibida · ${request.requesterName}" else "Solicitud enviada", style = MaterialTheme.typography.labelLarge)
            Text("Registro ${request.id.take(8)} · ${activityDate(request.createdAt)}", style = MaterialTheme.typography.bodySmall)
            Text("Fecha de recolección: ${request.proposedDate.ifBlank { "Por acordar" }}")
            if (request.requestedQuantity.isNotBlank()) Text("Cantidad: ${request.requestedQuantity}")
            if (owner && request.intendedUse.isNotBlank()) Text("Uso previsto: ${request.intendedUse}")
            if (request.message.isNotBlank()) Text(request.message)
            if (request.isPending) {
                Text(if (owner) "Te corresponde aceptar o rechazar." else "Esperando respuesta del generador.", style = MaterialTheme.typography.titleSmall)
                request.responseExpiresAt?.let { Text("Responder antes de ${activityDate(it)}", color = MaterialTheme.colorScheme.primary) }
                if (owner) { Button(enabled = !busy, onClick = { transition("accept") }, modifier = Modifier.fillMaxWidth()) { Text("Aceptar solicitud") }; OutlinedButton(enabled = !busy, onClick = { transition("reject") }, modifier = Modifier.fillMaxWidth()) { Text("Rechazar") } }
            }
            if (request.isAccepted) {
                request.reservationExpiresAt?.let { Text("Límite de la reserva: ${activityDate(it)}", style = MaterialTheme.typography.bodySmall) }
                if (waiting) {
                    HorizontalDivider()
                    Text("Nueva fecha propuesta: ${proposal?.get("proposedDate")}", style = MaterialTheme.typography.titleMedium)
                    Text("Motivo: ${proposal?.get("reason")}")
                    Text("Respuesta hasta ${activityDate((proposal?.get("expiresAt") as? Number)?.toLong())}", style = MaterialTheme.typography.bodySmall)
                    Text(if (mustRespond) "Te corresponde responder." else "Esperando confirmación de la otra persona.")
                    Text("El acuerdo anterior sigue vigente hasta aceptar el cambio.", style = MaterialTheme.typography.bodySmall)
                    if (mustRespond) {
                        Button(enabled = !busy && !sending, onClick = { proposalAction = "accept"; reason = "" }, modifier = Modifier.fillMaxWidth()) { Text("Aceptar nueva fecha") }
                        OutlinedButton(enabled = !busy && !sending, onClick = { proposalAction = "decline"; reason = "" }, modifier = Modifier.fillMaxWidth()) { Text("No puedo asistir · cancelar reserva") }
                    }
                }
                val canPropose = !waiting || (owner && proposal?.get("requires") == "OWNER")
                if (request.passValidatedAt == null) OutlinedButton(enabled = !busy && !sending && canPropose, onClick = { proposing = !proposing }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Event, null); Spacer(Modifier.width(8.dp)); Text(if (owner && waiting) "Proponer otra fecha" else "Solicitar cambio de fecha") }
                if (waiting && !canPropose) Text("Espera la respuesta a la propuesta pendiente antes de enviar otra fecha.", style = MaterialTheme.typography.bodySmall)
                if (proposing) {
                    OutlinedTextField(proposedDate, {}, readOnly = true, label = { Text("Nueva fecha de recolección") }, trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, "Elegir fecha") } }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("Motivo obligatorio") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = !sending && canPropose && proposedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && reason.isNotBlank(), onClick = { updateSchedule() }) { Text(if (sending) "Enviando…" else "Enviar propuesta") }
                }
            }
            key(request.id, request.proposedDate) { WithdrawalPassActions(request, owner) }
            if (request.isAccepted && request.passValidatedAt != null) {
                val confirmed = if (owner) request.authorConfirmedAt != null else request.requesterConfirmedAt != null
                if (confirmed) Text("Tu confirmación está registrada. Falta la otra persona.")
                else Button(enabled = !busy, onClick = { transition("confirm") }, modifier = Modifier.fillMaxWidth()) { Text(if (owner) "Confirmar que entregué" else "Confirmar que recibí") }
            }
            if ((request.isPending || request.isAccepted) && request.passValidatedAt == null) TextButton(enabled = !busy, onClick = { transition("cancel") }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar con motivo") }
            (request.closeReason ?: request.cancellationReason ?: request.rejectionReason)?.takeIf { it.isNotBlank() }?.let { Text("Motivo: $it") }
            if (request.isCompleted) {
                HorizontalDivider()
                Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.primary)
                Text("Comprobante de aprovechamiento", style = MaterialTheme.typography.titleLarge)
                Text("Entrega confirmada por ambas personas", style = MaterialTheme.typography.titleSmall)
                request.receipt?.let { receipt ->
                    Text("${receipt["quantity"] ?: ""} ${receipt["unit"] ?: ""} · ${receipt["title"] ?: request.wasteTitle}")
                    Text("Entrega: ${receipt["authorName"] ?: "Generador"}\nRecibe: ${receipt["requesterName"] ?: request.requesterName}")
                    Text(activityDate(request.completedAt), style = MaterialTheme.typography.bodySmall)
                    Box {
                        OutlinedButton(enabled = !busy, onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Guardar o compartir comprobante") }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            listOf("PDF" to "pdf", "JSON" to "json").forEach { (label, format) -> DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; runCatching { ReceiptExport.share(context, request, format) }.onFailure { notify("No se pudo compartir el comprobante.") } }) }
                        }
                    }
                    Text("Registro de entrega; no es una factura fiscal. La exportación omite teléfonos y ubicación exacta.", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = { showHistory = !showHistory }) { Icon(Icons.Default.History, null); Spacer(Modifier.width(8.dp)); Text(if (showHistory) "Ocultar movimientos" else "Ver trazabilidad") }
            if (showHistory) {
                Text("● Solicitud creada · ${activityDate(request.createdAt)}", style = MaterialTheme.typography.bodySmall)
                request.timeline.forEach { event ->
                    Text("● ${activityAction(event["action"] ?: event["status"])} · ${activityDate((event["at"] as? Number)?.toLong())}", style = MaterialTheme.typography.bodySmall)
                    (event["reason"] as? String)?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                request.passValidatedAt?.let { Text("● Pase validado · ${activityDate(it)}", style = MaterialTheme.typography.bodySmall) }
                request.completedAt?.let { Text("● Entrega completada · ${activityDate(it)}", style = MaterialTheme.typography.bodySmall) }
                Text("Última actualización: ${activityDate(request.updatedAt)}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (showDatePicker) {
        val picker = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(enabled = picker.selectedDateMillis != null, onClick = { picker.selectedDateMillis?.let { proposedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it)) }; showDatePicker = false }) { Text("Elegir fecha") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Volver") } }) { DatePicker(picker) }
    }
    proposalAction?.let { action -> AlertDialog(onDismissRequest = { if (!sending) proposalAction = null }, title = { Text(if (action == "accept") "Aceptar nueva fecha" else "Cancelar la reserva") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (action == "accept") "El pase anterior dejará de ser válido. Explica brevemente tu confirmación." else "El material volverá a estar disponible. Indica por qué no puedes aceptar la fecha."); OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("Motivo obligatorio") }) } }, confirmButton = { TextButton(enabled = !sending && reason.isNotBlank(), onClick = { updateSchedule(action) }) { Text("Confirmar") } }, dismissButton = { TextButton(enabled = !sending, onClick = { proposalAction = null }) { Text("Volver") } }) }
}
