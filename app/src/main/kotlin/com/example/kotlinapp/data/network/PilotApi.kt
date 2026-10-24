package com.example.kotlinapp.data.network

import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

object PilotApi {
    val configured: Boolean
        get() = BuildConfig.WOODMATCH_API_URL.isNotBlank()

    fun operationId(): String = UUID.randomUUID().toString()

    suspend fun call(operation: String, data: Map<String, Any?> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            check(configured) {
                "La comunidad todavía no está disponible en esta versión. Puedes explorar las especies."
            }
            check(AuthRepository.isAuthenticated) { "Inicia sesión para continuar." }
            val token =
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                    ?: error("Vuelve a iniciar sesión para continuar.")
            val connection =
                URL(BuildConfig.WOODMATCH_API_URL).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.instanceFollowRedirects = false
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("X-WoodMatch-Version", BuildConfig.VERSION_NAME)
                val body =
                    JSONObject()
                        .put("operation", operation)
                        .put("data", JSONObject(data))
                        .toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val ok = connection.responseCode in 200..299
                val stream = if (ok) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                val parsed = runCatching { JSONObject(response) }.getOrElse { JSONObject() }
                if (!ok) PilotTelemetry.remoteFailure(operation, connection.responseCode)
                check(ok) {
                    parsed.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "No se pudo completar la operación. Comprueba tu conexión."
                }
                parsed.optJSONObject("data") ?: error("No se recibió una respuesta válida.")
            } finally {
                connection.disconnect()
            }
        }
}
