package com.example.kotlinapp.data.model

enum class RequestStatus(val displayName: String) {
    PENDIENTE("Pendiente de Aceptación"),
    ACEPTADA("Aceptada / En Coordinación"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada"),
    COMPLETADA("Retiro Completado"),
    VENCIDA("Vencida")
}

data class WithdrawalRequest(
    val id: String = "",
    val wasteId: String = "",
    val authorId: String = "",
    val passValidatedAt: Long? = null,
    val authorConfirmedAt: Long? = null,
    val requesterConfirmedAt: Long? = null,
    val receipt: Map<String, Any>? = null,
    val wasteTitle: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterPhone: String = "",
    val requesterRole: String = "RECEPTOR",
    val intendedUse: String = "",
    val proposedDate: String = "",
    val requestedQuantity: String = "",
    val message: String = "",
    val status: String = RequestStatus.PENDIENTE.name,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val responseExpiresAt: Long? = null,
    val reservationExpiresAt: Long? = null,
    val reschedule: Map<String, Any>? = null,
    val timeline: List<Map<String, Any>> = emptyList(),
    val cancellationReason: String? = null,
    val closeReason: String? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val typedStatus: RequestStatus
        get() = try {
            RequestStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            RequestStatus.PENDIENTE
        }

    @get:com.google.firebase.firestore.Exclude
    val isPending: Boolean get() = typedStatus == RequestStatus.PENDIENTE
    @get:com.google.firebase.firestore.Exclude
    val isAccepted: Boolean get() = typedStatus == RequestStatus.ACEPTADA
    @get:com.google.firebase.firestore.Exclude
    val isCompleted: Boolean get() = typedStatus == RequestStatus.COMPLETADA
    @get:com.google.firebase.firestore.Exclude
    val isClosed: Boolean get() = typedStatus in listOf(RequestStatus.RECHAZADA, RequestStatus.CANCELADA, RequestStatus.COMPLETADA, RequestStatus.VENCIDA)
}
