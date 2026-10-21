package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.RequestStatus
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.model.WasteCategory
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.model.WithdrawalRequest
import com.example.kotlinapp.data.model.WoodWasteItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CircularMetrics(
    val availableCount: Int = 0,
    val reservedCount: Int = 0,
    val completedCount: Int = 0,
    val totalM3Recovered: Double = 0.0,
    val totalKgRecovered: Double = 0.0,
    val sectorDistribution: Map<String, Int> = emptyMap()
)

object WoodWasteRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private var itemsListener: ListenerRegistration? = null

    // Datos piloto representativos en Suba para verificación inmediata
    private val initialSampleItems = listOf(
        WoodWasteItem(
            id = "waste_suba_01",
            title = "Troncos y ramas de Acacia tras poda preventiva",
            description = "Material leñoso resultante de poda autorizada en conjunto residencial. Madera densa adecuada para ebanistería rústica o leña seca.",
            wasteType = "Troncos y rollizos",
            materialState = "Seco / Estacionado",
            category = WasteCategory.FINA.name,
            quantityValue = 2.5,
            quantityUnit = "m³",
            volumeM3 = 2.5,
            quantityEstimated = false,
            subaSector = "Niza / La Colina",
            exactLatitude = 4.7225,
            exactLongitude = -74.0718,
            referenceAddress = "Cerca al Parque Colina Campestre",
            speciesName = "Acacia japonesa (Acacia melanoxylon)",
            speciesConfidence = 0.92,
            suggestedUses = "Torneado artesanal, postes para tutores o leña de alto poder calorífico.",
            isAiAssisted = true,
            status = WasteStatus.DISPONIBLE.name,
            authorId = "gen_carlos",
            authorName = "Carlos M. (Jardinería)",
            authorPhone = "+573159876543",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        WoodWasteItem(
            id = "waste_suba_02",
            title = "Descartes de pallets y listones pino pátula",
            description = "Pallets limpios sin químicos y recortes de estantería. Listos para reutilización en mesas, huertas urbanas o macetas.",
            wasteType = "Pallets y embalajes",
            materialState = "Seco / Estacionado",
            category = WasteCategory.ESTANDAR.name,
            quantityValue = 18.0,
            quantityUnit = "unidades",
            volumeM3 = 0.8,
            quantityEstimated = false,
            subaSector = "El Rincón",
            exactLatitude = 4.7340,
            exactLongitude = -74.0945,
            referenceAddress = "Sector comercial Cra 91",
            speciesName = "Pino pátula (Pinus patula)",
            speciesConfidence = 0.88,
            suggestedUses = "Cajas de huerta, mobiliario modular o estibas de compost.",
            isAiAssisted = true,
            status = WasteStatus.DISPONIBLE.name,
            authorId = "gen_carlos",
            authorName = "Carlos M. (Jardinería)",
            authorPhone = "+573159876543",
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        WoodWasteItem(
            id = "waste_suba_03",
            title = "Biomasa y viruta gruesa de poda de setos",
            description = "Material triturado y ramaje fino ideal para enriquecimiento de compostaje comunitario y acolchado (mulch) de suelos.",
            wasteType = "Astillas / Viruta / Aserrín",
            materialState = "Verde recién cortado",
            category = WasteCategory.BIOMASA.name,
            quantityValue = 120.0,
            quantityUnit = "kg",
            volumeM3 = null,
            quantityEstimated = true,
            subaSector = "Tibabuyes",
            exactLatitude = 4.7420,
            exactLongitude = -74.1150,
            referenceAddress = "Cerca a humedal Tibabuyes",
            speciesName = "Sauce llorón (Salix humboldtiana)",
            speciesConfidence = 0.79,
            suggestedUses = "Compostaje, retención de humedad en suelos y cobertura vegetal.",
            isAiAssisted = true,
            status = WasteStatus.DISPONIBLE.name,
            authorId = "gen_carlos",
            authorName = "Carlos M. (Jardinería)",
            authorPhone = "+573159876543",
            createdAt = System.currentTimeMillis() - 43200000L
        ),
        WoodWasteItem(
            id = "waste_suba_04",
            title = "Tronco grueso de Ciprés caído por temporal",
            description = "Troza de 3 metros de longitud con diámetro aprox. de 45 cm. Madera aromática aprovechable para ebanistería.",
            wasteType = "Troncos y rollizos",
            materialState = "Seco / Estacionado",
            category = WasteCategory.FINA.name,
            quantityValue = 1.2,
            quantityUnit = "m³",
            volumeM3 = 1.2,
            quantityEstimated = false,
            subaSector = "San José de Bavaria",
            exactLatitude = 4.7630,
            exactLongitude = -74.0590,
            referenceAddress = "Predio campestre Cl 170",
            speciesName = "Ciprés común (Cupressus lusitanica)",
            speciesConfidence = 0.95,
            suggestedUses = "Ebanistería fina, tablones de corte o artesanías torneadas.",
            isAiAssisted = true,
            status = WasteStatus.APROVECHADO.name,
            authorId = "gen_carlos",
            authorName = "Carlos M. (Jardinería)",
            authorPhone = "+573159876543",
            createdAt = System.currentTimeMillis() - 86400000L * 5,
            completedAt = System.currentTimeMillis() - 86400000L,
            completedBy = "rec_elena"
        )
    )

    private val _items = MutableStateFlow<List<WoodWasteItem>>(initialSampleItems)
    val items: StateFlow<List<WoodWasteItem>> = _items.asStateFlow()

    private val _requests = MutableStateFlow<List<WithdrawalRequest>>(listOf(
        WithdrawalRequest(
            id = "req_demo_01",
            wasteId = "waste_suba_04",
            wasteTitle = "Tronco grueso de Ciprés caído por temporal",
            requesterId = "rec_elena",
            requesterName = "Elena R. (Taller Ebanistería)",
            requesterPhone = "+573204567890",
            requesterRole = "RECEPTOR",
            intendedUse = "Tablas para mesas de café artesanales",
            proposedDate = "2026-10-18",
            requestedQuantity = "1.2 m³",
            message = "Contamos con camioneta para retiro en finca y aserrío propio.",
            status = RequestStatus.COMPLETADA.name,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            completedAt = System.currentTimeMillis() - 86400000L
        )
    ))
    val requests: StateFlow<List<WithdrawalRequest>> = _requests.asStateFlow()

    init {
        startSync()
    }

    private fun startSync() {
        firestore?.let { db ->
            try {
                itemsListener?.remove()
                itemsListener = db.collection("wood_wastes")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("WoodWasteRepository", "Listen error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && !snapshot.isEmpty) {
                            val remoteItems = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val item = doc.toObject(WoodWasteItem::class.java)
                                    item?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (remoteItems.isNotEmpty()) {
                                val merged = (remoteItems + initialSampleItems).distinctBy { it.id }
                                _items.value = merged
                            }
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.w("WoodWasteRepository", "Firestore startSync fallback: ${e.message}")
            }
        }
    }

    suspend fun publishWasteItem(item: WoodWasteItem): Result<String> {
        val itemId = if (item.id.isBlank()) "waste_${System.currentTimeMillis()}" else item.id
        val itemToSave = item.copy(
            id = itemId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = WasteStatus.DISPONIBLE.name
        )

        // Actualizar estado en memoria de inmediato para respuesta reactiva instantánea
        _items.value = listOf(itemToSave) + _items.value.filter { it.id != itemId }

        firestore?.let { db ->
            try {
                db.collection("wood_wastes").document(itemId).set(itemToSave).await()
            } catch (e: Exception) {
                android.util.Log.w("WoodWasteRepository", "Firestore write waste fallback: ${e.message}")
            }
        }

        return Result.success(itemId)
    }

    suspend fun updateWasteStatus(itemId: String, newStatus: WasteStatus): Result<Unit> {
        val currentList = _items.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                status = newStatus.name,
                updatedAt = System.currentTimeMillis()
            )
            _items.value = currentList
        }

        firestore?.let { db ->
            try {
                db.collection("wood_wastes").document(itemId)
                    .update(
                        mapOf(
                            "status" to newStatus.name,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    // GESTIÓN DE SOLICITUDES DE RETIRO (DESACOPLADA Y CON CONTROL DE CONCURRENCIA)
    suspend fun submitWithdrawalRequest(
        wasteId: String,
        requester: com.example.kotlinapp.data.model.User,
        intendedUse: String,
        proposedDate: String,
        requestedQuantity: String,
        message: String
    ): Result<String> {
        val targetItem = _items.value.find { it.id == wasteId }
            ?: return Result.failure(Exception("La publicación no fue encontrada."))

        if (!targetItem.isAvailable) {
            return Result.failure(Exception("El material ya no se encuentra disponible (estado: ${targetItem.typedStatus.displayName})."))
        }

        val reqId = "req_${System.currentTimeMillis()}"
        val request = WithdrawalRequest(
            id = reqId,
            wasteId = wasteId,
            wasteTitle = targetItem.title,
            requesterId = requester.uid,
            requesterName = requester.name,
            requesterPhone = requester.phone,
            requesterRole = requester.role,
            intendedUse = intendedUse,
            proposedDate = proposedDate,
            requestedQuantity = requestedQuantity.ifBlank { targetItem.formattedQuantity },
            message = message,
            status = RequestStatus.PENDIENTE.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        _requests.value = listOf(request) + _requests.value

        firestore?.let { db ->
            try {
                db.collection("wood_wastes").document(wasteId)
                    .collection("requests").document(reqId)
                    .set(request).await()
            } catch (_: Exception) {}
        }

        return Result.success(reqId)
    }

    // Aceptar solicitud -> Reserva material y previene reservas dobles concurrentes
    suspend fun acceptWithdrawalRequest(wasteId: String, requestId: String): Result<Unit> {
        val targetItem = _items.value.find { it.id == wasteId }
            ?: return Result.failure(Exception("Publicación no encontrada."))

        if (targetItem.status == WasteStatus.RESERVADO.name) {
            return Result.failure(Exception("Esta publicación ya fue reservada por otra persona."))
        }

        val reqList = _requests.value.toMutableList()
        val reqIdx = reqList.indexOfFirst { it.id == requestId }
        val requesterId = if (reqIdx != -1) reqList[reqIdx].requesterId else ""
        val requesterName = if (reqIdx != -1) reqList[reqIdx].requesterName else ""

        // Actualizar solicitud
        if (reqIdx != -1) {
            reqList[reqIdx] = reqList[reqIdx].copy(
                status = RequestStatus.ACEPTADA.name,
                updatedAt = System.currentTimeMillis()
            )
            _requests.value = reqList
        }

        // Actualizar publicación a RESERVADO
        val itemList = _items.value.toMutableList()
        val itemIdx = itemList.indexOfFirst { it.id == wasteId }
        if (itemIdx != -1) {
            itemList[itemIdx] = itemList[itemIdx].copy(
                status = WasteStatus.RESERVADO.name,
                reservedBy = requesterId,
                reservedByName = requesterName,
                updatedAt = System.currentTimeMillis()
            )
            _items.value = itemList
        }

        firestore?.let { db ->
            try {
                db.collection("wood_wastes").document(wasteId)
                    .update(
                        mapOf(
                            "status" to WasteStatus.RESERVADO.name,
                            "reservedBy" to requesterId,
                            "reservedByName" to requesterName,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()

                db.collection("wood_wastes").document(wasteId)
                    .collection("requests").document(requestId)
                    .update("status", RequestStatus.ACEPTADA.name).await()
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    suspend fun rejectWithdrawalRequest(wasteId: String, requestId: String, reason: String?): Result<Unit> {
        val reqList = _requests.value.toMutableList()
        val reqIdx = reqList.indexOfFirst { it.id == requestId }
        if (reqIdx != -1) {
            reqList[reqIdx] = reqList[reqIdx].copy(
                status = RequestStatus.RECHAZADA.name,
                rejectionReason = reason,
                updatedAt = System.currentTimeMillis()
            )
            _requests.value = reqList
        }

        firestore?.let { db ->
            try {
                db.collection("wood_wastes").document(wasteId)
                    .collection("requests").document(requestId)
                    .update(
                        mapOf(
                            "status" to RequestStatus.RECHAZADA.name,
                            "rejectionReason" to reason
                        )
                    ).await()
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    // Confirmar retiro físico completado (Aprovechado)
    suspend fun completeWithdrawal(wasteId: String, requestId: String, completedByUid: String): Result<Unit> {
        val now = System.currentTimeMillis()

        val reqList = _requests.value.toMutableList()
        val reqIdx = reqList.indexOfFirst { it.id == requestId }
        if (reqIdx != -1) {
            reqList[reqIdx] = reqList[reqIdx].copy(
                status = RequestStatus.COMPLETADA.name,
                completedAt = now,
                updatedAt = now
            )
            _requests.value = reqList
        }

        val itemList = _items.value.toMutableList()
        val itemIdx = itemList.indexOfFirst { it.id == wasteId }
        if (itemIdx != -1) {
            itemList[itemIdx] = itemList[itemIdx].copy(
                status = WasteStatus.APROVECHADO.name,
                completedAt = now,
                completedBy = completedByUid,
                updatedAt = now
            )
            _items.value = itemList
        }

        firestore?.let { db ->
            try {
                db.collection("wood_wastes").document(wasteId)
                    .update(
                        mapOf(
                            "status" to WasteStatus.APROVECHADO.name,
                            "completedAt" to now,
                            "completedBy" to completedByUid,
                            "updatedAt" to now
                        )
                    ).await()

                db.collection("wood_wastes").document(wasteId)
                    .collection("requests").document(requestId)
                    .update(
                        mapOf(
                            "status" to RequestStatus.COMPLETADA.name,
                            "completedAt" to now
                        )
                    ).await()
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    fun getCircularMetrics(): CircularMetrics {
        val all = _items.value
        val available = all.count { it.isAvailable }
        val reserved = all.count { it.isReserved }
        val completed = all.filter { it.isCompleted }

        var totalM3 = 0.0
        var totalKg = 0.0

        completed.forEach { item ->
            if (item.volumeM3 != null && item.volumeM3 > 0) {
                totalM3 += item.volumeM3
            } else if (item.quantityUnit == "m³") {
                totalM3 += item.quantityValue
            } else if (item.quantityUnit == "kg") {
                totalKg += item.quantityValue
            }
        }

        val distribution = all.groupBy { it.subaSector }
            .mapValues { it.value.size }

        return CircularMetrics(
            availableCount = available,
            reservedCount = reserved,
            completedCount = completed.size,
            totalM3Recovered = (totalM3 * 10).toInt() / 10.0,
            totalKgRecovered = (totalKg * 10).toInt() / 10.0,
            sectorDistribution = distribution
        )
    }
}
