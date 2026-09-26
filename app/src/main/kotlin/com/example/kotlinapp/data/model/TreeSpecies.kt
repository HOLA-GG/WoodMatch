package com.example.kotlinapp.data.model

data class TreeSpecies(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val season: String, // "Primavera", "Verano", "Otoño", "Invierno"
    val co2AbsorptionPerYearKg: Int,
    val description: String,
    val nativeRegion: String,
    val conservationStatus: String,
    val tag: String,
    val category: String = "Árbol", // "Árbol", "Arbusto", "Flor / Planta"
    val imageUrl: String = "",
    val family: String = "", // Familia botánica (Estándar Pl@ntNet / APG IV)
    val regionGroup: String = "Andina" // Grupo regional para consultas ligeras
)
