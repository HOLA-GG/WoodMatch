package com.example.kotlinapp.data.model

data class SubaSector(
    val id: String,
    val name: String,
    val description: String,
    val referenceLatitude: Double,
    val referenceLongitude: Double
)

object SubaSectors {
    val ALL = listOf(
        SubaSector(
            id = "suba_centro",
            name = "Suba Centro",
            description = "Zona histórica y urbana de Suba",
            referenceLatitude = 4.7472,
            referenceLongitude = -74.0841
        ),
        SubaSector(
            id = "niza_colina",
            name = "Niza / La Colina",
            description = "Sector residencial y comercial Niza y Colina Campestre",
            referenceLatitude = 4.7214,
            referenceLongitude = -74.0725
        ),
        SubaSector(
            id = "el_rincon",
            name = "El Rincón",
            description = "Sector de alta densidad y actividad comunitaria",
            referenceLatitude = 4.7335,
            referenceLongitude = -74.0952
        ),
        SubaSector(
            id = "tibabuyes",
            name = "Tibabuyes",
            description = "Zona occidental de Suba y ronda del humedal",
            referenceLatitude = 4.7431,
            referenceLongitude = -74.1167
        ),
        SubaSector(
            id = "san_jose",
            name = "San José de Bavaria",
            description = "Zona campestre, predios residenciales y arborización",
            referenceLatitude = 4.7645,
            referenceLongitude = -74.0583
        ),
        SubaSector(
            id = "britalia",
            name = "Britalia / Carmelo",
            description = "Sector residencial central y corredor vial",
            referenceLatitude = 4.7501,
            referenceLongitude = -74.0558
        ),
        SubaSector(
            id = "lombardia",
            name = "Lombardía / Compartir",
            description = "Sectores noroccidentales y de parques barriales",
            referenceLatitude = 4.7490,
            referenceLongitude = -74.1030
        ),
        SubaSector(
            id = "guaymaral",
            name = "Guaymaral / Torca",
            description = "Corredor norte rural y periurbano de Suba",
            referenceLatitude = 4.8050,
            referenceLongitude = -74.0380
        )
    )

    fun findById(id: String): SubaSector? = ALL.find { it.id.equals(id, ignoreCase = true) }
    fun findByName(name: String): SubaSector? = ALL.find { it.name.equals(name, ignoreCase = true) }
    val names: List<String> = ALL.map { it.name }
}
