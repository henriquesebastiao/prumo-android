package com.henriquesebastiao.prumo.feature.reading.ui

import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookDetail
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.Note
import com.henriquesebastiao.prumo.feature.reading.model.WeekPages
import java.time.Instant
import java.time.LocalDate

/** The books of the boards (Livros 01 to 07), for previews and tests. */
internal object PreviewBooks {
    val cleanCode = Book(
        id = "b1",
        title = "Código Limpo",
        author = "Robert C. Martin",
        totalPages = 425,
        currentPage = 212,
        status = BookStatus.Reading,
        owned = true,
        startedOn = LocalDate.of(2026, 10, 14),
        finishedOn = null,
        hasCover = false,
        coverUpdatedAt = null,
        createdAt = Instant.parse("2026-10-14T12:00:00Z"),
        progressPercent = 50,
        remainingPages = 213,
        pacePagesPerDay = 12.4,
        forecastFinishOn = LocalDate.of(2026, 12, 4),
        lastProgressAt = Instant.parse("2026-11-16T22:00:00Z"),
        notesCount = 2,
    )

    val domCasmurro = cleanCode.copy(
        id = "b2",
        title = "Dom Casmurro",
        author = "Machado de Assis",
        totalPages = 256,
        currentPage = 256,
        status = BookStatus.Read,
        startedOn = LocalDate.of(2026, 8, 2),
        finishedOn = LocalDate.of(2026, 9, 1),
        progressPercent = 100,
        remainingPages = 0,
        pacePagesPerDay = null,
        forecastFinishOn = null,
        notesCount = 0,
    )

    val hobbit = cleanCode.copy(
        id = "b3",
        title = "O Hobbit",
        author = "J.R.R. Tolkien",
        totalPages = 336,
        currentPage = 0,
        status = BookStatus.WantToRead,
        owned = false,
        startedOn = null,
        progressPercent = 0,
        remainingPages = 336,
        pacePagesPerDay = null,
        forecastFinishOn = null,
        lastProgressAt = null,
        notesCount = 0,
    )

    val shelf = listOf(cleanCode, domCasmurro, hobbit)

    val notes = listOf(
        Note(
            "n1",
            "b1",
            "Código Limpo",
            "Robert C. Martin",
            "Reler o capítulo de tratamento de erros antes do projeto novo.",
            103,
            LocalDate.of(2026, 10, 28),
        ),
        Note(
            "n2",
            "b1",
            "Código Limpo",
            "Robert C. Martin",
            "Aplicar a regra de funções pequenas no módulo de faturas.",
            35,
            LocalDate.of(2026, 10, 18),
        ),
    )

    val detail = BookDetail(
        cleanCode,
        listOf(
            WeekPages(LocalDate.of(2026, 10, 12), 40),
            WeekPages(LocalDate.of(2026, 10, 19), 62),
            WeekPages(LocalDate.of(2026, 10, 26), 55),
            WeekPages(LocalDate.of(2026, 11, 2), 31),
            WeekPages(LocalDate.of(2026, 11, 9), 24),
        ),
    )
}
