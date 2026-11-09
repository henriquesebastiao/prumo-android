package com.henriquesebastiao.prumo.feature.reading.data

import com.henriquesebastiao.prumo.feature.reading.BookRefDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The wire format of Livros (openapi/openapi.json of the server). Only the fields the app uses; the
// contract test checks them against the schema (D04).

/** A page of a list. */
@Serializable
data class PageDto<T>(val items: List<T>, val total: Int, val limit: Int, val offset: Int)

/** A small collection, not paginated (`{items}`). */
@Serializable
data class ItemsDto<T>(val items: List<T>)

/** Schema `BookRead`. */
@Serializable
data class BookDto(
    val id: String,
    val title: String,
    val author: String,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("current_page") val currentPage: Int,
    val status: String,
    val owned: Boolean,
    @SerialName("started_on") val startedOn: String?,
    @SerialName("finished_on") val finishedOn: String?,
    @SerialName("has_cover") val hasCover: Boolean,
    @SerialName("cover_updated_at") val coverUpdatedAt: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("progress_percent") val progressPercent: Int,
    @SerialName("remaining_pages") val remainingPages: Int,
    @SerialName("pace_pages_per_day") val pacePagesPerDay: Double?,
    @SerialName("forecast_finish_on") val forecastFinishOn: String?,
    @SerialName("last_progress_at") val lastProgressAt: String?,
    @SerialName("notes_count") val notesCount: Int,
)

/** Schema `BookDetail`: a book with its pages per week (R9). */
@Serializable
data class BookDetailDto(
    val id: String,
    val title: String,
    val author: String,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("current_page") val currentPage: Int,
    val status: String,
    val owned: Boolean,
    @SerialName("started_on") val startedOn: String?,
    @SerialName("finished_on") val finishedOn: String?,
    @SerialName("has_cover") val hasCover: Boolean,
    @SerialName("cover_updated_at") val coverUpdatedAt: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("progress_percent") val progressPercent: Int,
    @SerialName("remaining_pages") val remainingPages: Int,
    @SerialName("pace_pages_per_day") val pacePagesPerDay: Double?,
    @SerialName("forecast_finish_on") val forecastFinishOn: String?,
    @SerialName("last_progress_at") val lastProgressAt: String?,
    @SerialName("notes_count") val notesCount: Int,
    @SerialName("weekly_pages") val weeklyPages: List<WeekPagesDto>,
)

/** Schema `WeekPages`. */
@Serializable
data class WeekPagesDto(@SerialName("week_start") val weekStart: String, val pages: Int)

/** `POST /reading/books` (schema `BookCreate`). */
@Serializable
data class BookCreateDto(
    val title: String,
    val author: String,
    @SerialName("total_pages") val totalPages: Int,
    val status: String,
    val owned: Boolean,
    /** 0 unless the book was started before (R3, R4); the server refuses a null. */
    @SerialName("current_page") val currentPage: Int,
    @SerialName("started_on") val startedOn: String?,
    @SerialName("finished_on") val finishedOn: String?,
)

/** `PATCH /reading/books/{id}` (schema `BookUpdate`): the page moves through progress (R2). */
@Serializable
data class BookUpdateDto(
    val title: String,
    val author: String,
    @SerialName("total_pages") val totalPages: Int,
    val status: String,
    val owned: Boolean,
    @SerialName("started_on") val startedOn: String?,
    @SerialName("finished_on") val finishedOn: String?,
)

/** `POST /reading/books/{id}/progress` (schema `ProgressCreate`). */
@Serializable
data class ProgressCreateDto(val page: Int)

/** Schema `NoteRead`. */
@Serializable
data class NoteDto(
    val id: String,
    val book: BookRefDto,
    val content: String,
    val page: Int?,
    @SerialName("noted_on") val notedOn: String,
)

/** `POST /reading/books/{id}/notes` (schema `NoteCreate`): a null date is today (R10). */
@Serializable
data class NoteCreateDto(val content: String, val page: Int?, @SerialName("noted_on") val notedOn: String?)

/** `PATCH` of a note (schema `NoteUpdate`): without a date it stays out (the server refuses a null one). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NoteUpdateDto(
    val content: String,
    val page: Int?,
    @SerialName("noted_on") @EncodeDefault(EncodeDefault.Mode.NEVER) val notedOn: String? = null,
)
