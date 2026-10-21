package com.example.kotlinapp.data.network

import com.example.kotlinapp.BuildConfig
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
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isNotBlank() && !apiKey.contains("YOUR_GEMINI_API_KEY")) {
            val apiResult = callGeminiRest(apiKey, userPrompt)
            if (apiResult.isSuccess) {
                return@withContext apiResult
            }
        }

        // Si la clave no está configurada o hay fallo de red, usamos el motor experto local estructurado
        val fallbackResponse = getExpertFallbackResponse(userPrompt)
        val notice = if (apiKey.isBlank() || apiKey.contains("YOUR_GEMINI_API_KEY")) {
            "(Modo Local - Sin clave GEMINI)\n\n"
        } else {
            "(Respaldo Local Inteligente)\n\n"
        }
        Result.success(cleanFormatting(notice + fallbackResponse))
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

    private fun callGeminiRest(apiKey: String, prompt: String): Result<String> {
        val modelsToTry = listOf(MODEL_NAME, FALLBACK_MODEL_NAME)
        for (model in modelsToTry) {
            val result = executeGeminiRequest(apiKey, model, prompt)
            if (result.isSuccess) {
                return Result.success(cleanFormatting(result.getOrThrow()))
            }
        }
        return Result.failure(Exception("No se pudo conectar con el servicio Gemini."))
    }

    private fun executeGeminiRequest(apiKey: String, model: String, prompt: String): Result<String> {
        return try {
            val url = URL("$BASE_URL/$model:generateContent?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12000
                readTimeout = 18000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            // Prompt de sistema blindado contra desvíos de tema y con límite de formato en 3 puntos
            val systemInstruction = "Eres el Asistente Botánico y de Gestión Forestal de WoodMatch para la localidad de Suba, Bogotá. " +
                    "Tu función exclusiva es asesorar a usuarios nuevos sobre especies vegetales, madera urbana y cómo crear reportes de residuos en Suba. " +
                    "REGLA 1 (ANTI-DESVÍO): NUNCA hables de temas ajenos a la botánica, madera, árboles urbanos o reciclaje leñoso en Suba. Si te preguntan otra cosa, responde cortésmente que solo orientas temas de madera y flora de Suba. " +
                    "REGLA 2 (ESTRUCTURA): Responde OBLIGATORIAMENTE en MÁXIMO 3 PUNTOS CLAVE numerados (1, 2 y 3). " +
                    "REGLA 3 (SINTAXIS): No uses texto de relleno ni introducciones largas. No uses asteriscos dobles (**) ni cursivas (*), usa texto limpio y legible."

            val jsonBody = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)

                val systemObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", partsArr)
                }
                put("systemInstruction", systemObj)

                // Límite estricto de longitud de salida para evitar respuestas largas
                val genConfig = JSONObject().apply {
                    put("maxOutputTokens", 280)
                    put("temperature", 0.2)
                }
                put("generationConfig", genConfig)
            }

            connection.outputStream.bufferedWriter().use { it.write(jsonBody.toString()) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val rawResponse = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                val root = JSONObject(rawResponse)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    Result.success(text.trim())
                } else {
                    Result.failure(Exception("Respuesta vacía"))
                }
            } else {
                Result.failure(Exception("Error de comunicación ($responseCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Motor de conocimiento botánico local estructurado en 3 puntos clave
     */
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
