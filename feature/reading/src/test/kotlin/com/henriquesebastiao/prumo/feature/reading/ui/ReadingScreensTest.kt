package com.henriquesebastiao.prumo.feature.reading.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.Page
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless screens of Livros with the boards' books (PreviewBooks). */
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
@RunWith(AndroidJUnit4::class)
class ReadingScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        PrumoTheme(dynamicColor = false) { ModuleTheme(ReadingFeature.SEED, content) }
    }

    private val noActions = ReadingActions({}, {}, {})

    @Test
    fun theShelfShowsTheBookBeingReadAndEveryCover() {
        var opened = ""
        show {
            ShelfScreen(
                state = ShelfUiState(loading = false, books = PreviewBooks.shelf),
                onBack = {},
                events = ShelfEvents({ opened = it.id }, {}, {}, {}, {}, {}, {}, {}, {}),
                actions = noActions,
            )
        }
        compose.onNodeWithText("Lendo agora").assertIsDisplayed()
        compose.onNodeWithText("pág. 212 de 425").assertIsDisplayed()
        compose.onNodeWithText("3 livros").assertIsDisplayed()
        compose.onNodeWithText("Lido · tenho").assertIsDisplayed()
        compose.onNodeWithContentDescription("Atualizar página").assertIsDisplayed()
        compose.onNodeWithText("O Hobbit").performClick()
        assertEquals(PreviewBooks.hobbit.id, opened)
    }

    @Test
    fun anEmptyShelfInvitesToAddABookWithoutTheToolbar() {
        var added = 0
        show {
            ShelfScreen(
                state = ShelfUiState(loading = false),
                onBack = {},
                events = ShelfEvents({}, { added++ }, {}, {}, {}, {}, {}, {}, {}),
                actions = noActions,
            )
        }
        compose.onNodeWithText("Sua estante está vazia").assertIsDisplayed()
        compose.onNodeWithContentDescription("Atualizar página").assertDoesNotExist()
        compose.onNodeWithContentDescription("Buscar livro").assertDoesNotExist()
        compose.onNodeWithText("Adicionar livro").performClick()
        assertEquals(1, added)
    }

    @Test
    fun theReadShowTheirYearAndNotOwningIsAFilter() {
        var year: ReadYear? = null
        var owned: Boolean? = null
        val read =
            listOf(
                PreviewBooks.domCasmurro,
                PreviewBooks.domCasmurro.copy(
                    id = "b9",
                    title = "Memórias Póstumas",
                    finishedOn = java.time.LocalDate.of(2025, 3, 2),
                ),
            )
        show {
            ShelfScreen(
                state = ShelfUiState(
                    loading = false,
                    books = read,
                    filter = com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter(status = BookStatus.Read),
                ),
                onBack = {},
                events = ShelfEvents({}, {}, {}, {}, { owned = it }, { year = it }, {}, {}, {}),
                actions = noActions,
            )
        }
        compose.onNodeWithText("Dom Casmurro").assertIsDisplayed()
        compose.onNodeWithText("Memórias Póstumas").assertDoesNotExist()
        compose.onNodeWithText("1 livro").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ano dos lidos: 2026").performClick()
        compose.onNodeWithText("2025").performClick()
        assertEquals(ReadYear.Of(2025), year)
        compose.onNodeWithText("Não tenho").performClick()
        assertEquals(false, owned)
    }

    @Test
    fun theBookShowsTheServersProgressAndTheQuickPages() {
        var pages = 0
        show {
            BookDetailScreen(
                state = BookDetailUiState(loading = false, detail = PreviewBooks.detail, notes = PreviewBooks.notes),
                onBack = {},
                events = detailEvents(onAddPages = { pages = it }),
                actions = noActions,
            )
        }
        compose.onNodeWithText("Página 212 de 425").assertIsDisplayed()
        compose.onNodeWithText("50%").assertIsDisplayed()
        compose.onNodeWithText("faltam 213 páginas").assertIsDisplayed()
        compose.onNodeWithContentDescription("Avançar 10 páginas").performClick()
        assertEquals(10, pages)
        compose.onNodeWithText(PreviewBooks.notes.first().content).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("pág. 103 · 28/10/2026").assertIsDisplayed()
    }

    @Test
    fun aBookNotStartedOffersToStartIt() {
        var started = 0
        show {
            BookDetailScreen(
                state = BookDetailUiState(
                    loading = false,
                    detail = PreviewBooks.detail.copy(book = PreviewBooks.hobbit, weeklyPages = emptyList()),
                ),
                onBack = {},
                events = detailEvents(onStartReading = { started++ }),
                actions = noActions,
            )
        }
        compose.onNodeWithContentDescription("Avançar 1 página").assertDoesNotExist()
        compose.onNodeWithText("Começar a ler").performClick()
        assertEquals(1, started)
    }

    @Test
    fun theProgressSheetSavesOnlyAPageThatExists() {
        val state =
            ProgressUiState(loading = false, books = listOf(PreviewBooks.cleanCode), bookId = PreviewBooks.cleanCode.id)
        show { ProgressSheetContent(state, "500", {}, {}, {}, {}, {}) }
        compose.onNodeWithText("Atualizar progresso").assertIsDisplayed()
        compose.onNodeWithText("Código Limpo · estava na pág. 212").assertIsDisplayed()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test
    fun theProgressSheetPreviewsThePagesGained() {
        val state =
            ProgressUiState(loading = false, books = listOf(PreviewBooks.cleanCode), bookId = PreviewBooks.cleanCode.id)
        show { ProgressSheetContent(state, "236", {}, {}, {}, {}, {}) }
        compose.onNodeWithText("+24 páginas").assertIsDisplayed()
        compose.onNodeWithText("Salvar").assertIsEnabled()
    }

    @Test
    fun theNoteSheetOffersTheBooksWhenOpenedFromTheHome() {
        var chosen = ""
        val state = NoteUiState(
            loading = false,
            books = PreviewBooks.shelf.sortedBy { if (it.status == BookStatus.Reading) 0 else 1 },
            bookId = PreviewBooks.cleanCode.id,
        )
        show {
            NoteSheetContent(
                state,
                content = "",
                page = "212",
                events = NoteEvents({ chosen = it }, {}, {}, {}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Nova anotação").assertIsDisplayed()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithText("Dom Casmurro").performClick()
        assertEquals(PreviewBooks.domCasmurro.id, chosen)
    }

    @Test
    fun theFormAsksThePageAndTheStartOfABookBeingRead() {
        show {
            BookEditorScreen(
                state = BookEditorUiState(status = BookStatus.Reading),
                fields = BookEditorFields("Sapiens", "Yuval Noah Harari", "464", "", canSave = true),
                events = BookEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
                onClose = {},
            )
        }
        compose.onNodeWithText("Novo livro").assertIsDisplayed()
        compose.onNodeWithText("Página atual").assertIsDisplayed()
        compose.onNodeWithText("Início").assertIsDisplayed()
        compose.onNodeWithText("Término").assertDoesNotExist()
        compose.onNodeWithText("Salvar livro").performScrollTo().assertIsEnabled()
    }

    @Test
    fun aCoverThatFailedKeepsTheFormWithTheReason() {
        show {
            BookEditorScreen(
                state = BookEditorUiState(
                    editingId = "new",
                    book = PreviewBooks.hobbit,
                    coverError = AppError.Api(413, "reading.cover_too_large"),
                ),
                fields = BookEditorFields("O Hobbit", "J.R.R. Tolkien", "336", "", canSave = true),
                events = BookEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
                onClose = {},
            )
        }
        compose.onNodeWithText("Editar livro").assertIsDisplayed()
        compose.onNodeWithText("O livro foi salvo, mas a capa não", substring = true).assertIsDisplayed()
    }

    @Test
    fun allTheNotesShowTheirBookAndLoadMore() {
        var more = 0
        show {
            NotesScreen(
                state = NotesUiState(loading = false, notes = Page(PreviewBooks.notes, 5)),
                events = NotesEvents({}, {}, { more++ }, {}),
                onBack = {},
            )
        }
        compose.onNodeWithText("Código Limpo · pág. 103 · 28/10/2026").assertIsDisplayed()
        compose.onNodeWithText("Carregar mais").performClick()
        assertEquals(1, more)
    }

    private fun detailEvents(onAddPages: (Int) -> Unit = {}, onStartReading: () -> Unit = {}) = BookDetailEvents(
        onEdit = {},
        onDelete = {},
        onRemoveCover = {},
        onAddPages = onAddPages,
        onStartReading = onStartReading,
        onMarkRead = {},
        onNote = {},
        onRetry = {},
        onActionErrorShown = {},
    )
}
