package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
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
import com.henriquesebastiao.prumo.core.designsystem.component.PeriodSelector
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
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
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.Invoice
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceDetail
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardDetailUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val card: CreditCard? = null,
    /** Every invoice of the card, oldest first, as the arrows go. */
    val invoices: List<Invoice> = emptyList(),
    /** The invoice shown; the current one at first. */
    val closingOn: LocalDate? = null,
    val detail: InvoiceDetail? = null,
    val detailError: AppError? = null,
    val busy: Boolean = false,
    val actionError: AppError? = null,
    val gone: Boolean = false,
    val context: FinanceContext = FinanceContext(),
) {
    val index: Int get() = invoices.indexOfFirst { it.closingOn == closingOn }
}

@HiltViewModel(assistedFactory = CardDetailViewModel.Factory::class)
class CardDetailViewModel @AssistedInject constructor(
    @Assisted private val cardId: String,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(CardDetailUiState(context = account.preferences.value.financeContext(clock)))
    val state: StateFlow<CardDetailUiState> = mutableState.asStateFlow()
    private var detailJob: Job? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) load() } }
    }

    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            val card = repository.card(cardId)
            val invoices = repository.invoices(cardId)
            when {
                card is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = card.error) }

                invoices is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = invoices.error) }

                else -> {
                    val value = (card as Outcome.Ok).value
                    val list = (invoices as Outcome.Ok).value.sortedBy { it.closingOn }
                    val shown = mutableState.value.closingOn?.takeIf { day -> list.any { it.closingOn == day } }
                        ?: value.currentInvoice.closingOn
                    mutableState.update { it.copy(loading = false, card = value, invoices = list, closingOn = shown) }
                    loadInvoice(shown)
                }
            }
        }
    }

    /** Goes [step] invoices back (−1) or forward (+1). */
    fun move(step: Int) {
        val current = mutableState.value
        val target = current.invoices.getOrNull(current.index + step) ?: return
        mutableState.update { it.copy(closingOn = target.closingOn) }
        loadInvoice(target.closingOn)
    }

    private fun loadInvoice(closingOn: LocalDate) {
        detailJob?.cancel()
        mutableState.update { it.copy(detailError = null) }
        detailJob = viewModelScope.launch {
            when (val outcome = repository.invoice(cardId, closingOn)) {
                is Outcome.Ok -> mutableState.update { it.copy(detail = outcome.value) }
                is Outcome.Failed -> mutableState.update { it.copy(detailError = outcome.error) }
            }
        }
    }

    fun setArchived(archived: Boolean) = act { repository.setCardArchived(cardId, archived) }

    fun delete() = act(onDone = { mutableState.update { it.copy(gone = true) } }) { repository.deleteCard(cardId) }

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
        fun create(cardId: String): CardDetailViewModel
    }
}

/** What the card screen does. */
internal class CardDetailEvents(
    val onEdit: () -> Unit,
    val onArchive: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onMove: (Int) -> Unit,
    val onPay: (Invoice) -> Unit,
    val onOpen: (Transaction) -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal fun CardDetailRoute(
    cardId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CardDetailViewModel = hiltViewModel<CardDetailViewModel, CardDetailViewModel.Factory>(
        creationCallback = { it.create(cardId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<FinanceSheet?>(null) }
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    CardDetailScreen(
        state = state,
        onBack = navigator::back,
        events = CardDetailEvents(
            onEdit = { navigator.navigate(CardEditor(cardId)) },
            onArchive = viewModel::setArchived,
            onDelete = viewModel::delete,
            onMove = viewModel::move,
            onPay = { sheet = FinanceSheet.PayInvoice(cardId, it.closingOn) },
            onOpen = { navigator.navigate(TransactionDetail(it.id)) },
            onRetry = viewModel::load,
        ),
        onActionErrorShown = viewModel::actionErrorShown,
        modifier = modifier,
    )
    FinanceSheetHost(sheet, onDismiss = { sheet = null })
}

/** A card (board 13): its invoices one at a time, with the arrows, the payment and the purchases. */
@Composable
internal fun CardDetailScreen(
    state: CardDetailUiState,
    onBack: () -> Unit,
    events: CardDetailEvents,
    onActionErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = state.card
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
                title = card?.let { stringResource(R.string.finance_card_named, it.name) }.orEmpty(),
                onBack = onBack,
                subtitle = card?.let { stringResource(R.string.finance_card_days, it.closingDay, it.dueDay) },
                actions = {
                    if (card != null) {
                        IconButton(onClick = events.onEdit) {
                            Icon(
                                painterResource(PrumoIcons.Edit),
                                contentDescription = stringResource(R.string.finance_edit),
                            )
                        }
                        OverflowMenu(cardMenu(card, state.busy, events, onDelete = { confirmDelete = true }))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                card == null && state.loading -> LoadingState(stringResource(R.string.finance_loading))
                card == null -> ErrorState(state.error ?: AppError.Unexpected(), onRetry = events.onRetry)
                else -> CardContent(state, events)
            }
        }
    }
    if (confirmDelete && card != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.finance_card_delete_title),
            body = stringResource(R.string.finance_card_delete_body, card.name),
            confirmLabel = stringResource(R.string.finance_delete),
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
private fun cardMenu(card: CreditCard, busy: Boolean, events: CardDetailEvents, onDelete: () -> Unit) = buildList {
    if (card.archived) {
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
    if (card.transactionsCount == 0) {
        add(MenuOption(stringResource(R.string.finance_delete), PrumoIcons.Delete) { if (!busy) onDelete() })
    }
}

@Composable
private fun CardContent(state: CardDetailUiState, events: CardDetailEvents) {
    val context = state.context
    val invoice = state.invoices.getOrNull(state.index) ?: state.card?.currentInvoice ?: return
    val detail = state.detail?.takeIf { it.invoice.closingOn == invoice.closingOn }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            PeriodSelector(
                label = invoiceTitle(invoice.dueOn).replaceFirstChar { it.titlecase() },
                previousLabel = stringResource(R.string.finance_previous_invoice),
                nextLabel = stringResource(R.string.finance_next_invoice),
                onPrevious = if (state.index > 0) ({ events.onMove(-1) }) else null,
                onNext = if (state.index < state.invoices.lastIndex) ({ events.onMove(1) }) else null,
                trailing = { StatusChip(invoice.status.label(), invoice.status.tone(), small = true) },
            )
        }
        item {
            SectionCard(tone = SectionTone.Container) {
                FitText(context.money(invoice.amountCents), style = PrumoType.dataDisplay)
                Text(
                    stringResource(
                        R.string.finance_invoice_period,
                        Formats.shortDate(invoice.startsOn),
                        Formats.shortDate(invoice.endsOn),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    KpiTile(
                        dayText(invoice.closingOn, context),
                        stringResource(R.string.finance_closes),
                        Modifier.weight(1f),
                    )
                    KpiTile(Formats.date(invoice.dueOn), stringResource(R.string.finance_due), Modifier.weight(1f))
                }
                if (invoice.paidCents > 0) {
                    Text(
                        stringResource(
                            R.string.finance_paid_so_far,
                            context.money(invoice.paidCents),
                            context.money(invoice.remainingCents),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (invoice.remainingCents > 0) {
                    Button(onClick = { events.onPay(invoice) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.finance_pay_invoice))
                    }
                }
            }
        }
        item {
            SectionHeader(
                stringResource(R.string.finance_entries),
                trailing = pluralStringResource(
                    R.plurals.finance_entries_count,
                    invoice.transactionsCount,
                    invoice.transactionsCount,
                ),
            )
        }
        val error = state.detailError
        when {
            error != null -> item { Text(financeErrorText(error), color = MaterialTheme.colorScheme.error) }

            detail == null -> item {
                LoadingState(stringResource(R.string.finance_loading), Modifier.padding(Spacing.xl))
            }

            detail.transactions.isEmpty() -> item {
                Text(stringResource(R.string.finance_no_entries), style = MaterialTheme.typography.bodyMedium)
            }
        }
        val transactions = detail?.transactions.orEmpty()
        itemsIndexed(transactions, key = { _, transaction -> transaction.id }) { index, transaction ->
            GroupedListItem(
                transactionRow(transaction, context, onClick = { events.onOpen(transaction) }, showDate = true),
                index = index,
                count = transactions.size,
                modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
            )
        }
        val payments = detail?.payments.orEmpty()
        if (payments.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.finance_payments), Modifier.padding(top = Spacing.md)) }
            itemsIndexed(payments, key = { _, payment -> payment.id }) { index, payment ->
                GroupedListItem(
                    transactionRow(payment, context, onClick = { events.onOpen(payment) }, showDate = true),
                    index = index,
                    count = payments.size,
                    modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
                )
            }
        }
    }
}

@PhonePreviews
@Composable
private fun CardDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            CardDetailScreen(
                state = CardDetailUiState(
                    loading = false,
                    card = FinancePreview.card,
                    invoices = listOf(FinancePreview.paidInvoice, FinancePreview.invoice),
                    closingOn = FinancePreview.invoice.closingOn,
                    detail = InvoiceDetail(
                        FinancePreview.invoice,
                        FinancePreview.card.ref,
                        FinancePreview.transactions.filter { it.creditCard != null },
                        emptyList(),
                    ),
                ),
                onBack = {},
                events = CardDetailEvents({}, {}, {}, {}, {}, {}, {}),
                onActionErrorShown = {},
            )
        }
    }
}
