package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedList
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.SeededAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Invoice
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
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

data class TransactionDetailUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val transaction: Transaction? = null,
    /** The invoice of a card purchase, with its status (A10, D16). */
    val invoice: Invoice? = null,
    val deleting: Boolean = false,
    val deleteError: AppError? = null,
    val gone: Boolean = false,
    val context: FinanceContext = FinanceContext(),
)

@HiltViewModel(assistedFactory = TransactionDetailViewModel.Factory::class)
class TransactionDetailViewModel @AssistedInject constructor(
    @Assisted private val transactionId: String,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        TransactionDetailUiState(context = account.preferences.value.financeContext(clock)),
    )
    val state: StateFlow<TransactionDetailUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) load() } }
    }

    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            when (val outcome = repository.transaction(transactionId)) {
                is Outcome.Ok -> {
                    val transaction = outcome.value
                    val card = transaction.creditCard
                    val closing = transaction.invoiceClosingOn
                    val invoice = if (card != null &&
                        closing != null
                    ) {
                        repository.invoice(card.id, closing).getOrNull()?.invoice
                    } else {
                        null
                    }
                    mutableState.update { it.copy(loading = false, transaction = transaction, invoice = invoice) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    /** [all] removes every installment of the purchase (F10). */
    fun delete(all: Boolean) {
        if (mutableState.value.deleting) return
        mutableState.update { it.copy(deleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val outcome = repository.deleteTransaction(transactionId, all)) {
                is Outcome.Ok -> mutableState.update { it.copy(deleting = false, gone = true) }
                is Outcome.Failed -> mutableState.update { it.copy(deleting = false, deleteError = outcome.error) }
            }
        }
    }

    fun deleteErrorShown() = mutableState.update { it.copy(deleteError = null) }

    @AssistedFactory
    interface Factory {
        fun create(transactionId: String): TransactionDetailViewModel
    }
}

@Composable
internal fun TransactionDetailRoute(
    transactionId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailViewModel =
        hiltViewModel<TransactionDetailViewModel, TransactionDetailViewModel.Factory>(
            creationCallback = { it.create(transactionId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    TransactionDetailScreen(
        state = state,
        onBack = navigator::back,
        onEdit = { navigator.navigate(TransactionEditor(transactionId = transactionId)) },
        onDelete = viewModel::delete,
        onCard = { navigator.navigate(CardDetail(it)) },
        onRetry = viewModel::load,
        onDeleteErrorShown = viewModel::deleteErrorShown,
        modifier = modifier,
    )
}

/** A transaction (web parity): its value, day, category, account or card, invoice and notes. */
@Composable
internal fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onCard: (String) -> Unit,
    onRetry: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transaction = state.transaction
    val snackbar = remember { SnackbarHostState() }
    val deleteError = state.deleteError?.let { financeErrorText(it) }
    LaunchedEffect(deleteError) {
        if (deleteError != null) {
            snackbar.showSnackbar(deleteError)
            onDeleteErrorShown()
        }
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                title = transaction?.title().orEmpty(),
                onBack = onBack,
                subtitle = transaction?.kind?.label(),
                actions = {
                    if (transaction != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                painterResource(PrumoIcons.Edit),
                                contentDescription = stringResource(R.string.finance_edit),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }, enabled = !state.deleting) {
                            Icon(
                                painterResource(PrumoIcons.Delete),
                                contentDescription = stringResource(R.string.finance_delete),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                transaction == null && state.loading -> LoadingState(stringResource(R.string.finance_loading))
                transaction == null -> ErrorState(state.error ?: AppError.Unexpected(), onRetry = onRetry)
                else -> TransactionContent(transaction, state, onCard)
            }
        }
    }
    if (confirmDelete && transaction != null) {
        DeleteDialog(
            transaction,
            onDelete = { all ->
                confirmDelete = false
                onDelete(all)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Confirms the delete; an installment asks whether it's only this one or the whole purchase (F10). */
@Composable
private fun DeleteDialog(transaction: Transaction, onDelete: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val number = transaction.installmentNumber
    val count = transaction.installmentCount
    if (number == null || count == null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.finance_transaction_delete_title),
            body = stringResource(R.string.finance_transaction_delete_body, transaction.title()),
            confirmLabel = stringResource(R.string.finance_delete),
            onConfirm = { onDelete(false) },
            onDismiss = onDismiss,
            destructive = true,
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(PrumoIcons.Delete), contentDescription = null) },
        title = { Text(stringResource(R.string.finance_transaction_delete_title)) },
        text = { Text(stringResource(R.string.finance_delete_installments, number, count)) },
        confirmButton = {
            TextButton(onClick = { onDelete(true) }) {
                Text(stringResource(R.string.finance_delete_all_installments), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDelete(false) }) {
                Text(stringResource(R.string.finance_delete_one_installment), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun TransactionContent(transaction: Transaction, state: TransactionDetailUiState, onCard: (String) -> Unit) {
    val context = state.context
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screen)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionCard(tone = SectionTone.Container, hero = true) {
            SeededAvatar(transaction.seed(), transaction.icon(), cookie = true, size = 56.dp)
            Text(
                signedAmount(transaction.kind, transaction.amountCents, context.currency),
                style = PrumoType.dataDisplay,
                color = amountColor(transaction.kind, transaction.amountCents),
            )
            val day = Formats.date(transaction.occurredOn)
            Text(
                transaction.occurredTime?.let { day + SEPARATOR + Formats.time(it) } ?: day,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (transaction.occurredOn.isAfter(context.today)) {
                StatusChip(stringResource(R.string.finance_scheduled), StatusTone.Neutral)
            }
        }
        GroupedList(detailRows(transaction, state, onCard))
        transaction.notes?.let { notes ->
            SectionCard(
                tone = SectionTone.Container,
                icon = PrumoIcons.Notes,
                overline = stringResource(R.string.finance_notes),
            ) {
                Text(notes, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun detailRows(
    transaction: Transaction,
    state: TransactionDetailUiState,
    onCard: (String) -> Unit,
): List<GroupedRow> {
    val rows = mutableListOf<GroupedRow>()
    transaction.category?.let {
        rows +=
            GroupedRow(
                stringResource(R.string.finance_category),
                RowAction.Static {
                    Text(it.name)
                },
                leading = { FinanceAvatar(it.color, it.icon) },
            )
    }
    val account = transaction.account
    val destination = transaction.destinationAccount
    when {
        account != null && destination != null -> {
            rows +=
                GroupedRow(
                    stringResource(R.string.finance_from),
                    RowAction.Static {
                        Text(account.name)
                    },
                    icon = PrumoIcons.AccountBalance,
                )
            rows +=
                GroupedRow(
                    stringResource(R.string.finance_to),
                    RowAction.Static {
                        Text(destination.name)
                    },
                    icon = PrumoIcons.AccountBalance,
                )
        }

        account != null -> rows += GroupedRow(
            stringResource(R.string.finance_account),
            RowAction.Static { Text(account.name) },
            icon = PrumoIcons.AccountBalance,
        )
    }
    val card = transaction.creditCard
    if (card != null) {
        rows += GroupedRow(
            stringResource(R.string.finance_card),
            RowAction.Click({ onCard(card.id) }, trailing = { Text(card.name) }),
            icon = PrumoIcons.CreditCard,
        )
    }
    val number = transaction.installmentNumber
    val count = transaction.installmentCount
    if (number != null && count != null) {
        rows += GroupedRow(
            stringResource(R.string.finance_installment_label),
            RowAction.Static { Text(stringResource(R.string.finance_installment_of, number, count)) },
            icon = PrumoIcons.CalendarMonth,
        )
    }
    val closing = transaction.invoiceClosingOn
    if (card != null && closing != null && transaction.kind == TransactionKind.Expense) {
        val invoice = state.invoice
        rows += GroupedRow(
            invoiceTitle(invoice?.dueOn ?: closing),
            RowAction.Click(
                { onCard(card.id) },
                trailing = invoice?.let { { StatusChip(it.status.label(), it.status.tone(), small = true) } },
            ),
            summary = invoice?.let { invoiceDates(it, state.context) },
            icon = PrumoIcons.ReceiptLong,
        )
    }
    return rows
}

@PhonePreviews
@Composable
private fun TransactionDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            TransactionDetailScreen(
                state = TransactionDetailUiState(
                    loading = false,
                    transaction = FinancePreview.installment,
                    invoice = FinancePreview.invoice,
                ),
                onBack = {},
                onEdit = {},
                onDelete = {},
                onCard = {},
                onRetry = {},
                onDeleteErrorShown = {},
            )
        }
    }
}
