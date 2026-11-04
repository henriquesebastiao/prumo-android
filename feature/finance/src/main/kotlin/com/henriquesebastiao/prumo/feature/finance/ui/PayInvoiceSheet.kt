package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
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
import com.henriquesebastiao.prumo.feature.finance.model.OpenInvoice
import com.henriquesebastiao.prumo.feature.finance.model.PaymentInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PayInvoiceUiState(
    val request: Long? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    /** The invoices that can be paid: one, or the open ones to choose from (from the toolbar). */
    val invoices: List<OpenInvoice> = emptyList(),
    val selected: Int = 0,
    val total: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val accountId: String? = null,
    val on: LocalDate = LocalDate.of(2026, 11, 17),
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: FinanceContext = FinanceContext(),
) {
    val invoice: OpenInvoice? get() = invoices.getOrNull(selected)
}

@HiltViewModel
class PayInvoiceViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    /** The amount of a partial payment, as digits of cents. */
    var amount by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(PayInvoiceUiState())
    val state: StateFlow<PayInvoiceUiState> = mutableState.asStateFlow()
    private var wanted: Pair<String?, LocalDate?> = null to null

    /** A new opening of the sheet; the same one again (a rotation) keeps what was typed. */
    fun start(request: Long, cardId: String?, closingOn: LocalDate?) {
        if (mutableState.value.request == request) return
        amount = ""
        wanted = cardId to closingOn
        val context = account.preferences.value.financeContext(clock)
        mutableState.value = PayInvoiceUiState(request = request, on = context.today, context = context)
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val (cardId, closingOn) = wanted
            val accounts = repository.accounts().getOrNull()?.items.orEmpty()
            val invoices = if (cardId != null && closingOn != null) {
                val card = repository.card(cardId)
                val detail = repository.invoice(cardId, closingOn)
                if (card is Outcome.Ok && detail is Outcome.Ok) {
                    Outcome.Ok(
                        listOf(
                            OpenInvoice(
                                card.value.ref,
                                card.value.limitCents,
                                card.value.availableLimitCents,
                                detail.value.invoice,
                            ),
                        ),
                    )
                } else {
                    (card as? Outcome.Failed) ?: (detail as Outcome.Failed)
                }
            } else {
                when (val overview = repository.overview()) {
                    is Outcome.Ok -> Outcome.Ok(overview.value.openInvoices.filter { it.invoice.remainingCents > 0 })
                    is Outcome.Failed -> overview
                }
            }
            when (invoices) {
                is Outcome.Ok -> {
                    val cards = repository.cards().getOrNull().orEmpty()
                    mutableState.update { state ->
                        val first = invoices.value.firstOrNull()
                        val preferred = cards.firstOrNull { it.id == first?.card?.id }?.paymentAccount?.id
                        state.copy(
                            loading = false,
                            invoices = invoices.value,
                            accounts = accounts,
                            accountId = preferred ?: accounts.firstOrNull { it.id == repository.lastAccountId }?.id
                                ?: accounts.firstOrNull()?.id,
                        )
                    }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = invoices.error) }
            }
        }
    }

    fun select(index: Int) = mutableState.update { it.copy(selected = index, saveError = null) }

    fun onTotalChange(total: Boolean) {
        if (!total && amount.isEmpty()) amount = Inputs.digitsOf(mutableState.value.invoice?.invoice?.remainingCents)
        mutableState.update { it.copy(total = total, saveError = null) }
    }

    fun onAmountChange(digits: String) {
        amount = digits
        mutableState.update { it.copy(saveError = null) }
    }

    fun onAccountChange(id: String) = mutableState.update { it.copy(accountId = id) }

    fun onDateChange(date: LocalDate) = mutableState.update { it.copy(on = date) }

    /** The amount paid: the invoice's remaining (Total) or what was typed (Parcial). */
    val paying: Long?
        get() {
            val current = mutableState.value
            return if (current.total) current.invoice?.invoice?.remainingCents else Inputs.number(amount)
        }

    fun pay() {
        val current = mutableState.value
        val open = current.invoice ?: return
        val accountId = current.accountId ?: return
        val cents = paying?.takeIf { it > 0 } ?: return
        if (current.saving) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (
                val outcome = repository.payInvoice(
                    open.card.id,
                    open.invoice.closingOn,
                    PaymentInput(cents, accountId, current.on),
                )
            ) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }
}

/** What the payment sheet does. */
internal class PayInvoiceEvents(
    val onSelect: (Int) -> Unit,
    val onTotalChange: (Boolean) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onAccountChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onPay: () -> Unit,
    val onRetry: () -> Unit,
)

/** "Pagar fatura" (board 14) over the current screen. */
@Composable
internal fun PayInvoiceSheet(
    cardId: String?,
    closingOn: LocalDate?,
    onDismiss: () -> Unit,
    viewModel: PayInvoiceViewModel = hiltViewModel(),
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, cardId, closingOn) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PayInvoiceContent(
            state = if (current) state else PayInvoiceUiState(),
            amount = viewModel.amount,
            paying = viewModel.paying,
            events = PayInvoiceEvents(
                onSelect = viewModel::select,
                onTotalChange = viewModel::onTotalChange,
                onAmountChange = viewModel::onAmountChange,
                onAccountChange = viewModel::onAccountChange,
                onDateChange = viewModel::onDateChange,
                onPay = viewModel::pay,
                onRetry = viewModel::load,
            ),
        )
    }
}

/** The content of the payment sheet, without state. */
@Composable
internal fun PayInvoiceContent(
    state: PayInvoiceUiState,
    amount: String,
    paying: Long?,
    events: PayInvoiceEvents,
    modifier: Modifier = Modifier,
) {
    val context = state.context
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .navigationBarsPadding()
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        val open = state.invoice
        Text(
            open?.let { stringResource(R.string.finance_pay_invoice_of, invoiceMonth(it)) }
                ?: stringResource(R.string.finance_pay_invoice),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        when {
            state.loading -> LoadingState(stringResource(R.string.finance_loading))

            state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

            open == null -> Text(stringResource(R.string.finance_nothing_to_pay))

            else -> {
                if (state.invoices.size > 1) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        state.invoices.forEachIndexed { index, item ->
                            FilterChip(
                                selected = index == state.selected,
                                onClick = { events.onSelect(index) },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.finance_invoice_line,
                                            item.card.name,
                                            invoiceMonth(item),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
                val remaining = open.invoice.remainingCents
                ChoiceGroup(
                    options = listOf(true, false),
                    selected = state.total,
                    label = {
                        if (it) {
                            stringResource(
                                R.string.finance_pay_total,
                                context.money(remaining),
                            )
                        } else {
                            stringResource(R.string.finance_pay_partial)
                        }
                    },
                    onSelect = events.onTotalChange,
                )
                if (!state.total) {
                    ValueField(
                        digits = amount,
                        onDigitsChange = events.onAmountChange,
                        label = stringResource(R.string.finance_pay_amount),
                        transformation = MoneyTransformation(context.currency),
                        large = true,
                    )
                    // C7 (D16): a preview of what is left; the invoice the server returns is what counts.
                    val typed = Inputs.number(amount) ?: 0L
                    if (typed in 1 until remaining) {
                        Tag(
                            stringResource(R.string.finance_pay_left, context.money(remaining - typed)),
                            icon = PrumoIcons.Info,
                        )
                    }
                }
                Text(stringResource(R.string.finance_pay_with), style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    state.accounts.forEach { account ->
                        FilterChip(
                            selected = account.id == state.accountId,
                            onClick = { events.onAccountChange(account.id) },
                            label = { Text(account.name) },
                        )
                    }
                }
                DateField(state.on, events.onDateChange, stringResource(R.string.finance_date))
                Text(
                    stringResource(R.string.finance_pay_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.saveError?.let { Text(financeErrorText(it), color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = events.onPay,
                    enabled = (paying ?: 0) > 0 && state.accountId != null && !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.finance_pay_amount_button, context.money(paying ?: 0)))
                }
            }
        }
    }
}

/** The invoice's month name, as on the web ("dezembro"). */
private fun invoiceMonth(open: OpenInvoice): String = Formats.month(open.invoice.dueOn)

@PhonePreviews
@Composable
private fun PayInvoicePreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                PayInvoiceContent(
                    state = PayInvoiceUiState(
                        loading = false,
                        invoices = FinancePreview.overview.openInvoices,
                        total = false,
                        accounts = FinancePreview.accounts,
                        accountId = "a2",
                    ),
                    amount = "50000",
                    paying = 50_000,
                    events = PayInvoiceEvents({}, {}, {}, {}, {}, {}, {}),
                )
            }
        }
    }
}
