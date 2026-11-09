package com.henriquesebastiao.prumo.feature.reading.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.component.rememberOpeningFocus
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.reading.R
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    /** Which opening of the sheet this state is for (a new one starts empty). */
    val request: Long? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    /** The books it can update: those being read, or the one the sheet was opened for. */
    val books: List<Book> = emptyList(),
    val bookId: String? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val saved: Boolean = false,
) {
    val book: Book? get() = books.firstOrNull { it.id == bookId }
}

@HiltViewModel
class ProgressViewModel @Inject constructor(private val repository: ReadingRepository) : ViewModel() {
    /** The page typed, as digits. */
    var page by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = mutableState.asStateFlow()

    private var wanted: String? = null

    /** A new opening of the sheet ([request]); the same one again (a rotation) keeps what was typed. */
    fun start(request: Long, bookId: String?) {
        if (mutableState.value.request == request) return
        page = ""
        wanted = bookId
        mutableState.value = ProgressUiState(request = request)
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val wanted = wanted
            val outcome = if (wanted != null) {
                when (val one = repository.book(wanted)) {
                    is Outcome.Ok -> Outcome.Ok(listOf(one.value.book))
                    is Outcome.Failed -> one
                }
            } else {
                when (val reading = repository.books(ShelfFilter(status = BookStatus.Reading))) {
                    is Outcome.Ok -> Outcome.Ok(reading.value.items)
                    is Outcome.Failed -> reading
                }
            }
            when (outcome) {
                is Outcome.Ok -> mutableState.update {
                    val books = outcome.value
                    val chosen = books.firstOrNull { book -> book.id == wanted }
                        ?: books.firstOrNull { book -> book.id == repository.lastBookId }
                        ?: books.maxByOrNull { book -> book.lastProgressAt ?: book.createdAt }
                    it.copy(loading = false, books = books, bookId = chosen?.id)
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun select(bookId: String) {
        page = ""
        mutableState.update { it.copy(bookId = bookId, saveError = null) }
    }

    fun onPageChange(digits: String) {
        page = digits
        mutableState.update { it.copy(saveError = null) }
    }

    fun save() {
        val value = Inputs.number(page)?.toInt() ?: return
        send(value)
    }

    /** "Terminei" (board 06, R4): the last page; the server makes the book read and dates it. */
    fun finish() {
        val book = mutableState.value.book ?: return
        send(book.totalPages)
    }

    private fun send(page: Int) {
        val bookId = mutableState.value.bookId ?: return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = repository.addProgress(bookId, page)) {
                is Outcome.Ok -> {
                    repository.lastBookId = bookId
                    mutableState.update { it.copy(saving = false, saved = true) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }
}

/** "Atualizar progresso" (board 06) over the current screen: the page now, or "Terminei". */
@Composable
internal fun ProgressSheet(bookId: String?, onDismiss: () -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, bookId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.saved) { if (current && state.saved) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ProgressSheetContent(
            state = if (current) state else ProgressUiState(),
            page = viewModel.page,
            onSelect = viewModel::select,
            onPageChange = viewModel::onPageChange,
            onSave = viewModel::save,
            onFinish = viewModel::finish,
            onRetry = viewModel::load,
        )
    }
}

/** The content of the progress sheet, without state (previews and tests call it directly). */
@Composable
fun ProgressSheetContent(
    state: ProgressUiState,
    page: String,
    onSelect: (String) -> Unit,
    onPageChange: (String) -> Unit,
    onSave: () -> Unit,
    onFinish: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            stringResource(R.string.reading_progress_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        val book = state.book
        when {
            state.loading -> LoadingState(stringResource(R.string.reading_loading))
            state.error != null -> ErrorState(state.error, onRetry = onRetry)
            book == null -> Text(stringResource(R.string.reading_no_books_reading))
            else -> ProgressForm(state, book, page, onSelect, onPageChange, onSave, onFinish)
        }
    }
}

@Composable
private fun ProgressForm(
    state: ProgressUiState,
    book: Book,
    page: String,
    onSelect: (String) -> Unit,
    onPageChange: (String) -> Unit,
    onSave: () -> Unit,
    onFinish: () -> Unit,
) {
    if (state.books.size > 1) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            state.books.forEach { option ->
                FilterChip(
                    selected = option.id == book.id,
                    onClick = { onSelect(option.id) },
                    label = { Text(option.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
    Tag(
        stringResource(R.string.reading_was_on_page, book.title, book.currentPage),
        icon = PrumoIcons.History,
        small = true,
    )
    val typed = Inputs.number(page)?.toInt()
    ValueField(
        digits = page,
        onDigitsChange = onPageChange,
        label = stringResource(R.string.reading_current_page),
        transformation = IntegerTransformation,
        large = true,
        suffix = stringResource(R.string.reading_of_total, book.totalPages),
        error = fieldError(state.saveError, "page")
            ?: if (typed != null &&
                typed > book.totalPages
            ) {
                stringResource(R.string.reading_page_over, book.totalPages)
            } else {
                null
            },
        focusRequester = rememberOpeningFocus(),
        maxDigits = PAGE_DIGITS,
    )
    // D19: the wave follows what is typed and the pages gained show as a preview; the percent and
    // the book's status are the server's (R2–R5) once saved.
    val shown = (typed ?: book.currentPage).coerceIn(0, book.totalPages)
    LinearWavyProgressIndicator(progress = { shown / book.totalPages.toFloat() }, modifier = Modifier.fillMaxWidth())
    if (typed != null && typed > book.currentPage && typed <= book.totalPages) {
        val gained = typed - book.currentPage
        Text(
            pluralStringResource(R.plurals.reading_pages_today, gained, gained),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.saveError?.takeIf { !it.isFieldError() }?.let {
        Text(readingErrorText(it), color = MaterialTheme.colorScheme.error)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onFinish, enabled = !state.saving && book.currentPage < book.totalPages) {
            Icon(painterResource(PrumoIcons.DoneAll), contentDescription = null)
            Text(stringResource(R.string.reading_finished_button), Modifier.padding(start = Spacing.sm))
        }
        Spacer(Modifier.weight(1f))
        // Board 06: "Terminei" at the start, "Salvar" a large filled button at the end.
        Button(
            onClick = onSave,
            enabled = !state.saving && typed != null && typed <= book.totalPages,
            modifier = Modifier.heightIn(min = 56.dp),
            contentPadding = PaddingValues(horizontal = Spacing.xl),
        ) { Text(stringResource(R.string.reading_save), style = MaterialTheme.typography.titleMedium) }
    }
}

private const val PAGE_DIGITS = 5

@PhonePreviews
@Composable
private fun ProgressSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            ProgressSheetContent(
                state = ProgressUiState(loading = false, books = listOf(PreviewBooks.cleanCode), bookId = "b1"),
                page = "236",
                onSelect = {},
                onPageChange = {},
                onSave = {},
                onFinish = {},
                onRetry = {},
            )
        }
    }
}
