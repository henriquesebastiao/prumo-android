package com.henriquesebastiao.prumo.feature.reading.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedList
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.reading.R
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookDetail
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.Note
import com.henriquesebastiao.prumo.feature.reading.model.WeekPages
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val loading: Boolean = true,
    /** A pull to refresh, until the book comes back. */
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val detail: BookDetail? = null,
    val notes: List<Note> = emptyList(),
    /** A write is on its way (the +1 buttons wait for it). */
    val busy: Boolean = false,
    val actionError: AppError? = null,
    /** The book became read while open: the hero celebrates once (the design's expressive confirmation). */
    val celebrating: Boolean = false,
    /** The book was deleted: the screen goes back. */
    val gone: Boolean = false,
    val context: ReadingContext = ReadingContext(),
)

@HiltViewModel(assistedFactory = BookDetailViewModel.Factory::class)
class BookDetailViewModel @AssistedInject constructor(
    @Assisted private val bookId: String,
    private val repository: ReadingRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(BookDetailUiState(context = account.preferences.value.readingContext(clock)))
    val state: StateFlow<BookDetailUiState> = mutableState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) refresh() } }
    }

    /** Pull to refresh: asks the server first, not the device's copy (D21). */
    fun pull() {
        mutableState.update { it.copy(refreshing = true) }
        repository.expireCache()
        refresh()
    }

    fun refresh() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            when (val outcome = repository.book(bookId)) {
                is Outcome.Ok -> mutableState.update {
                    val before = it.detail?.book?.status
                    val finished =
                        before != null && before != BookStatus.Read && outcome.value.book.status == BookStatus.Read
                    it.copy(
                        loading = false,
                        refreshing = false,
                        detail = outcome.value,
                        celebrating = it.celebrating || finished,
                    )
                }

                is Outcome.Failed -> mutableState.update {
                    it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
        viewModelScope.launch {
            val outcome = repository.bookNotes(bookId)
            if (outcome is Outcome.Ok) mutableState.update { it.copy(notes = outcome.value) }
        }
    }

    /** +1, +10, +25 (board 05): the page moves on, never past the last one (the server's limit, R4). */
    fun addPages(pages: Int) {
        val book = mutableState.value.detail?.book ?: return
        progress(min(book.currentPage + pages, book.totalPages))
    }

    /** "Marcar como lido" (R4): the page goes to the last one, and the server makes the book read. */
    fun markRead() {
        val book = mutableState.value.detail?.book ?: return
        progress(book.totalPages)
    }

    /** "Começar a ler" (R3): the status becomes reading even on page 0. */
    fun startReading() {
        val book = mutableState.value.detail?.book ?: return
        act { repository.updateBook(book.id, book.toInput().copy(status = BookStatus.Reading)) }
    }

    fun delete() = act(onDone = { mutableState.update { it.copy(gone = true) } }) { repository.deleteBook(bookId) }

    fun removeCover() = act { repository.deleteCover(bookId) }

    fun onActionErrorShown() = mutableState.update { it.copy(actionError = null) }

    private fun progress(page: Int) {
        repository.lastBookId = bookId
        act { repository.addProgress(bookId, page) }
    }

    private fun <T> act(onDone: () -> Unit = {}, block: suspend () -> Outcome<T>) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            when (val outcome = block()) {
                is Outcome.Ok -> {
                    mutableState.update { it.copy(busy = false) }
                    onDone()
                }

                is Outcome.Failed -> mutableState.update { it.copy(busy = false, actionError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(bookId: String): BookDetailViewModel
    }
}

/** The book as the form sends it back (a PATCH changes the fields it carries). */
internal fun Book.toInput() = BookInput(
    title = title,
    author = author,
    totalPages = totalPages,
    status = status,
    owned = owned,
    currentPage = null,
    startedOn = startedOn,
    finishedOn = finishedOn,
)

internal class BookDetailEvents(
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onRemoveCover: () -> Unit,
    val onAddPages: (Int) -> Unit,
    val onStartReading: () -> Unit,
    val onMarkRead: () -> Unit,
    val onNote: (Note) -> Unit,
    val onRetry: () -> Unit,
    val onActionErrorShown: () -> Unit,
    val onRefresh: () -> Unit = {},
)

@Composable
internal fun BookDetailRoute(
    bookId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: BookDetailViewModel = hiltViewModel<BookDetailViewModel, BookDetailViewModel.Factory>(
        creationCallback = { it.create(bookId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    BookDetailScreen(
        state = state,
        onBack = navigator::back,
        events = BookDetailEvents(
            onEdit = { navigator.navigate(BookEditorKey(bookId)) },
            onDelete = viewModel::delete,
            onRemoveCover = viewModel::removeCover,
            onAddPages = viewModel::addPages,
            onStartReading = viewModel::startReading,
            onMarkRead = viewModel::markRead,
            onNote = { sheet = SheetRequest(SheetKind.Note, bookId, it.id) },
            onRetry = viewModel::refresh,
            onRefresh = viewModel::pull,
            onActionErrorShown = viewModel::onActionErrorShown,
        ),
        actions = ReadingActions(
            onProgress = { sheet = SheetRequest(SheetKind.Progress, bookId) },
            onNote = { sheet = SheetRequest(SheetKind.Note, bookId) },
        ),
        modifier = modifier,
    )
    ReadingSheet(sheet, onDismiss = { sheet = null })
}

/**
 * A book (board 05): its cover and tags, the progress as the hero with quick +1/+10/+25, the dates,
 * the pace and pages per week (R7–R9, from the server) and the notes; the toolbar records.
 */
@Composable
internal fun BookDetailScreen(
    state: BookDetailUiState,
    onBack: () -> Unit,
    events: BookDetailEvents,
    actions: ReadingActions,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val actionError = state.actionError?.let { readingErrorText(it) }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbar.showSnackbar(actionError)
            events.onActionErrorShown()
        }
    }
    val book = state.detail?.book
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(title = "", onBack = onBack) {
                if (book != null) {
                    IconButton(onClick = events.onEdit) {
                        Icon(
                            painterResource(PrumoIcons.Edit),
                            contentDescription = stringResource(R.string.reading_edit_book),
                        )
                    }
                    OverflowMenu(
                        buildList {
                            if (book.hasCover) {
                                add(
                                    MenuOption(
                                        stringResource(R.string.reading_remove_cover),
                                        PrumoIcons.HideImage,
                                        events.onRemoveCover,
                                    ),
                                )
                            }
                            add(
                                MenuOption(stringResource(R.string.reading_delete_book), PrumoIcons.Delete) {
                                    confirmDelete =
                                        true
                                },
                            )
                        },
                        label = stringResource(R.string.reading_more),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = TOOLBAR_SPACE)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                book == null && state.loading -> LoadingState(stringResource(R.string.reading_loading))

                book == null -> ErrorState(state.error ?: AppError.Unexpected(), onRetry = events.onRetry)

                else -> {
                    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = events.onRefresh) {
                        BookContent(state, book, events)
                    }
                    ReadingToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
                }
            }
        }
    }
    if (confirmDelete && book != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.reading_delete_title),
            body = stringResource(R.string.reading_delete_body, book.title),
            confirmLabel = stringResource(R.string.reading_delete_book),
            onConfirm = {
                confirmDelete = false
                events.onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
private fun BookContent(state: BookDetailUiState, book: Book, events: BookDetailEvents) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(bottom = TOOLBAR_SPACE)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Header(book)
        ProgressHero(state, book, events)
        Dates(book)
        // Board 05 has the notes right below the dates; the pace (R7–R9, not on the board) follows them.
        SectionHeader(
            stringResource(R.string.reading_notes),
            trailing = state.notes.size.takeIf { it > 0 }?.toString(),
        )
        if (state.notes.isEmpty()) {
            Text(
                stringResource(R.string.reading_no_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            GroupedList(
                state.notes.map { note ->
                    GroupedRow(
                        title = note.content,
                        summary = noteMeta(note),
                        icon = PrumoIcons.StickyNote2,
                        action = RowAction.Click(onClick = { events.onNote(note) }, chevron = false),
                    )
                },
                style = ListStyle.Separated,
            )
        }
        Pace(book, state.detail?.weeklyPages.orEmpty())
    }
}

/** "pág. 103 · 28/10/2026", or only the date without a page. */
@Composable
internal fun noteMeta(note: Note): String =
    note.page?.let { stringResource(R.string.reading_note_page_date, it, Formats.date(note.notedOn)) }
        ?: Formats.date(note.notedOn)

@Composable
private fun Header(book: Book) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        BookCover(book, width = 100.dp, height = 148.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                book.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.reading_book_meta, book.author, book.totalPages),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Tag(book.status.label(), small = true)
                if (book.owned) Tag(stringResource(R.string.reading_owned), icon = PrumoIcons.Check, small = true)
            }
        }
    }
}

@Composable
private fun ProgressHero(state: BookDetailUiState, book: Book, events: BookDetailEvents) {
    SectionCard(
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.AutoStories,
        overline = stringResource(R.string.reading_progress),
        title = stringResource(R.string.reading_page_title, book.currentPage, book.totalPages),
        celebrating = state.celebrating,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            FitText(
                stringResource(R.string.reading_percent, book.progressPercent),
                style = MaterialTheme.typography.displayMediumEmphasized.copy(fontFeatureSettings = "tnum"),
            )
            if (book.remainingPages > 0) {
                Text(
                    pluralStringResource(R.plurals.reading_pages_left, book.remainingPages, book.remainingPages),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
        }
        LinearWavyProgressIndicator(progress = { book.progressPercent / PERCENT }, modifier = Modifier.fillMaxWidth())
        when (book.status) {
            BookStatus.Reading -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QUICK_PAGES.forEach { pages ->
                    val label = pluralStringResource(R.plurals.reading_add_pages_label, pages, pages)
                    Button(
                        onClick = { events.onAddPages(pages) },
                        enabled = !state.busy && book.currentPage < book.totalPages,
                        modifier = Modifier.weight(1f).semantics { contentDescription = label },
                    ) {
                        Text(stringResource(R.string.reading_add_pages, pages))
                    }
                }
            }

            BookStatus.WantToRead -> FilledTonalButton(
                onClick = events.onStartReading,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.reading_start_reading)) }

            BookStatus.Read -> Unit
        }
        if (book.status == BookStatus.Reading) {
            FilledTonalButton(onClick = events.onMarkRead, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(PrumoIcons.DoneAll), contentDescription = null)
                Text(stringResource(R.string.reading_mark_read), Modifier.padding(start = Spacing.sm))
            }
        }
    }
}

/** Início and, for a finished book, Término; while reading, the forecast (R8) when there is one. */
@Composable
private fun Dates(book: Book) {
    val none = stringResource(R.string.reading_no_date)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        KpiTile(
            book.startedOn?.let(Formats::date) ?: none,
            stringResource(R.string.reading_started),
            Modifier.weight(1f),
        )
        val forecast = book.forecastFinishOn
        when {
            book.finishedOn != null -> KpiTile(
                Formats.date(book.finishedOn),
                stringResource(R.string.reading_finished),
                Modifier.weight(1f),
            )

            book.status == BookStatus.Reading && forecast != null -> KpiTile(
                stringResource(R.string.reading_forecast_value, Formats.date(forecast)),
                stringResource(R.string.reading_forecast),
                Modifier.weight(1f),
            )

            else -> KpiTile(
                if (book.status == BookStatus.Reading) stringResource(R.string.reading_in_progress) else none,
                stringResource(R.string.reading_finished),
                Modifier.weight(1f),
            )
        }
    }
}

/** The pace of the last two weeks and the pages of each week (R7, R9): values of the server. */
@Composable
private fun Pace(book: Book, weeks: List<WeekPages>) {
    if (book.status == BookStatus.WantToRead) return
    if (book.status == BookStatus.Read && book.pacePagesPerDay == null && weeks.none { it.pages > 0 }) return
    SectionCard(
        tone = SectionTone.Container,
        icon = PrumoIcons.AutoStories,
        title = stringResource(R.string.reading_pace),
    ) {
        val pace = book.pacePagesPerDay
        val forecast = book.forecastFinishOn
        val text = when {
            pace != null && forecast != null && book.status == BookStatus.Reading ->
                stringResource(R.string.reading_pace_forecast, Formats.decimal(pace, 1), Formats.date(forecast))

            pace != null -> pluralStringResource(
                R.plurals.reading_pace_value,
                pace.toInt().coerceAtLeast(1),
                Formats.decimal(pace, 1),
            )

            // A finished book has no pace to wait for: only its weeks show.
            book.status == BookStatus.Read -> null

            else -> stringResource(R.string.reading_no_pace)
        }
        text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (weeks.any { it.pages > 0 }) {
            Text(stringResource(R.string.reading_per_week), style = MaterialTheme.typography.labelLarge)
            WeekBars(weeks)
        }
    }
}

/** One bar per week, as tall as its pages against the tallest; read out as "dd/mm: n páginas". */
@Composable
private fun WeekBars(weeks: List<WeekPages>) {
    val most = weeks.maxOf { it.pages }.coerceAtLeast(1)
    val description = weeks.map {
        stringResource(R.string.reading_week_label, Formats.shortDate(it.weekStart), it.pages)
    }
        .joinToString("; ")
    Row(
        Modifier.fillMaxWidth().height(BARS_HEIGHT).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeks.forEach { week ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                FitText(
                    week.pages.toString(),
                    style = PrumoType.dataSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(BAR_SHARE * week.pages / most + MIN_BAR)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(
                    Formats.shortDate(week.weekStart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

private val QUICK_PAGES = listOf(1, 10, 25)
private const val PERCENT = 100f
private val BARS_HEIGHT = 120.dp
private const val BAR_SHARE = 0.6f
private const val MIN_BAR = 0.02f

@PhonePreviews
@Composable
private fun BookDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            BookDetailScreen(
                state = BookDetailUiState(loading = false, detail = PreviewBooks.detail, notes = PreviewBooks.notes),
                onBack = {},
                events = BookDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                actions = ReadingActions({}, {}),
            )
        }
    }
}
