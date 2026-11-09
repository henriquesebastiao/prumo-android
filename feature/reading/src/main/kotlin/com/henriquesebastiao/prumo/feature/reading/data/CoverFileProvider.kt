package com.henriquesebastiao.prumo.feature.reading.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shares a file in the cache with the camera app, for a book's cover photo. Its own class, so it
 * does not clash with another FileProvider; the paths are in the manifest (`reading_cover_paths`).
 */
class CoverFileProvider : FileProvider() {
    companion object {
        /** A new, empty file for the camera to write the photo to, as a URI it can open. */
        fun newPhoto(context: Context): Uri {
            val dir = File(context.cacheDir, "covers").apply { mkdirs() }
            val file = File(dir, "cover-${System.currentTimeMillis()}.jpg")
            return getUriForFile(context, "${context.packageName}.covers", file)
        }
    }
}
