package com.henriquesebastiao.prumo.feature.reading.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.henriquesebastiao.prumo.feature.reading.model.CoverImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads a picture picked from the gallery or taken with the camera for a cover. The server takes
 * JPEG, PNG or WebP up to 5 MB and does the rest (R6: EXIF orientation, 800 px, WebP); a picture
 * that is too big or of another type (a camera photo, a HEIF) is turned upright, made smaller and
 * sent as JPEG. Nothing about the book is decided here.
 */
@Singleton
class CoverImages @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun read(uri: Uri): CoverImage? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val type = resolver.getType(uri)
        val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            ?: return@withContext null
        if (bytes.size <= MAX_BYTES && type in ACCEPTED) return@withContext CoverImage(bytes, type!!)
        shrink(bytes)?.let { CoverImage(it, JPEG) }
    }

    private fun shrink(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
        val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            postRotate(rotation(bytes).toFloat())
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        return ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
    }

    private fun rotation(bytes: ByteArray): Int = when (
        runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull()?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> DEGREES_90
        ExifInterface.ORIENTATION_ROTATE_180 -> DEGREES_180
        ExifInterface.ORIENTATION_ROTATE_270 -> DEGREES_270
        else -> 0
    }

    private companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
        const val MAX_SIDE = 1600
        const val QUALITY = 85
        const val JPEG = "image/jpeg"
        const val DEGREES_90 = 90
        const val DEGREES_180 = 180
        const val DEGREES_270 = 270
        val ACCEPTED = setOf(JPEG, "image/png", "image/webp")
    }
}
