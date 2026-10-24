package com.example.kotlinapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.kotlinapp.data.model.TreeReport
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object LegacyReports {
    fun encode(report: TreeReport): String =
        JSONObject()
            .put("id", report.id)
            .put("description", report.description)
            .put("condition", report.condition)
            .put("species", report.species)
            .put("latitude", report.latitude)
            .put("longitude", report.longitude)
            .toString()

    fun decode(value: String): TreeReport {
        val row = JSONObject(value)
        // A JSON file cannot grant access to another installation's private image URI.
        val sourceId = row.optString("id", value)
        val id =
            sourceId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
                ?: UUID.nameUUIDFromBytes(sourceId.toByteArray()).toString()
        return TreeReport(
            id = id,
            description = row.optString("description").take(3000),
            condition = row.optString("condition", "Por revisar").take(120),
            species = row.optString("species").take(160),
            latitude = row.optDouble("latitude", 4.745).takeIf { it.isFinite() && it in 4.66..4.86 } ?: 4.745,
            longitude = row.optDouble("longitude", -74.085).takeIf { it.isFinite() && it in -74.18..-74.01 } ?: -74.085,
        )
    }

    fun read(context: Context, uri: Uri): String {
        val output = java.io.ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 1024 * 1024)
                output.write(buffer, 0, count)
            }
        }
        val text = output.toString("UTF-8")
        val rows =
            if (text.trimStart().startsWith("[")) JSONArray(text)
            else
                JSONObject(text).optJSONArray("reports")
                    ?: error("El archivo no contiene reportes.")
        require(rows.length() <= 100)
        val safe = JSONArray()
        for (index in 0 until rows.length()) safe.put(
            JSONObject(encode(decode(rows.getJSONObject(index).toString())))
        )
        return safe.toString()
    }
}
