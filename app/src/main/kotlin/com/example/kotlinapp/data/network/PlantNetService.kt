package com.example.kotlinapp.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.TreeSpecies
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class PlantNetCandidate(
    val scientificName: String,
    val authorship: String,
    val family: String,
    val commonNames: List<String>,
    val score: Double,
    val imageUrl: String,
    val organ: String
) {
    val scorePercentage: Int get() = (score * 100).toInt().coerceIn(0, 100)
    val primaryCommonName: String get() = commonNames.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: scientificName

    fun toTreeSpecies(regionGroup: String = "Andina"): TreeSpecies {
        return TreeSpecies(
            id = "pn_${UUID.randomUUID().toString().take(8)}",
            commonName = primaryCommonName,
            scientificName = scientificName,
            season = "Sin datos",
            co2AbsorptionPerYearKg = -1,
            description = "Candidato botánico con puntuación del servicio de ${(score * 100).toInt()}%; no es una identificación confirmada. Perteneciente a la familia $family.",
            nativeRegion = "Sin datos",
            conservationStatus = "Sin verificar",
            tag = "Reconocimiento Inteligente",
            category = "Sin verificar",
            imageUrl = imageUrl,
            family = family,
            regionGroup = regionGroup
        )
    }
}

data class PlantNetIdentificationResult(
    val bestMatch: String,
    val candidates: List<PlantNetCandidate>,
    val remainingRequests: Int? = null,
    val project: String = "Flora Andina y Neotropical"
)

object PlantNetService {
    private const val BASE_URL = "https://my-api.plantnet.org/v2/identify/all"
    private const val SPECIES_URL = "https://my-api.plantnet.org/v2/species"

    // Mapeo inteligente de nombres botánicos populares en español a género científico aceptado
    private val commonNameToGenusMap = mapOf(
        "rosa" to "Rosa",
        "rosas" to "Rosa",
        "rosal" to "Rosa",
        "tulipan" to "Tulipa",
        "tulipán" to "Tulipa",
        "tulipanes" to "Tulipa",
        "girasol" to "Helianthus",
        "girasoles" to "Helianthus",
        "orquidea" to "Cattleya",
        "orquídea" to "Cattleya",
        "orquideas" to "Cattleya",
        "orquídeas" to "Cattleya",
        "margarita" to "Bellis",
        "margaritas" to "Bellis",
        "clavel" to "Dianthus",
        "claveles" to "Dianthus",
        "lirio" to "Lilium",
        "lirios" to "Lilium",
        "hortensia" to "Hydrangea",
        "hortensias" to "Hydrangea",
        "lavanda" to "Lavandula",
        "pino" to "Pinus",
        "pinos" to "Pinus",
        "cedro" to "Cedrela",
        "cedros" to "Cedrela",
        "eucalipto" to "Eucalyptus",
        "eucaliptos" to "Eucalyptus",
        "roble" to "Quercus",
        "robles" to "Quercus",
        "ceiba" to "Ceiba",
        "palma" to "Ceroxylon",
        "palmas" to "Ceroxylon",
        "guayacan" to "Handroanthus",
        "guayacán" to "Handroanthus",
        "helecho" to "Pteris",
        "helechos" to "Pteris",
        "cactus" to "Cactaceae",
        "bambu" to "Bambusa",
        "bambú" to "Bambusa",
        "ficus" to "Ficus",
        "sauce" to "Salix",
        "olivo" to "Olea",
        "olivos" to "Olea",
        "jazmin" to "Jasminum",
        "jazmín" to "Jasminum",
        "monstera" to "Monstera",
        "cafe" to "Coffea",
        "café" to "Coffea",
        "aguacate" to "Persea",
        "mango" to "Mangifera"
    )

    /**
     * Identifica una planta a partir de una URL de imagen accesible públicamente
     */
    suspend fun identifyByUrl(
        imageUrl: String,
        organ: String = "auto"
    ): Result<PlantNetIdentificationResult> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Selecciona una fotografía local para identificarla. No se aceptan enlaces externos."))
    }

    /**
     * Identifica una planta a partir de un Uri de imagen local (Galería o Cámara)
     */
    suspend fun identifyFromUri(
        context: Context,
        uri: Uri,
        organ: String = "auto"
    ): Result<PlantNetIdentificationResult> = withContext(Dispatchers.IO) {
        try {
            val imageBytes = readAndCompressImage(context, uri)
                ?: return@withContext Result.failure(Exception("No se pudo leer o procesar la imagen seleccionada."))

            identifyByBytes(imageBytes, organ)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envío multipart/form-data con los bytes de la fotografía a Pl@ntNet v2
     */
    suspend fun identifyByBytes(
        imageBytes: ByteArray,
        organ: String = "auto"
    ): Result<PlantNetIdentificationResult> = withContext(Dispatchers.IO) {
        try {
            check(com.example.kotlinapp.data.repository.AuthRepository.isAuthenticated) { "Inicia sesión para consultar identificación botánica." }
            require(imageBytes.size <= 3 * 1024 * 1024) { "La imagen supera 3 MB." }
            val response = PilotApi.call("identifyPlant", mapOf("image" to android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP), "organ" to organ))
            Result.success(parseIdentificationJson(response.toString()))
        } catch (error: Exception) {
            if (error.message?.contains("límite", ignoreCase = true) == true) return@withContext Result.failure(Exception("Se alcanzó el límite de identificación de hoy. Puedes continuar sin identificar la especie."))
            Result.failure(Exception("Identificación no disponible. Inicia sesión y comprueba tu conexión; puedes continuar sin identificar la especie."))
        }
    }

    private fun parseIdentificationJson(jsonString: String): PlantNetIdentificationResult {
        val root = JSONObject(jsonString)
        val bestMatch = root.optString("bestMatch", "Desconocida")
        val remaining = if (root.has("remainingIdentificationRequests")) {
            root.optInt("remainingIdentificationRequests")
        } else null

        val resultsArray = root.optJSONArray("results")
        val candidates = mutableListOf<PlantNetCandidate>()

        if (resultsArray != null) {
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val score = item.optDouble("score", 0.0)

                val speciesObj = item.optJSONObject("species")
                val scientificName = speciesObj?.optString("scientificNameWithoutAuthor") ?: ""
                val authorship = speciesObj?.optString("scientificNameAuthorship") ?: ""

                val familyObj = speciesObj?.optJSONObject("family")
                val family = familyObj?.optString("scientificNameWithoutAuthor") ?: ""

                val commonNamesList = mutableListOf<String>()
                val commonNamesArr = speciesObj?.optJSONArray("commonNames")
                if (commonNamesArr != null) {
                    for (c in 0 until commonNamesArr.length()) {
                        commonNamesList.add(commonNamesArr.getString(c))
                    }
                }

                // Imagen de referencia provista por Pl@ntNet
                var imageUrl = ""
                var detectedOrgan = "leaf"
                val imagesArr = item.optJSONArray("images")
                if (imagesArr != null && imagesArr.length() > 0) {
                    val firstImg = imagesArr.getJSONObject(0)
                    detectedOrgan = firstImg.optString("organ", "leaf")
                    val urlObj = firstImg.optJSONObject("url")
                    imageUrl = urlObj?.optString("m")
                        ?: urlObj?.optString("s")
                        ?: urlObj?.optString("o")
                        ?: ""
                }

                candidates.add(
                    PlantNetCandidate(
                        scientificName = scientificName,
                        authorship = authorship,
                        family = family,
                        commonNames = commonNamesList,
                        score = score,
                        imageUrl = imageUrl,
                        organ = detectedOrgan
                    )
                )
            }
        }

        return PlantNetIdentificationResult(
            bestMatch = bestMatch,
            candidates = candidates,
            remainingRequests = remaining
        )
    }

    private fun parseErrorMessage(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            root.optString("message", root.optString("error", jsonString))
        } catch (_: Exception) {
            jsonString.take(150)
        }
    }

    private fun readAndCompressImage(context: Context, uri: Uri): ByteArray? {
        // Importa, limita el tamaño y recodifica la foto antes de enviarla.
        // Así se evita decodificar una imagen de cámara completa en memoria.
        val local = com.example.kotlinapp.data.image.PhotoStorage.importPhoto(context, uri)
        val file = java.io.File(requireNotNull(local.path))
        return try { file.readBytes().takeIf { it.size <= 3 * 1024 * 1024 } } finally { file.delete() }
    }

    /**
     * Consulta el catálogo botánico en vivo mediante el endpoint GET /v2/species de Pl@ntNet.
     * Permite buscar por nombres populares ("rosa", "tulipan", "girasol", "orquidea")
     * o nombres científicos / de género ("Rosa", "Tulipa", "Monstera", etc.).
     */
    suspend fun searchSpeciesOnline(
        query: String,
        lang: String = "es"
    ): Result<List<TreeSpecies>> = withContext(Dispatchers.IO) {
        try {
            check(com.example.kotlinapp.data.repository.AuthRepository.isAuthenticated)
            val response = PilotApi.call("searchSpecies", mapOf("query" to query.take(120)))
            val values = response.optJSONArray("species") ?: JSONArray()
            Result.success((0 until values.length()).mapNotNull { index ->
                val row = values.optJSONObject(index) ?: return@mapNotNull null
                val scientific = row.optString("scientificName").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                TreeSpecies(id = "remote_${scientific.hashCode()}", commonName = row.optString("commonName", scientific), scientificName = scientific, season = "Sin datos", co2AbsorptionPerYearKg = -1, description = "Dato consultado en Pl@ntNet; requiere revisión.", nativeRegion = "Sin datos", conservationStatus = "Sin verificar", tag = "Consulta remota")
            })
        } catch (_: Exception) { Result.failure(Exception("Consulta remota no disponible. Puedes buscar en el catálogo local.")) }
    }

    private fun getRepresentativeBotanicalImage(genus: String, scientific: String): String {
        return when {
            genus.equals("Rosa", ignoreCase = true) || scientific.contains("rosa") ->
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600"
            genus.equals("Tulipa", ignoreCase = true) || scientific.contains("tulipa") ->
                "https://images.unsplash.com/photo-1520763185298-1b434c919102?w=600"
            genus.equals("Helianthus", ignoreCase = true) || scientific.contains("helianthus") ->
                "https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=600"
            genus.equals("Cattleya", ignoreCase = true) || genus.equals("Orchis", ignoreCase = true) || scientific.contains("orchis") ->
                "https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=600"
            genus.equals("Monstera", ignoreCase = true) || scientific.contains("monstera") ->
                "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?w=600"
            genus.equals("Lavandula", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1528183429752-a97d0bf99b5a?w=600"
            genus.equals("Pinus", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=600"
            genus.equals("Quercus", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?w=600"
            genus.equals("Coffea", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600"
            else ->
                "https://images.unsplash.com/photo-1448375240586-882707db888b?w=600"
        }
    }
}
