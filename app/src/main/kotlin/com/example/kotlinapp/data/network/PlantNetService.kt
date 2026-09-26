package com.example.kotlinapp.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.TreeSpecies
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
        val calculatedCo2 = when {
            organ.equals("bark", ignoreCase = true) || primaryCommonName.contains("árbol", ignoreCase = true) -> 35
            organ.equals("leaf", ignoreCase = true) -> 22
            else -> 18
        }

        return TreeSpecies(
            id = "pn_${UUID.randomUUID().toString().take(8)}",
            commonName = primaryCommonName,
            scientificName = scientificName,
            season = "Primavera",
            co2AbsorptionPerYearKg = calculatedCo2,
            description = "Espécimen botánico identificado con ${(score * 100).toInt()}% de precisión. Perteneciente a la familia $family.",
            nativeRegion = "$regionGroup, Colombia y Neotrópico",
            conservationStatus = "Identificación Verificada",
            tag = "Reconocimiento Inteligente",
            category = if (calculatedCo2 > 25) "Árbol" else "Flor / Planta",
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

    // Clave configurada desde BuildConfig / .env
    var apiKey: String = BuildConfig.PLANTNET_API_KEY
        private set

    fun updateApiKey(newKey: String) {
        if (newKey.isNotBlank()) {
            apiKey = newKey.trim()
        }
    }

    /**
     * Identifica una planta a partir de una URL de imagen accesible públicamente
     */
    suspend fun identifyByUrl(
        imageUrl: String,
        organ: String = "auto"
    ): Result<PlantNetIdentificationResult> = withContext(Dispatchers.IO) {
        try {
            val encodedImg = URLEncoder.encode(imageUrl, "UTF-8")
            val organParam = if (organ != "auto") "&organs=$organ" else ""
            val fullUrl = "$BASE_URL?images=$encodedImg$organParam&lang=es&api-key=$apiKey"

            val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    Exception("No se pudo identificar la especie ($responseCode). Intenta con una foto más nítida.")
                )
            }

            val result = parseIdentificationJson(responseBody)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val boundary = "===PlantNetBoundary${System.currentTimeMillis()}==="
            val organParam = if (organ != "auto") "&organs=$organ" else ""
            val fullUrl = "$BASE_URL?lang=es$organParam&api-key=$apiKey"

            val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 20000
                readTimeout = 25000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
            }

            DataOutputStream(connection.outputStream).use { out ->
                // Campo 'images' como archivo binario JPEG
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"images\"; filename=\"plant.jpg\"\r\n")
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                out.write(imageBytes)
                out.writeBytes("\r\n")

                if (organ != "auto") {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"organs\"\r\n\r\n")
                    out.writeBytes(organ)
                    out.writeBytes("\r\n")
                }

                out.writeBytes("--$boundary--\r\n")
                out.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    Exception("No se pudo identificar la especie ($responseCode). Intenta con una foto con buena iluminación.")
                )
            }

            val result = parseIdentificationJson(responseBody)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
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
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (originalBitmap == null) return null

        // Redimensionar suavemente si excede 1200px para acelerar la subida y respetar límite de 50MB
        val maxDimension = 1200
        val width = originalBitmap.width
        val height = originalBitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
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
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa al menos 2 letras para buscar en el catálogo botánico."))
        }

        try {
            // Resolver si es nombre común mapeado o prefijo directo
            val lower = trimmed.lowercase()
            val resolvedPrefix = commonNameToGenusMap[lower] ?: trimmed.replaceFirstChar { it.uppercase() }
            val encodedPrefix = URLEncoder.encode(resolvedPrefix, "UTF-8")

            val fullUrl = "$SPECIES_URL?api-key=$apiKey&lang=$lang&prefix=$encodedPrefix"

            val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream?.bufferedReader()?.use { it.readText() } ?: "[]"

            if (responseCode !in 200..299) {
                val errMsg = parseErrorMessage(responseText)
                return@withContext Result.failure(Exception("Error de consulta botánica ($responseCode): $errMsg"))
            }

            val jsonArray = JSONArray(responseText)
            val speciesList = mutableListOf<TreeSpecies>()

            for (i in 0 until jsonArray.length().coerceAtMost(30)) {
                val item = jsonArray.getJSONObject(i)
                val id = item.optString("id", UUID.randomUUID().toString().take(8))
                val scientificWithoutAuthor = item.optString("scientificNameWithoutAuthor", "")
                val scientificAuthor = item.optString("scientificNameAuthorship", "")
                val commonNamesArray = item.optJSONArray("commonNames")

                val commonNames = mutableListOf<String>()
                if (commonNamesArray != null) {
                    for (j in 0 until commonNamesArray.length()) {
                        commonNames.add(commonNamesArray.getString(j))
                    }
                }

                val primaryCommon = commonNames.firstOrNull()?.replaceFirstChar { it.uppercase() }
                    ?: scientificWithoutAuthor.ifBlank { "Espécimen Botánico" }

                val fullScientific = if (scientificAuthor.isNotBlank()) {
                    "$scientificWithoutAuthor $scientificAuthor"
                } else {
                    scientificWithoutAuthor
                }

                // Determinar categoría botánica y CO2 estimado
                val lowerSci = scientificWithoutAuthor.lowercase()
                val isTree = lowerSci.startsWith("quercus") || lowerSci.startsWith("pinus") ||
                        lowerSci.startsWith("cedrela") || lowerSci.startsWith("eucalyptus") ||
                        lowerSci.startsWith("ceiba") || lowerSci.startsWith("handroanthus") ||
                        lowerSci.startsWith("ceroxylon") || primaryCommon.contains("árbol", ignoreCase = true)
                val isShrub = lowerSci.startsWith("fuchsia") || lowerSci.startsWith("hydrangea") ||
                        lowerSci.startsWith("lavandula") || primaryCommon.contains("arbusto", ignoreCase = true)

                val category = when {
                    isTree -> "Árbol"
                    isShrub -> "Arbusto"
                    else -> "Flor / Planta"
                }

                val co2 = when (category) {
                    "Árbol" -> 35
                    "Arbusto" -> 14
                    else -> 8
                }

                val imageUrl = getRepresentativeBotanicalImage(resolvedPrefix, lowerSci)

                speciesList.add(
                    TreeSpecies(
                        id = "pn_sp_$id",
                        commonName = primaryCommon,
                        scientificName = fullScientific,
                        season = "Primavera",
                        co2AbsorptionPerYearKg = co2,
                        description = "Especie botánica indexada por Pl@ntNet (GET /v2/species). Nombre común: $primaryCommon. Taxonomía verificada.",
                        nativeRegion = "Registro botánico global y neotropical",
                        conservationStatus = "Catalogado",
                        tag = "Pl@ntNet GET",
                        category = category,
                        imageUrl = imageUrl,
                        family = resolvedPrefix,
                        regionGroup = "Andina"
                    )
                )
            }

            Result.success(speciesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
