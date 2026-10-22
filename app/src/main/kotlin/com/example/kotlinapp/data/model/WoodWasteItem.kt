package com.example.kotlinapp.data.model

enum class WasteStatus(val displayName: String) {
    DISPONIBLE("Disponible"),
    RESERVADO("En Reserva"),
    APROVECHADO("Aprovechado / Retirado"),
    INACTIVO("Inactivo / Cancelado")
}

enum class WasteCategory(val displayName: String, val description: String) {
    BIOMASA("Biomasa y Compost", "Ramas menudas, hojas, viruta y madera degradada para suelo"),
    ESTANDAR("Madera Estándar", "Listones, pallets, rollizos medianos para construcción o tutores"),
    FINA("Madera Fina y Noble", "Troncos aprovechables para ebanistería, torneado y artesanía")
}

data class WoodWasteItem(
    val id: String = "",
    val schemaVersion: Int = 2,
    val title: String = "",
    val description: String = "",
    val wasteType: String = "Ramas y podas de arbolado",
    val materialState: String = "Verde recién cortado",
    val category: String = WasteCategory.ESTANDAR.name,
    // Cantidades estructuradas (sin métricas engañosas)
    val quantityValue: Double = 1.0,
    val quantityUnit: String = "m³", // "m³", "kg", "bultos", "unidades"
    val volumeM3: Double? = null,
    val quantityEstimated: Boolean = true,
    // Ubicación protegida en Suba
    val subaSector: String = "Suba Centro",
    val exactLatitude: Double = 4.7472,
    val exactLongitude: Double = -74.0841,
    val referenceAddress: String = "",
    // Especie y botánica (Pl@ntNet)
    val speciesName: String = "Especie no identificada",
    val speciesConfidence: Double = 0.0,
    // Asistencia de IA (Gemini - Firebase AI Logic)
    val suggestedUses: String = "",
    val isAiAssisted: Boolean = false,
    // Estado y trazabilidad
    val status: String = WasteStatus.DISPONIBLE.name,
    val authorId: String = "",
    val authorName: String = "",
    val authorPhone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reservedBy: String? = null,
    val reservedByName: String? = null,
    val completedAt: Long? = null,
    val completedBy: String? = null,
    val imageUri: String? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val typedStatus: WasteStatus
        get() = try {
            WasteStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            WasteStatus.DISPONIBLE
        }

    @get:com.google.firebase.firestore.Exclude
    val typedCategory: WasteCategory
        get() = try {
            WasteCategory.valueOf(category.uppercase())
        } catch (_: Exception) {
            WasteCategory.ESTANDAR
        }

    @get:com.google.firebase.firestore.Exclude
    val isAvailable: Boolean get() = typedStatus == WasteStatus.DISPONIBLE
    @get:com.google.firebase.firestore.Exclude
    val isReserved: Boolean get() = typedStatus == WasteStatus.RESERVADO
    @get:com.google.firebase.firestore.Exclude
    val isCompleted: Boolean get() = typedStatus == WasteStatus.APROVECHADO

    @get:com.google.firebase.firestore.Exclude
    val formattedQuantity: String
        get() = "${if (quantityValue % 1.0 == 0.0) quantityValue.toInt().toString() else quantityValue.toString()} $quantityUnit" +
                if (quantityEstimated) " (Aprox.)" else ""
}
