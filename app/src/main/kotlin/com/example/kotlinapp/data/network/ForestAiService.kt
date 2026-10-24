package com.example.kotlinapp.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ForestAiService {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"
    private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Consulta al Consultor Botánico Inteligente sobre especies, cuidados o aprovechamiento.
     * Restringido a máximo 3 puntos clave y enfocado en la localidad de Suba, Bogotá.
     */
    suspend fun askBotanicalConsultant(userPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (com.example.kotlinapp.data.repository.AuthRepository.isAuthenticated) {
                try {
                    val response =
                        PilotApi.call("botanicalAdvice", mapOf("prompt" to userPrompt.take(2000)))
                    val text = response.optString("text")
                    if (!text.isNullOrBlank())
                        return@withContext Result.success(
                            "Sugerencia Gemini · Requiere revisión humana\n\n" +
                                cleanFormatting(text.take(3000))
                        )
                } catch (error: Exception) {
                    val notice =
                        if (error.message?.contains("límite", ignoreCase = true) == true)
                            "Se alcanzó el límite de consultas de hoy."
                        else "El asistente no está disponible en este momento."
                    return@withContext Result.success(
                        notice + "\n\nOrientación local:\n" + getExpertFallbackResponse(userPrompt)
                    )
                }
            }
            Result.success(
                cleanFormatting(
                    "Orientación local · Sin consulta a IA remota\n\n" +
                        getExpertFallbackResponse(userPrompt)
                )
            )
        }

    /**
     * Guía interactiva paso a paso para que usuarios nuevos clasifiquen y reporten residuos en
     * Suba. Retorna una respuesta sintetizada estrictamente en 3 puntos clave.
     */
    suspend fun guideNewUserReport(
        wasteType: String,
        sector: String,
        details: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val prompt =
                "Usuario nuevo en Suba necesita reportar residuo leñoso.\n" +
                    "Tipo/Especie: $wasteType\n" +
                    "Sector de Suba: $sector\n" +
                    "Detalles adicionales: ${if (details.isBlank()) "Sin detalles adicionales" else details}\n" +
                    "Genera una respuesta estructurada estrictamente en 3 puntos numerados:\n" +
                    "1. Diagnóstico del material y estado.\n" +
                    "2. Potencial circular y uso recomendado en Suba.\n" +
                    "3. Pasos exactos para completar este reporte en la app."

            askBotanicalConsultant(prompt)
        }

    /** Genera una guía de siembra y cuidados para una especie específica en máximo 3 puntos. */
    suspend fun getCareGuide(speciesName: String, region: String): Result<String> =
        withContext(Dispatchers.IO) {
            val prompt =
                "Guía botánica para '$speciesName' en $region (Suba, Bogotá). " +
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
        if (query.contains("co2", ignoreCase = true) || query.contains("carbon", ignoreCase = true))
            return "1. El carbono almacenado depende de la especie, tamaño y condiciones del árbol.\n" +
                "2. La app no dispone de mediciones para ordenar las especies de Suba por captura de CO₂.\n" +
                "3. Puedes explorar la guía local; para una orientación específica, vuelve a consultar al asistente cuando esté disponible."
        return "1. Añade una foto clara y elige el punto real del hallazgo o material.\n" +
            "2. Describe su condición y, si lo ofreces, la cantidad aproximada. Una foto no confirma especie, resistencia ni autorización para retirarlo.\n" +
            "3. Revisa la ubicación antes de publicar. No quemes madera pintada o tratada; prioriza su reutilización cuando sea adecuada."
    }
}
