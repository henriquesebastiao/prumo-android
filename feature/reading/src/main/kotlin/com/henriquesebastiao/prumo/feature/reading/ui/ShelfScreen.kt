package com.henriquesebastiao.prumo.feature.reading.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.reading.R
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter
import com.henriquesebastiao.prumo.feature.reading.model.ShelfOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The year "Lidos" shows (the web's select): the latest with a finished book, one year, or all. */
sealed interface ReadYear {
    data object Latest : ReadYear

    data object All : ReadYear

    data class Of(val year: Int) : ReadYear
}

data class ShelfUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val books: List<Book> = emptyList(),
    val filter: ShelfFilter = ShelfFilter(),
    val readYear: ReadYear = ReadYear.Latest,
) {
    /** With "Lidos", the years in which the books were finished (`finished_on`), newest first. */
    val readYears: List<Int>
        get() = if (filter.status == BookStatus.Read) {
            books.mapNotNull { it.finishedOn?.year }.distinct().sortedDescending()
        } else {
            emptyList()
        }

    /** The year "Lidos" shows, or null for every book. */
    val shownYear: Int?
        get() = when (val year = readYear) {
            ReadYear.Latest -> readYears.firstOrNull()
            ReadYear.All -> null
            is ReadYear.Of -> year.year
        }

    /**
     * The books on the grid. "Lidos" shows one year at a time, as the web does with the whole shelf
     * the server sent: a choice of what to show, from the dates the server keeps.
     */
    val shown: List<Book>
        get() = shownYear?.let { year -> books.filter { it.finishedOn?.year == year } } ?: books

    /**
     * The hero (board 01): the book being read that moved last, among those the filter shows. Which
     * one leads is the shelf's choice of what to show, not a rule of the domain.
     */
    val readingNow: Book?
        get() = books.filter { it.status == BookStatus.Reading }
            .maxByOrNull { it.lastProgressAt ?: it.createdAt }

    /** Nothing on the shelf at all (not a filter that found nothing): the empty state of board 03. */
    val emptyShelf: Boolean
        get() = !loading && error == null && books.isEmpty() && filter.copy(order = ShelfOrder.Newest) == ShelfFilter()
}

@HiltViewModel
class ShelfViewModel @Inject constructor(private val repository: ReadingRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ShelfUiState())
    val state: StateFlow<ShelfUiState> = mutableState.asStateFlow()

    private var loading: Job? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load(refresh = true) } }
    }

    /** Pull to refresh (and "Tentar novamente"): asks the server first, not the device's copy (D21). */
    fun pull() {
        repository.expireCache()
        load(refresh = true)
    }

    fun load(refresh: Boolean = false) {
        loading?.cancel()
        mutableState.update { it.copy(loading = !refresh && it.books.isEmpty(), refreshing = refresh, error = null) }
        loading = viewModelScope.launch {
            val outcome = repository.books(mutableState.value.filter)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, books = outcome.value.items)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    /** A status chip: the statuses are exclusive, and the chosen one again shows every book. */
    fun selectStatus(status: BookStatus) {
        mutableState.update { it.copy(readYear = ReadYear.Latest) }
        setFilter { it.copy(status = if (it.status == status) null else status) }
    }

    /** "Tenho" or "Não tenho": exclusive, and they combine with any status (R1). */
    fun selectOwned(owned: Boolean) = setFilter { it.copy(owned = if (it.owned == owned) null else owned) }

    /** The year of "Lidos": no new request, the shelf came whole. */
    fun selectYear(year: ReadYear) = mutableState.update { it.copy(readYear = year) }

    /** The web's "Ordenar": newest, title or author, sorted by the server. */
    fun setOrder(order: ShelfOrder) = setFilter { it.copy(order = order) }

    /** The search on title and author, sent after a short pause in the typing. */
    fun search(query: String) {
        mutableState.update { it.copy(filter = it.filter.copy(query = query)) }
        loading?.cancel()
        loading = viewModelScope.launch {
            delay(SEARCH_PAUSE)
            load(refresh = true)
        }
    }

    private fun setFilter(change: (ShelfFilter) -> ShelfFilter) {
        mutableState.update { it.copy(filter = change(it.filter)) }
        load(refresh = true)
    }

    private companion object {
        const val SEARCH_PAUSE = 300L
    }
}

internal class ShelfEvents(
    val onOpen: (Book) -> Unit,
    val onAdd: () -> Unit,
    val onNotes: () -> Unit,
    val onStatus: (BookStatus) -> Unit,
    val onOwned: (Boolean) -> Unit,
    val onYear: (ReadYear) -> Unit,
    val onSearch: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onOrder: (ShelfOrder) -> Unit,
)

@Composable
internal fun ShelfRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ShelfViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    ShelfScreen(
        state = state,
        onBack = navigator::back,
        events = ShelfEvents(
            onOpen = { navigator.navigate(BookDetailKey(it.id)) },
            onAdd = { navigator.navigate(BookEditorKey()) },
            onNotes = { navigator.navigate(ReadingNotes) },
            onStatus = viewModel::selectStatus,
            onOwned = viewModel::selectOwned,
            onYear = viewModel::selectYear,
            onSearch = viewModel::search,
            onRefresh = viewModel::pull,
            onOrder = viewModel::setOrder,
        ),
        actions = ReadingActions(
            onProgress = { sheet = SheetRequest(SheetKind.Progress) },
            onNote = { sheet = SheetRequest(SheetKind.Note) },
            onAddBook = { navigator.navigate(BookEditorKey()) },
        ),
        modifier = modifier,
    )
    ReadingSheet(sheet, onDismiss = { sheet = null })
}

/**
 * Livros (boards 01 to 03): the book being read as the hero, the shelf in a grid of covers, status
 * filters that exclude each other and "Tenho" apart; the toolbar records.
 */
@Composable
internal fun ShelfScreen(
    state: ShelfUiState,
    onBack: () -> Unit,
    events: ShelfEvents,
    actions: ReadingActions,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var searching by rememberSaveable { mutableStateOf(state.filter.query.isNotEmpty()) }
    Scaffold(
        modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            PrumoTopBar(stringResource(R.string.reading_name), onBack, large = true, scrollBehavior = scroll) {
                // Board 03: an empty shelf has nothing to search and no notes.
                if (!state.emptyShelf) {
                    OrderMenu(state.filter.order, events.onOrder)
                    IconButton(onClick = events.onNotes) {
                        Icon(
                            painterResource(PrumoIcons.StickyNote2),
                            contentDescription = stringResource(R.string.reading_all_notes),
                        )
                    }
                    IconButton(onClick = {
                        if (searching) events.onSearch("")
                        searching = !searching
                    }) {
                        Icon(
                            painterResource(if (searching) PrumoIcons.Close else PrumoIcons.Search),
                            contentDescription = stringResource(
                                if (searching) R.string.reading_close_search else R.string.reading_search,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.reading_loading))

                state.error != null && state.books.isEmpty() -> ErrorState(state.error, onRetry = events.onRefresh)

                state.emptyShelf -> EmptyState(
                    icon = PrumoIcons.MenuBook,
                    title = stringResource(R.string.reading_empty_title),
                    body = stringResource(R.string.reading_empty_body),
                    hero = true,
                    action = {
                        // The design system's hero empty state: one tonal M button.
                        FilledTonalButton(onClick = events.onAdd, modifier = Modifier.heightIn(min = 56.dp)) {
                            Text(stringResource(R.string.reading_action_add_book))
                        }
                    },
                )

                else -> PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = events.onRefresh) {
                    Shelf(state, searching, events)
                }
            }
            // Without books there is nothing to record (board 03): the toolbar goes.
            if (!state.emptyShelf && !state.loading) {
                ReadingToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
            }
        }
    }
}

@Composable
private fun Shelf(state: ShelfUiState, searching: Boolean, events: ShelfEvents) {
    val filter = state.filter
    val grid = rememberLazyGridState()
    // The search field comes in above the filters: the grid keeps its place by key, so it has to
    // be brought into view.
    LaunchedEffect(searching) { if (searching) grid.scrollToItem(0) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) = {
            GridItemSpan(maxLineSpan)
        }
        if (searching) {
            item(key = "search", span = full) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { if (filter.query.isEmpty()) focus.requestFocus() }
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = events.onSearch,
                    placeholder = { Text(stringResource(R.string.reading_search_hint)) },
                    leadingIcon = { Icon(painterResource(PrumoIcons.Search), contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        }
        item(key = "filters", span = full) { Filters(state, events) }
        state.readingNow?.let { book ->
            item(key = "now", span = full) { ReadingNow(book) { events.onOpen(book) } }
        }
        item(key = "header", span = full) {
            val title = when {
                filter.status != null -> R.string.reading_shelf
                filter.owned == true -> R.string.reading_owned
                filter.owned == false -> R.string.reading_not_owned
                else -> R.string.reading_shelf
            }
            SectionHeader(
                stringResource(title),
                trailing = pluralStringResource(R.plurals.reading_books_count, state.shown.size, state.shown.size),
            )
        }
        if (state.shown.isEmpty()) {
            item(key = "none", span = full) {
                Text(
                    stringResource(R.string.reading_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.shown, key = { it.id }) { book -> ShelfItem(book) { events.onOpen(book) } }
    }
}

/** "Ordenar" (the web's shelf menu): the chosen order has a check. */
@Composable
private fun OrderMenu(order: ShelfOrder, onOrder: (ShelfOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(PrumoIcons.Sort), contentDescription = stringResource(R.string.reading_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ShelfOrder.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (option) {
                                    ShelfOrder.Newest -> R.string.reading_sort_newest
                                    ShelfOrder.Title -> R.string.reading_sort_title
                                    ShelfOrder.Author -> R.string.reading_sort_author
                                },
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    trailingIcon = if (option == order) {
                        { Icon(painterResource(PrumoIcons.Check), contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        onOrder(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun Filters(state: ShelfUiState, events: ShelfEvents) {
    val filter = state.filter
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            BookStatus.Reading to R.string.reading_filter_reading,
            BookStatus.WantToRead to R.string.reading_filter_want,
            BookStatus.Read to R.string.reading_filter_read,
        ).forEach { (status, label) ->
            FilterChip(
                selected = filter.status == status,
                onClick = { events.onStatus(status) },
                label = { Text(stringResource(label)) },
            )
            if (status == BookStatus.Read && filter.status == status && state.readYears.isNotEmpty()) {
                YearChip(state.shownYear, state.readYears, events.onYear)
            }
        }
        // "Tenho" and "Não tenho" combine with any status (board 01): a rule sets them apart.
        VerticalDivider(Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        listOf(true to R.string.reading_owned, false to R.string.reading_not_owned).forEach { (owned, label) ->
            val selected = filter.owned == owned
            FilterChip(
                selected = selected,
                onClick = { events.onOwned(owned) },
                label = { Text(stringResource(label)) },
                leadingIcon = if (selected) {
                    { Icon(painterResource(PrumoIcons.Check), contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}

/** The year of "Lidos" (the web's select): the years with a finished book, then all of them. */
@Composable
private fun YearChip(year: Int?, years: List<Int>, onYear: (ReadYear) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = year?.toString() ?: stringResource(R.string.reading_all_years)
    val description = stringResource(R.string.reading_read_year, label)
    Box {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(label) },
            trailingIcon = { Icon(painterResource(PrumoIcons.ArrowDropDown), contentDescription = null) },
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val options = years.map { ReadYear.Of(it) to it.toString() } +
                (ReadYear.All to stringResource(R.string.reading_all_years))
            options.forEach { (option, text) ->
                val checked = if (option is ReadYear.Of) option.year == year else year == null
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
                    trailingIcon = if (checked) {
                        { Icon(painterResource(PrumoIcons.Check), contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        onYear(option)
                    },
                )
            }
        }
    }
}

/** Board 01: the book being read, its cover, progress (R5, from the server) and the wave. */
@Composable
private fun ReadingNow(book: Book, onOpen: () -> Unit) {
    SectionCard(
        tone = SectionTone.Primary,
        hero = true,
        overline = stringResource(R.string.reading_reading_now),
        onClick = onOpen,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            BookCover(
                book,
                width = 76.dp,
                height = 112.dp,
                container = MaterialTheme.colorScheme.surface,
                content = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FitText(
                        stringResource(R.string.reading_percent, book.progressPercent),
                        style = PrumoType.dataHeadline,
                    )
                    Text(
                        stringResource(R.string.reading_page_of, book.currentPage, book.totalPages),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
            }
        }
        LinearWavyProgressIndicator(progress = { book.progressPercent / PERCENT }, modifier = Modifier.fillMaxWidth())
    }
}

/** A cover in the grid, its title and "Lendo · tenho". */
@Composable
private fun ShelfItem(book: Book, onOpen: () -> Unit) {
    val status = book.status.label()
    Column(
        Modifier
            .clickable(onClick = onOpen)
            .semantics { role = Role.Button },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        val scheme = MaterialTheme.colorScheme
        val (container, content) = when (book.status) {
            BookStatus.Reading -> scheme.primaryContainer to scheme.onPrimaryContainer
            BookStatus.Read -> scheme.tertiaryContainer to scheme.onTertiaryContainer
            BookStatus.WantToRead -> scheme.secondaryContainer to scheme.onSecondaryContainer
        }
        BookCover(
            book,
            width = COVER_WIDTH,
            height = COVER_HEIGHT,
            container = container,
            content = content,
            modifier = Modifier.fillMaxWidth().aspectRatio(COVER_WIDTH / COVER_HEIGHT),
        )
        Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            if (book.owned) stringResource(R.string.reading_shelf_owned, status) else status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val COLUMNS = 3
private const val PERCENT = 100f
private val COVER_WIDTH = 112.dp
private val COVER_HEIGHT = 164.dp

@PhonePreviews
@Composable
private fun ShelfPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            ShelfScreen(
                state = ShelfUiState(loading = false, books = PreviewBooks.shelf),
                onBack = {},
                events = ShelfEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                actions = ReadingActions({}, {}, {}),
            )
        }
    }
}

@PhonePreviews
@Composable
private fun EmptyShelfPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            ShelfScreen(
                state = ShelfUiState(loading = false),
                onBack = {},
                events = ShelfEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                actions = ReadingActions({}, {}, {}),
            )
        }
    }
}
