package com.henriquesebastiao.prumo.feature.reading.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Where Livros opens: the shelf. */
@Serializable
data object ReadingHome : NavKey

@Serializable
data class BookDetailKey(val bookId: String) : NavKey

/** A new book ([bookId] null) or the edit of one. */
@Serializable
data class BookEditorKey(val bookId: String? = null) : NavKey

/** Every note of every book, with a search (the web's "Anotações" tab). */
@Serializable
data object ReadingNotes : NavKey
