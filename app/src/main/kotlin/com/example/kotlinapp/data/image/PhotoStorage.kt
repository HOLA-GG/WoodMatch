package com.example.kotlinapp.data.image
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
object PhotoStorage {
fun importPhoto(context: Context, uri: Uri): Uri {
    val inputFile = File.createTempFile("image_", ".tmp", context.cacheDir)
    try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            inputFile.outputStream().use { output ->
                val buffer = ByteArray(8192); var total = 0
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= 15 * 1024 * 1024)
                    output.write(buffer, 0, count)
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(inputFile.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeFile(inputFile.path, BitmapFactory.Options().apply { inSampleSize = sample }))
        val directory = File(context.filesDir, "publication_photos").apply { mkdirs() }
        val target = File(directory, "${UUID.randomUUID()}.jpg")
        try { target.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) } }
        finally { bitmap.recycle() }
        // Re-encoding removes embedded location/EXIF metadata.
        return Uri.fromFile(target)
    } finally { inputFile.delete() }
}

}
