package com.example.kotlinapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.kotlinapp.data.model.TreeReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Reportes y fotos privados de esta instalación. No requieren Firebase Storage. */
object ReportRepository {
    private const val PREFS_NAME = "woodmatch_reports_storage"
    private const val KEY_REPORTS = "persisted_reports_json"
    private const val REPORT_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun load(): List<TreeReport> {
        val context = appContext ?: return emptyList()
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REPORTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(saved)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val timestamp = item.optLong("timestamp")
                TreeReport(
                    id = item.getString("id"),
                    userId = item.getString("userId"),
                    userName = item.optString("userName", "Usuario"),
                    description = item.optString("description"),
                    species = item.optString("species"),
                    condition = item.optString("condition"),
                    latitude = item.optDouble("latitude"),
                    longitude = item.optDouble("longitude"),
                    imageUri = item.optString("imageUri").takeIf { it.isNotBlank() },
                    timestamp = timestamp,
                    formattedDate = item.optString("formattedDate", formatDate(timestamp)),
                    expiresAt = item.optLong("expiresAt", timestamp + REPORT_VALIDITY_MS),
                    status = item.optString("status", "PENDIENTE")
                )
            }.sortedByDescending { it.timestamp }
        } catch (error: Exception) {
            android.util.Log.e("ReportRepository", "No se pudieron leer los reportes locales", error)
            emptyList()
        }
    }

    private fun save(reports: List<TreeReport>): Boolean {
        val context = appContext ?: return false
        val array = JSONArray()
        reports.forEach { report ->
            array.put(JSONObject().apply {
                put("id", report.id)
                put("userId", report.userId)
                put("userName", report.userName)
                put("description", report.description)
                put("species", report.species)
                put("condition", report.condition)
                put("latitude", report.latitude)
                put("longitude", report.longitude)
                put("imageUri", report.imageUri ?: "")
                put("timestamp", report.timestamp)
                put("formattedDate", report.formattedDate)
                put("expiresAt", report.expiresAt)
                put("status", report.status)
            })
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_REPORTS, array.toString()).commit()
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun savePhotoLocally(source: String, id: String): String {
        val context = requireNotNull(appContext)
        val directory = File(context.filesDir, "report_photos").apply { mkdirs() }
        val destination = File(directory, "$id.jpg")
        context.contentResolver.openInputStream(Uri.parse(source)).use { input ->
            requireNotNull(input) { "No se pudo abrir la foto seleccionada" }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(destination).toString()
    }

    suspend fun submitReport(
        userId: String,
        userName: String,
        description: String,
        species: String,
        condition: String,
        latitude: Double,
        longitude: Double,
        imageUri: String?
    ): Result<TreeReport> = withContext(Dispatchers.IO) {
        runCatching {
            checkNotNull(appContext) { "El almacenamiento local no está disponible" }
            val id = "rep_${UUID.randomUUID()}"
            val now = System.currentTimeMillis()
            val localPhoto = imageUri?.let { savePhotoLocally(it, id) }
            val report = TreeReport(
                id = id,
                userId = userId,
                userName = userName,
                description = description.trim(),
                species = species,
                condition = condition,
                latitude = latitude,
                longitude = longitude,
                imageUri = localPhoto,
                timestamp = now,
                formattedDate = formatDate(now),
                expiresAt = now + REPORT_VALIDITY_MS,
                status = "PENDIENTE"
            )
            check(save(listOf(report) + load())) { "No se pudo guardar el reporte en el teléfono" }
            report
        }
    }

    suspend fun deleteUserReports(userId: String) = withContext(Dispatchers.IO) {
        val all = load()
        val context = appContext ?: return@withContext
        all.filter { it.userId == userId }.forEach { report ->
            report.imageUri?.let { source ->
                val path = Uri.parse(source).path ?: return@let
                val file = File(path)
                if (file.canonicalPath.startsWith(File(context.filesDir, "report_photos").canonicalPath + File.separator)) file.delete()
            }
        }
        check(save(all.filterNot { it.userId == userId })) { "No se pudieron eliminar los reportes locales." }
    }

    suspend fun getUserReports(userId: String): List<TreeReport> = withContext(Dispatchers.IO) {
        load().filter { it.userId == userId }
    }

    suspend fun getAllReports(): List<TreeReport> = withContext(Dispatchers.IO) { load() }
}
