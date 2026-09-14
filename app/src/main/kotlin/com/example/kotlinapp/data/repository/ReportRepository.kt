package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.TreeReport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ReportRepository {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val localReports = mutableListOf(
        TreeReport(
            id = "rep_demo_1",
            userId = "user_demo",
            userName = "Guardián del Bosque",
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
            userId = "user_demo",
            userName = "Guardián del Bosque",
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
            userId = "admin_demo",
            userName = "Super Administrador",
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
    ): Result<TreeReport> {
        val reportId = UUID.randomUUID().toString()
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

        // Always store in memory
        localReports.add(0, report)

        // Try Firestore if accessible
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
                // Continue gracefully with local fallback
                android.util.Log.w("ReportRepository", "Firestore submit fallback: ${e.message}")
            }
        }

        return Result.success(report)
    }

    suspend fun getUserReports(userId: String): List<TreeReport> {
        firestore?.let { db ->
            try {
                val snapshot = db.collection("reports")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val remoteReports = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val desc = doc.getString("description") ?: ""
                        val species = doc.getString("species") ?: "Especie nativa"
                        val condition = doc.getString("condition") ?: "Caído"
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
                            species = species,
                            condition = condition,
                            latitude = lat,
                            longitude = lng,
                            imageUri = img,
                            timestamp = ts,
                            formattedDate = formatted
                        )
                    }
                    if (remoteReports.isNotEmpty()) {
                        return remoteReports
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ReportRepository", "Firestore query failed, using local: ${e.message}")
            }
        }

        // Return local reports for user, or all local if demo
        val userFiltered = localReports.filter { it.userId == userId }
        return if (userFiltered.isNotEmpty()) userFiltered else localReports
    }

    suspend fun getAllReports(): List<TreeReport> {
        firestore?.let { db ->
            try {
                val snapshot = db.collection("reports")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val remote = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val uId = doc.getString("userId") ?: ""
                        val uName = doc.getString("userName") ?: "Usuario"
                        val desc = doc.getString("description") ?: ""
                        val species = doc.getString("species") ?: "Especie nativa"
                        val condition = doc.getString("condition") ?: "Caído"
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
                            species = species,
                            condition = condition,
                            latitude = lat,
                            longitude = lng,
                            imageUri = img,
                            timestamp = ts,
                            formattedDate = formatted
                        )
                    }
                    if (remote.isNotEmpty()) return remote
                }
            } catch (e: Exception) {
                android.util.Log.w("ReportRepository", "Firestore getAll fallback: ${e.message}")
            }
        }

        return localReports
    }
}
