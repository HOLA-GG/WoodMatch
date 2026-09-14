package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.TreeSpecies

object SpeciesRepository {
    private val allSpecies = listOf(
        TreeSpecies(
            id = "sp_1",
            commonName = "Guayacán Amarillo",
            scientificName = "Handroanthus chrysanthus",
            season = "Primavera",
            co2AbsorptionPerYearKg = 28,
            description = "Árbol emblemático neotropical conocido por su espectacular floración dorada sincrónica. Es vital para polinizadores nativos y fija nutrientes en suelos erosionados.",
            nativeRegion = "Bosques secos y premontanos andinos",
            conservationStatus = "Vulnerable",
            tag = "Floración Dorada"
        ),
        TreeSpecies(
            id = "sp_2",
            commonName = "Roble Andino",
            scientificName = "Quercus humboldtii",
            season = "Otoño",
            co2AbsorptionPerYearKg = 42,
            description = "Especie clímax de los bosques de niebla de la cordillera. Sus bellotas nutren a aves y roedores, y su madera densa retiene grandes cantidades de carbono a largo plazo.",
            nativeRegion = "Cordilleras Central y Oriental (2.000 - 3.200 msnm)",
            conservationStatus = "Casi Amenazado",
            tag = "Captura Alta de CO2"
        ),
        TreeSpecies(
            id = "sp_3",
            commonName = "Palma de Cera",
            scientificName = "Ceroxylon quindiuense",
            season = "Verano",
            co2AbsorptionPerYearKg = 18,
            description = "Árbol nacional de Colombia y la palma más alta del planeta, alcanzando hasta 60 metros. Hábitat exclusivo del loro orejiamarillo.",
            nativeRegion = "Valle de Cocora, Quindío",
            conservationStatus = "En Peligro",
            tag = "Patrimonio Natural"
        ),
        TreeSpecies(
            id = "sp_4",
            commonName = "Cedro Rosado",
            scientificName = "Cedrela odorata",
            season = "Verano",
            co2AbsorptionPerYearKg = 35,
            description = "Árbol de porte majestuoso con aroma característico en su madera y follaje caducifolio. Muy apreciado en silvicultura regenerativa.",
            nativeRegion = "Bosques húmedos tropicales",
            conservationStatus = "Vulnerable",
            tag = "Aromático Forestal"
        ),
        TreeSpecies(
            id = "sp_5",
            commonName = "Nogal Cafetero",
            scientificName = "Cordia alliodora",
            season = "Primavera",
            co2AbsorptionPerYearKg = 30,
            description = "Árbol de rápido crecimiento ideal para sistemas agroforestales con café. Proporciona sombra regulada y protege cuencas hidrográficas.",
            nativeRegion = "Región Andina y Pacífica",
            conservationStatus = "Preocupación Menor",
            tag = "Agroforestería"
        ),
        TreeSpecies(
            id = "sp_6",
            commonName = "Pino Romerón",
            scientificName = "Retrophyllum rospigliosii",
            season = "Invierno",
            co2AbsorptionPerYearKg = 38,
            description = "La única conífera nativa de Colombia, adaptada a zonas frías de páramo y subpáramo. Sus copas capturan humedad de la niebla.",
            nativeRegion = "Bosques altoandinos",
            conservationStatus = "Vulnerable",
            tag = "Conífera Nativa"
        ),
        TreeSpecies(
            id = "sp_7",
            commonName = "Yarumo Blanco",
            scientificName = "Cecropia telenitida",
            season = "Otoño",
            co2AbsorptionPerYearKg = 24,
            description = "Árbol pionero de hojas plateadas por el envés. Restaura claros en el bosque y convive simbióticamente con hormigas protectoras.",
            nativeRegion = "Vertientes andinas",
            conservationStatus = "Preocupación Menor",
            tag = "Pionero Ecológico"
        )
    )

    fun getSpecies(): List<TreeSpecies> = allSpecies

    fun searchSpecies(query: String, seasonFilter: String?): List<TreeSpecies> {
        return allSpecies.filter { species ->
            val matchesQuery = query.isBlank() ||
                    species.commonName.contains(query, ignoreCase = true) ||
                    species.scientificName.contains(query, ignoreCase = true) ||
                    species.description.contains(query, ignoreCase = true)

            val matchesSeason = seasonFilter == null ||
                    seasonFilter == "Todos" ||
                    species.season.equals(seasonFilter, ignoreCase = true)

            matchesQuery && matchesSeason
        }
    }
}
