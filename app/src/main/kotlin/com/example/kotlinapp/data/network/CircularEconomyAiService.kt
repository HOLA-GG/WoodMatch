package com.example.kotlinapp.data.network

import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.WasteCategory
import kotlinx.coroutines.tasks.await
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
    val remote: Boolean = false,
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
        if (com.example.kotlinapp.data.repository.AuthRepository.isAuthenticated) {
            try {
                val prompt = "Sugiere usos prudentes de este residuo leñoso, sin certificar especie ni resistencia: tipo=${wasteType.take(120)}, estado=${materialState.take(120)}, especie reportada=${speciesName.take(160)}. Máximo 1000 caracteres."
                val response = PilotApi.call("botanicalAdvice", mapOf("prompt" to prompt))
                val text = (response.optString("text"))?.take(1500)
                if (!text.isNullOrBlank()) return@withContext AiValorizationSuggestion(
                    determineFallbackCategory(wasteType, materialState), text,
                    "Categoría interna estimada localmente; recomendaciones de Gemini pendientes de tu revisión.", remote = true)
            } catch (_: Exception) { /* Local guidance remains available and is labelled. */ }
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

}
