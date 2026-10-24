package com.example.kotlinapp.data.network

import android.util.Log
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.SubaSectors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

/**
 * Resultado estructurado y tipado de la validación y extracción por IA
 */
data class ResultadoValidacion(
    val valido: Boolean,
    val punto: GeoPoint? = null,
    val tipo: String? = null,
    val resumen: String? = null,
    val error: String? = null
)

/**
 * Servicio ultra-eficiente de validación y extracción geográfica para Suba, Bogotá.
 * Diseñado con Acondicionamiento Semántico de Entrada y Gemini 2.5 Flash / Flash Lite
 * con System Instructions estrictas y respuesta JSON compacta para mínimo consumo de tokens.
 */
class ValidadorReportesService(
    apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {
    companion object {
        private const val TAG = "ValidadorReportes"
        private const val MODEL_PRIMARY = "gemini-2.5-flash"
        private const val MODEL_FALLBACK = "gemini-2.0-flash"

        private const val SYSTEM_INSTRUCTION_TEXT =
            "Actúa como un extractor geográfico estricto para Suba, Bogotá.\n" +
            "Tu única tarea es recibir reportes de residuos, extraer las coordenadas estimadas del humedal o barrio mencionado de Suba, y clasificar el tipo de basura.\n" +
            "RESPONDE ÚNICAMENTE CON EL OBJETO JSON SOLICITADO. Está prohibido saludar, despedirse o agregar texto extra. Si el reporte no es en Suba o no tiene sentido, devuelve un JSON con \"valido\": false.\n" +
            "Formato JSON requerido:\n" +
            "{\n" +
            "  \"valido\": true,\n" +
            "  \"lat\": 4.7450,\n" +
            "  \"lon\": -74.0850,\n" +
            "  \"tipo\": \"Escombros\",\n" +
            "  \"resumen\": \"Breve descripción\"\n" +
            "}\n" +
            "Los tipos permitidos son estrictamente: \"Escombros\", \"Plásticos\", \"Orgánicos\", \"General\"."
    }

    private val effectiveApiKey: String = apiKeyProvider().trim()

    /**
     * Procesa el reporte del ciudadano gastando el mínimo de tokens.
     * Retorna mediante callback según el contrato solicitado por el usuario.
     */
    suspend fun procesarReporteConIA(
        reporteUsuario: String,
        callback: (GeoPoint?, String?, String?) -> Unit
    ) {
        val resultado = procesarReporte(reporteUsuario)
        if (resultado.valido && resultado.punto != null) {
            callback(resultado.punto, resultado.tipo, resultado.resumen)
        } else {
            callback(null, null, resultado.error ?: "Reporte no válido o fuera de Suba")
        }
    }

    /**
     * Versión suspend para uso idiomático con Coroutines en Jetpack Compose
     */
    suspend fun procesarReporte(reporteUsuario: String): ResultadoValidacion = withContext(Dispatchers.IO) {
        val textoLimpio = reporteUsuario.trim()

        // 1. Filtro local: Si el texto es muy corto (< 8 caracteres), ni siquiera llamamos a la IA (Ahorro total de tokens)
        if (textoLimpio.length < 8) {
            return@withContext ResultadoValidacion(
                valido = false,
                error = "Reporte demasiado corto (mínimo 8 caracteres para ubicar en Suba)"
            )
        }

        // Legacy location suggestions are estimates only. The publication wizard always
        // requires a real GPS fix or a user-selected point and explicit confirmation.
        procesarHeuristicaLocalSuba(textoLimpio)
    }

    private fun parsearRespuestaJson(rawJson: String): ResultadoValidacion {
        return try {
            // Limpieza preventiva si el modelo incluye markdown backticks
            val cleanedJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanedJson)
            val esValido = json.optBoolean("valido", false)

            if (esValido) {
                val lat = json.optDouble("lat", 4.7450)
                val lon = json.optDouble("lon", -74.0850)
                val tipo = json.optString("tipo", "General")
                val resumen = json.optString("resumen", "Residuo detectado en Suba")

                ResultadoValidacion(
                    valido = true,
                    punto = GeoPoint(lat, lon),
                    tipo = tipo,
                    resumen = resumen
                )
            } else {
                ResultadoValidacion(
                    valido = false,
                    error = "La IA determinó que el reporte no es válido o está fuera de la localidad de Suba"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta JSON de IA: $rawJson", e)
            ResultadoValidacion(
                valido = false,
                error = "Respuesta no estructurada: ${e.message}"
            )
        }
    }

    /**
     * Extractor heurístico local de coordenadas para Suba cuando no hay conectividad o API key.
     */
    private fun procesarHeuristicaLocalSuba(texto: String): ResultadoValidacion {
        val lower = texto.lowercase()

        // Determinar tipo de residuo
        val tipo = when {
            lower.contains("escombro") || lower.contains("cemento") || lower.contains("ladrillo") || lower.contains("teja") -> "Escombros"
            lower.contains("plastico") || lower.contains("plástico") || lower.contains("botella") || lower.contains("bolsa") -> "Plásticos"
            lower.contains("tronco") || lower.contains("rama") || lower.contains("arbol") || lower.contains("árbol") || lower.contains("madera") || lower.contains("poda") || lower.contains("organico") || lower.contains("orgánico") -> "Orgánicos"
            else -> "General"
        }

        // Buscar correspondencia geográfica en Suba
        var matchedSector = SubaSectors.ALL.find { sector ->
            lower.contains(sector.name.lowercase()) || lower.contains(sector.description.lowercase())
        }

        val punto = when {
            lower.contains("conejera") -> GeoPoint(4.7560, -74.0920)
            lower.contains("juan amarillo") || lower.contains("tibabuyes") -> GeoPoint(4.7180, -74.1020)
            lower.contains("córdoba") || lower.contains("cordoba") -> GeoPoint(4.7080, -74.0670)
            lower.contains("van der hammen") || lower.contains("hammen") -> GeoPoint(4.7850, -74.0750)
            lower.contains("torca") || lower.contains("guaymaral") -> GeoPoint(4.7950, -74.0380)
            lower.contains("mirador de los nevados") || lower.contains("nevados") -> GeoPoint(4.7480, -74.0815)
            lower.contains("san josé") || lower.contains("san jose") -> GeoPoint(4.7600, -74.0620)
            lower.contains("rincon") || lower.contains("rincón") -> GeoPoint(4.7289, -74.0921)
            lower.contains("tuna") -> GeoPoint(4.7420, -74.0980)
            lower.contains("pinar") -> GeoPoint(4.7520, -74.0900)
            matchedSector != null -> GeoPoint(matchedSector.referenceLatitude, matchedSector.referenceLongitude)
            lower.contains("suba") -> GeoPoint(4.7450, -74.0850)
            else -> null
        }

        return if (punto != null) {
            ResultadoValidacion(
                valido = true,
                punto = punto,
                tipo = tipo,
                resumen = texto.take(100)
            )
        } else {
            ResultadoValidacion(
                valido = false,
                error = "No se identificó un humedal, barrio o punto de referencia en Suba. Por favor menciona el lugar (ej: Humedal La Conejera, Suba Rincón, etc.)."
            )
        }
    }
}
