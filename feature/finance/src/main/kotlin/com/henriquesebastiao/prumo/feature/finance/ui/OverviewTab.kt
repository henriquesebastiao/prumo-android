package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.BarPair
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.LegendRow
import com.henriquesebastiao.prumo.core.designsystem.component.PairedBars
import com.henriquesebastiao.prumo.core.designsystem.component.PeriodSelector
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.ShareBar
import com.henriquesebastiao.prumo.core.designsystem.component.ShareSlice
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.seededPrimary
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.CategoryShare
import com.henriquesebastiao.prumo.feature.finance.model.OpenInvoice
import com.henriquesebastiao.prumo.feature.finance.model.Overview
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewUiState(
    val month: YearMonth = YearMonth.of(2026, 11),
    val loading: Boolean = true,
    val error: AppError? = null,
    val overview: Overview? = null,
    val context: FinanceContext = FinanceContext(),
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val context = account.preferences.value.financeContext(clock)
    private val mutableState = MutableStateFlow(OverviewUiState(month = context.month, context = context))
    val state: StateFlow<OverviewUiState> = mutableState.asStateFlow()
    private var job: Job? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load() } }
    }

    /** Pull to refresh in any tab: every read of Finanças asks the server first (D21). */
    fun pull() {
        repository.expireCache()
        load()
    }

    /** The overview of the selected month (F12); what is on screen stays while it loads. */
    fun load() {
        val month = mutableState.value.month
        mutableState.update { it.copy(loading = true, error = null) }
        job?.cancel()
        job = viewModelScope.launch {
            when (val outcome = repository.overview(month)) {
                is Outcome.Ok -> mutableState.update { it.copy(loading = false, overview = outcome.value) }
                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun shiftMonth(months: Long) {
        mutableState.update { it.copy(month = it.month.plusMonths(months)) }
        load()
    }
}

/** What the overview does besides changing the month. */
internal class OverviewEvents(
    val onPreviousMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onRetry: () -> Unit,
    val onAddAccount: () -> Unit,
    val onCategory: (CategoryShare) -> Unit,
    val onPay: (OpenInvoice) -> Unit,
    val onAccounts: () -> Unit,
)

/** Resumo (boards 01 to 04): the month's balance, income × expense, categories and open invoices. */
@Composable
internal fun OverviewTab(state: OverviewUiState, events: OverviewEvents, modifier: Modifier = Modifier) {
    val overview = state.overview
    when {
        overview == null && state.loading -> LoadingState(stringResource(R.string.finance_loading), modifier)

        overview == null -> ErrorState(
            state.error ?: AppError.Unexpected(),
            onRetry = events.onRetry,
            modifier = modifier,
        )

        overview.accountsCount == 0 -> EmptyState(
            icon = PrumoIcons.AccountBalanceWallet,
            title = stringResource(R.string.finance_empty_title),
            body = stringResource(R.string.finance_empty_body),
            hero = true,
            modifier = modifier,
            action = {
                // The design system's hero empty state: one tonal M button.
                FilledTonalButton(onClick = events.onAddAccount, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.finance_add_account))
                }
            },
        )

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                PeriodSelector(
                    label = Formats.monthYear(state.month),
                    previousLabel = stringResource(R.string.finance_previous_month),
                    nextLabel = stringResource(R.string.finance_next_month),
                    onPrevious = events.onPreviousMonth,
                    onNext = events.onNextMonth,
                )
            }
            item { BalanceCard(overview, state.context, events.onAccounts) }
            item { IncomeExpenseCard(overview, state.context) }
            item { CategoriesCard(overview.expensesByCategory, expense = true, state.context, events.onCategory) }
            if (overview.incomeByCategory.isNotEmpty()) {
                item { CategoriesCard(overview.incomeByCategory, expense = false, state.context, events.onCategory) }
            }
            item { SeriesCard(overview) }
            item { InvoicesCard(overview.openInvoices, state.context, events.onPay) }
        }
    }
}

/** Room under a list for the floating toolbar. */
internal val TOOLBAR_SPACE = 112.dp

@Composable
private fun BalanceCard(overview: Overview, context: FinanceContext, onAccounts: () -> Unit) {
    SectionCard(
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.AccountBalanceWallet,
        overline = pluralStringResource(
            R.plurals.finance_accounts_count,
            overview.accountsCount,
            overview.accountsCount,
        ),
        title = stringResource(R.string.finance_total_balance),
        onClick = onAccounts,
    ) {
        Text(context.money(overview.totalBalanceCents), style = PrumoType.dataDisplay)
        if (overview.until != context.today) {
            Text(
                stringResource(R.string.finance_until, Formats.date(overview.until)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // Board 01: each account's balance in a light block, the blocks 2 dp apart.
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overview.accounts.forEach { balance ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            balance.account.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(context.money(balance.balanceCents), style = PrumoType.dataMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseCard(overview: Overview, context: FinanceContext) {
    SectionCard(icon = PrumoIcons.CompareArrows, title = stringResource(R.string.finance_income_expense)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                context.money(overview.incomeCents),
                stringResource(R.string.finance_income),
                Modifier.weight(1f),
                valueColor = PrumoTheme.extendedColors.success,
            )
            KpiTile(
                context.money(overview.expenseCents),
                stringResource(R.string.finance_expenses),
                Modifier.weight(1f),
            )
        }
        val percent = overview.expensePercent
        if (percent != null) {
            LinearProgressIndicator(
                progress = { percent.coerceIn(0, PERCENT) / PERCENT.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Board 01: the share on the left, what is left (or short) on the right.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                if (percent != null) {
                    stringResource(R.string.finance_expense_share, percent)
                } else {
                    stringResource(R.string.finance_no_income)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(
                    if (overview.resultCents >= 0) R.string.finance_result_left else R.string.finance_result_short,
                    context.money(kotlin.math.abs(overview.resultCents)),
                ),
                style = PrumoType.dataSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Board 02: a stacked bar in each category's color and its legend; a row opens its transactions. */
@Composable
private fun CategoriesCard(
    shares: List<CategoryShare>,
    expense: Boolean,
    context: FinanceContext,
    onCategory: (CategoryShare) -> Unit,
) {
    val title =
        stringResource(if (expense) R.string.finance_expenses_by_category else R.string.finance_income_by_category)
    SectionCard(icon = PrumoIcons.DonutSmall, title = title) {
        if (shares.isEmpty()) {
            Text(stringResource(R.string.finance_no_expenses), style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        val colors = shares.map { seededPrimary(it.category.color.seed) }
        ShareBar(
            shares.mapIndexed { index, share -> ShareSlice(share.amountCents, colors[index]) },
            description = title,
        )
        shares.forEachIndexed { index, share ->
            LegendRow(
                color = colors[index],
                label = share.category.name,
                value = context.money(share.amountCents),
                share = stringResource(R.string.finance_percent, share.percent),
                modifier = Modifier.clickable(onClickLabel = share.category.name) { onCategory(share) },
            )
        }
    }
}

@Composable
private fun SeriesCard(overview: Overview) {
    SectionCard(icon = PrumoIcons.BarChart, title = stringResource(R.string.finance_six_months)) {
        val income = PrumoTheme.extendedColors.success
        PairedBars(
            overview.series.map { BarPair(Formats.shortMonth(it.month), it.incomeCents, it.expenseCents) },
            firstColor = income,
            secondColor = MaterialTheme.colorScheme.outline,
            description = stringResource(R.string.finance_six_months_description),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            LegendRow(income, stringResource(R.string.finance_income), "", null, Modifier.weight(1f))
            LegendRow(
                MaterialTheme.colorScheme.outline,
                stringResource(R.string.finance_expenses),
                "",
                null,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InvoicesCard(invoices: List<OpenInvoice>, context: FinanceContext, onPay: (OpenInvoice) -> Unit) {
    SectionCard(icon = PrumoIcons.CreditCard, title = stringResource(R.string.finance_open_invoices)) {
        if (invoices.isEmpty()) {
            Text(stringResource(R.string.finance_no_open_invoices), style = MaterialTheme.typography.bodyMedium)
        }
        invoices.forEach { open ->
            val invoice = open.invoice
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.finance_invoice_line, open.card.name, Formats.month(invoice.dueOn)),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(
                        R.string.finance_closes_due,
                        dayText(invoice.closingOn, context),
                        Formats.shortDate(invoice.dueOn),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    StatusChip(invoice.status.label(), invoice.status.tone(), small = true)
                    Text(
                        context.money(invoice.remainingCents),
                        style = PrumoType.dataMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (invoice.remainingCents > 0) {
                        FilledTonalButton(onClick = { onPay(open) }) { Text(stringResource(R.string.finance_pay)) }
                    }
                }
            }
        }
    }
}

private const val PERCENT = 100

@PhonePreviews
@Composable
private fun OverviewPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                OverviewTab(
                    state = OverviewUiState(loading = false, overview = FinancePreview.overview),
                    events = OverviewEvents({}, {}, {}, {}, {}, {}, {}),
                )
            }
        }
    }
}

@PhonePreviews
@Composable
private fun OverviewEmptyPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                OverviewTab(
                    state = OverviewUiState(
                        loading = false,
                        overview = FinancePreview.overview.copy(accountsCount = 0, accounts = emptyList()),
                    ),
                    events = OverviewEvents({}, {}, {}, {}, {}, {}, {}),
                )
            }
        }
    }
}
