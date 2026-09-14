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
    val tag: String
)
