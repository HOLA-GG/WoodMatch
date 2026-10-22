package com.example.kotlinapp.data.network

import com.example.kotlinapp.BuildConfig
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ForestAiService {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"
    private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Consulta al Consultor Botánico Inteligente sobre especies, cuidados o aprovechamiento.
     * Restringido a máximo 3 puntos clave y enfocado en la localidad de Suba, Bogotá.
     */
    suspend fun askBotanicalConsultant(userPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (com.example.kotlinapp.data.repository.AuthRepository.isAuthenticated) {
            try {
                val response = com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("botanicalAdvice").call(mapOf("prompt" to userPrompt.take(2000))).await()
                val text = (response.data as Map<*, *>)["text"] as? String
                if (!text.isNullOrBlank()) return@withContext Result.success("Sugerencia Gemini · Requiere revisión humana\n\n" + cleanFormatting(text.take(3000)))
            } catch (_: Exception) { /* Explicit local fallback below. */ }
        }
        Result.success(cleanFormatting("Orientación local · Sin consulta a IA remota\n\n" + getExpertFallbackResponse(userPrompt)))
    }

    /**
     * Guía interactiva paso a paso para que usuarios nuevos clasifiquen y reporten residuos en Suba.
     * Retorna una respuesta sintetizada estrictamente en 3 puntos clave.
     */
    suspend fun guideNewUserReport(wasteType: String, sector: String, details: String): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Usuario nuevo en Suba necesita reportar residuo leñoso.\n" +
                "Tipo/Especie: $wasteType\n" +
                "Sector de Suba: $sector\n" +
                "Detalles adicionales: ${if (details.isBlank()) "Sin detalles adicionales" else details}\n" +
                "Genera una respuesta estructurada estrictamente en 3 puntos numerados:\n" +
                "1. Diagnóstico del material y estado.\n" +
                "2. Potencial circular y uso recomendado en Suba.\n" +
                "3. Pasos exactos para completar este reporte en la app."

        askBotanicalConsultant(prompt)
    }

    /**
     * Genera una guía de siembra y cuidados para una especie específica en máximo 3 puntos.
     */
    suspend fun getCareGuide(speciesName: String, region: String): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Guía botánica para '$speciesName' en $region (Suba, Bogotá). " +
                "Genera EXACTAMENTE 3 puntos numerados: " +
                "1. Suelo y exposición solar. " +
                "2. Riego y prevención de plagas. " +
                "3. Beneficio ecológico y captura de CO2. Breve, directo y sin asteriscos."

        askBotanicalConsultant(prompt)
    }

    private fun cleanFormatting(text: String): String {
        return text.replace("**", "").replace("*", "")
    }

    private fun getExpertFallbackResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("estiba") || q.contains("tabla") || q.contains("palet") -> {
                "1. Diagnóstico: Madera aserrada de pino o eucalipto, ideal para carpintería comunitaria o mobiliario.\n" +
                "2. Potencial circular en Suba: Apta para talleres de ebanistería en Suba Centro o composteras urbanas.\n" +
                "3. Paso para el reporte: Selecciona 'Estibas / Tablas', indica si están secas o húmedas y publica tu punto en el mapa."
            }
            q.contains("poda") || q.contains("rama") || q.contains("follaje") -> {
                "1. Diagnóstico: Residuo verde leñoso derivado de mantenimiento de jardines o arbolado urbano.\n" +
                "2. Potencial circular en Suba: Excelente para triturado (chipping), acolchado de suelos y compostaje en huertas.\n" +
                "3. Paso para el reporte: Especifica el volumen aproximado en bultos o m³ y ubica el sector de Suba para recolección."
            }
            q.contains("tronco") || q.contains("troza") -> {
                "1. Diagnóstico: Madera rolliza o trozas macizas procedentes de talas autorizadas o caídas de árboles.\n" +
                "2. Potencial circular en Suba: Aprovechable para bioconstrucción, postes de cercado o torneado artesanal.\n" +
                "3. Paso para el reporte: Indica diámetro aproximado, especie si la conoces y si se requiere vehículo pesado."
            }
            q.contains("co2") || q.contains("carbon") || q.contains("absorb") || q.contains("aire") -> {
                "1. Diagnóstico de absorción de Carbono: El Roble andino (Quercus humboldtii), la Ceiba y el Aliso son los mayores capturadores de CO₂ en Suba.\n" +
                "2. Impacto en Suba: Claves para mitigar emisiones en corredores como la Reserva Thomas van der Hammen y Cerros de Suba.\n" +
                "3. Paso para el reporte: Si identificas estos árboles en riesgo o requieres retiro de ramas, crea un reporte en la app seleccionando 'Árbol Nativo'."
            }
            q.contains("roble") -> {
                "1. Diagnóstico: Especie nativa protegida (Quercus humboldtii), clave en los Cerros de Suba.\n" +
                "2. Cuidado en Suba: Requiere suelo profundo, buen drenaje y conservación sin intervenciones agresivas.\n" +
                "3. Reporte: Si encuentras madera caída, repórtala como 'Madera Nativa Protegida' para gestión ambiental distrital."
            }
            else -> {
                "1. Diagnóstico: Material leñoso o especie urbana identificado en la localidad de Suba.\n" +
                "2. Potencial circular: Se evalúa su reutilización en compostaje, bioenergía o carpintería según humedad y dureza.\n" +
                "3. Paso para el reporte: Completa el sector de Suba, añade una fotografía clara y pulsa 'Publicar Residuo'."
            }
        }
    }
}
