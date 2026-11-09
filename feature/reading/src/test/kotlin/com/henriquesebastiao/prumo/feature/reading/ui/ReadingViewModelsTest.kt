package com.henriquesebastiao.prumo.feature.reading.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.NoteInput
import com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter
import com.henriquesebastiao.prumo.feature.reading.model.ShelfOrder
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReadingViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    // 17/11/2026 at 14:30 in São Paulo, as the design boards.
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)
    private val account = FakeAccountRepository()
    private val repository = FakeReadingRepository()
    private val cleanCode = PreviewBooks.cleanCode
    private val hobbit = PreviewBooks.hobbit

    @Test
    fun theShelfShowsTheBookBeingReadAsTheHero() = runTest {
        val vm = ShelfViewModel(repository)

        assertEquals(3, vm.state.value.books.size)
        assertEquals(cleanCode.id, vm.state.value.readingNow?.id)
        assertFalse(vm.state.value.emptyShelf)
    }

    @Test
    fun theStatusFiltersExcludeEachOtherAndOwningCombines() = runTest {
        val vm = ShelfViewModel(repository)

        vm.selectStatus(BookStatus.Read)
        vm.toggleOwned()
        assertEquals(ShelfFilter(status = BookStatus.Read, owned = true), repository.filters.last())

        vm.selectStatus(BookStatus.WantToRead)
        assertEquals(BookStatus.WantToRead, repository.filters.last().status)

        vm.selectStatus(BookStatus.WantToRead)
        assertNull(repository.filters.last().status)
        assertTrue(repository.filters.last().owned)
    }

    @Test
    fun theSearchWaitsForAPauseInTheTyping() = runTest {
        val vm = ShelfViewModel(repository)
        val asked = repository.filters.size

        vm.search("m")
        vm.search("machado")
        advanceTimeBy(SEARCH_PAUSE)

        assertEquals(asked + 1, repository.filters.size)
        assertEquals(listOf("Dom Casmurro"), vm.state.value.books.map { it.title })
    }

    @Test
    fun theOrderIsTheServersAndKeepsAnEmptyShelfEmpty() = runTest {
        repository.books = emptyList()
        val vm = ShelfViewModel(repository)

        vm.setOrder(ShelfOrder.Author)

        assertEquals(ShelfOrder.Author, repository.filters.last().order)
        assertTrue(vm.state.value.emptyShelf)
    }

    @Test
    fun anEmptyShelfIsOnlyEmptyWithoutFilters() = runTest {
        repository.books = emptyList()
        val vm = ShelfViewModel(repository)
        assertTrue(vm.state.value.emptyShelf)

        vm.toggleOwned()
        assertFalse(vm.state.value.emptyShelf)
    }

    @Test
    fun theQuickPagesNeverGoPastTheLastPage() = runTest {
        repository.books = listOf(cleanCode.copy(currentPage = 420))
        val vm = BookDetailViewModel(cleanCode.id, repository, account, clock)

        vm.addPages(10)

        assertEquals(cleanCode.id to 425, repository.sent.single())
        assertEquals(cleanCode.id, repository.lastBookId)
    }

    @Test
    fun finishingABookWhileOpenCelebratesOnce() = runTest {
        val vm = BookDetailViewModel(cleanCode.id, repository, account, clock)
        assertFalse(vm.state.value.celebrating)

        vm.markRead()

        assertEquals(cleanCode.id to cleanCode.totalPages, repository.sent.single())
        assertEquals(BookStatus.Read, vm.state.value.detail?.book?.status)
        assertTrue(vm.state.value.celebrating)
    }

    @Test
    fun aBookAlreadyReadDoesNotCelebrate() = runTest {
        val vm = BookDetailViewModel(PreviewBooks.domCasmurro.id, repository, account, clock)

        assertFalse(vm.state.value.celebrating)
    }

    @Test
    fun startingABookKeepsWhatItHasAndChangesTheStatus() = runTest {
        val vm = BookDetailViewModel(hobbit.id, repository, account, clock)

        vm.startReading()

        @Suppress("UNCHECKED_CAST")
            val (id, input) = repository.sent.single() as Pair<String, BookInput>
        assertEquals(hobbit.id, id)
        assertEquals(BookStatus.Reading, input.status)
        assertEquals(hobbit.title, input.title)
        assertEquals(hobbit.totalPages, input.totalPages)
    }

    @Test
    fun aRefusedActionIsShownAndDeletingClosesTheBook() = runTest {
        val vm = BookDetailViewModel(cleanCode.id, repository, account, clock)
        repository.failure = AppError.Api(422, "reading.page_out_of_range")

        vm.addPages(1)
        assertEquals("reading.page_out_of_range", (vm.state.value.actionError as AppError.Api).code)

        repository.failure = null
        vm.delete()
        assertTrue(vm.state.value.gone)
    }

    @Test
    fun theProgressSheetStartsWithTheLastBookUsed() = runTest {
        val other = cleanCode.copy(id = "b9", title = "Sapiens", lastProgressAt = Instant.parse("2026-11-17T10:00:00Z"))
        repository.books = listOf(cleanCode, other, hobbit)
        val vm = ProgressViewModel(repository)

        vm.start(request = 1, bookId = null)
        // Without a last book, the one read most recently.
        assertEquals(listOf(cleanCode.id, other.id), vm.state.value.books.map { it.id })
        assertEquals(other.id, vm.state.value.bookId)

        repository.lastBookId = cleanCode.id
        vm.start(request = 2, bookId = null)
        assertEquals(cleanCode.id, vm.state.value.bookId)
    }

    @Test
    fun theProgressSheetSavesThePageOrFinishes() = runTest {
        val vm = ProgressViewModel(repository)
        vm.start(request = 1, bookId = cleanCode.id)

        vm.onPageChange("236")
        vm.save()
        assertTrue(vm.state.value.saved)
        assertEquals(cleanCode.id to 236, repository.sent.single())

        vm.start(request = 2, bookId = cleanCode.id)
        assertEquals("", vm.page)
        vm.finish()
        assertEquals(cleanCode.id to cleanCode.totalPages, repository.sent.last())
    }

    @Test
    fun theProgressSheetKeepsWhatWasTypedOnTheSameOpening() = runTest {
        val vm = ProgressViewModel(repository)
        vm.start(request = 1, bookId = null)
        vm.onPageChange("230")

        vm.start(request = 1, bookId = null)

        assertEquals("230", vm.page)
    }

    @Test
    fun aNewNoteSuggestsTheCurrentPageAndSaves() = runTest {
        val vm = NoteViewModel(repository, account, clock)
        vm.start(request = 1, bookId = cleanCode.id, noteId = null)
        assertEquals("212", vm.page)

        vm.onContentChange("  Comparar os exemplos de testes  ")
        vm.save()

        assertTrue(vm.state.value.done)
        assertEquals(cleanCode.id to NoteInput("Comparar os exemplos de testes", 212), repository.sent.single())
    }

    @Test
    fun aNoteIsEditedAndDeleted() = runTest {
        val vm = NoteViewModel(repository, account, clock)
        vm.start(request = 1, bookId = cleanCode.id, noteId = "n1")
        assertEquals(PreviewBooks.notes.first().content, vm.content)
        assertEquals("103", vm.page)

        vm.onPageChange("")
        vm.onDateChange(LocalDate.of(2026, 10, 29))
        vm.save()
        val edited = NoteInput(PreviewBooks.notes.first().content, null, LocalDate.of(2026, 10, 29))
        assertEquals("n1" to edited, repository.sent.last())

        vm.start(request = 2, bookId = cleanCode.id, noteId = "n1")
        vm.delete()
        assertEquals("delete note n1", repository.sent.last())
    }

    @Test
    fun aNoteFromTheHomePutsTheBooksBeingReadFirst() = runTest {
        val vm = NoteViewModel(repository, account, clock)
        vm.start(request = 1, bookId = null, noteId = null)

        assertEquals(BookStatus.Reading, vm.state.value.books.first().status)
        assertEquals(cleanCode.id, vm.state.value.bookId)

        vm.select(hobbit.id)
        // A book not started has no page to suggest.
        assertEquals("", vm.page)
    }

    @Test
    fun allTheNotesSearchAndLoadMore() = runTest {
        repository.notes = PreviewBooks.notes + PreviewBooks.notes.first().copy(id = "n3", content = "Terceira")
        val vm = NotesViewModel(repository)
        assertEquals(2, vm.state.value.notes.items.size)
        assertTrue(vm.state.value.notes.hasMore)

        vm.loadMore()
        assertEquals(3, vm.state.value.notes.items.size)
        assertFalse(vm.state.value.notes.hasMore)

        vm.search("funções")
        advanceTimeBy(SEARCH_PAUSE)
        assertEquals("funções", repository.noteQueries.last())
        assertEquals(listOf("n2"), vm.state.value.notes.items.map { it.id })
    }

    private companion object {
        const val SEARCH_PAUSE = 301L
    }
}
