package com.example.kotlinapp.data.network

import com.example.kotlinapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ForestAiService {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    /**
     * Consulta al Consultor Botánico Inteligente sobre especies, cuidados, siembra o ecología.
     */
    suspend fun askBotanicalConsultant(userPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isNotBlank()) {
            val apiResult = callGeminiRest(apiKey, userPrompt)
            if (apiResult.isSuccess) {
                return@withContext apiResult
            }
        }

        // Si la clave no está configurada o hubo fallo de red, usamos el motor experto integrado
        val fallbackResponse = getExpertFallbackResponse(userPrompt)
        Result.success(fallbackResponse)
    }

    /**
     * Genera una guía de siembra y cuidados para una especie específica.
     */
    suspend fun getCareGuide(speciesName: String, region: String): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Proporciona una guía clara, práctica y profesional para plantar y cuidar el espécimen '$speciesName' " +
                "en la región $region (Colombia/Neotrópico). Incluye: 1. Suelo y drenaje, 2. Riego y exposición solar, " +
                "3. Prevención de plagas, 4. Beneficio ecológico y captura de carbono. Máximo 4 párrafos cortos y concisos."

        askBotanicalConsultant(prompt)
    }

    private fun callGeminiRest(apiKey: String, prompt: String): Result<String> {
        return try {
            val url = URL("$BASE_URL?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val systemInstruction = "Eres el Asistente Botánico y Forestal Oficial de WoodMatch. Eres un experto " +
                    "en silvicultura, botánica andina y neotropical, reforestación comunitaria y protección de flora. " +
                    "Responde siempre en español, con un tono amable, pedagógico, profesional y directo. " +
                    "Nunca menciones APIs, claves, modelos ni backends técnicos."

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
     * Motor de conocimiento botánico local para respuestas instantáneas y continuas
     */
    private fun getExpertFallbackResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("roble") -> {
                "🌲 **Recomendación para el Roble Andino (*Quercus humboldtii*):**\n\n" +
                        "• **Hábitat:** Especie nativa de pisos montanos (1.800 a 3.200 msnm).\n" +
                        "• **Suelo:** Requiere suelos profundos con buen contenido de materia orgánica y micorrizas asociadas.\n" +
                        "• **Riego y Sol:** Moderado; prefiere exposición solar directa una vez establecido.\n" +
                        "• **Impacto Ecológico:** Excelente captador de carbono (~40 kg CO₂/año) y generador de sotobosque para aves migratorias."
            }
            q.contains("guayacan") || q.contains("guayacán") -> {
                "🌸 **Recomendación para el Guayacán (*Handroanthus*):**\n\n" +
                        "• **Floración:** Espectacular floración masiva (amarilla o rosada) que atrae polinizadores vitales.\n" +
                        "• **Suelo y Luz:** Requiere exposición a pleno sol y suelos bien drenados; es muy tolerante a sequías una vez enraizado.\n" +
                        "• **Cuidados:** Evitar el encharcamiento en los primeros 6 meses de siembra."
            }
            q.contains("palma de cera") || q.contains("cera") -> {
                "🌴 **Palma de Cera del Quindío (*Ceroxylon quindiuense*):**\n\n" +
                        "• **Importancia:** Árbol nacional y refugio del Loro Orejiamarillo.\n" +
                        "• **Condiciones:** Requiere niebla frecuente, humedad constante y alturas superiores a 2.000 msnm.\n" +
                        "• **Consejo de Siembra:** No tolerará climas cálidos ni suelos compactados sin drenaje."
            }
            q.contains("ceiba") -> {
                "🌳 **Ceiba Bonga (*Ceiba pentandra*):**\n\n" +
                        "• **Espacio:** Requiere grandes extensiones de terreno abierto por su sistema radicular tabular.\n" +
                        "• **Clima:** Cálido y húmedo o bosque seco tropical (0 a 1.200 msnm).\n" +
                        "• **Captura:** Una de las mayores fijadoras de biomasa y CO₂ en tierras bajas."
            }
            q.contains("co2") || q.contains("carbono") || q.contains("fijacion") -> {
                "🌱 **Especies con Mayor Captura de Carbono:**\n\n" +
                "1. **Ceiba Bonga** (~50 kg CO₂/año en valles cálidos)\n" +
                "2. **Roble Andino** (~40 kg CO₂/año en zonas altas)\n" +
                "3. **Caracolí** (~38 kg CO₂/año en riberas y piedemonte)\n" +
                "4. **Nogal Cafetero** (~32 kg CO₂/año en zonas cafeteras)\n\n" +
                "*Consejo:* Combinar especies de dosel alto con arbustos fijadores de nitrógeno maximiza la salud del suelo."
            }
            q.contains("plaga") || q.contains("hongo") || q.contains("hoja amarilla") -> {
                "🍃 **Manejo Ecológico de Plagas en Árboles Jóvenes:**\n\n" +
                "• **Hojas amarillentas:** Puede indicar exceso de humedad (asfixia radicular) o deficiencia de hierro/nitrógeno. Verifica el drenaje.\n" +
                "• **Pulgones o cochinillas:** Una solución suave de agua con jabón potásico biodegradable al atardecer controla la población sin dañar polinizadores.\n" +
                "• **Cortes y podas:** Aplica ceniza vegetal o pasta cicatrizante natural para sellar ramas y evitar hongos."
            }
            else -> {
                "🌿 **Consejos Generales de Reforestación y Cuidado:**\n\n" +
                "• **Aclimatación:** Planta preferiblemente al inicio de temporadas de lluvia para asegurar el enraizamiento profundo.\n" +
                "• **Plateo:** Mantén un radio de 50 cm libre de malezas alrededor del tallo joven durante el primer año.\n" +
                "• **Asociación:** Mezcla árboles maderables con leguminosas arbustivas para enriquecer el nitrógeno del suelo de forma natural."
            }
        }
    }
}
