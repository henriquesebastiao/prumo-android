package com.henriquesebastiao.prumo.feature.reading.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.reading.R
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.CoverFileProvider
import com.henriquesebastiao.prumo.feature.reading.data.CoverImages
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.CoverImage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookEditorUiState(
    /** The book being edited (it gets the new book's id after a first save). */
    val editingId: String? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    /** The book as the server has it: its cover until a new one is picked. */
    val book: Book? = null,
    val status: BookStatus = BookStatus.WantToRead,
    val owned: Boolean = false,
    val startedOn: LocalDate? = null,
    val finishedOn: LocalDate? = null,
    /** A picture picked for the cover, sent after the book is saved. */
    val cover: CoverImage? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    /** The book was saved but its cover was not: shown on the cover, the form stays. */
    val coverError: AppError? = null,
    val coverUnreadable: Boolean = false,
    val done: Boolean = false,
    val context: ReadingContext = ReadingContext(),
)

@HiltViewModel(assistedFactory = BookEditorViewModel.Factory::class)
class BookEditorViewModel @AssistedInject constructor(
    @Assisted bookId: String?,
    private val repository: ReadingRepository,
    private val covers: CoverImages,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var title by mutableStateOf("")
        private set
    var author by mutableStateOf("")
        private set

    /** The pages, as digits. */
    var pages by mutableStateOf("")
        private set

    /** The current page of a book started before (a new book being read), as digits. */
    var currentPage by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(
        BookEditorUiState(
            editingId = bookId,
            loading = bookId != null,
            context = account.preferences.value.readingContext(clock),
        ),
    )
    val state: StateFlow<BookEditorUiState> = mutableState.asStateFlow()

    init {
        if (bookId != null) load()
    }

    fun load() {
        val id = mutableState.value.editingId ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.book(id)) {
                is Outcome.Ok -> {
                    val book = outcome.value.book
                    title = book.title
                    author = book.author
                    pages = book.totalPages.toString()
                    mutableState.update {
                        it.copy(
                            loading = false,
                            book = book,
                            status = book.status,
                            owned = book.owned,
                            startedOn = book.startedOn,
                            finishedOn = book.finishedOn,
                        )
                    }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun onTitleChange(text: String) = edit { title = text }

    fun onAuthorChange(text: String) = edit { author = text }

    fun onPagesChange(digits: String) = edit { pages = digits }

    fun onCurrentPageChange(digits: String) = edit { currentPage = digits }

    fun onStatusChange(status: BookStatus) = mutableState.update { it.copy(status = status, saveError = null) }

    fun onOwnedChange(owned: Boolean) = mutableState.update { it.copy(owned = owned) }

    fun onStartedChange(date: LocalDate?) = mutableState.update { it.copy(startedOn = date, saveError = null) }

    fun onFinishedChange(date: LocalDate?) = mutableState.update { it.copy(finishedOn = date, saveError = null) }

    /** A picture from the gallery or the camera; a photo too big is made smaller first (R6). */
    fun onCoverPicked(uri: Uri) {
        viewModelScope.launch {
            val image = covers.read(uri)
            mutableState.update {
                if (image != null) {
                    it.copy(cover = image, coverError = null, coverUnreadable = false)
                } else {
                    it.copy(coverUnreadable = true)
                }
            }
        }
    }

    val canSave: Boolean
        get() = title.isNotBlank() && author.isNotBlank() && (Inputs.number(pages) ?: 0L) > 0

    fun save() {
        if (!canSave) return
        val state = mutableState.value
        val input = BookInput(
            title = title.trim(),
            author = author.trim(),
            totalPages = Inputs.number(pages)!!.toInt(),
            status = state.status,
            owned = state.owned,
            currentPage = if (state.editingId == null && state.status == BookStatus.Reading) {
                Inputs.number(currentPage)?.toInt()
            } else {
                null
            },
            startedOn = state.startedOn.takeIf { state.status != BookStatus.WantToRead },
            finishedOn = state.finishedOn.takeIf { state.status == BookStatus.Read },
        )
        mutableState.update { it.copy(saving = true, saveError = null, coverError = null) }
        viewModelScope.launch {
            val id = state.editingId
            val saved = if (id == null) repository.createBook(input) else repository.updateBook(id, input)
            when (saved) {
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = saved.error) }
                is Outcome.Ok -> sendCover(saved.value)
            }
        }
    }

    private suspend fun sendCover(book: Book) {
        val cover = mutableState.value.cover
        if (cover == null) {
            mutableState.update { it.copy(saving = false, done = true) }
            return
        }
        when (val sent = repository.setCover(book.id, cover)) {
            is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }

            // The book exists now: a new try edits it instead of creating another.
            is Outcome.Failed -> mutableState.update {
                it.copy(saving = false, editingId = book.id, book = book, coverError = sent.error)
            }
        }
    }

    private fun edit(change: () -> Unit) {
        change()
        mutableState.update { it.copy(saveError = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(bookId: String?): BookEditorViewModel
    }
}

/** One callback per control of the form. */
@Suppress("LongParameterList")
internal class BookEditorEvents(
    val onTitleChange: (String) -> Unit,
    val onAuthorChange: (String) -> Unit,
    val onPagesChange: (String) -> Unit,
    val onCurrentPageChange: (String) -> Unit,
    val onStatusChange: (BookStatus) -> Unit,
    val onOwnedChange: (Boolean) -> Unit,
    val onStartedChange: (LocalDate?) -> Unit,
    val onFinishedChange: (LocalDate?) -> Unit,
    val onCamera: () -> Unit,
    val onGallery: () -> Unit,
    val onSave: () -> Unit,
    val onRetry: () -> Unit,
)

/** What the form shows of the view model's text, typed synchronously (CLAUDE.md, "Campos de texto"). */
internal class BookEditorFields(
    val title: String,
    val author: String,
    val pages: String,
    val currentPage: String,
    val canSave: Boolean,
)

@Composable
internal fun BookEditorRoute(
    bookId: String?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: BookEditorViewModel = hiltViewModel<BookEditorViewModel, BookEditorViewModel.Factory>(
        key = "book-editor-$bookId",
        creationCallback = { it.create(bookId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    val context = LocalContext.current
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = photo
        if (taken && uri != null) viewModel.onCoverPicked(Uri.parse(uri))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onCoverPicked(uri)
    }
    BookEditorScreen(
        state = state,
        fields = BookEditorFields(
            viewModel.title,
            viewModel.author,
            viewModel.pages,
            viewModel.currentPage,
            viewModel.canSave,
        ),
        events = BookEditorEvents(
            onTitleChange = viewModel::onTitleChange,
            onAuthorChange = viewModel::onAuthorChange,
            onPagesChange = viewModel::onPagesChange,
            onCurrentPageChange = viewModel::onCurrentPageChange,
            onStatusChange = viewModel::onStatusChange,
            onOwnedChange = viewModel::onOwnedChange,
            onStartedChange = viewModel::onStartedChange,
            onFinishedChange = viewModel::onFinishedChange,
            onCamera = {
                val uri = CoverFileProvider.newPhoto(context)
                photo = uri.toString()
                camera.launch(uri)
            },
            onGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSave = viewModel::save,
            onRetry = viewModel::load,
        ),
        onClose = navigator::back,
        modifier = modifier,
    )
}

/**
 * A new book or its edit (board 04): the cover from the camera or the gallery, title, author,
 * pages, the status as a connected group ("Lendo" asks the page and the start, "Lido" both dates)
 * and "Tenho este livro".
 */
@Composable
internal fun BookEditorScreen(
    state: BookEditorUiState,
    fields: BookEditorFields,
    events: BookEditorEvents,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state.editingId != null
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                stringResource(if (editing) R.string.reading_edit_book else R.string.reading_new_book),
                onBack = onClose,
                close = true,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.reading_loading))
                state.error != null -> ErrorState(state.error, onRetry = events.onRetry)
                else -> Form(state, fields, events)
            }
        }
    }
}

@Composable
private fun Form(state: BookEditorUiState, fields: BookEditorFields, events: BookEditorEvents) {
    val editing = state.editingId != null
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CoverPicker(state, events)
        OutlinedTextField(
            value = fields.title,
            onValueChange = events.onTitleChange,
            label = { Text(stringResource(R.string.reading_title)) },
            isError = fieldError(state.saveError, "title") != null,
            trailingIcon = if (fields.title.isNotEmpty()) {
                {
                    IconButton(onClick = { events.onTitleChange("") }) {
                        Icon(
                            painterResource(PrumoIcons.Close),
                            contentDescription = stringResource(R.string.reading_clear_title),
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Board 04: the author and the pages share a row.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = fields.author,
                onValueChange = events.onAuthorChange,
                label = { Text(stringResource(R.string.reading_author)) },
                isError = fieldError(state.saveError, "author") != null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier.weight(AUTHOR_WEIGHT),
            )
            ValueField(
                digits = fields.pages,
                onDigitsChange = events.onPagesChange,
                label = stringResource(R.string.reading_pages),
                transformation = IntegerTransformation,
                modifier = Modifier.weight(1f),
                error = fieldError(state.saveError, "total_pages"),
                maxDigits = PAGE_DIGITS,
            )
        }
        Text(stringResource(R.string.reading_status), style = MaterialTheme.typography.labelLarge)
        ChoiceGroup(
            options = BookStatus.entries,
            selected = state.status,
            label = { it.label() },
            onSelect = events.onStatusChange,
        )
        if (state.status == BookStatus.Reading && !editing) {
            ValueField(
                digits = fields.currentPage,
                onDigitsChange = events.onCurrentPageChange,
                label = stringResource(R.string.reading_current_page),
                transformation = IntegerTransformation,
                error = fieldError(state.saveError, "current_page"),
                maxDigits = PAGE_DIGITS,
            )
        }
        if (state.status != BookStatus.WantToRead) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DateField(
                    value = state.startedOn,
                    onValueChange = events.onStartedChange,
                    label = stringResource(R.string.reading_started),
                    modifier = Modifier.weight(1f),
                    onClear = { events.onStartedChange(null) },
                    today = state.context.today,
                )
                if (state.status == BookStatus.Read) {
                    DateField(
                        value = state.finishedOn,
                        onValueChange = events.onFinishedChange,
                        label = stringResource(R.string.reading_finished),
                        modifier = Modifier.weight(1f),
                        onClear = { events.onFinishedChange(null) },
                        supportingText = fieldError(state.saveError, "finished_on"),
                        today = state.context.today,
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = state.owned, role = Role.Switch, onValueChange = events.onOwnedChange)
                .padding(start = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.reading_owned_switch),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = state.owned, onCheckedChange = null)
        }
        state.saveError?.takeIf { !it.isFieldError() }?.let {
            Text(readingErrorText(it), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = events.onSave,
            enabled = fields.canSave && !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.reading_save_book), style = MaterialTheme.typography.titleMedium) }
    }
}

/** Board 04: the cover (the one picked, the book's, or the placeholder), then the camera and the gallery. */
@Composable
private fun CoverPicker(state: BookEditorUiState, events: BookEditorEvents) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        val cover = state.cover
        if (cover != null) {
            AsyncImage(
                model = cover.bytes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(COVER_WIDTH, COVER_HEIGHT).clip(MaterialTheme.shapes.medium),
            )
        } else if (state.book?.hasCover == true) {
            BookCover(state.book, COVER_WIDTH, COVER_HEIGHT)
        } else {
            Box(Modifier.size(COVER_WIDTH, COVER_HEIGHT).clip(MaterialTheme.shapes.medium)) {
                Box(
                    Modifier.fillMaxSize().padding(0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(PrumoIcons.AddPhotoAlternate),
                                contentDescription = null,
                                Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.reading_cover), style = MaterialTheme.typography.labelLarge)
            FilledTonalButton(onClick = events.onCamera, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(PrumoIcons.PhotoCamera), contentDescription = null)
                Text(stringResource(R.string.reading_camera), Modifier.padding(start = Spacing.sm))
            }
            OutlinedButton(onClick = events.onGallery, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(PrumoIcons.Image), contentDescription = null)
                Text(stringResource(R.string.reading_gallery), Modifier.padding(start = Spacing.sm))
            }
            val coverError = state.coverError
            when {
                coverError != null -> Text(
                    stringResource(R.string.reading_cover_failed, readingErrorText(coverError)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )

                state.coverUnreadable -> Text(
                    stringResource(R.string.reading_cover_failed, stringResource(R.string.reading_cover_unreadable)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )

                else -> Text(
                    stringResource(R.string.reading_cover_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val PAGE_DIGITS = 5
private const val AUTHOR_WEIGHT = 1.8f
private val COVER_WIDTH = 112.dp
private val COVER_HEIGHT = 164.dp

@PhonePreviews
@Composable
private fun BookEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(ReadingFeature.SEED) {
            BookEditorScreen(
                state = BookEditorUiState(),
                fields = BookEditorFields("O Hobbit", "J.R.R. Tolkien", "336", "", canSave = true),
                events = BookEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
                onClose = {},
            )
        }
    }
}
