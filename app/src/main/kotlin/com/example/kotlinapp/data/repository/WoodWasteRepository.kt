package com.example.kotlinapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.network.PilotApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Counts are limited to the authenticated user's readable data, never global claims.
data class CircularMetrics(
    val availableCount: Int = 0,
    val reservedCount: Int = 0,
    val completedCount: Int = 0,
    val totalM3Recovered: Double = 0.0,
    val totalKgRecovered: Double = 0.0,
    val sectorDistribution: Map<String, Int> = emptyMap(),
)

object WoodWasteRepository {
    private val _items = MutableStateFlow<List<WoodWasteItem>>(emptyList())
    val items = _items.asStateFlow()
    private val _requests = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val requests = _requests.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _fromCache = MutableStateFlow(false)
    val fromCache = _fromCache.asStateFlow()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    private fun receiptFile(uid: String) = context?.let { File(it.filesDir, "receipts_$uid.json") }

    private fun saveReceipts(uid: String, requests: List<WithdrawalRequest>) {
        val values =
            requests
                .filter { it.isCompleted }
                .map { request ->
                    JSONObject()
                        .put("id", request.id)
                        .put("wasteTitle", request.wasteTitle)
                        .put("authorId", request.authorId)
                        .put("requesterId", request.requesterId)
                        .put("completedAt", request.completedAt)
                        .put("status", request.status)
                        .put("receipt", JSONObject(request.receipt.orEmpty()))
                }
        runCatching { receiptFile(uid)?.writeText(org.json.JSONArray(values).toString()) }
    }

    fun cachedReceipts(uid: String): List<WithdrawalRequest> =
        runCatching {
                val list =
                    org.json.JSONArray(receiptFile(uid)?.takeIf { it.exists() }?.readText() ?: "[]")
                (0 until list.length()).map { index ->
                    val row = list.getJSONObject(index)
                    WithdrawalRequest(
                        id = row.getString("id"),
                        wasteTitle = row.getString("wasteTitle"),
                        authorId = row.getString("authorId"),
                        requesterId = row.getString("requesterId"),
                        completedAt = row.optLong("completedAt"),
                        status = "COMPLETADA",
                        receipt =
                            row.optJSONObject("receipt")?.let { r ->
                                r.keys().asSequence().associateWith { r.get(it) }
                            },
                    )
                }
            }
            .getOrDefault(emptyList())

    fun clearReceipts(uid: String) {
        receiptFile(uid)?.delete()
    }

    fun refreshSession() {
        listeners.forEach { it.remove() }
        listeners.clear()
        _items.value = emptyList()
        _requests.value = emptyList()
        _error.value = null
        if (!AuthRepository.isAuthenticated) return
        val user = AuthRepository.currentUser.value ?: return
        _requests.value = cachedReceipts(user.uid)
        val db = FirebaseFirestore.getInstance()
        val community = mutableMapOf<String, WoodWasteItem>()
        val owned = mutableMapOf<String, WoodWasteItem>()
        fun publishItems() {
            _items.value = (community + owned).values.sortedByDescending { it.createdAt }
        }
        val activeQuery =
            db.collection("wood_wastes")
                .whereIn("schemaVersion", listOf(2, 3))
                .whereIn("status", listOf("HALLAZGO", "DISPONIBLE", "RESERVADO"))
                .limit(100)
        listeners +=
            activeQuery.addSnapshotListener(
                com.google.firebase.firestore.MetadataChanges.INCLUDE
            ) { snapshot, failure ->
                if (AuthRepository.currentUser.value?.uid != user.uid) return@addSnapshotListener
                if (failure != null) {
                    _error.value = "No se pudo actualizar la comunidad. Comprueba tu conexión."
                    return@addSnapshotListener
                }
                _fromCache.value = snapshot?.metadata?.isFromCache == true
                community.clear()
                snapshot?.documents?.forEach { doc ->
                    runCatching { doc.toObject(WoodWasteItem::class.java)?.copy(id = doc.id) }
                        .getOrNull()
                        ?.let { community[doc.id] = it }
                }
                _error.value = null
                publishItems()
            }
        listeners +=
            db.collection("wood_wastes")
                .whereEqualTo("authorId", user.uid)
                .limit(100)
                .addSnapshotListener { snapshot, failure ->
                    if (AuthRepository.currentUser.value?.uid != user.uid)
                        return@addSnapshotListener
                    if (failure != null) {
                        _error.value = "No se pudieron cargar tus publicaciones."
                        return@addSnapshotListener
                    }
                    owned.clear()
                    snapshot?.documents?.forEach { doc ->
                        runCatching { doc.toObject(WoodWasteItem::class.java)?.copy(id = doc.id) }
                            .getOrNull()
                            ?.let { owned[doc.id] = it }
                    }
                    publishItems()
                }
        val outgoing = mutableMapOf<String, WithdrawalRequest>()
        val incoming = mutableMapOf<String, WithdrawalRequest>()
        listOf("authorId" to incoming, "requesterId" to outgoing).forEach { (field, target) ->
            listeners +=
                db.collection("withdrawal_requests")
                    .whereEqualTo(field, user.uid)
                    .limit(100)
                    .addSnapshotListener { snapshot, failure ->
                        if (AuthRepository.currentUser.value?.uid != user.uid)
                            return@addSnapshotListener
                        if (failure != null) {
                            _error.value = "No se pudieron actualizar tus solicitudes."
                            return@addSnapshotListener
                        }
                        target.clear()
                        snapshot?.documents?.forEach { doc ->
                            runCatching {
                                    doc.toObject(WithdrawalRequest::class.java)?.copy(id = doc.id)
                                }
                                .getOrNull()
                                ?.let { target[doc.id] = it }
                        }
                        val all =
                            (cachedReceipts(user.uid).associateBy { it.id } + incoming + outgoing)
                                .values
                                .sortedByDescending { it.createdAt }
                        _requests.value = all
                        saveReceipts(user.uid, all)
                    }
        }
    }

    suspend fun saveEntry(
        item: WoodWasteItem,
        localPhoto: String?,
        operationId: String,
        editing: Boolean = item.revision > 0,
    ): Result<String> = runCatching {
        check(AuthRepository.isAuthenticated) { "Inicia sesión para publicar." }
        var path = item.imagePath
        if (localPhoto != null) {
            val bytes =
                withContext(Dispatchers.IO) {
                    val ctx = requireNotNull(context)
                    val normalized =
                        com.example.kotlinapp.data.image.PhotoStorage.importPhoto(
                            ctx,
                            Uri.parse(localPhoto),
                        )
                    val file = File(requireNotNull(normalized.path))
                    try {
                        file.readBytes()
                    } finally {
                        file.delete()
                    }
                }
            path =
                PilotApi.call(
                        "uploadPhoto",
                        mapOf(
                            "id" to item.id,
                            "operationId" to operationId,
                            "image" to
                                android.util.Base64.encodeToString(
                                    bytes,
                                    android.util.Base64.NO_WRAP,
                                ),
                        ),
                    )
                    .getString("path")
        }
        val data =
            mapOf(
                "id" to item.id,
                "expectedRevision" to item.revision,
                "operationId" to operationId,
                "entryType" to item.entryType,
                "title" to item.title,
                "description" to item.description,
                "wasteType" to item.wasteType,
                "materialState" to item.materialState,
                "category" to item.category,
                "quantityValue" to item.quantityValue,
                "quantityUnit" to item.quantityUnit,
                "subaSector" to item.subaSector,
                "exactLatitude" to item.exactLatitude,
                "exactLongitude" to item.exactLongitude,
                "locationConfirmed" to item.locationConfirmed,
                "exactLocationConsent" to item.exactLocationConsent,
                "authorizedToOffer" to item.authorizedToOffer,
                "speciesName" to item.speciesName,
                "imagePath" to path,
            )
        PilotApi.call(if (editing) "editEntry" else "publishEntry", data).getString("id")
    }

    suspend fun photoUrl(item: WoodWasteItem): String? =
        if (item.imagePath != null)
            PilotApi.call("photoUrl", mapOf("id" to item.id)).getString("url")
        else item.imageUri

    suspend fun publishWasteItem(item: WoodWasteItem): Result<String> =
        saveEntry(
            item.copy(id = item.id.ifBlank { PilotApi.operationId() }),
            item.imageUri,
            PilotApi.operationId(),
        )

    suspend fun updateWasteStatus(itemId: String, newStatus: WasteStatus): Result<Unit> =
        runCatching {
            require(newStatus == WasteStatus.INACTIVO)
            PilotApi.call(
                "deactivateEntry",
                mapOf("id" to itemId, "operationId" to PilotApi.operationId()),
            )
            Unit
        }

    suspend fun submitWithdrawalRequest(
        wasteId: String,
        requester: User,
        intendedUse: String,
        proposedDate: String,
        requestedQuantity: String,
        message: String,
    ): Result<String> =
        request(
            wasteId,
            intendedUse,
            proposedDate,
            requestedQuantity,
            message,
            PilotApi.operationId(),
        )

    suspend fun request(
        wasteId: String,
        intendedUse: String,
        proposedDate: String,
        quantity: String,
        message: String,
        op: String,
    ): Result<String> = runCatching {
        PilotApi.call(
                "createWithdrawal",
                mapOf(
                    "id" to op,
                    "wasteId" to wasteId,
                    "intendedUse" to intendedUse,
                    "proposedDate" to proposedDate,
                    "requestedQuantity" to quantity,
                    "message" to message,
                    "operationId" to op,
                ),
            )
            .getString("id")
    }

    suspend fun transition(
        requestId: String,
        action: String,
        op: String = PilotApi.operationId(),
    ): Result<Unit> = runCatching {
        PilotApi.call(
            "transitionWithdrawal",
            mapOf("requestId" to requestId, "action" to action, "operationId" to op),
        )
        Unit
    }

    suspend fun acceptWithdrawalRequest(wasteId: String, requestId: String) =
        transition(requestId, "accept")

    suspend fun rejectWithdrawalRequest(wasteId: String, requestId: String, reason: String?) =
        transition(requestId, "reject")

    suspend fun completeWithdrawal(wasteId: String, requestId: String, completedByUid: String) =
        transition(requestId, "confirm")

    suspend fun issuePass(requestId: String): Result<String> = runCatching {
        PilotApi.call(
                "issuePass",
                mapOf("requestId" to requestId, "operationId" to PilotApi.operationId()),
            )
            .getString("token")
    }

    suspend fun redeemPass(token: String): Result<Unit> = runCatching {
        val parts = token.split(':')
        require(parts.size == 2) { "El pase no es válido." }
        PilotApi.call(
            "redeemPass",
            mapOf(
                "requestId" to parts[0],
                "code" to parts[1],
                "operationId" to PilotApi.operationId(),
            ),
        )
        Unit
    }

    fun getCircularMetrics(): CircularMetrics {
        val all = _items.value
        val completed = _requests.value.filter { it.isCompleted }
        return CircularMetrics(
            availableCount = all.count { it.isAvailable },
            reservedCount = all.count { it.isReserved },
            completedCount = completed.size,
            sectorDistribution = all.groupingBy { it.subaSector }.eachCount(),
        )
    }
}
