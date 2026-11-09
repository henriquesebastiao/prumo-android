package com.henriquesebastiao.prumo.feature.reading.data

import com.henriquesebastiao.prumo.core.model.wireOf
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookDetail
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.Note
import com.henriquesebastiao.prumo.feature.reading.model.NoteInput
import com.henriquesebastiao.prumo.feature.reading.model.Page
import com.henriquesebastiao.prumo.feature.reading.model.WeekPages
import java.time.Instant
import java.time.LocalDate

internal fun BookDto.toModel() = Book(
    id = id,
    title = title,
    author = author,
    totalPages = totalPages,
    currentPage = currentPage,
    status = wireOf(status, BookStatus.WantToRead),
    owned = owned,
    startedOn = startedOn?.let(LocalDate::parse),
    finishedOn = finishedOn?.let(LocalDate::parse),
    hasCover = hasCover,
    coverUpdatedAt = coverUpdatedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
    progressPercent = progressPercent,
    remainingPages = remainingPages,
    pacePagesPerDay = pacePagesPerDay,
    forecastFinishOn = forecastFinishOn?.let(LocalDate::parse),
    lastProgressAt = lastProgressAt?.let(Instant::parse),
    notesCount = notesCount,
)

internal fun BookDetailDto.toModel() = BookDetail(
    book = BookDto(
        id = id,
        title = title,
        author = author,
        totalPages = totalPages,
        currentPage = currentPage,
        status = status,
        owned = owned,
        startedOn = startedOn,
        finishedOn = finishedOn,
        hasCover = hasCover,
        coverUpdatedAt = coverUpdatedAt,
        createdAt = createdAt,
        progressPercent = progressPercent,
        remainingPages = remainingPages,
        pacePagesPerDay = pacePagesPerDay,
        forecastFinishOn = forecastFinishOn,
        lastProgressAt = lastProgressAt,
        notesCount = notesCount,
    ).toModel(),
    weeklyPages = weeklyPages.map { WeekPages(LocalDate.parse(it.weekStart), it.pages) },
)

internal fun NoteDto.toModel() = Note(
    id = id,
    bookId = book.id,
    bookTitle = book.title,
    bookAuthor = book.author,
    content = content,
    page = page,
    notedOn = LocalDate.parse(notedOn),
)

internal fun <D, M> PageDto<D>.toPage(map: (D) -> M) = Page(items.map(map), total)

internal fun BookInput.toCreateDto() = BookCreateDto(
    title = title,
    author = author,
    totalPages = totalPages,
    status = status.wire,
    owned = owned,
    currentPage = currentPage ?: 0,
    startedOn = startedOn?.toString(),
    finishedOn = finishedOn?.toString(),
)

internal fun BookInput.toUpdateDto() = BookUpdateDto(
    title = title,
    author = author,
    totalPages = totalPages,
    status = status.wire,
    owned = owned,
    startedOn = startedOn?.toString(),
    finishedOn = finishedOn?.toString(),
)

internal fun NoteInput.toCreateDto() = NoteCreateDto(content = content, page = page, notedOn = notedOn?.toString())

internal fun NoteInput.toUpdateDto() = NoteUpdateDto(content = content, page = page, notedOn = notedOn?.toString())
