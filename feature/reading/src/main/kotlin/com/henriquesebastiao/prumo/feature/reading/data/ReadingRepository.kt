package com.henriquesebastiao.prumo.feature.reading.data

import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.requestBody
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookDetail
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.CoverImage
import com.henriquesebastiao.prumo.feature.reading.model.Note
import com.henriquesebastiao.prumo.feature.reading.model.NoteInput
import com.henriquesebastiao.prumo.feature.reading.model.Page
import com.henriquesebastiao.prumo.feature.reading.model.READING_MODULE_ID
import com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Livros on the server. Every write tells [changes] (and the Home, through [DataChanges]), so open
 * screens load again; nothing is computed here (D02).
 */
@Suppress("TooManyFunctions")
interface ReadingRepository {
    /** Emits after each successful write of Livros. */
    val changes: Flow<Unit>

    /** The book last used in a sheet during this run of the app, or null (as Veículos does, D15). */
    var lastBookId: String?

    /** The shelf, in the filter's order (newest first by default, as the web). */
    suspend fun books(filter: ShelfFilter = ShelfFilter(), offset: Int = 0): Outcome<Page<Book>>

    suspend fun book(id: String): Outcome<BookDetail>

    suspend fun createBook(input: BookInput): Outcome<Book>

    suspend fun updateBook(id: String, input: BookInput): Outcome<Book>

    suspend fun deleteBook(id: String): Outcome<Unit>

    /** R2–R4: a new current page; the answer is the book, with its new status and progress. */
    suspend fun addProgress(id: String, page: Int): Outcome<Book>

    suspend fun setCover(id: String, image: CoverImage): Outcome<Book>

    suspend fun deleteCover(id: String): Outcome<Unit>

    /** A book's notes, newest first (R10). */
    suspend fun bookNotes(bookId: String): Outcome<List<Note>>

    /** Every note, newest first, searched in the text (R10). */
    suspend fun notes(query: String? = null, offset: Int = 0): Outcome<Page<Note>>

    suspend fun createNote(bookId: String, input: NoteInput): Outcome<Note>

    suspend fun updateNote(bookId: String, id: String, input: NoteInput): Outcome<Note>

    suspend fun deleteNote(bookId: String, id: String): Outcome<Unit>

    /** Where a book's cover is (authenticated, R6), or null without one; changes with the cover. */
    fun coverUrl(book: Book): String?

    companion object {
        /** A page of a list (the server's default). */
        const val PAGE_SIZE = 50

        /** The shelf comes whole: the server's maximum page. */
        const val ALL = 200
    }
}

@Suppress("TooManyFunctions")
@Singleton
class DefaultReadingRepository @Inject constructor(
    private val calls: ServerCalls,
    private val dataChanges: DataChanges,
) : ReadingRepository {
    override val changes: Flow<Unit> = dataChanges.changes.filter { it == READING_MODULE_ID }.map { }

    @Volatile
    override var lastBookId: String? = null

    private suspend fun <T> api(block: suspend ReadingApi.() -> T): Outcome<T> =
        calls.module(ReadingApi::class.java, block)

    /** A write: tells the screens and the Home when it worked. */
    private suspend fun <T> write(block: suspend ReadingApi.() -> T): Outcome<T> =
        api(block).onOk { dataChanges.changed(READING_MODULE_ID) }

    override suspend fun books(filter: ShelfFilter, offset: Int) = api {
        books(
            status = filter.status?.wire,
            owned = if (filter.owned) true else null,
            query = filter.query.trim().ifEmpty { null },
            sort = filter.order.sort,
            limit = ReadingRepository.ALL,
            offset = offset,
        )
    }.map { page -> page.toPage { it.toModel() } }

    override suspend fun book(id: String) = api { book(id) }.map { it.toModel() }

    override suspend fun createBook(input: BookInput) =
        write { createBook(requestBody(BookCreateDto.serializer(), input.toCreateDto())) }.map { it.toModel() }

    override suspend fun updateBook(id: String, input: BookInput) =
        write { updateBook(id, requestBody(BookUpdateDto.serializer(), input.toUpdateDto())) }.map { it.toModel() }

    override suspend fun deleteBook(id: String) = write { deleteBook(id) }

    override suspend fun addProgress(id: String, page: Int) =
        write { addProgress(id, requestBody(ProgressCreateDto.serializer(), ProgressCreateDto(page))) }
            .map { it.toModel() }

    override suspend fun setCover(id: String, image: CoverImage): Outcome<Book> {
        val part = MultipartBody.Part.createFormData(
            "file",
            "cover",
            image.bytes.toRequestBody(image.mimeType.toMediaType()),
        )
        return write { setCover(id, part) }.map { it.toModel() }
    }

    override suspend fun deleteCover(id: String) = write { deleteCover(id) }

    override suspend fun bookNotes(bookId: String) = api {
        bookNotes(bookId)
    }.map { list -> list.items.map { it.toModel() } }

    override suspend fun notes(query: String?, offset: Int) = api {
        notes(query?.trim()?.ifEmpty { null }, ReadingRepository.PAGE_SIZE, offset)
    }.map { page -> page.toPage { it.toModel() } }

    override suspend fun createNote(bookId: String, input: NoteInput) =
        write { createNote(bookId, requestBody(NoteCreateDto.serializer(), input.toCreateDto())) }.map { it.toModel() }

    override suspend fun updateNote(bookId: String, id: String, input: NoteInput) =
        write { updateNote(bookId, id, requestBody(NoteUpdateDto.serializer(), input.toUpdateDto())) }
            .map { it.toModel() }

    override suspend fun deleteNote(bookId: String, id: String) = write { deleteNote(bookId, id) }

    override fun coverUrl(book: Book): String? {
        if (!book.hasCover) return null
        val base = calls.baseUrl?.trimEnd('/') ?: return null
        val version = book.coverUpdatedAt?.toEpochMilli() ?: 0
        return "$base/api/v1/reading/books/${book.id}/cover?v=$version"
    }
}
