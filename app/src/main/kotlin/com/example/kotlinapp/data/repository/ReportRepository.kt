package com.example.kotlinapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.kotlinapp.data.model.TreeReport
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ReportRepository {
    private var prefs: SharedPreferences? = null
    private const val PREFS_NAME = "woodmatch_reports_storage"
    private const val KEY_REPORTS = "persisted_reports_json"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val defaultDemoReports = listOf(
        TreeReport(
            id = "rep_demo_1",
            userId = "admin_101",
            userName = "Guardaparques Central",
            description = "Roble centenario afectado por fuertes vientos cerca de la senda principal.",
            species = "Roble Andino (Quercus humboldtii)",
            condition = "Caído",
            latitude = 4.6097,
            longitude = -74.0817,
            imageUri = null,
            timestamp = System.currentTimeMillis() - 86400000L,
            formattedDate = formatDate(System.currentTimeMillis() - 86400000L)
        ),
        TreeReport(
            id = "rep_demo_2",
            userId = "admin_101",
            userName = "Guardaparques Central",
            description = "Guayacán joven en recuperación con brotes saludables tras reforestación comunitaria.",
            species = "Guayacán Amarillo",
            condition = "Sano",
            latitude = 4.6180,
            longitude = -74.0720,
            imageUri = null,
            timestamp = System.currentTimeMillis() - 172800000L,
            formattedDate = formatDate(System.currentTimeMillis() - 172800000L)
        ),
        TreeReport(
            id = "rep_demo_3",
            userId = "user_202",
            userName = "Carlos Bosques",
            description = "Yarumo con fractura de rama superior sobre el sendero de senderistas.",
            species = "Yarumo Blanco",
            condition = "En riesgo",
            latitude = 4.6250,
            longitude = -74.0910,
            imageUri = null,
            timestamp = System.currentTimeMillis() - 259200000L,
            formattedDate = formatDate(System.currentTimeMillis() - 259200000L)
        )
    )

    private val localReports = mutableListOf<TreeReport>()

    init {
        localReports.addAll(defaultDemoReports)
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadReportsFromStorage()
        }
    }

    private fun loadReportsFromStorage() {
        val storedJson = prefs?.getString(KEY_REPORTS, null) ?: return
        try {
            val jsonArray = JSONArray(storedJson)
            val loaded = mutableListOf<TreeReport>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loaded.add(
                    TreeReport(
                        id = obj.getString("id"),
                        userId = obj.getString("userId"),
                        userName = obj.optString("userName", "Usuario"),
                        description = obj.optString("description", ""),
                        species = obj.optString("species", "Especie nativa"),
                        condition = obj.optString("condition", "Caído"),
                        latitude = obj.optDouble("latitude", 4.6097),
                        longitude = obj.optDouble("longitude", -74.0817),
                        imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        formattedDate = obj.optString("formattedDate", formatDate(System.currentTimeMillis()))
                    )
                )
            }
            if (loaded.isNotEmpty()) {
                val map = (loaded + localReports).associateBy { it.id }.toMutableMap()
                localReports.clear()
                localReports.addAll(map.values.sortedByDescending { it.timestamp })
            }
        } catch (e: Exception) {
            android.util.Log.d("ReportRepository", "Error cargando reportes guardados: ${e.message}")
        }
    }

    private fun saveReportsToStorage() {
        try {
            val jsonArray = JSONArray()
            for (rep in localReports) {
                val obj = JSONObject().apply {
                    put("id", rep.id)
                    put("userId", rep.userId)
                    put("userName", rep.userName)
                    put("description", rep.description)
                    put("species", rep.species)
                    put("condition", rep.condition)
                    put("latitude", rep.latitude)
                    put("longitude", rep.longitude)
                    put("imageUri", rep.imageUri ?: "")
                    put("timestamp", rep.timestamp)
                    put("formattedDate", rep.formattedDate)
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(KEY_REPORTS, jsonArray.toString())?.apply()
        } catch (e: Exception) {
            android.util.Log.d("ReportRepository", "Error persistiendo reportes: ${e.message}")
        }
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
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
        val reportId = "rep_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val formatted = formatDate(now)

        val report = TreeReport(
            id = reportId,
            userId = userId,
            userName = userName,
            description = description,
            species = species,
            condition = condition,
            latitude = latitude,
            longitude = longitude,
            imageUri = imageUri,
            timestamp = now,
            formattedDate = formatted
        )

        // 1. Guardar siempre en memoria y almacenamiento local inmediato
        localReports.add(0, report)
        saveReportsToStorage()

        // 2. Sincronización en la nube (Firestore) si los permisos y red lo permiten
        firestore?.let { db ->
            try {
                val reportData = hashMapOf(
                    "id" to report.id,
                    "userId" to report.userId,
                    "userName" to report.userName,
                    "description" to report.description,
                    "species" to report.species,
                    "condition" to report.condition,
                    "latitude" to report.latitude,
                    "longitude" to report.longitude,
                    "imageUri" to (report.imageUri ?: ""),
                    "timestamp" to report.timestamp,
                    "formattedDate" to report.formattedDate
                )
                db.collection("reports").document(reportId).set(reportData).await()
            } catch (e: Exception) {
                // Si Firestore retorna PERMISSION_DENIED (reglas de seguridad de consola) o falta red,
                // el reporte se conserva 100% íntegro localmente sin fallar la UI del usuario.
                android.util.Log.d("ReportRepository", "Sincronización remota aplazada: ${e.message}")
            }
        }

        Result.success(report)
    }

    suspend fun getUserReports(userId: String): List<TreeReport> = withContext(Dispatchers.IO) {
        firestore?.let { db ->
            try {
                // Consulta sin orderBy en el servidor para no requerir índice compuesto en Firestore
                val snapshot = db.collection("reports")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val remoteReports = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val desc = doc.getString("description") ?: ""
                        val sp = doc.getString("species") ?: "Especie nativa"
                        val cond = doc.getString("condition") ?: "Caído"
                        val lat = doc.getDouble("latitude") ?: 0.0
                        val lng = doc.getDouble("longitude") ?: 0.0
                        val img = doc.getString("imageUri")?.takeIf { it.isNotBlank() }
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val formatted = doc.getString("formattedDate") ?: formatDate(ts)
                        val uName = doc.getString("userName") ?: "Usuario"

                        TreeReport(
                            id = id,
                            userId = userId,
                            userName = uName,
                            description = desc,
                            species = sp,
                            condition = cond,
                            latitude = lat,
                            longitude = lng,
                            imageUri = img,
                            timestamp = ts,
                            formattedDate = formatted
                        )
                    }

                    // Combinar reportes remotos y locales
                    val combined = (remoteReports + localReports.filter { it.userId == userId })
                        .associateBy { it.id }
                        .values
                        .sortedByDescending { it.timestamp }

                    if (combined.isNotEmpty()) {
                        return@withContext combined
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("ReportRepository", "Consulta remota fallback local: ${e.message}")
            }
        }

        // Retornar reportes locales del usuario ordenados por fecha
        val userFiltered = localReports.filter { it.userId == userId }.sortedByDescending { it.timestamp }
        if (userFiltered.isNotEmpty()) userFiltered else localReports.sortedByDescending { it.timestamp }
    }

    suspend fun getAllReports(): List<TreeReport> = withContext(Dispatchers.IO) {
        firestore?.let { db ->
            try {
                val snapshot = db.collection("reports")
                    .limit(50)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val remote = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val uId = doc.getString("userId") ?: ""
                        val uName = doc.getString("userName") ?: "Usuario"
                        val desc = doc.getString("description") ?: ""
                        val sp = doc.getString("species") ?: "Especie nativa"
                        val cond = doc.getString("condition") ?: "Caído"
                        val lat = doc.getDouble("latitude") ?: 0.0
                        val lng = doc.getDouble("longitude") ?: 0.0
                        val img = doc.getString("imageUri")?.takeIf { it.isNotBlank() }
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val formatted = doc.getString("formattedDate") ?: formatDate(ts)

                        TreeReport(
                            id = id,
                            userId = uId,
                            userName = uName,
                            description = desc,
                            species = sp,
                            condition = cond,
                            latitude = lat,
                            longitude = lng,
                            imageUri = img,
                            timestamp = ts,
                            formattedDate = formatted
                        )
                    }

                    val combined = (remote + localReports)
                        .associateBy { it.id }
                        .values
                        .sortedByDescending { it.timestamp }

                    if (combined.isNotEmpty()) return@withContext combined
                }
            } catch (e: Exception) {
                android.util.Log.d("ReportRepository", "Firestore getAll fallback local: ${e.message}")
            }
        }

        localReports.sortedByDescending { it.timestamp }
    }
}
