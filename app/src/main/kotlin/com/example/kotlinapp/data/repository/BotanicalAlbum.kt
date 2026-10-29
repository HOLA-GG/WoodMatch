package com.example.kotlinapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.kotlinapp.data.image.PhotoStorage
import com.example.kotlinapp.data.network.PlantNetCandidate
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

data class BotanicalRecord(val id: String, val photo: String, val scientific: String, val common: String,
    val score: Double, val tags: List<String>, val createdAt: Long, val care: String = "")

/** Account-separated, local-only album. Photos and care are never synchronized remotely. */
object BotanicalAlbum {
    val tags = listOf("Árbol", "Arbusto", "Otras plantas", "Hoja", "Flor", "Fruto", "Corteza")
    private fun folder(context: Context, uid: String): File {
        require(uid.isNotBlank())
        val key = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(context.filesDir, "botanical_album/$key").apply { mkdirs() }
    }
    private fun encode(row: BotanicalRecord) = JSONObject().put("id", row.id).put("scientific", row.scientific)
        .put("common", row.common).put("score", row.score).put("tags", JSONArray(row.tags))
        .put("createdAt", row.createdAt).put("care", row.care).put("photo", row.id + ".jpg")
    private fun decode(value: JSONObject, directory: File): BotanicalRecord {
        val id = value.getString("id"); require(id.matches(Regex("[a-zA-Z0-9_-]{1,80}")))
        val scientific = value.getString("scientific").take(200); require(scientific.isNotBlank())
        val tagValues = value.optJSONArray("tags") ?: JSONArray()
        return BotanicalRecord(id, File(directory, "$id.jpg").absolutePath, scientific,
            value.optString("common", scientific).take(200), value.optDouble("score", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0,
            (0 until tagValues.length()).map { tagValues.optString(it) }.filter { it in tags }.distinct(),
            value.optLong("createdAt"), value.optString("care").take(4000))
    }
    @Synchronized fun read(context: Context, uid: String): List<BotanicalRecord> {
        val directory = folder(context, uid); val file = File(directory, "album.json")
        if (!file.exists()) return emptyList()
        val values = JSONArray(file.readText())
        return (0 until values.length()).mapNotNull { runCatching { decode(values.getJSONObject(it), directory) }.getOrNull() }
            .filter { File(it.photo).exists() }.sortedByDescending { it.createdAt }
    }
    private fun write(directory: File, rows: List<BotanicalRecord>) {
        val temporary = File(directory, "album.tmp")
        temporary.writeText(JSONArray(rows.map(::encode)).toString())
        check(temporary.renameTo(File(directory, "album.json"))) { "No se pudo guardar la colección." }
    }
    @Synchronized fun save(context: Context, uid: String, photo: Uri, candidate: PlantNetCandidate, selectedTags: List<String>, care: String): BotanicalRecord {
        require(AuthRepository.currentUser.value?.uid == uid) { "La sesión cambió. Abre de nuevo tu colección." }
        val directory = folder(context, uid); val rows = read(context, uid)
        require(rows.size < 200) { "La colección admite hasta 200 registros. Exporta un respaldo antes de añadir más." }
        val imported = PhotoStorage.importPhoto(context, photo)
        val id = UUID.randomUUID().toString(); val destination = File(directory, "$id.jpg")
        File(requireNotNull(imported.path)).let { source -> source.copyTo(destination); source.delete() }
        val row = BotanicalRecord(id, destination.path, candidate.scientificName, candidate.primaryCommonName,
            candidate.score, selectedTags.filter { it in tags }.distinct(), System.currentTimeMillis(), care.take(4000))
        try {
            require(AuthRepository.currentUser.value?.uid == uid) { "La sesión cambió. Abre de nuevo tu colección." }
            write(directory, rows + row)
        } catch (error: Exception) { destination.delete(); throw error }
        return row
    }
    @Synchronized fun updateCare(context: Context, uid: String, id: String, care: String) {
        require(AuthRepository.currentUser.value?.uid == uid) { "La sesión cambió. Abre de nuevo tu colección." }
        val directory = folder(context, uid)
        val rows = read(context, uid)
        require(rows.any { it.id == id }) { "El registro ya no está en esta colección." }
        write(directory, rows.map { if (it.id == id) it.copy(care = care.take(4000)) else it })
    }
    @Synchronized fun export(context: Context, uid: String, output: Uri) {
        val rows = read(context, uid)
        ZipOutputStream(requireNotNull(context.contentResolver.openOutputStream(output))).use { zip ->
            zip.putNextEntry(ZipEntry("album.json")); zip.write(JSONArray(rows.map(::encode)).toString().toByteArray()); zip.closeEntry()
            rows.forEach { row -> zip.putNextEntry(ZipEntry(row.id + ".jpg")); File(row.photo).inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
        }
    }
    @Synchronized fun importBackup(context: Context, uid: String, input: Uri): Int {
        require(AuthRepository.currentUser.value?.uid == uid) { "La sesión cambió. Abre de nuevo tu colección." }
        val staging = File(context.cacheDir, "album_import_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var total = 0L; var count = 0
            ZipInputStream(requireNotNull(context.contentResolver.openInputStream(input))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++count <= 201 && !entry.isDirectory && (entry.name == "album.json" || entry.name.matches(Regex("[a-zA-Z0-9_-]{1,80}\\.jpg")))) { "El respaldo contiene archivos no válidos." }
                    val file = File(staging, entry.name); require(!file.exists())
                    file.outputStream().use { out ->
                        val buffer = ByteArray(8192); var size = 0
                        while (true) { val n = zip.read(buffer); if (n < 0) break; size += n; total += n
                            require(size <= 2 * 1024 * 1024 && total <= 210L * 1024 * 1024) { "El respaldo supera el límite permitido." }; out.write(buffer, 0, n) }
                    }; zip.closeEntry()
                }
            }
            val metadata = File(staging, "album.json"); require(metadata.exists()) { "No se encontró el índice de la colección." }
            val values = JSONArray(metadata.readText()); require(values.length() <= 200)
            val directory = folder(context, uid); val existing = read(context, uid)
            val incoming = (0 until values.length()).map { decode(values.getJSONObject(it), staging) }
            require(incoming.map { it.id }.distinct().size == incoming.size)
            val additions = incoming.filter { candidate -> existing.none { it.id == candidate.id } }
            require(existing.size + additions.size <= 200) { "La colección admite hasta 200 registros." }
            additions.forEach { require(File(it.photo).exists()) { "Falta una fotografía del respaldo." } }
            val copied = mutableListOf<File>()
            try {
                val imported = additions.map { row ->
                    val normalized = PhotoStorage.importPhoto(context, Uri.fromFile(File(row.photo)))
                    val destination = File(directory, row.id + ".jpg")
                    File(requireNotNull(normalized.path)).let { it.copyTo(destination, overwrite = true); it.delete() }; copied += destination
                    row.copy(photo = destination.path)
                }
                require(AuthRepository.currentUser.value?.uid == uid) { "La sesión cambió. Abre de nuevo tu colección." }
                write(directory, existing + imported)
            } catch (error: Exception) { copied.forEach { it.delete() }; throw error }
            return additions.size
        } finally { staging.listFiles()?.forEach { it.delete() }; staging.delete() }
    }
}
