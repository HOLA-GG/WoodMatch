package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.TreeSpecies
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object SpeciesRepository {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    // Catálogo inicial curado con taxonomía Pl@ntNet y fotografías botánicas de libre acceso
    private val defaultSpecies = listOf(
        // REGIÓN CARIBE / BOSQUE SECO
        TreeSpecies(
            id = "sp_1",
            commonName = "Guayacán Amarillo",
            scientificName = "Handroanthus chrysanthus",
            season = "Primavera",
            co2AbsorptionPerYearKg = 28,
            description = "Árbol emblemático neotropical conocido por su floración dorada sincrónica. Es vital para polinizadores nativos y fija nutrientes en suelos secos.",
            nativeRegion = "Bosques secos y caribeños (0 - 1.500 msnm)",
            conservationStatus = "Vulnerable",
            tag = "Floración Dorada",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Handroanthus_chrysanthus_Venezuela_01.jpg/640px-Handroanthus_chrysanthus_Venezuela_01.jpg",
            family = "Bignoniaceae",
            regionGroup = "Caribe"
        ),
        TreeSpecies(
            id = "sp_caracoli",
            commonName = "Caracolí",
            scientificName = "Anacardium excelsum",
            season = "Verano",
            co2AbsorptionPerYearKg = 45,
            description = "Árbol majestuoso del bosque seco y valles interandinos. Desarrolla troncos rectos de gran porte que protegen cuencas hídricas.",
            nativeRegion = "Costa Caribe y Valle del Magdalena",
            conservationStatus = "Preocupación Menor",
            tag = "Gigante del Valle",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Anacardium_excelsum_leaves.jpg/640px-Anacardium_excelsum_leaves.jpg",
            family = "Anacardiaceae",
            regionGroup = "Caribe"
        ),

        // REGIÓN ANDINA
        TreeSpecies(
            id = "sp_2",
            commonName = "Roble Andino",
            scientificName = "Quercus humboldtii",
            season = "Otoño",
            co2AbsorptionPerYearKg = 42,
            description = "Especie clímax de los bosques de niebla cordilleranos. Sus bellotas nutren a aves y roedores, y retiene grandes cantidades de carbono a largo plazo.",
            nativeRegion = "Cordilleras Central y Oriental (2.000 - 3.200 msnm)",
            conservationStatus = "Casi Amenazado",
            tag = "Captura Alta de CO2",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Quercus_humboldtii_leaves.jpg/640px-Quercus_humboldtii_leaves.jpg",
            family = "Fagaceae",
            regionGroup = "Andina"
        ),
        TreeSpecies(
            id = "sp_3",
            commonName = "Palma de Cera",
            scientificName = "Ceroxylon quindiuense",
            season = "Verano",
            co2AbsorptionPerYearKg = 18,
            description = "Árbol nacional de Colombia y la palma más alta del mundo (hasta 60 m). Hábitat exclusivo del loro orejiamarillo en laderas andinas.",
            nativeRegion = "Valle de Cocora y Cordillera Central",
            conservationStatus = "En Peligro",
            tag = "Patrimonio Natural",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Valle_de_Cocora_08.jpg/640px-Valle_de_Cocora_08.jpg",
            family = "Arecaceae",
            regionGroup = "Andina"
        ),
        TreeSpecies(
            id = "sp_4",
            commonName = "Chilco Andino",
            scientificName = "Fuchsia boliviana",
            season = "Primavera",
            co2AbsorptionPerYearKg = 8,
            description = "Arbusto nativo de flores pendulares color carmesí intenso. Es una de las fuentes principales de néctar para colibríes montanos.",
            nativeRegion = "Bosques andinos y bordes de niebla",
            conservationStatus = "Preocupación Menor",
            tag = "Polinizadores",
            category = "Arbusto",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Fuchsia_boliviana_1.jpg/640px-Fuchsia_boliviana_1.jpg",
            family = "Onagraceae",
            regionGroup = "Andina"
        ),
        TreeSpecies(
            id = "sp_7",
            commonName = "Yarumo Blanco",
            scientificName = "Cecropia telenitida",
            season = "Otoño",
            co2AbsorptionPerYearKg = 24,
            description = "Árbol pionero de hojas plateadas por el envés. Restaura claros en el bosque y convive simbióticamente con hormigas protectoras.",
            nativeRegion = "Vertientes andinas (1.500 - 2.600 msnm)",
            conservationStatus = "Preocupación Menor",
            tag = "Pionero Ecológico",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/Cecropia_peltata_leaves.jpg/640px-Cecropia_peltata_leaves.jpg",
            family = "Urticaceae",
            regionGroup = "Andina"
        ),

        // PÁRAMO Y ALTA MONTAÑA
        TreeSpecies(
            id = "sp_5",
            commonName = "Frailejón de Páramo",
            scientificName = "Espeletia grandiflora",
            season = "Invierno",
            co2AbsorptionPerYearKg = 12,
            description = "Planta insignia reguladora del ciclo hídrico en los páramos. Retiene la humedad de la niebla y la filtra hacia cuencas de agua potable.",
            nativeRegion = "Páramo de Chingaza y Sumapaz (3.000 - 4.200 msnm)",
            conservationStatus = "Vulnerable",
            tag = "Fábrica de Agua",
            category = "Flor / Planta",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/Frailejones_Chingaza.jpg/640px-Frailejones_Chingaza.jpg",
            family = "Asteraceae",
            regionGroup = "Páramo"
        ),
        TreeSpecies(
            id = "sp_6",
            commonName = "Pino Romerón",
            scientificName = "Retrophyllum rospigliosii",
            season = "Invierno",
            co2AbsorptionPerYearKg = 38,
            description = "La única conífera nativa de Colombia, adaptada a zonas frías de páramo y subpáramo. Sus copas capturan humedad de la niebla.",
            nativeRegion = "Subpáramo y bosque altoandino",
            conservationStatus = "Vulnerable",
            tag = "Conífera Nativa",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Retrophyllum_rospigliosii_habitus.jpg/640px-Retrophyllum_rospigliosii_habitus.jpg",
            family = "Podocarpaceae",
            regionGroup = "Páramo"
        ),

        // AMAZONÍA
        TreeSpecies(
            id = "sp_ceiba",
            commonName = "Ceiba Amazónica",
            scientificName = "Ceiba pentandra",
            season = "Verano",
            co2AbsorptionPerYearKg = 60,
            description = "Coloso sagrado de la selva tropical con raíces tablares gigantescas. Soporta una diversidad inmensa de epífitas, aves y primates.",
            nativeRegion = "Cuenca del Amazonas y Putumayo",
            conservationStatus = "Preocupación Menor",
            tag = "Árbol Sagrado",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Ceiba_pentandra_buttress_roots.jpg/640px-Ceiba_pentandra_buttress_roots.jpg",
            family = "Malvaceae",
            regionGroup = "Amazonía"
        ),
        TreeSpecies(
            id = "sp_victoria",
            commonName = "Victoria Regia",
            scientificName = "Victoria amazonica",
            season = "Primavera",
            co2AbsorptionPerYearKg = 15,
            description = "El nenúfar más grande del planeta con hojas flotantes de hasta 3 metros capaces de soportar aves zancudas sobre lagunas amazónicas.",
            nativeRegion = "Lagos y meandros del Río Amazonas",
            conservationStatus = "Vulnerable",
            tag = "Flora Acuática",
            category = "Flor / Planta",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Victoria_amazonica_in_Kew_Gardens.jpg/640px-Victoria_amazonica_in_Kew_Gardens.jpg",
            family = "Nymphaeaceae",
            regionGroup = "Amazonía"
        ),

        // ORINOQUÍA / LLANOS
        TreeSpecies(
            id = "sp_moriche",
            commonName = "Palma Moriche",
            scientificName = "Mauritia flexuosa",
            season = "Verano",
            co2AbsorptionPerYearKg = 22,
            description = "El 'árbol de la vida' de los llanos orientales. Forma morichales en torno a nacimientos de agua donde beben jaguares y tapires.",
            nativeRegion = "Llanos Orientales y sabanas inundables",
            conservationStatus = "Preocupación Menor",
            tag = "Madre del Agua",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Mauritia_flexuosa_-_buriti.jpg/640px-Mauritia_flexuosa_-_buriti.jpg",
            family = "Arecaceae",
            regionGroup = "Orinoquía"
        ),

        // PACÍFICO / CHOCÓ
        TreeSpecies(
            id = "sp_chonta",
            commonName = "Palma de Chontaduro",
            scientificName = "Bactris gasipaes",
            season = "Otoño",
            co2AbsorptionPerYearKg = 20,
            description = "Palma espinosa fundamental para la soberanía alimentaria de las comunidades afro e indígenas del Pacífico biogeográfico.",
            nativeRegion = "Selvas pluviales del Chocó y Pacífico",
            conservationStatus = "Preocupación Menor",
            tag = "Nutrición Ancestral",
            category = "Árbol",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bactris_gasipaes_fruits.jpg/640px-Bactris_gasipaes_fruits.jpg",
            family = "Arecaceae",
            regionGroup = "Pacífico"
        )
    )

    private val _speciesList = MutableStateFlow<List<TreeSpecies>>(defaultSpecies)
    val speciesList: StateFlow<List<TreeSpecies>> = _speciesList.asStateFlow()

    fun getSpecies(): List<TreeSpecies> = _speciesList.value

    /**
     * Consulta optimizada bajo demanda por región:
     * No descarga toda la base de datos de golpe para evitar saturación de lecturas en Firestore.
     */
    suspend fun fetchSpeciesByRegion(region: String?, limit: Long = 15): List<TreeSpecies> {
        val selectedRegion = if (region.isNullOrBlank() || region.equals("Todas", ignoreCase = true)) null else region

        firestore?.let { db ->
            try {
                val query = if (selectedRegion != null) {
                    db.collection("tree_species")
                        .whereEqualTo("regionGroup", selectedRegion)
                        .limit(limit)
                } else {
                    db.collection("tree_species")
                        .limit(limit)
                }

                val snapshot = query.get().await()
                if (!snapshot.isEmpty) {
                    val remote = snapshot.documents.mapNotNull { doc -> mapDocToSpecies(doc) }
                    if (remote.isNotEmpty()) {
                        // Combina con la lista local sin duplicar IDs
                        val currentMap = _speciesList.value.associateBy { it.id }.toMutableMap()
                        remote.forEach { currentMap[it.id] = it }
                        _speciesList.value = currentMap.values.toList()
                        return remote
                    }
                } else if (selectedRegion == null) {
                    // Seed inicial en primer inicio de la app
                    for (species in defaultSpecies.take(6)) {
                        db.collection("tree_species").document(species.id).set(species)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SpeciesRepository", "Firestore regional query fallback: ${e.message}")
            }
        }

        // Retorno local filtrado
        return if (selectedRegion == null) {
            _speciesList.value.take(limit.toInt())
        } else {
            _speciesList.value.filter {
                it.regionGroup.equals(selectedRegion, ignoreCase = true) ||
                        it.nativeRegion.contains(selectedRegion, ignoreCase = true)
            }.take(limit.toInt())
        }
    }

    suspend fun fetchFromFirestore(): List<TreeSpecies> {
        return fetchSpeciesByRegion(null, limit = 20)
    }

    private fun mapDocToSpecies(doc: com.google.firebase.firestore.DocumentSnapshot): TreeSpecies {
        return TreeSpecies(
            id = doc.getString("id") ?: doc.id,
            commonName = doc.getString("commonName") ?: "Especie",
            scientificName = doc.getString("scientificName") ?: "",
            season = doc.getString("season") ?: "Primavera",
            co2AbsorptionPerYearKg = doc.getLong("co2AbsorptionPerYearKg")?.toInt() ?: 25,
            description = doc.getString("description") ?: "",
            nativeRegion = doc.getString("nativeRegion") ?: "",
            conservationStatus = doc.getString("conservationStatus") ?: "Preocupación Menor",
            tag = doc.getString("tag") ?: "Nativo",
            category = doc.getString("category") ?: "Árbol",
            imageUrl = doc.getString("imageUrl") ?: "",
            family = doc.getString("family") ?: "",
            regionGroup = doc.getString("regionGroup") ?: "Andina"
        )
    }

    suspend fun addSpecies(species: TreeSpecies): Result<Unit> {
        val updated = _speciesList.value.toMutableList()
        updated.add(0, species)
        _speciesList.value = updated

        firestore?.let { db ->
            try {
                db.collection("tree_species").document(species.id).set(species).await()
            } catch (e: Exception) {
                android.util.Log.w("SpeciesRepository", "Firestore add species fallback: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    fun searchSpecies(
        query: String,
        seasonFilter: String?,
        categoryFilter: String? = null,
        regionFilter: String? = null
    ): List<TreeSpecies> {
        return _speciesList.value.filter { species ->
            val matchesQuery = query.isBlank() ||
                    species.commonName.contains(query, ignoreCase = true) ||
                    species.scientificName.contains(query, ignoreCase = true) ||
                    species.description.contains(query, ignoreCase = true) ||
                    species.family.contains(query, ignoreCase = true)

            val matchesSeason = seasonFilter == null ||
                    seasonFilter == "Todos" ||
                    species.season.equals(seasonFilter, ignoreCase = true)

            val matchesCategory = categoryFilter == null ||
                    categoryFilter == "Todos" ||
                    species.category.equals(categoryFilter, ignoreCase = true)

            val matchesRegion = regionFilter == null ||
                    regionFilter == "Todas" ||
                    species.regionGroup.equals(regionFilter, ignoreCase = true) ||
                    species.nativeRegion.contains(regionFilter, ignoreCase = true)

            matchesQuery && matchesSeason && matchesCategory && matchesRegion
        }
    }

    /**
     * Búsqueda en vivo al endpoint GET /v2/species de Pl@ntNet.
     * Incorpora dinámicamente los especímenes encontrados al catálogo local.
     */
    suspend fun searchOnlinePlantNet(query: String): Result<List<TreeSpecies>> {
        val result = com.example.kotlinapp.data.network.PlantNetService.searchSpeciesOnline(query)
        result.onSuccess { remoteSpecies ->
            if (remoteSpecies.isNotEmpty()) {
                val currentMap = _speciesList.value.associateBy { it.id }.toMutableMap()
                remoteSpecies.forEach { currentMap[it.id] = it }
                _speciesList.value = currentMap.values.toList()
            }
        }
        return result
    }
}
