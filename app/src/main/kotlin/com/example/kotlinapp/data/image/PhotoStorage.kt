package com.example.kotlinapp.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Bounded decoding, orientation correction, and EXIF removal. Call on Dispatchers.IO. */
object PhotoStorage {
    fun importPhoto(context: Context, uri: Uri): Uri {
        val input = File.createTempFile("image_", ".tmp", context.cacheDir)
        var target: File? = null
        try {
            context.contentResolver.openInputStream(uri).use { source ->
                requireNotNull(source)
                input.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 15 * 1024 * 1024)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(input.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            val original =
                requireNotNull(
                    BitmapFactory.decodeFile(
                        input.path,
                        BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.RGB_565
                        },
                    )
                )
            val orientation =
                ExifInterface(input)
                    .getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
            val matrix =
                Matrix().apply {
                    when (orientation) {
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                        ExifInterface.ORIENTATION_TRANSPOSE -> {
                            setRotate(90f)
                            postScale(-1f, 1f)
                        }
                        ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                        ExifInterface.ORIENTATION_TRANSVERSE -> {
                            setRotate(270f)
                            postScale(-1f, 1f)
                        }
                        ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                    }
                }
            val bitmap =
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            if (bitmap !== original) original.recycle()
            val directory = File(context.filesDir, "publication_photos").apply { mkdirs() }
            target = File(directory, "${UUID.randomUUID()}.jpg")
            try {
                var quality = 85
                do {
                    target.outputStream().use {
                        require(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it))
                    }
                    quality -= 10
                } while (target.length() > 1024 * 1024 && quality >= 35)
                require(target.length() <= 1024 * 1024)
            } finally {
                bitmap.recycle()
            }
            return Uri.fromFile(target)
        } catch (e: Exception) {
            target?.delete()
            throw e
        } finally {
            input.delete()
        }
    }
}
