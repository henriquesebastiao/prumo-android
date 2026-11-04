package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.insertSeparators
import androidx.paging.map
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.ErrorCard
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.errorTitle
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionFilter
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An app error carried through Paging, which only takes throwables. */
class PagingError(val error: AppError) : Exception()

/**
 * The pages of transactions of [filter] from the server (offset paging, newest first). The first
 * page also brings the totals of the whole filter, handed to [onTotals].
 */
class TransactionPagingSource(
    private val repository: FinanceRepository,
    private val filter: TransactionFilter,
    private val onTotals: (TransactionTotals) -> Unit,
) : PagingSource<Int, Transaction>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        val offset = params.key ?: 0
        return when (val outcome = repository.transactions(filter, offset, params.loadSize)) {
            is Outcome.Ok -> {
                val page = outcome.value
                if (offset == 0) onTotals(TransactionTotals(page.total, page.incomeCents, page.expenseCents))
                val end = offset + page.items.size
                LoadResult.Page(
                    data = page.items,
                    prevKey = null,
                    nextKey = end.takeIf { it < page.total && page.items.isNotEmpty() },
                )
            }

            is Outcome.Failed -> LoadResult.Error(PagingError(outcome.error))
        }
    }

    /** A refresh starts over from the newest. */
    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? = null
}

/** The count and totals of the filter, from the server (D34 of the server; A6, D16). */
data class TransactionTotals(val count: Int, val incomeCents: Long, val expenseCents: Long)

/** A row of the list: a day's header or a transaction. */
sealed interface TransactionItem {
    data class Day(val date: LocalDate) : TransactionItem

    data class Row(val transaction: Transaction) : TransactionItem
}

/** The filters of the list (board 07): period, account, card, category and kind. */
data class TransactionFilters(
    /** Null is every month. */
    val month: YearMonth? = null,
    val account: Account? = null,
    val card: CreditCard? = null,
    val category: Category? = null,
    val kind: TransactionKind? = null,
) {
    fun toFilter(query: String) = TransactionFilter(
        from = month?.atDay(1),
        to = month?.atEndOfMonth(),
        kind = kind,
        accountId = account?.id,
        creditCardId = card?.id,
        categoryId = category?.id,
        query = query.trim().ifEmpty { null },
    )
}

data class TransactionsUiState(
    val filters: TransactionFilters = TransactionFilters(),
    val totals: TransactionTotals? = null,
    val accounts: List<Account> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val context: FinanceContext = FinanceContext(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val context = account.preferences.value.financeContext(clock)

    /** The search, as typed. */
    var query by mutableStateOf("")
        private set
    private val queries = MutableStateFlow("")
    private val refreshes = MutableStateFlow(0)

    private val mutableState = MutableStateFlow(
        TransactionsUiState(filters = TransactionFilters(month = context.month), context = context),
    )
    val state: StateFlow<TransactionsUiState> = mutableState.asStateFlow()

    val pages: Flow<PagingData<TransactionItem>> = combine(
        mutableState.map { it.filters }.distinctUntilChanged(),
        queries.debounce(SEARCH_DELAY_MS).distinctUntilChanged(),
        refreshes,
    ) { filters, query, _ -> filters.toFilter(query) }
        .flatMapLatest { filter ->
            Pager(PagingConfig(pageSize = FinanceRepository.PAGE_SIZE, enablePlaceholders = false)) {
                TransactionPagingSource(repository, filter) { totals ->
                    mutableState.update { it.copy(totals = totals) }
                }
            }.flow
        }
        .map { paging -> paging.map<Transaction, TransactionItem> { TransactionItem.Row(it) }.byDay() }
        .cachedIn(viewModelScope)

    init {
        loadChoices()
        viewModelScope.launch {
            repository.changes.collect {
                loadChoices()
                refreshes.update { it + 1 }
            }
        }
    }

    /** What the filter chips offer: the active accounts, cards and categories. */
    private fun loadChoices() {
        viewModelScope.launch {
            val accounts = repository.accounts().getOrNull()?.items.orEmpty()
            val cards = repository.cards().getOrNull().orEmpty()
            val categories = repository.categories().getOrNull().orEmpty()
            mutableState.update { it.copy(accounts = accounts, cards = cards, categories = categories) }
        }
    }

    fun onQueryChange(text: String) {
        query = text
        queries.value = text
    }

    fun setFilters(filters: TransactionFilters) {
        mutableState.update { it.copy(filters = filters, totals = null) }
    }

    /**
     * The filter of a destination ("Ver todas" of an account, card or category): every month, only
     * that one. The month, when given, is the category's on the overview.
     */
    fun show(accountId: String?, cardId: String?, categoryId: String?, month: YearMonth? = null) {
        viewModelScope.launch {
            val accounts = repository.accounts().getOrNull()?.items.orEmpty()
            val cards = repository.cards().getOrNull().orEmpty()
            val categories = repository.categories().getOrNull().orEmpty()
            setFilters(
                TransactionFilters(
                    month = month,
                    account = accounts.firstOrNull { it.id == accountId },
                    card = cards.firstOrNull { it.id == cardId },
                    category = categories.firstOrNull { it.id == categoryId },
                ),
            )
        }
    }

    private companion object {
        const val SEARCH_DELAY_MS = 350L
    }
}

/** A day's header before its first transaction. */
private fun PagingData<TransactionItem>.byDay(): PagingData<TransactionItem> = insertSeparators { before, after ->
    val next = (after as? TransactionItem.Row)?.transaction?.occurredOn ?: return@insertSeparators null
    val previous = (before as? TransactionItem.Row)?.transaction?.occurredOn
    if (previous != next) TransactionItem.Day(next) else null
}

/** What the transactions tab does. */
internal class TransactionsEvents(
    val onQueryChange: (String) -> Unit,
    val onFilters: (TransactionFilters) -> Unit,
    val onOpen: (Transaction) -> Unit,
)

/** Transações (board 07): search, filter chips, the filter's totals and the list by day. */
@Composable
internal fun TransactionsTab(
    state: TransactionsUiState,
    query: String,
    items: LazyPagingItems<TransactionItem>,
    events: TransactionsEvents,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = events.onQueryChange,
                placeholder = { Text(stringResource(R.string.finance_search_hint)) },
                leadingIcon = { Icon(painterResource(PrumoIcons.Search), contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { events.onQueryChange("") }) {
                            Icon(
                                painterResource(PrumoIcons.Close),
                                contentDescription = stringResource(R.string.finance_clear),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { FilterChips(state, events.onFilters) }
        item { Totals(state.totals, state.context) }
        transactionItems(items, state.context, events.onOpen)
    }
}

/** Every page of [items]: day headers and segmented rows, then the loading or the error. */
internal fun LazyListScope.transactionItems(
    pages: LazyPagingItems<TransactionItem>,
    context: FinanceContext,
    onOpen: (Transaction) -> Unit,
) {
    val refresh = pages.loadState.refresh
    when {
        refresh is LoadState.Loading && pages.itemCount == 0 -> item {
            LoadingState(stringResource(R.string.finance_loading), Modifier.padding(top = Spacing.xl))
        }

        refresh is LoadState.Error && pages.itemCount == 0 -> item {
            val error = (refresh.error as? PagingError)?.error ?: AppError.Unexpected()
            ErrorCard(
                errorTitle(error),
                financeErrorText(error),
                retryLabel = stringResource(R.string.finance_retry),
                onRetry = pages::retry,
            )
        }

        refresh is LoadState.NotLoading && pages.itemCount == 0 -> item {
            EmptyState(
                icon = PrumoIcons.ReceiptLong,
                title = stringResource(R.string.finance_transactions_empty),
                body = stringResource(R.string.finance_transactions_empty_hint),
            )
        }
    }
    items(count = pages.itemCount, key = { index ->
        when (val item = pages.peekOrNull(index)) {
            is TransactionItem.Day -> "day-${item.date}"
            is TransactionItem.Row -> item.transaction.id
            null -> "placeholder-$index"
        }
    }) { index -> TransactionPageItem(pages, index, context, onOpen) }
    val append = pages.loadState.append
    if (append is LoadState.Error) {
        item {
            val error = (append.error as? PagingError)?.error ?: AppError.Unexpected()
            ErrorCard(
                errorTitle(error),
                financeErrorText(error),
                retryLabel = stringResource(R.string.finance_retry),
                onRetry = pages::retry,
            )
        }
    }
}

/** The [index]th item of [pages]: a day's header, or a row with the corners of its place in the day. */
@Composable
private fun TransactionPageItem(
    pages: LazyPagingItems<TransactionItem>,
    index: Int,
    context: FinanceContext,
    onOpen: (Transaction) -> Unit,
) {
    when (val item = pages[index]) {
        is TransactionItem.Day -> SectionHeader(
            dayTitle(item.date, context),
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
        )

        is TransactionItem.Row -> {
            val first = pages.peekOrNull(index - 1).let { it == null || it is TransactionItem.Day }
            val last = pages.peekOrNull(index + 1).let { it == null || it is TransactionItem.Day }
            val (position, count) = when {
                first && last -> 0 to 1
                first -> 0 to 2
                last -> 1 to 2
                else -> 1 to 3
            }
            GroupedListItem(
                transactionRow(item.transaction, context, onClick = { onOpen(item.transaction) }),
                index = position,
                count = count,
                modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
            )
        }

        null -> Box(Modifier.fillMaxWidth().padding(vertical = 36.dp))
    }
}

private fun LazyPagingItems<TransactionItem>.peekOrNull(index: Int): TransactionItem? =
    if (index in 0 until itemCount) peek(index) else null

/** "Hoje · 17/11", "Ontem · 16/11" or "Sábado, 14 de novembro". */
@Composable
private fun dayTitle(date: LocalDate, context: FinanceContext): String = when (date) {
    context.today -> stringResource(R.string.finance_day_today, Formats.shortDate(date))
    context.today.minusDays(1) -> stringResource(R.string.finance_day_yesterday, Formats.shortDate(date))
    else -> Formats.weekdayAndDate(date)
}

@Composable
private fun Totals(totals: TransactionTotals?, context: FinanceContext) {
    if (totals == null) return
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            pluralStringResource(R.plurals.finance_transactions_count, totals.count, totals.count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            signedAmount(TransactionKind.Income, totals.incomeCents, context.currency),
            style = MaterialTheme.typography.labelLarge,
            color = PrumoTheme.extendedColors.success,
        )
        Text(
            signedAmount(TransactionKind.Expense, totals.expenseCents, context.currency),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FilterChips(state: TransactionsUiState, onFilters: (TransactionFilters) -> Unit) {
    val filters = state.filters
    val today = state.context.month
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        MenuChip(
            label = filters.month?.let(Formats::monthYear) ?: stringResource(R.string.finance_filter_all_months),
            selected = filters.month != null,
            choices =
                listOf(
                    Choice(stringResource(R.string.finance_filter_all_months)) {
                        onFilters(filters.copy(month = null))
                    },
                ) +
                    (0L until MONTHS_OFFERED).map { back ->
                        val month = today.minusMonths(back)
                        Choice(Formats.monthYear(month)) { onFilters(filters.copy(month = month)) }
                    },
        )
        MenuChip(
            label = filters.account?.name ?: stringResource(R.string.finance_filter_account),
            selected = filters.account != null,
            choices =
                listOf(
                    Choice(stringResource(R.string.finance_filter_all_accounts)) {
                        onFilters(filters.copy(account = null))
                    },
                ) +
                    state.accounts.map { Choice(it.name) { onFilters(filters.copy(account = it, card = null)) } },
        )
        if (state.cards.isNotEmpty()) {
            MenuChip(
                label = filters.card?.let { stringResource(R.string.finance_card_named, it.name) }
                    ?: stringResource(R.string.finance_filter_card),
                selected = filters.card != null,
                choices =
                    listOf(
                        Choice(stringResource(R.string.finance_filter_all_cards)) {
                            onFilters(filters.copy(card = null))
                        },
                    ) +
                        state.cards.map { Choice(it.name) { onFilters(filters.copy(card = it, account = null)) } },
            )
        }
        MenuChip(
            label = filters.category?.name ?: stringResource(R.string.finance_filter_category),
            selected = filters.category != null,
            choices =
                listOf(
                    Choice(stringResource(R.string.finance_filter_all_categories)) {
                        onFilters(filters.copy(category = null))
                    },
                ) +
                    state.categories.map { Choice(it.name) { onFilters(filters.copy(category = it)) } },
        )
        MenuChip(
            label = filters.kind?.label() ?: stringResource(R.string.finance_filter_kind),
            selected = filters.kind != null,
            choices = listOf(
                Choice(stringResource(R.string.finance_filter_all_kinds)) { onFilters(filters.copy(kind = null)) },
                Choice(stringResource(R.string.finance_filter_expenses)) {
                    onFilters(filters.copy(kind = TransactionKind.Expense))
                },
                Choice(stringResource(R.string.finance_filter_incomes)) {
                    onFilters(filters.copy(kind = TransactionKind.Income))
                },
                Choice(stringResource(R.string.finance_filter_transfers)) {
                    onFilters(filters.copy(kind = TransactionKind.Transfer))
                },
            ),
        )
    }
}

private const val MONTHS_OFFERED = 12L

@PhonePreviews
@Composable
private fun TransactionsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                val items = remember {
                    flowOf(
                        PagingData.from(
                            FinancePreview.transactions.map<Transaction, TransactionItem> {
                                TransactionItem.Row(it)
                            },
                        ),
                    ).map { it.byDay() }
                }.collectAsLazyPagingItems()
                Column {
                    TransactionsTab(
                        state = TransactionsUiState(
                            filters = TransactionFilters(month = YearMonth.of(2026, 11)),
                            totals = TransactionTotals(65, 3_130_000, 1_999_054),
                            accounts = FinancePreview.accounts,
                        ),
                        query = "",
                        items = items,
                        events = TransactionsEvents({}, {}, {}),
                    )
                }
            }
        }
    }
}
