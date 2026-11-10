package com.henriquesebastiao.prumo.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** How an export of the account's data ended. */
sealed interface ExportResult {
    data object Saved : ExportResult

    /** The server didn't send the archive. */
    data class Failed(val error: AppError) : ExportResult

    /** The archive came, but the file the user chose couldn't be written. */
    data object NotWritten : ExportResult
}

/** The account's data as a file (`GET /me/export`, D38 of the server), for "Exportar dados". */
interface DataExportRepository {
    /**
     * Downloads the archive and writes it into [target], a document the user just created
     * (`ACTION_CREATE_DOCUMENT`). When it fails, the document is removed, so no empty file is left.
     */
    suspend fun export(target: Uri): ExportResult
}

class DefaultDataExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calls: ServerCalls,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DataExportRepository {
    override suspend fun export(target: Uri): ExportResult = withContext(io) {
        // The download goes to the cache first: a failure there is the server's, one after it the file's.
        val temp = File.createTempFile("export", ".zip", context.cacheDir)
        try {
            val download = calls.system {
                export().use { body -> temp.outputStream().use { body.byteStream().copyTo(it) } }
            }
            val result = when (download) {
                is Outcome.Failed -> ExportResult.Failed(download.error)
                is Outcome.Ok -> if (copy(temp, target)) ExportResult.Saved else ExportResult.NotWritten
            }
            if (result != ExportResult.Saved) discard(target)
            result
        } finally {
            temp.delete()
        }
    }

    private fun copy(from: File, to: Uri): Boolean = try {
        val out = context.contentResolver.openOutputStream(to, "wt") ?: return false
        out.use { sink -> from.inputStream().use { it.copyTo(sink) } }
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun discard(document: Uri) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, document)
        } catch (_: IOException) {
            // Nothing else to do: the provider keeps the empty file.
        } catch (_: SecurityException) {
        } catch (_: UnsupportedOperationException) {
        } catch (_: IllegalArgumentException) {
        }
    }
}
