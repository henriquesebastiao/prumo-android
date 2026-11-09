package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PillTabs
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import kotlinx.coroutines.flow.flowOf

/** What each tab of the home shows. */
internal class FinanceTabs(
    val overview: OverviewUiState,
    val transactions: TransactionsUiState,
    val query: String,
    val pages: LazyPagingItems<TransactionItem>,
    val accounts: AccountsUiState,
)

/** What each tab does. */
internal class FinanceTabEvents(
    val overview: OverviewEvents,
    val transactions: TransactionsEvents,
    val accounts: AccountsEvents,
    val cards: CardsEvents,
)

@Composable
internal fun FinanceHomeRoute(
    key: FinanceHome,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    overview: OverviewViewModel = hiltViewModel(),
    transactions: TransactionsViewModel = hiltViewModel(),
    accounts: AccountsViewModel = hiltViewModel<AccountsViewModel, AccountsViewModel.Factory>(
        key = "active",
        creationCallback = { it.create(false) },
    ),
) {
    var tab by rememberSaveable { mutableStateOf(key.tab) }
    var sheet by rememberSaveable { mutableStateOf<FinanceSheet?>(null) }
    // A destination with a filter ("Ver todas" of an account): applied once, not again after a rotation.
    var applied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(key) {
        val filtered = listOfNotNull(key.accountId, key.cardId, key.categoryId).isNotEmpty()
        if (!applied && filtered) {
            transactions.show(key.accountId, key.cardId, key.categoryId)
        }
        applied = true
    }
    val overviewState by overview.state.collectAsStateWithLifecycle()
    val transactionsState by transactions.state.collectAsStateWithLifecycle()
    val accountsState by accounts.state.collectAsStateWithLifecycle()
    val pages = transactions.pages.collectAsLazyPagingItems()
    FinanceHomeScreen(
        tab = tab,
        onTab = { tab = it },
        tabs = FinanceTabs(overviewState, transactionsState, transactions.query, pages, accountsState),
        events = FinanceTabEvents(
            overview = OverviewEvents(
                onPreviousMonth = { overview.shiftMonth(-1) },
                onNextMonth = { overview.shiftMonth(1) },
                onRetry = overview::load,
                onAddAccount = { navigator.navigate(AccountEditor()) },
                onCategory = { share ->
                    transactions.show(null, null, share.category.id, overviewState.month)
                    tab = FinanceTab.Transactions
                },
                onPay = { sheet = FinanceSheet.PayInvoice(it.card.id, it.invoice.closingOn) },
                onAccounts = { tab = FinanceTab.Accounts },
            ),
            transactions = TransactionsEvents(
                onQueryChange = transactions::onQueryChange,
                onFilters = transactions::setFilters,
                onOpen = { navigator.navigate(TransactionDetail(it.id)) },
            ),
            accounts = AccountsEvents(
                onOpen = { navigator.navigate(AccountDetail(it.id)) },
                onAdd = { navigator.navigate(AccountEditor()) },
                onArchived = { navigator.navigate(ArchivedAccounts) },
                onRetry = accounts::load,
            ),
            cards = CardsEvents(
                onOpen = { navigator.navigate(CardDetail(it.id)) },
                onAdd = { navigator.navigate(CardEditor()) },
                onArchived = { navigator.navigate(ArchivedAccounts) },
                onRetry = accounts::load,
            ),
        ),
        actions = FinanceActions(
            onExpense = { navigator.navigate(TransactionEditor(EntryKind.Expense)) },
            onIncome = { navigator.navigate(TransactionEditor(EntryKind.Income)) },
            onTransfer = { navigator.navigate(TransactionEditor(EntryKind.Transfer)) },
            onPayInvoice = { sheet = FinanceSheet.PayInvoice() },
        ),
        onBack = navigator::back,
        onCategories = { navigator.navigate(FinanceCategories) },
        modifier = modifier,
    )
    FinanceSheetHost(sheet, onDismiss = { sheet = null })
}

/** Finanças (board 01): the tabs in pills, Categorias and the search at the top, the toolbar below. */
@Composable
internal fun FinanceHomeScreen(
    tab: FinanceTab,
    onTab: (FinanceTab) -> Unit,
    tabs: FinanceTabs,
    events: FinanceTabEvents,
    actions: FinanceActions,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The large title (board 01) collapses into the small bar as a tab's list scrolls (board 02).
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            PrumoTopBar(stringResource(R.string.finance_name), onBack, large = true, scrollBehavior = scroll) {
                IconButton(onClick = onCategories) {
                    Icon(
                        painterResource(PrumoIcons.Category),
                        contentDescription = stringResource(R.string.finance_categories),
                    )
                }
                IconButton(onClick = { onTab(FinanceTab.Transactions) }) {
                    Icon(
                        painterResource(PrumoIcons.Search),
                        contentDescription = stringResource(R.string.finance_search),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                PillTabs(
                    options = FinanceTab.entries,
                    selected = tab,
                    label = { it.label() },
                    onSelect = onTab,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
                )
                val content = Modifier.weight(1f)
                when (tab) {
                    FinanceTab.Overview -> OverviewTab(tabs.overview, events.overview, content)

                    FinanceTab.Transactions -> TransactionsTab(
                        tabs.transactions,
                        tabs.query,
                        tabs.pages,
                        events.transactions,
                        content,
                    )

                    FinanceTab.Accounts -> AccountsTab(tabs.accounts, events.accounts, content)

                    FinanceTab.Cards -> CardsTab(tabs.accounts, events.cards, content)
                }
            }
            FinanceToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
        }
    }
}

@Composable
private fun FinanceTab.label(): String = stringResource(
    when (this) {
        FinanceTab.Overview -> R.string.finance_tab_overview
        FinanceTab.Transactions -> R.string.finance_tab_transactions
        FinanceTab.Accounts -> R.string.finance_tab_accounts
        FinanceTab.Cards -> R.string.finance_tab_cards
    },
)

@PhonePreviews
@Composable
private fun FinanceHomePreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            FinanceHomeScreen(
                tab = FinanceTab.Overview,
                onTab = {},
                tabs = FinanceTabs(
                    overview = OverviewUiState(loading = false, overview = FinancePreview.overview),
                    transactions = TransactionsUiState(),
                    query = "",
                    pages = flowOf(PagingData.empty<TransactionItem>()).collectAsLazyPagingItems(),
                    accounts = AccountsUiState(loading = false, accounts = FinancePreview.accounts),
                ),
                events = FinanceTabEvents(
                    OverviewEvents({}, {}, {}, {}, {}, {}, {}),
                    TransactionsEvents({}, {}, {}),
                    AccountsEvents({}, {}, {}, {}),
                    CardsEvents({}, {}, {}, {}),
                ),
                actions = FinanceActions({}, {}, {}, {}),
                onBack = {},
                onCategories = {},
            )
        }
    }
}
