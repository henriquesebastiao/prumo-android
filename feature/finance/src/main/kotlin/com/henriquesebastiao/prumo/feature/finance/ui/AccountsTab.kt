package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.RowValue
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val accounts: List<Account> = emptyList(),
    val totalBalanceCents: Long = 0,
    val cards: List<CreditCard> = emptyList(),
    val context: FinanceContext = FinanceContext(),
)

/** The accounts and cards: active ones for the tabs, or the archived ones ([archived]). */
@HiltViewModel(assistedFactory = AccountsViewModel.Factory::class)
class AccountsViewModel @AssistedInject constructor(
    @Assisted private val archived: Boolean,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(AccountsUiState(context = account.preferences.value.financeContext(clock)))
    val state: StateFlow<AccountsUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load() } }
    }

    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            val accounts = repository.accounts(archived)
            val cards = repository.cards(archived)
            mutableState.update { state ->
                when {
                    accounts is Outcome.Failed -> state.copy(loading = false, error = accounts.error)

                    cards is Outcome.Failed -> state.copy(loading = false, error = cards.error)

                    else -> state.copy(
                        loading = false,
                        accounts = (accounts as Outcome.Ok).value.items,
                        totalBalanceCents = accounts.value.totalBalanceCents,
                        cards = (cards as Outcome.Ok).value,
                    )
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(archived: Boolean): AccountsViewModel
    }
}

/** What the accounts tab does. */
internal class AccountsEvents(
    val onOpen: (Account) -> Unit,
    val onAdd: () -> Unit,
    val onArchived: () -> Unit,
    val onRetry: () -> Unit,
)

/** An account: its avatar, kind ("fora do total" when out of F2) and balance (F1). */
@Composable
internal fun accountRow(account: Account, context: FinanceContext, onClick: () -> Unit): GroupedRow {
    val kind = account.kind.label()
    return GroupedRow(
        title = account.name,
        summary = if (account.includeInTotal) {
            kind
        } else {
            kind + SEPARATOR + stringResource(
                R.string.finance_out_of_total,
            )
        },
        action = RowAction.Click(
            onClick = onClick,
            chevron = false,
            trailing = { RowValue(context.money(account.balanceCents)) },
        ),
        leading = { FinanceAvatar(account.color, account.icon) },
    )
}

/** Contas (board 05): the accounts, the total balance, a note and the archived ones. */
@Composable
internal fun AccountsTab(state: AccountsUiState, events: AccountsEvents, modifier: Modifier = Modifier) {
    when {
        state.loading -> LoadingState(stringResource(R.string.finance_loading), modifier)

        state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
        ) {
            if (state.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.AccountBalance,
                        title = stringResource(R.string.finance_accounts_empty),
                        body = stringResource(R.string.finance_accounts_empty_hint),
                    )
                }
            }
            itemsIndexed(state.accounts, key = { _, account -> account.id }) { index, account ->
                GroupedListItem(
                    accountRow(account, state.context) { events.onOpen(account) },
                    index = index,
                    count = state.accounts.size,
                    modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
                )
            }
            if (state.accounts.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.finance_total_balance),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        FitText(state.context.money(state.totalBalanceCents), style = PrumoType.dataMedium)
                    }
                }
                item { GroupNote(stringResource(R.string.finance_accounts_note)) }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = Spacing.md), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = events.onAdd) {
                        Icon(painterResource(PrumoIcons.Add), contentDescription = null)
                        Text(stringResource(R.string.finance_new_account), Modifier.padding(start = Spacing.sm))
                    }
                }
            }
            item {
                GroupedListItem(
                    GroupedRow(
                        stringResource(R.string.finance_archived),
                        RowAction.Click(events.onArchived),
                        icon = PrumoIcons.Inventory2,
                        neutral = true,
                    ),
                    index = 0,
                    count = 1,
                )
            }
        }
    }
}

/** The archived accounts and cards: each opens its screen, where "Reativar" is. */
@Composable
internal fun ArchivedRoute(
    onBack: () -> Unit,
    onAccount: (Account) -> Unit,
    onCard: (CreditCard) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel<AccountsViewModel, AccountsViewModel.Factory>(
        key = "archived",
        creationCallback = { it.create(true) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ArchivedScreen(state, onBack, onAccount, onCard, viewModel::load, modifier)
}

@Composable
internal fun ArchivedScreen(
    state: AccountsUiState,
    onBack: () -> Unit,
    onAccount: (Account) -> Unit,
    onCard: (CreditCard) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier, topBar = { PrumoTopBar(stringResource(R.string.finance_archived), onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.finance_loading))

                state.error != null -> ErrorState(state.error, onRetry = onRetry)

                state.accounts.isEmpty() && state.cards.isEmpty() -> EmptyState(
                    icon = PrumoIcons.Inventory2,
                    title = stringResource(R.string.finance_archived_empty),
                )

                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.screen)) {
                    itemsIndexed(state.accounts, key = { _, account -> account.id }) { index, account ->
                        GroupedListItem(
                            accountRow(account, state.context) { onAccount(account) },
                            index = index,
                            count = state.accounts.size,
                            modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
                        )
                    }
                    itemsIndexed(state.cards, key = { _, card -> card.id }) { index, card ->
                        GroupedListItem(
                            GroupedRow(
                                title = card.name,
                                summary = stringResource(R.string.finance_card_days, card.closingDay, card.dueDay),
                                action = RowAction.Click({ onCard(card) }),
                                leading = { FinanceAvatar(card.color, "credit-card") },
                            ),
                            index = index,
                            count = state.cards.size,
                            modifier = Modifier.padding(
                                top = if (index ==
                                    0
                                ) {
                                    Spacing.lg
                                } else {
                                    0.dp
                                },
                                bottom = ListStyle.Segmented.gap,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun AccountsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                AccountsTab(
                    state = AccountsUiState(
                        loading = false,
                        accounts = FinancePreview.accounts,
                        totalBalanceCents = 715_427,
                    ),
                    events = AccountsEvents({}, {}, {}, {}),
                )
            }
        }
    }
}
