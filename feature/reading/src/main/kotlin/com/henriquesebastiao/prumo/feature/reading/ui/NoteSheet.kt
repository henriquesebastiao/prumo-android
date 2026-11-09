package com.henriquesebastiao.prumo.feature.reading.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.MultilineField
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.component.rememberOpeningFocus
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.reading.R
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.NoteInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteUiState(
    val request: Long? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    /** The books a new note can go to (the one asked for, or the shelf with those being read first). */
    val books: List<Book> = emptyList(),
    val bookId: String? = null,
    /** The note being edited, or null for a new one. */
    val noteId: String? = null,
    /** The note's day; a new one is "Hoje" (the server dates it in the user's zone, R10). */
    val notedOn: LocalDate? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: ReadingContext = ReadingContext(),
) {
    val book: Book? get() = books.firstOrNull { it.id == bookId }
}

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: ReadingRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    var content by mutableStateOf("")
        private set

    /** The page, as digits; empty for none. */
    var page by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(NoteUiState())
    val state: StateFlow<NoteUiState> = mutableState.asStateFlow()

    private var wanted: String? = null

    fun start(request: Long, bookId: String?, noteId: String?) {
        if (mutableState.value.request == request) return
        content = ""
        page = ""
        wanted = bookId
        mutableState.value = NoteUiState(
            request = request,
            noteId = noteId,
            context = account.preferences.value.readingContext(clock),
        )
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val wanted = wanted
            if (wanted != null) loadBook(wanted) else loadShelf()
        }
    }

    private suspend fun loadBook(bookId: String) {
        when (val outcome = repository.book(bookId)) {
            is Outcome.Ok -> {
                val book = outcome.value.book
                val noteId = mutableState.value.noteId
                if (noteId != null) {
                    val note = (repository.bookNotes(bookId) as? Outcome.Ok)?.value?.firstOrNull { it.id == noteId }
                    if (note != null) {
                        content = note.content
                        page = note.page?.toString().orEmpty()
                        mutableState.update { it.copy(notedOn = note.notedOn) }
                    }
                } else if (book.currentPage > 0) {
                    // Board 07: the page comes suggested with the book's current page.
                    page = book.currentPage.toString()
                }
                mutableState.update { it.copy(loading = false, books = listOf(book), bookId = book.id) }
            }

            is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
        }
    }

    private suspend fun loadShelf() {
        when (val outcome = repository.books()) {
            is Outcome.Ok -> {
                val books = outcome.value.items.sortedBy { if (it.status == BookStatus.Reading) 0 else 1 }
                val chosen = books.firstOrNull { it.id == repository.lastBookId } ?: books.firstOrNull()
                chosen?.takeIf { it.currentPage > 0 }?.let { page = it.currentPage.toString() }
                mutableState.update { it.copy(loading = false, books = books, bookId = chosen?.id) }
            }

            is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
        }
    }

    fun select(bookId: String) {
        val book = mutableState.value.books.firstOrNull { it.id == bookId } ?: return
        page = if (book.currentPage > 0) book.currentPage.toString() else ""
        mutableState.update { it.copy(bookId = bookId, saveError = null) }
    }

    fun onContentChange(text: String) {
        content = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onPageChange(digits: String) {
        page = digits
        mutableState.update { it.copy(saveError = null) }
    }

    /** The web's "Data" of a note (D35 of the server): a past day, or another. */
    fun onDateChange(date: LocalDate) = mutableState.update { it.copy(notedOn = date, saveError = null) }

    fun save() {
        val bookId = mutableState.value.bookId ?: return
        if (content.isBlank()) return
        val input = NoteInput(content.trim(), Inputs.number(page)?.toInt(), mutableState.value.notedOn)
        val noteId = mutableState.value.noteId
        send {
            if (noteId == null) {
                repository.createNote(bookId, input)
            } else {
                repository.updateNote(bookId, noteId, input)
            }
        }
    }

    fun delete() {
        val bookId = mutableState.value.bookId ?: return
        val noteId = mutableState.value.noteId ?: return
        send { repository.deleteNote(bookId, noteId) }
    }

    private fun send(block: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = block()) {
                is Outcome.Ok -> {
                    repository.lastBookId = mutableState.value.bookId
                    mutableState.update { it.copy(saving = false, done = true) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }
}

/** "Nova anotação" (board 07) over the current screen, or the edit of [noteId]. */
@Composable
internal fun NoteSheet(
    bookId: String?,
    noteId: String?,
    onDismiss: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel(),
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, bookId, noteId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        NoteSheetContent(
            state = if (current) state else NoteUiState(),
            content = viewModel.content,
            page = viewModel.page,
            events = NoteEvents(
                onSelect = viewModel::select,
                onContentChange = viewModel::onContentChange,
                onPageChange = viewModel::onPageChange,
                onDateChange = viewModel::onDateChange,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
                onRetry = viewModel::load,
            ),
        )
    }
}

class NoteEvents(
    val onSelect: (String) -> Unit,
    val onContentChange: (String) -> Unit,
    val onPageChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onRetry: () -> Unit,
)

/** The content of the note sheet, without state (previews and tests call it directly). */
@Composable
fun NoteSheetContent(
    state: NoteUiState,
    content: String,
    page: String,
    events: NoteEvents,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .navigationBarsPadding()
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            stringResource(if (state.noteId == null) R.string.reading_new_note else R.string.reading_edit_note),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        val book = state.book
        when {
            state.loading -> LoadingState(stringResource(R.string.reading_loading))

            state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

            book == null -> Text(stringResource(R.string.reading_no_books))

            else -> {
                if (state.books.size > 1) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        state.books.forEach { option ->
                            FilterChip(
                                selected = option.id == book.id,
                                onClick = { events.onSelect(option.id) },
                                label = { Text(option.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
                DateField(
                    value = state.notedOn ?: state.context.today,
                    onValueChange = events.onDateChange,
                    label = stringResource(R.string.reading_note_date),
                    today = state.context.today,
                    asTag = true,
                )
                MultilineField(
                    value = content,
                    onValueChange = events.onContentChange,
                    label = stringResource(R.string.reading_note),
                    error = fieldError(state.saveError, "content"),
                    focusRequester = rememberOpeningFocus(enabled = state.noteId == null),
                )
                // Board 07: the page and "Salvar" side by side, right above the keyboard.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    ValueField(
                        digits = page,
                        onDigitsChange = events.onPageChange,
                        label = stringResource(R.string.reading_note_page),
                        transformation = IntegerTransformation,
                        modifier = Modifier.weight(1f),
                        error = fieldError(state.saveError, "page"),
                        maxDigits = PAGE_DIGITS,
                    )
                    Button(
                        onClick = events.onSave,
                        enabled = !state.saving && content.isNotBlank(),
                        modifier = Modifier.heightIn(min = 56.dp),
                        contentPadding = PaddingValues(horizontal = Spacing.xl),
                    ) { Text(stringResource(R.string.reading_save), style = MaterialTheme.typography.titleMedium) }
                }
                state.saveError?.takeIf { !it.isFieldError() }?.let {
                    Text(readingErrorText(it), color = MaterialTheme.colorScheme.error)
                }
                if (state.noteId != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !state.saving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(painterResource(PrumoIcons.Delete), contentDescription = null)
                        Text(stringResource(R.string.reading_delete_note), Modifier.padding(start = Spacing.sm))
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.reading_delete_note_title),
            body = stringResource(R.string.reading_delete_note_body),
            confirmLabel = stringResource(R.string.reading_delete_note),
            onConfirm = {
                confirmDelete = false
                events.onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

private const val PAGE_DIGITS = 5

@PhonePreviews
@Composable
private fun NoteSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            NoteSheetContent(
                state = NoteUiState(loading = false, books = listOf(PreviewBooks.cleanCode), bookId = "b1"),
                content = "Comparar os exemplos de testes com o que fazemos no projeto do trabalho",
                page = "236",
                events = NoteEvents({}, {}, {}, {}, {}, {}, {}),
            )
        }
    }
}
