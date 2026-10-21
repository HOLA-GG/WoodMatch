package com.example.kotlinapp.data.network

import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.WasteCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiValorizationSuggestion(
    val recommendedCategory: WasteCategory,
    val suggestedUses: String,
    val technicalNotes: String,
    val disclaimer: String = "Sugerencia orientativa generada por IA. No constituye certificación estructural ni resistencia mecánica."
)

object CircularEconomyAiService {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"
    private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun suggestValorization(
        wasteType: String,
        materialState: String,
        speciesName: String
    ): AiValorizationSuggestion = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isNotBlank() && !apiKey.contains("YOUR_GEMINI_API_KEY")) {
            val prompt = "Actúa como especialista en economía circular de madera urbana y biomasa en Bogotá y Suba. " +
                    "Dado el residuo: Tipo='$wasteType', Estado='$materialState', Especie='$speciesName'. " +
                    "Devuelve en formato JSON con las claves: " +
                    "'category' (uno de: BIOMASA, ESTANDAR, FINA), " +
                    "'uses' (2-3 usos circulares prácticos y recomendados), " +
                    "'notes' (1 recomendación breve de secado, curado o precauciones). " +
                    "No incluyas markdown adicional, ni asteriscos, solo el JSON puro."

            try {
                val apiJson = callGeminiWithFallback(apiKey, prompt)
                if (apiJson != null) {
                    val catStr = apiJson.optString("category", "ESTANDAR").uppercase()
                    val cat = try {
                        WasteCategory.valueOf(catStr)
                    } catch (_: Exception) {
                        determineFallbackCategory(wasteType, materialState)
                    }
                    val rawUses = apiJson.optString("uses", "Aprovechamiento en carpintería o tutores de huerta.")
                    val rawNotes = apiJson.optString("notes", "Verificar humedad antes del uso.")
                    val cleanUses = rawUses.replace("**", "").replace("*", "").trim()
                    val cleanNotes = rawNotes.replace("**", "").replace("*", "").trim()

                    return@withContext AiValorizationSuggestion(
                        recommendedCategory = cat,
                        suggestedUses = cleanUses,
                        technicalNotes = cleanNotes
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("CircularAiService", "Fallo consulta Gemini: ${e.message}")
            }
        }

        // Motor heurístico experto local (sin latencia y tolerante a fallos offline)
        getLocalExpertSuggestion(wasteType, materialState, speciesName)
    }

    private fun determineFallbackCategory(wasteType: String, materialState: String): WasteCategory {
        val lowerType = wasteType.lowercase()
        val lowerState = materialState.lowercase()

        return when {
            lowerType.contains("astilla") || lowerType.contains("viruta") || lowerType.contains("aserrín") || lowerState.contains("degradado") ->
                WasteCategory.BIOMASA
            lowerType.contains("tronco") || lowerType.contains("rollizo") ->
                WasteCategory.FINA
            else ->
                WasteCategory.ESTANDAR
        }
    }

    private fun getLocalExpertSuggestion(
        wasteType: String,
        materialState: String,
        speciesName: String
    ): AiValorizationSuggestion {
        val lowerType = wasteType.lowercase()
        val lowerSpecies = speciesName.lowercase()
        val lowerState = materialState.lowercase()

        val category: WasteCategory
        val uses: String
        val notes: String

        when {
            lowerType.contains("astilla") || lowerType.contains("viruta") || lowerType.contains("aserrín") || lowerState.contains("degradado") -> {
                category = WasteCategory.BIOMASA
                uses = "Excelente para compostaje urbano, sustrato de hongos o acolchado (mulch) para retención hídrica en suelos de Suba."
                notes = "Asegurar ventilación y evitar almacenamiento en pilas húmedas cerradas para prevenir fermentación indeseada."
            }
            lowerType.contains("pallets") || lowerType.contains("embalajes") || lowerType.contains("listones") -> {
                category = WasteCategory.ESTANDAR
                uses = "Desarme para cajones de huertas urbanas, composteras modulares, estanterías o bancos de taller."
                notes = "Comprobar ausencia de clavos o grapas oxidadas antes de corte. Verificar sellos HT (tratamiento térmico sin químicos)."
            }
            lowerType.contains("tronco") || lowerType.contains("rollizo") -> {
                if (lowerSpecies.contains("acacia") || lowerSpecies.contains("ciprés") || lowerSpecies.contains("roble") || lowerSpecies.contains("eucalipto")) {
                    category = WasteCategory.FINA
                    uses = "Aprovechamiento en aserrío para tablones, piezas de torneado, postes perimetrales o tablas de corte artesanales."
                    notes = if (lowerState.contains("verde")) "Requiere sellado de cabezas con cera/pintura para evitar rajaduras y secado bajo sombra." else "Madera estacionada lista para cepillado y dimensionado."
                } else {
                    category = WasteCategory.ESTANDAR
                    uses = "Leña seca de alto rendimiento, tutores de siembra para jardinería o rodajas decorativas."
                    notes = "Mantener elevado del suelo sobre tarimas para evitar ataque de xilófagos o humedad por capilaridad."
                }
            }
            else -> {
                category = WasteCategory.ESTANDAR
                uses = "Ensamble de cercados, tutores para arbolado joven o leña para cocción/calefacción."
                notes = "Clasificar por calibres y almacenar en lugar cubierto con buena circulación de aire."
            }
        }

        return AiValorizationSuggestion(
            recommendedCategory = category,
            suggestedUses = uses,
            technicalNotes = notes
        )
    }

    private fun callGeminiWithFallback(apiKey: String, prompt: String): JSONObject? {
        val models = listOf(MODEL_NAME, FALLBACK_MODEL_NAME)
        for (model in models) {
            val res = executeGeminiCall(apiKey, model, prompt)
            if (res != null) return res
        }
        return null
    }

    private fun executeGeminiCall(apiKey: String, model: String, prompt: String): JSONObject? {
        return try {
            val url = URL("$BASE_URL/$model:generateContent?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val body = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().put("temperature", 0.2))
            }

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            if (connection.responseCode in 200..299) {
                val respText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(respText)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        val clean = text.replace("```json", "").replace("```", "").trim()
                        return JSONObject(clean)
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
