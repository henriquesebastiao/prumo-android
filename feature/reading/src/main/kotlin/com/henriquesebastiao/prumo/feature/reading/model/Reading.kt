package com.henriquesebastiao.prumo.feature.reading.model

import com.henriquesebastiao.prumo.core.model.Wire
import java.time.Instant
import java.time.LocalDate

/** The module's id on the server (`/api/v1/modules`, the Home's summaries). */
const val READING_MODULE_ID = "reading"

// Livros as the API gives it (docs/dominio.md of the server, "Livros"). Every derived number
// (progress, pages left, pace, forecast, pages per week) comes ready from the server (D02).

/** R1: what the user does with a book, independent of owning it. */
enum class BookStatus(override val wire: String) : Wire {
    WantToRead("want_to_read"),
    Reading("reading"),
    Read("read"),
}

/** A book of the shelf (schema `BookRead`). */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val status: BookStatus,
    val owned: Boolean,
    val startedOn: LocalDate?,
    val finishedOn: LocalDate?,
    val hasCover: Boolean,
    /** When the cover last changed: part of its address, so a new cover is fetched again. */
    val coverUpdatedAt: Instant?,
    val createdAt: Instant,
    /** R5: rounded by the server, 100 only when finished. */
    val progressPercent: Int,
    val remainingPages: Int,
    /** R7: pages per day over the last two weeks, or null without progress. */
    val pacePagesPerDay: Double?,
    /** R8: only for a book being read with a pace. */
    val forecastFinishOn: LocalDate?,
    val lastProgressAt: Instant?,
    val notesCount: Int,
)

/** One week of reading (R9): the pages read from its Monday to its Sunday. */
data class WeekPages(val weekStart: LocalDate, val pages: Int)

/** A book with its pages per week (schema `BookDetail`, R9: up to the last 8 weeks). */
data class BookDetail(val book: Book, val weeklyPages: List<WeekPages>)

/** A note on a book (schema `NoteRead`). [book] is the book's title and author. */
data class Note(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val content: String,
    val page: Int?,
    val notedOn: LocalDate,
)

/** What a book form sends. [currentPage] only counts on a new book (R2: progress has its own route). */
data class BookInput(
    val title: String,
    val author: String,
    val totalPages: Int,
    val status: BookStatus,
    val owned: Boolean,
    val currentPage: Int?,
    val startedOn: LocalDate?,
    val finishedOn: LocalDate?,
)

/** What a note form sends; [notedOn] null lets the server use today (R10). */
data class NoteInput(val content: String, val page: Int?, val notedOn: LocalDate? = null)

/**
 * The filters of the shelf (`GET /reading/books`): a status, owning ("Tenho" true, "Não tenho"
 * false, either null), a search on title and author, and the order (the server's `sort`).
 */
data class ShelfFilter(
    val status: BookStatus? = null,
    val owned: Boolean? = null,
    val query: String = "",
    val order: ShelfOrder = ShelfOrder.Newest,
)

/** The orders of the web's shelf: added last first, by title, by author. */
enum class ShelfOrder(val sort: String) {
    Newest("-created_at"),
    Title("title"),
    Author("author"),
}

/** A page of a list (`{items, total, limit, offset}`); [hasMore] while the server has more. */
data class Page<T>(val items: List<T>, val total: Int) {
    val hasMore: Boolean get() = items.size < total

    operator fun plus(next: Page<T>) = Page(items + next.items, next.total)
}

/** A picture for a book's cover, already made small enough to send (R6: up to 5 MB). */
class CoverImage(val bytes: ByteArray, val mimeType: String)
