package com.example.kotlinapp.data.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.kotlinapp.data.image.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ForestAiService {

    /**
     * Consulta al Consultor Botánico Inteligente sobre especies, cuidados o aprovechamiento.
     * Orientación remota enfocada en la localidad de Suba, Bogotá.
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
                            "Orientación botánica · Requiere revisión humana\n\n" +
                                cleanFormatting(text.take(3000))
                        )
                    return@withContext Result.failure(IllegalStateException("El asistente botánico no devolvió una respuesta válida."))
                } catch (error: Exception) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            error.message ?: "No se pudo consultar el asistente botánico. Comprueba la conexión e inténtalo de nuevo.",
                            error,
                        )
                    )
                }
            }
            Result.failure(IllegalStateException("Inicia sesión para consultar el asistente botánico."))
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

    /** General candidate description; never presented as observations of the user's photograph. */
    suspend fun getCareGuide(speciesName: String, region: String): Result<String> =
        withContext(Dispatchers.IO) {
            val prompt =
                "Ficha botánica orientativa del candidato '$speciesName' para una persona en $region. " +
                    "La identificación es incierta y no has recibido la fotografía. No describas su ejemplar concreto. " +
                    "Responde brevemente en español con 4 apartados: " +
                    "1. Apariencia general y rasgos para comparar. " +
                    "2. Colores habituales y variaciones de hojas o flores; no deduzcas el color de la foto. " +
                    "3. Tallo o corteza y rasgos típicos, si se conocen. " +
                    "4. Cuidados generales: luz, suelo y riego, considerando Suba. " +
                    "Si el nombre solo identifica un género o hay variedades, indica esa limitación. " +
                    "Para cualquier dato desconocido escribe 'No disponible'; no inventes cifras de CO2, " +
                    "propiedades medicinales ni afirmes que la planta es comestible o segura. Sin asteriscos."

            askBotanicalConsultant(prompt)
        }

    /**
     * Reads the locally selected image only long enough to request optional,
     * reviewable report suggestions. The image never becomes a client-side log.
     */
    suspend fun suggestReportFromEvidence(
        context: Context,
        uri: Uri,
    ): Result<ReportEvidenceSuggestion> = withContext(Dispatchers.IO) {
        try {
            check(com.example.kotlinapp.data.repository.AuthRepository.isAuthenticated) {
                "Inicia sesión para consultar la evidencia."
            }
            val local = PhotoStorage.importPhoto(context, uri)
            val file = java.io.File(requireNotNull(local.path))
            val bytes = try {
                file.readBytes().also { require(it.size in 4..(1024 * 1024)) }
            } finally {
                file.delete()
            }
            val result = PilotApi.call(
                "analyzeEvidence",
                mapOf("image" to Base64.encodeToString(bytes, Base64.NO_WRAP)),
            )
            Result.success(
                ReportEvidenceSuggestion(
                    title = result.optString("title").take(120),
                    material = result.optString("material"),
                    condition = result.optString("condition").take(120),
                    description = result.optString("description").take(300),
                    quantityValue = if (result.has("quantityValue") && !result.isNull("quantityValue"))
                        result.optDouble("quantityValue") else null,
                    quantityUnit = result.optString("quantityUnit"),
                    confidence = result.optString("confidence", "baja"),
                ),
            )
        } catch (error: Exception) {
            Result.failure(Exception(error.message ?: "No se pudo analizar la evidencia."))
        }
    }

    private fun cleanFormatting(text: String): String {
        return text.replace("**", "").replace("*", "")
    }

}


/** Suggestions are estimates; the author reviews them before any field is changed. */
data class ReportEvidenceSuggestion(
    val title: String,
    val material: String,
    val condition: String,
    val description: String,
    val quantityValue: Double?,
    val quantityUnit: String,
    val confidence: String,
)
