package com.henriquesebastiao.prumo.feature.reading.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
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
import com.henriquesebastiao.prumo.feature.reading.model.Note
import com.henriquesebastiao.prumo.feature.reading.model.Page
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val query: String = "",
    val notes: Page<Note> = Page(emptyList(), 0),
    val loadingMore: Boolean = false,
)

@HiltViewModel
class NotesViewModel @Inject constructor(private val repository: ReadingRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = mutableState.asStateFlow()

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
        mutableState.update {
            it.copy(loading = !refresh && it.notes.items.isEmpty(), refreshing = refresh, error = null)
        }
        loading = viewModelScope.launch {
            val outcome = repository.notes(mutableState.value.query.trim().ifEmpty { null })
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, notes = outcome.value)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    /** The search in the notes' text (R10), sent after a short pause in the typing. */
    fun search(query: String) {
        mutableState.update { it.copy(query = query) }
        loading?.cancel()
        loading = viewModelScope.launch {
            delay(SEARCH_PAUSE)
            load(refresh = true)
        }
    }

    fun loadMore() {
        val state = mutableState.value
        if (state.loadingMore || !state.notes.hasMore) return
        mutableState.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val outcome = repository.notes(state.query.trim().ifEmpty { null }, offset = state.notes.items.size)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loadingMore = false, notes = it.notes + outcome.value)
                    is Outcome.Failed -> it.copy(loadingMore = false, error = outcome.error)
                }
            }
        }
    }

    private companion object {
        const val SEARCH_PAUSE = 300L
    }
}

internal class NotesEvents(
    val onSearch: (String) -> Unit,
    val onOpen: (Note) -> Unit,
    val onLoadMore: () -> Unit,
    val onRefresh: () -> Unit,
)

@Composable
internal fun NotesRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    NotesScreen(
        state = state,
        events = NotesEvents(
            onSearch = viewModel::search,
            onOpen = { sheet = SheetRequest(SheetKind.Note, it.bookId, it.id) },
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::pull,
        ),
        onBack = navigator::back,
        modifier = modifier,
    )
    ReadingSheet(sheet, onDismiss = { sheet = null })
}

/**
 * Every note of every book, newest first, with a search in the text (the web's "Anotações" tab,
 * R10). A note opens its sheet, to edit or delete it.
 */
@Composable
internal fun NotesScreen(state: NotesUiState, events: NotesEvents, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier, topBar = { PrumoTopBar(stringResource(R.string.reading_all_notes), onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.reading_loading))

                state.error != null && state.notes.items.isEmpty() && state.query.isEmpty() ->
                    ErrorState(state.error, onRetry = events.onRefresh)

                state.notes.items.isEmpty() && state.query.isEmpty() && !state.refreshing -> EmptyState(
                    icon = PrumoIcons.StickyNote2,
                    title = stringResource(R.string.reading_notes_empty_title),
                    body = stringResource(R.string.reading_notes_empty_body),
                    hero = true,
                )

                else -> PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = events.onRefresh) {
                    NoteList(state, events)
                }
            }
        }
    }
}

@Composable
private fun NoteList(state: NotesUiState, events: NotesEvents) {
    val notes = state.notes.items
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(ListStyle.Separated.gap),
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = events.onSearch,
                placeholder = { Text(stringResource(R.string.reading_notes_search)) },
                leadingIcon = { Icon(painterResource(PrumoIcons.Search), contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
            )
        }
        if (notes.isEmpty()) {
            item(key = "none") { Text(stringResource(R.string.reading_no_notes)) }
        }
        itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
            GroupedListItem(
                GroupedRow(
                    title = note.content,
                    summary = noteBookMeta(note),
                    icon = PrumoIcons.StickyNote2,
                    action = RowAction.Click(onClick = { events.onOpen(note) }, chevron = false),
                ),
                index = index,
                count = notes.size,
                style = ListStyle.Separated,
            )
        }
        if (state.notes.hasMore) {
            item(key = "more") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = events.onLoadMore, enabled = !state.loadingMore) {
                        Text(stringResource(R.string.reading_load_more))
                    }
                }
            }
        }
    }
}

/** "O Hobbit · pág. 103 · 28/10/2026", or without the page. */
@Composable
private fun noteBookMeta(note: Note): String = note.page
    ?.let { stringResource(R.string.reading_note_book_page, note.bookTitle, it, Formats.date(note.notedOn)) }
    ?: stringResource(R.string.reading_note_book, note.bookTitle, Formats.date(note.notedOn))

@PhonePreviews
@Composable
private fun NotesPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            NotesScreen(
                state = NotesUiState(loading = false, notes = Page(PreviewBooks.notes, PreviewBooks.notes.size)),
                events = NotesEvents({}, {}, {}, {}),
                onBack = {},
            )
        }
    }
}
