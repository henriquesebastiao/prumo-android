package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.MoneyTransformation
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionFilter
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

data class AccountDetailUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val account: Account? = null,
    /** The latest transactions of the account, newest first. */
    val latest: List<Transaction> = emptyList(),
    val busy: Boolean = false,
    /** Why the last action (adjust, archive, delete) failed, for a snackbar. */
    val actionError: AppError? = null,
    /** The adjustment dialog (F11). */
    val adjusting: Boolean = false,
    val adjustError: AppError? = null,
    val gone: Boolean = false,
    val context: FinanceContext = FinanceContext(),
)

@HiltViewModel(assistedFactory = AccountDetailViewModel.Factory::class)
class AccountDetailViewModel @AssistedInject constructor(
    @Assisted private val accountId: String,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    /** The real balance typed in the adjustment, as digits, and whether it's negative. */
    var balance by mutableStateOf("")
        private set
    var negative by mutableStateOf(false)
        private set
    var adjustOn by mutableStateOf(LocalDate.of(2026, 11, 17))
        private set

    private val mutableState = MutableStateFlow(
        AccountDetailUiState(context = account.preferences.value.financeContext(clock)),
    )
    val state: StateFlow<AccountDetailUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) load() } }
    }

    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            val latest = repository.transactions(
                TransactionFilter(accountId = accountId),
                limit = LATEST,
            ).getOrNull()?.items
            when (val outcome = repository.account(accountId)) {
                is Outcome.Ok -> mutableState.update {
                    it.copy(loading = false, account = outcome.value, latest = latest.orEmpty())
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun openAdjust() {
        val account = mutableState.value.account ?: return
        balance = Inputs.digitsOf(kotlin.math.abs(account.balanceCents))
        negative = account.balanceCents < 0
        adjustOn = mutableState.value.context.today
        mutableState.update { it.copy(adjusting = true, adjustError = null) }
    }

    fun closeAdjust() {
        mutableState.update { it.copy(adjusting = false) }
    }

    fun onBalanceChange(digits: String) {
        balance = digits
        mutableState.update { it.copy(adjustError = null) }
    }

    fun onNegativeChange(value: Boolean) {
        negative = value
    }

    fun onAdjustDateChange(date: LocalDate) {
        adjustOn = date
    }

    fun adjust() {
        val cents = Inputs.number(balance) ?: return
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, adjustError = null) }
        viewModelScope.launch {
            when (val outcome = repository.adjustBalance(accountId, if (negative) -cents else cents, adjustOn)) {
                is Outcome.Ok -> mutableState.update { it.copy(busy = false, adjusting = false) }
                is Outcome.Failed -> mutableState.update { it.copy(busy = false, adjustError = outcome.error) }
            }
        }
    }

    fun setArchived(archived: Boolean) = act { repository.setAccountArchived(accountId, archived) }

    fun delete() = act(onDone = {
        mutableState.update { it.copy(gone = true) }
    }) { repository.deleteAccount(accountId) }

    fun actionErrorShown() {
        mutableState.update { it.copy(actionError = null) }
    }

    private fun act(onDone: () -> Unit = {}, call: suspend () -> Outcome<*>) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            when (val outcome = call()) {
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
        fun create(accountId: String): AccountDetailViewModel
    }

    private companion object {
        const val LATEST = 10
    }
}

/** What the account screen does. */
internal class AccountDetailEvents(
    val onEdit: () -> Unit,
    val onArchive: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onAdjust: () -> Unit,
    val onSeeAll: () -> Unit,
    val onOpen: (Transaction) -> Unit,
    val onRetry: () -> Unit,
)

/** The adjustment dialog's texts and callbacks. */
internal class AdjustFields(
    val balance: String,
    val negative: Boolean,
    val on: LocalDate,
    val onBalanceChange: (String) -> Unit,
    val onNegativeChange: (Boolean) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
internal fun AccountDetailRoute(
    accountId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel = hiltViewModel<AccountDetailViewModel, AccountDetailViewModel.Factory>(
        creationCallback = { it.create(accountId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    AccountDetailScreen(
        state = state,
        onBack = navigator::back,
        events = AccountDetailEvents(
            onEdit = { navigator.navigate(AccountEditor(accountId)) },
            onArchive = viewModel::setArchived,
            onDelete = viewModel::delete,
            onAdjust = viewModel::openAdjust,
            onSeeAll = { navigator.navigate(FinanceHome(FinanceTab.Transactions, accountId = accountId)) },
            onOpen = { navigator.navigate(TransactionDetail(it.id)) },
            onRetry = viewModel::load,
        ),
        adjust = AdjustFields(
            balance = viewModel.balance,
            negative = viewModel.negative,
            on = viewModel.adjustOn,
            onBalanceChange = viewModel::onBalanceChange,
            onNegativeChange = viewModel::onNegativeChange,
            onDateChange = viewModel::onAdjustDateChange,
            onConfirm = viewModel::adjust,
            onDismiss = viewModel::closeAdjust,
        ),
        onActionErrorShown = viewModel::actionErrorShown,
        modifier = modifier,
    )
}

/** An account (web parity): its balance, data, latest transactions and its actions. */
@Composable
internal fun AccountDetailScreen(
    state: AccountDetailUiState,
    onBack: () -> Unit,
    events: AccountDetailEvents,
    adjust: AdjustFields,
    onActionErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = state.account
    val snackbar = remember { SnackbarHostState() }
    val actionError = state.actionError?.let { financeErrorText(it) }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbar.showSnackbar(actionError)
            onActionErrorShown()
        }
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                title = account?.name.orEmpty(),
                onBack = onBack,
                subtitle = account?.kind?.label(),
                actions = {
                    if (account != null) {
                        IconButton(onClick = events.onEdit) {
                            Icon(
                                painterResource(PrumoIcons.Edit),
                                contentDescription = stringResource(R.string.finance_edit),
                            )
                        }
                        OverflowMenu(accountMenu(account, state.busy, events, onDelete = { confirmDelete = true }))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                account == null && state.loading -> LoadingState(stringResource(R.string.finance_loading))
                account == null -> ErrorState(state.error ?: AppError.Unexpected(), onRetry = events.onRetry)
                else -> AccountContent(account, state, events)
            }
        }
    }
    if (confirmDelete && account != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.finance_account_delete_title),
            body = stringResource(R.string.finance_account_delete_body, account.name),
            confirmLabel = stringResource(R.string.finance_delete),
            onConfirm = {
                confirmDelete = false
                events.onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
    if (state.adjusting && account != null) AdjustDialog(account, state, adjust)
}

@Composable
private fun accountMenu(account: Account, busy: Boolean, events: AccountDetailEvents, onDelete: () -> Unit) =
    buildList {
        add(MenuOption(stringResource(R.string.finance_adjust), PrumoIcons.Tune) { if (!busy) events.onAdjust() })
        if (account.archived) {
            add(
                MenuOption(stringResource(R.string.finance_unarchive), PrumoIcons.Unarchive) {
                    if (!busy) events.onArchive(false)
                },
            )
        } else {
            add(
                MenuOption(stringResource(R.string.finance_archive), PrumoIcons.Archive) {
                    if (!busy) events.onArchive(true)
                },
            )
        }
        // F8: with transactions it can only be archived (D32 of the server).
        if (account.transactionsCount == 0) {
            add(MenuOption(stringResource(R.string.finance_delete), PrumoIcons.Delete) { if (!busy) onDelete() })
        }
    }

@Composable
private fun AccountContent(account: Account, state: AccountDetailUiState, events: AccountDetailEvents) {
    val context = state.context
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            SectionCard(
                tone = SectionTone.Primary,
                hero = true,
                overline = stringResource(R.string.finance_balance_today),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    FinanceAvatar(account.color, account.icon, cookie = true)
                    FitText(context.money(account.balanceCents), style = PrumoType.dataDisplay)
                }
                Text(
                    account.lastTransactionOn?.let {
                        stringResource(R.string.finance_last_movement, dayText(it, context))
                    }
                        ?: stringResource(R.string.finance_no_movement),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (account.archived) StatusChip(stringResource(R.string.finance_archived_one), StatusTone.Neutral)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                KpiTile(
                    context.money(account.initialBalanceCents),
                    stringResource(R.string.finance_initial_balance),
                    Modifier.weight(1f),
                )
                KpiTile(
                    account.monthTransactionsCount.toString(),
                    stringResource(R.string.finance_month_transactions, Formats.month(context.today)),
                    Modifier.weight(1f),
                )
            }
        }
        if (!account.includeInTotal) {
            item { StatusChip(stringResource(R.string.finance_out_of_total), StatusTone.Warning) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(stringResource(R.string.finance_latest), Modifier.weight(1f))
                if (state.latest.isNotEmpty()) {
                    TextButton(onClick = events.onSeeAll) {
                        Text(stringResource(R.string.finance_see_all))
                    }
                }
            }
        }
        if (state.latest.isEmpty()) {
            item { Text(stringResource(R.string.finance_no_transactions), style = MaterialTheme.typography.bodyMedium) }
        }
        itemsIndexed(state.latest, key = { _, transaction -> transaction.id }) { index, transaction ->
            GroupedListItem(
                transactionRow(transaction, context, onClick = { events.onOpen(transaction) }, showDate = true),
                index = index,
                count = state.latest.size,
                modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
            )
        }
    }
}

/** F11: the account's real balance; the server records the difference as an adjustment. */
@Composable
private fun AdjustDialog(account: Account, state: AccountDetailUiState, fields: AdjustFields) {
    AlertDialog(
        onDismissRequest = fields.onDismiss,
        title = { Text(stringResource(R.string.finance_adjust)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    stringResource(R.string.finance_adjust_current, state.context.money(account.balanceCents)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ValueField(
                    digits = fields.balance,
                    onDigitsChange = fields.onBalanceChange,
                    label = stringResource(R.string.finance_adjust_balance),
                    transformation = MoneyTransformation(state.context.currency),
                    error = state.adjustError?.let { financeErrorText(it) },
                )
                FilterChip(
                    selected = fields.negative,
                    onClick = { fields.onNegativeChange(!fields.negative) },
                    label = { Text(stringResource(R.string.finance_negative)) },
                )
                DateField(fields.on, fields.onDateChange, stringResource(R.string.finance_date))
                Text(
                    stringResource(R.string.finance_adjust_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = fields.onConfirm, enabled = fields.balance.isNotEmpty() && !state.busy) {
                Text(stringResource(R.string.finance_adjust_save))
            }
        },
        dismissButton = { TextButton(onClick = fields.onDismiss) { Text(stringResource(R.string.finance_cancel)) } },
    )
}

@PhonePreviews
@Composable
private fun AccountDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            AccountDetailScreen(
                state = AccountDetailUiState(
                    loading = false,
                    account = FinancePreview.nubank,
                    latest = FinancePreview.transactions.take(3),
                ),
                onBack = {},
                events = AccountDetailEvents({}, {}, {}, {}, {}, {}, {}),
                adjust = AdjustFields("", false, LocalDate.of(2026, 11, 17), {}, {}, {}, {}, {}),
                onActionErrorShown = {},
            )
        }
    }
}
