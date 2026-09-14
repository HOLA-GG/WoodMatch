package com.example.kotlinapp.data.model

data class TreeReport(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val description: String = "",
    val species: String = "Especie no identificada",
    val condition: String = "Caído", // "Caído", "Dañado", "Sano", "En riesgo"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = ""
)
