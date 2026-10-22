package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import android.net.Uri

data class CircularMetrics(
    val availableCount: Int = 0,
    val reservedCount: Int = 0,
    val completedCount: Int = 0,
    val totalM3Recovered: Double = 0.0,
    val totalKgRecovered: Double = 0.0,
    val sectorDistribution: Map<String, Int> = emptyMap()
)

object WoodWasteRepository {
    private val _items = MutableStateFlow<List<WoodWasteItem>>(emptyList())
    val items = _items.asStateFlow()
    private val _requests = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val requests = _requests.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val listeners = mutableListOf<ListenerRegistration>()

    fun refreshSession() {
        listeners.forEach { it.remove() }; listeners.clear()
        _items.value = emptyList(); _requests.value = emptyList(); _error.value = null
        if (!AuthRepository.isAuthenticated) return
        val user = AuthRepository.currentUser.value ?: return
        val db = FirebaseFirestore.getInstance()
        listeners += db.collection("wood_wastes").whereEqualTo("schemaVersion", 2).addSnapshotListener { snapshot, error ->
            if (AuthRepository.currentUser.value?.uid != user.uid || !AuthRepository.isAuthenticated) return@addSnapshotListener
            if (error != null) { _error.value = "No se pudo cargar el catálogo. Comprueba tu conexión y vuelve a entrar."; return@addSnapshotListener }
            _items.value = snapshot?.documents?.mapNotNull { runCatching { it.toObject(WoodWasteItem::class.java)?.copy(id = it.id) }.getOrNull() }.orEmpty()
        }
        val owned = mutableMapOf<String, WithdrawalRequest>()
        val requested = mutableMapOf<String, WithdrawalRequest>()
        listOf("authorId" to owned, "requesterId" to requested).forEach { (field, target) ->
            listeners += db.collection("withdrawal_requests").whereEqualTo(field, user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (AuthRepository.currentUser.value?.uid != user.uid || !AuthRepository.isAuthenticated) return@addSnapshotListener
                    if (error != null) { _error.value = "No se pudieron cargar tus solicitudes."; return@addSnapshotListener }
                    target.clear()
                    snapshot?.documents?.forEach { doc -> runCatching { doc.toObject(WithdrawalRequest::class.java)?.copy(id = doc.id) }.getOrNull()?.let { target[doc.id] = it } }
                    _requests.value = (owned + requested).values.sortedByDescending { it.createdAt }
                }
        }
    }

    private fun requireAccount() {
        check(AuthRepository.isAuthenticated) { "Inicia sesión para realizar esta acción." }
    }

    suspend fun publishWasteItem(item: WoodWasteItem): Result<String> = runCatching {
        requireAccount()
        val user = requireNotNull(AuthRepository.currentUser.value)
        require(user.canPublish) { "Tu perfil no puede publicar." }
        require(item.title.trim().length in 3..120 && item.description.length <= 3000) { "Revisa el título y la descripción." }
        require(item.quantityValue.isFinite() && item.quantityValue > 0 && item.quantityValue <= 1000000) { "Cantidad no válida." }
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("wood_wastes").document()
        var uploaded: com.google.firebase.storage.StorageReference? = null
        try {
            val photo = item.imageUri?.let { source ->
                val target = FirebaseStorage.getInstance().reference.child("waste_photos/${user.uid}/${ref.id}.jpg")
                target.putFile(Uri.parse(source), com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()).await(); uploaded = target
                target.downloadUrl.await().toString()
            }
            // Sólo referencias de sector: nunca publicar domicilio ni teléfono en el catálogo.
            val sector = SubaSectors.findByName(item.subaSector) ?: error("Selecciona un sector válido.")
            ref.set(item.copy(id = ref.id, authorId = user.uid, authorName = user.name,
                authorPhone = "", referenceAddress = "", exactLatitude = sector.referenceLatitude,
                exactLongitude = sector.referenceLongitude, imageUri = photo,
                reservedBy = null, reservedByName = null, completedAt = null, completedBy = null,
                status = WasteStatus.DISPONIBLE.name)).await()
            ref.id
        } catch (e: Exception) {
            uploaded?.let { runCatching { it.delete().await() } }
            throw IllegalStateException("No se pudo publicar. Comprueba la conexión y vuelve a intentarlo.", e)
        }
    }

    suspend fun updateWasteStatus(itemId: String, newStatus: WasteStatus): Result<Unit> = runCatching {
        requireAccount()
        require(newStatus == WasteStatus.INACTIVO) { "Gestiona el retiro desde su solicitud." }
        FirebaseFirestore.getInstance().collection("wood_wastes").document(itemId)
            .update(mapOf("status" to newStatus.name, "updatedAt" to System.currentTimeMillis())).await()
        Unit
    }

    suspend fun submitWithdrawalRequest(wasteId: String, requester: User, intendedUse: String,
        proposedDate: String, requestedQuantity: String, message: String): Result<String> = runCatching {
        requireAccount()
        val result = FirebaseFunctions.getInstance().getHttpsCallable("createWithdrawal").call(mapOf(
            "wasteId" to wasteId, "intendedUse" to intendedUse, "proposedDate" to proposedDate,
            "requestedQuantity" to requestedQuantity, "message" to message)).await()
        (result.data as Map<*, *>)["id"] as String
    }

    private suspend fun transition(requestId: String, action: String): Result<Unit> = runCatching {
        requireAccount()
        FirebaseFunctions.getInstance().getHttpsCallable("transitionWithdrawal")
            .call(mapOf("requestId" to requestId, "action" to action)).await()
        Unit
    }
    suspend fun acceptWithdrawalRequest(wasteId: String, requestId: String) = transition(requestId, "accept")
    suspend fun rejectWithdrawalRequest(wasteId: String, requestId: String, reason: String?) = transition(requestId, "reject")
    suspend fun completeWithdrawal(wasteId: String, requestId: String, completedByUid: String) = transition(requestId, "complete")

    suspend fun issuePass(requestId: String): Result<String> = runCatching {
        requireAccount()
        val response = FirebaseFunctions.getInstance().getHttpsCallable("issuePass").call(mapOf("requestId" to requestId)).await()
        (response.data as Map<*, *>)["token"] as String
    }
    suspend fun redeemPass(token: String): Result<Unit> = runCatching {
        requireAccount()
        FirebaseFunctions.getInstance().getHttpsCallable("redeemPass").call(mapOf("token" to token)).await()
        Unit
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
