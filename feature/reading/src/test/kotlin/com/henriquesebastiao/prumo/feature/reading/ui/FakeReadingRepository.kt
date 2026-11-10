package com.henriquesebastiao.prumo.feature.reading.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookDetail
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.CoverImage
import com.henriquesebastiao.prumo.feature.reading.model.Note
import com.henriquesebastiao.prumo.feature.reading.model.NoteInput
import com.henriquesebastiao.prumo.feature.reading.model.Page
import com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Livros in memory, with the boards' books. The server's answers are imitated only as far as the
 * tests need (a page at the end makes the book read, R4). [failure] makes the next writes fail,
 * [coverFailure] only the cover's; what the view models sent is kept in [sent].
 */
@Suppress("TooManyFunctions")
class FakeReadingRepository(var books: List<Book> = PreviewBooks.shelf, var notes: List<Note> = PreviewBooks.notes) :
    ReadingRepository {
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val changes = mutableChanges

    override var lastBookId: String? = null

    var failure: AppError? = null
    var coverFailure: AppError? = null
    var readFailure: AppError? = null

    val sent = mutableListOf<Any>()
    val filters = mutableListOf<ShelfFilter>()
    val noteQueries = mutableListOf<String?>()

    private fun <T> read(value: () -> T): Outcome<T> = readFailure?.let { Outcome.Failed(it) } ?: Outcome.Ok(value())

    private fun <T> write(body: Any, value: () -> T): Outcome<T> {
        failure?.let { return Outcome.Failed(it) }
        sent += body
        val result = value()
        mutableChanges.tryEmit(Unit)
        return Outcome.Ok(result)
    }

    override suspend fun books(filter: ShelfFilter, offset: Int): Outcome<Page<Book>> = read {
        filters += filter
        val found = books.filter { book ->
            (filter.status == null || book.status == filter.status) &&
                (filter.owned == null || book.owned == filter.owned) &&
                (
                    filter.query.isBlank() ||
                        "${book.title} ${book.author}".contains(filter.query.trim(), ignoreCase = true)
                    )
        }
        Page(found, found.size)
    }

    override suspend fun book(id: String): Outcome<BookDetail> = read {
        BookDetail(books.first { it.id == id }, PreviewBooks.detail.weeklyPages)
    }

    override suspend fun createBook(input: BookInput): Outcome<Book> = write(input) {
        PreviewBooks.hobbit.copy(id = "new", title = input.title, author = input.author).also { books = books + it }
    }

    override suspend fun updateBook(id: String, input: BookInput): Outcome<Book> = write(id to input) {
        val book = books.first { it.id == id }.copy(title = input.title, author = input.author, status = input.status)
        books = books.map { if (it.id == id) book else it }
        book
    }

    override suspend fun deleteBook(id: String): Outcome<Unit> = write("delete $id") {
        books = books.filterNot { it.id == id }
    }

    override suspend fun addProgress(id: String, page: Int): Outcome<Book> = write(id to page) {
        val book = books.first { it.id == id }
        val finished = page == book.totalPages
        val updated = book.copy(
            currentPage = page,
            status = if (finished) BookStatus.Read else BookStatus.Reading,
            finishedOn = if (finished) LocalDate.of(2026, 11, 17) else book.finishedOn,
            progressPercent = page * 100 / book.totalPages,
            remainingPages = book.totalPages - page,
        )
        books = books.map { if (it.id == id) updated else it }
        updated
    }

    override suspend fun setCover(id: String, image: CoverImage): Outcome<Book> {
        coverFailure?.let { return Outcome.Failed(it) }
        return write("cover $id") { books.first { it.id == id }.copy(hasCover = true) }
    }

    override suspend fun deleteCover(id: String): Outcome<Unit> = write("uncover $id") {}

    override suspend fun bookNotes(bookId: String): Outcome<List<Note>> = read { notes.filter { it.bookId == bookId } }

    override suspend fun notes(query: String?, offset: Int): Outcome<Page<Note>> = read {
        noteQueries += query
        val found = notes.filter { query == null || it.content.contains(query, ignoreCase = true) }
        Page(found.drop(offset).take(PAGE), found.size)
    }

    override suspend fun createNote(bookId: String, input: NoteInput): Outcome<Note> = write(bookId to input) {
        Note("n-new", bookId, "", "", input.content, input.page, LocalDate.of(2026, 11, 17))
    }

    override suspend fun updateNote(bookId: String, id: String, input: NoteInput): Outcome<Note> =
        write(id to input) { notes.first { it.id == id }.copy(content = input.content, page = input.page) }

    override suspend fun deleteNote(bookId: String, id: String): Outcome<Unit> = write("delete note $id") {
        notes = notes.filterNot { it.id == id }
    }

    override fun coverUrl(book: Book): String? = null

    companion object {
        /** A small page, so "Carregar mais" can be tested. */
        const val PAGE = 2
    }
}
