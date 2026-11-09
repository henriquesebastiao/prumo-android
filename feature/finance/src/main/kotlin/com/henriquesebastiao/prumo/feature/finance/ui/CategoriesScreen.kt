package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val kind: CategoryKind = CategoryKind.Expense,
    val loading: Boolean = true,
    val error: AppError? = null,
    val categories: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
    val showArchived: Boolean = false,
    val actionError: AppError? = null,
    val context: FinanceContext = FinanceContext(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(CategoriesUiState(context = account.preferences.value.financeContext(clock)))
    val state: StateFlow<CategoriesUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load() } }
    }

    /** Both kinds, active and archived, with the uses of this month (from the server). */
    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            val active = repository.categories()
            val archived = repository.categories(archived = true).getOrNull().orEmpty()
            when (active) {
                is Outcome.Ok -> mutableState.update {
                    it.copy(loading = false, categories = active.value, archived = archived)
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = active.error) }
            }
        }
    }

    fun selectKind(kind: CategoryKind) = mutableState.update { it.copy(kind = kind) }

    fun toggleArchived() = mutableState.update { it.copy(showArchived = !it.showArchived) }

    fun setArchived(category: Category, archived: Boolean) = act {
        repository.setCategoryArchived(category.id, archived)
    }

    fun delete(category: Category) = act { repository.deleteCategory(category.id) }

    fun actionErrorShown() = mutableState.update { it.copy(actionError = null) }

    private fun act(call: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            val outcome = call()
            if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
        }
    }
}

/** What the categories screen does. */
internal class CategoriesEvents(
    val onKind: (CategoryKind) -> Unit,
    val onEdit: (Category?) -> Unit,
    val onArchive: (Category, Boolean) -> Unit,
    val onDelete: (Category) -> Unit,
    val onToggleArchived: () -> Unit,
    val onRetry: () -> Unit,
    val onActionErrorShown: () -> Unit,
)

@Composable
internal fun CategoriesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<FinanceSheet?>(null) }
    CategoriesScreen(
        state = state,
        onBack = onBack,
        events = CategoriesEvents(
            onKind = viewModel::selectKind,
            onEdit = { sheet = FinanceSheet.Category(it?.id, expense = state.kind == CategoryKind.Expense) },
            onArchive = viewModel::setArchived,
            onDelete = viewModel::delete,
            onToggleArchived = viewModel::toggleArchived,
            onRetry = viewModel::load,
            onActionErrorShown = viewModel::actionErrorShown,
        ),
        modifier = modifier,
    )
    FinanceSheetHost(sheet, onDismiss = { sheet = null })
}

/** Categorias (board 10): expenses or income, with the month's use; edit, archive or delete each. */
@Composable
internal fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    events: CategoriesEvents,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val actionError = state.actionError?.let { financeErrorText(it) }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbar.showSnackbar(actionError)
            events.onActionErrorShown()
        }
    }
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(stringResource(R.string.finance_categories), onBack) {
                FilledTonalIconButton(onClick = { events.onEdit(null) }) {
                    Icon(
                        painterResource(PrumoIcons.Add),
                        contentDescription = stringResource(R.string.finance_new_category),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.finance_loading))
                state.error != null -> ErrorState(state.error, onRetry = events.onRetry)
                else -> CategoryList(state, events)
            }
        }
    }
}

@Composable
private fun CategoryList(state: CategoriesUiState, events: CategoriesEvents) {
    val shown = state.categories.filter { it.kind == state.kind }
    val archived = state.archived.filter { it.kind == state.kind }
    val month = Formats.month(state.context.today)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.screen)) {
        item {
            ChoiceGroup(
                options = CategoryKind.entries,
                selected = state.kind,
                label = {
                    stringResource(
                        if (it ==
                            CategoryKind.Expense
                        ) {
                            R.string.finance_expenses
                        } else {
                            R.string.finance_income
                        },
                    )
                },
                onSelect = events.onKind,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }
        itemsIndexed(shown, key = { _, category -> category.id }) { index, category ->
            GroupedListItem(
                categoryRow(category, month, state.context, events),
                index = index,
                count = shown.size,
                modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
            )
        }
        if (archived.isNotEmpty()) {
            item {
                GroupedListItem(
                    GroupedRow(
                        stringResource(R.string.finance_archived_categories),
                        RowAction.Click(events.onToggleArchived),
                        summary = pluralStringResource(
                            R.plurals.finance_categories_count,
                            archived.size,
                            archived.size,
                        ),
                        icon = PrumoIcons.Inventory2,
                        neutral = true,
                    ),
                    index = 0,
                    count = 1,
                    modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.md),
                )
            }
            if (state.showArchived) {
                item { SectionHeader(stringResource(R.string.finance_archived_categories)) }
                itemsIndexed(archived, key = { _, category -> "archived-${category.id}" }) { index, category ->
                    GroupedListItem(
                        GroupedRow(
                            title = category.name,
                            summary = pluralStringResource(
                                R.plurals.finance_transactions_count,
                                category.transactionsCount,
                                category.transactionsCount,
                            ),
                            action = RowAction.Static {
                                TextButton(onClick = { events.onArchive(category, false) }) {
                                    Text(stringResource(R.string.finance_unarchive))
                                }
                            },
                            leading = { FinanceAvatar(category.color, category.icon) },
                        ),
                        index = index,
                        count = archived.size,
                        modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
                    )
                }
            }
        }
    }
}

/** A category: "R$ 742,85 em novembro", and its menu (edit, and archive or delete by use: D32 of the server). */
@Composable
private fun categoryRow(
    category: Category,
    month: String,
    context: FinanceContext,
    events: CategoriesEvents,
): GroupedRow {
    val summary = if (category.monthTransactionsCount > 0) {
        stringResource(R.string.finance_category_month, context.money(category.monthTotalCents), month)
    } else {
        stringResource(R.string.finance_category_unused, month)
    }
    val options = buildList {
        add(MenuOption(stringResource(R.string.finance_edit), PrumoIcons.Edit) { events.onEdit(category) })
        if (category.transactionsCount > 0) {
            add(
                MenuOption(stringResource(R.string.finance_archive), PrumoIcons.Archive) {
                    events.onArchive(category, true)
                },
            )
        } else {
            add(MenuOption(stringResource(R.string.finance_delete), PrumoIcons.Delete) { events.onDelete(category) })
        }
    }
    return GroupedRow(
        title = category.name,
        summary = summary,
        action = RowAction.Click(
            onClick = { events.onEdit(category) },
            chevron = false,
            trailing = {
                OverflowMenu(options, label = stringResource(R.string.finance_category_actions, category.name))
            },
        ),
        leading = { FinanceAvatar(category.color, category.icon) },
    )
}

@PhonePreviews
@Composable
private fun CategoriesPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            CategoriesScreen(
                state = CategoriesUiState(
                    loading = false,
                    categories = FinancePreview.expenseCategories + FinancePreview.incomeCategories,
                    archived = listOf(FinancePreview.food.copy(id = "x", name = "Pets", archivedAt = Instant.EPOCH)),
                ),
                onBack = {},
                events = CategoriesEvents({}, {}, { _, _ -> }, {}, {}, {}, {}),
            )
        }
    }
}
