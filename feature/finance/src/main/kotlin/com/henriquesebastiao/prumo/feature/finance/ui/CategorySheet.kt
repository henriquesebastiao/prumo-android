package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.IconOption
import com.henriquesebastiao.prumo.core.designsystem.component.IconPicker
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryInput
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategorySheetUiState(
    val request: Long? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val editing: Category? = null,
    val kind: CategoryKind = CategoryKind.Expense,
    val color: FinanceColor = FinanceColor.Blue,
    val icon: String = CategoryIcons.first(),
    val saving: Boolean = false,
    val saveError: AppError? = null,
    /** The category saved (its id), or removed (empty): the sheet closes. */
    val done: String? = null,
)

@HiltViewModel
class CategorySheetViewModel @Inject constructor(private val repository: FinanceRepository) : ViewModel() {
    var name by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(CategorySheetUiState())
    val state: StateFlow<CategorySheetUiState> = mutableState.asStateFlow()
    private var categoryId: String? = null

    /** A new opening: a new category of [expense] kind, or [categoryId]'s edit. */
    fun start(request: Long, categoryId: String?, expense: Boolean) {
        if (mutableState.value.request == request) return
        this.categoryId = categoryId
        name = ""
        mutableState.value = CategorySheetUiState(
            request = request,
            loading = categoryId != null,
            kind = if (expense) CategoryKind.Expense else CategoryKind.Income,
        )
        if (categoryId != null) load()
    }

    fun load() {
        val id = categoryId ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // There's no GET of one category: the list (active and archived) has it.
            val found = (repository.categories() as? Outcome.Ok)?.value?.firstOrNull { it.id == id }
                ?: (repository.categories(archived = true) as? Outcome.Ok)?.value?.firstOrNull { it.id == id }
            if (found == null) {
                mutableState.update { it.copy(loading = false, error = AppError.Api(NOT_FOUND, "not_found")) }
                return@launch
            }
            name = found.name
            mutableState.update {
                it.copy(loading = false, editing = found, kind = found.kind, color = found.color, icon = found.icon)
            }
        }
    }

    fun onNameChange(text: String) {
        name = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onKindChange(kind: CategoryKind) = mutableState.update { it.copy(kind = kind) }

    fun onColorChange(color: FinanceColor) = mutableState.update { it.copy(color = color) }

    fun onIconChange(icon: String) = mutableState.update { it.copy(icon = icon) }

    fun save() {
        val current = mutableState.value
        if (name.isBlank() || current.saving) return
        val input = CategoryInput(current.kind, name.trim(), current.color, current.icon)
        val id = current.editing?.id
        submit(saved = true) {
            if (id ==
                null
            ) {
                repository.createCategory(input)
            } else {
                repository.updateCategory(id, input)
            }
        }
    }

    /** Archive when it has transactions, delete otherwise (F8, D32 of the server). */
    fun remove() {
        val editing = mutableState.value.editing ?: return
        submit(saved = false) {
            if (editing.transactionsCount >
                0
            ) {
                repository.setCategoryArchived(editing.id, true)
            } else {
                repository.deleteCategory(editing.id)
            }
        }
    }

    private fun submit(saved: Boolean, call: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = call()) {
                is Outcome.Ok -> mutableState.update {
                    it.copy(saving = false, done = if (saved) (outcome.value as? Category)?.id.orEmpty() else "")
                }

                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}

/** What the category sheet does. */
internal class CategorySheetEvents(
    val onNameChange: (String) -> Unit,
    val onKindChange: (CategoryKind) -> Unit,
    val onColorChange: (FinanceColor) -> Unit,
    val onIconChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onRemove: () -> Unit,
    val onRetry: () -> Unit,
)

/** Create or edit a category (board 11), over the current screen. [onSaved] gets the new category's id. */
@Composable
internal fun CategorySheet(
    categoryId: String?,
    expense: Boolean,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: CategorySheetViewModel = hiltViewModel(),
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, categoryId, expense) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) {
        val done = state.done
        if (current && done != null) {
            if (done.isNotEmpty()) onSaved(done)
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        CategorySheetContent(
            state = if (current) state else CategorySheetUiState(loading = true),
            name = viewModel.name,
            events = CategorySheetEvents(
                onNameChange = viewModel::onNameChange,
                onKindChange = viewModel::onKindChange,
                onColorChange = viewModel::onColorChange,
                onIconChange = viewModel::onIconChange,
                onSave = viewModel::save,
                onRemove = viewModel::remove,
                onRetry = viewModel::load,
            ),
        )
    }
}

/** The content of the category sheet, without state. */
@Composable
internal fun CategorySheetContent(
    state: CategorySheetUiState,
    name: String,
    events: CategorySheetEvents,
    modifier: Modifier = Modifier,
) {
    val editing = state.editing
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
            stringResource(if (editing == null) R.string.finance_new_category else R.string.finance_edit_category),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        when {
            state.loading -> LoadingState(stringResource(R.string.finance_loading))

            state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    // The preview changes with the color and icon (board 11).
                    FinanceAvatar(state.color, state.icon, cookie = true)
                    OutlinedTextField(
                        value = name,
                        onValueChange = events.onNameChange,
                        label = { Text(stringResource(R.string.finance_field_name)) },
                        isError = state.saveError != null,
                        supportingText = state.saveError?.let { { Text(financeErrorText(it)) } },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                val locked = (editing?.transactionsCount ?: 0) > 0
                ChoiceGroup(
                    options = CategoryKind.entries,
                    selected = state.kind,
                    label = {
                        stringResource(
                            if (it ==
                                CategoryKind.Expense
                            ) {
                                R.string.finance_kind_expense
                            } else {
                                R.string.finance_kind_income
                            },
                        )
                    },
                    onSelect = { if (!locked) events.onKindChange(it) },
                )
                if (locked) GroupNote(stringResource(R.string.finance_kind_locked))
                Text(stringResource(R.string.finance_icon), style = MaterialTheme.typography.labelLarge)
                val icons = if (state.icon in CategoryIcons) CategoryIcons else CategoryIcons + state.icon
                IconPicker(
                    options = icons.map { IconOption(ServerIcons.of(it), iconLabel(it)) },
                    selected = icons.indexOf(state.icon),
                    onSelect = { events.onIconChange(icons[it]) },
                )
                ColorField(state.color, events.onColorChange)
                if (editing != null) {
                    GroupNote(
                        if (editing.transactionsCount > 0) {
                            pluralStringResource(
                                R.plurals.finance_category_archive_note,
                                editing.transactionsCount,
                                editing.transactionsCount,
                            )
                        } else {
                            stringResource(R.string.finance_category_delete_note)
                        },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editing != null) {
                        TextButton(onClick = events.onRemove, enabled = !state.saving) {
                            val archive = editing.transactionsCount > 0
                            Icon(
                                painterResource(if (archive) PrumoIcons.Archive else PrumoIcons.Delete),
                                contentDescription = null,
                            )
                            Text(
                                stringResource(if (archive) R.string.finance_archive else R.string.finance_delete),
                                Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = events.onSave, enabled = name.isNotBlank() && !state.saving) {
                        Text(stringResource(R.string.finance_save))
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun CategorySheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                CategorySheetContent(
                    state = CategorySheetUiState(
                        editing = FinancePreview.leisure,
                        color = FinanceColor.Pink,
                        icon = "ticket",
                    ),
                    name = "Lazer",
                    events = CategorySheetEvents({}, {}, {}, {}, {}, {}, {}),
                )
            }
        }
    }
}
