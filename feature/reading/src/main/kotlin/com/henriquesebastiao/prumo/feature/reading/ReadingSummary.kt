package com.henriquesebastiao.prumo.feature.reading

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The module's Home summary (schema ReadingSummary of the server), with the fields the card uses.

@Serializable
data class ReadingSummaryDto(
    val module: String = "reading",
    @SerialName("books_count") val booksCount: Int,
    /** The book read most recently. */
    val current: CurrentBookDto? = null,
)

/** Schema CurrentBook: the progress comes from the API (R5 of the server). */
@Serializable
data class CurrentBookDto(
    val book: BookRefDto,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("progress_percent") val progressPercent: Int,
    @SerialName("has_cover") val hasCover: Boolean = false,
)

/** Schema BookRef. */
@Serializable
data class BookRefDto(val id: String, val title: String, val author: String)
