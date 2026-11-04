package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.Invoice

/** What the cards tab does. */
internal class CardsEvents(
    val onOpen: (CreditCard) -> Unit,
    val onAdd: () -> Unit,
    val onArchived: () -> Unit,
    val onRetry: () -> Unit,
)

/** Cartões (board 12): a card per credit card with its current invoice and limit, from the server (F5, F6). */
@Composable
internal fun CardsTab(state: AccountsUiState, events: CardsEvents, modifier: Modifier = Modifier) {
    when {
        state.loading -> LoadingState(stringResource(R.string.finance_loading), modifier)

        state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.cards.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.CreditCard,
                        title = stringResource(R.string.finance_cards_empty),
                        body = stringResource(R.string.finance_cards_empty_hint),
                    )
                }
            }
            items(state.cards, key = { it.id }) { card -> CardSummary(card, state.context) { events.onOpen(card) } }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = events.onAdd) {
                        Icon(painterResource(PrumoIcons.Add), contentDescription = null)
                        Text(stringResource(R.string.finance_new_card), Modifier.padding(start = Spacing.sm))
                    }
                }
            }
            item {
                GroupedListItem(
                    GroupedRow(
                        stringResource(R.string.finance_archived),
                        RowAction.Click(events.onArchived),
                        icon = PrumoIcons.Inventory2,
                    ),
                    index = 0,
                    count = 1,
                )
            }
        }
    }
}

@Composable
private fun CardSummary(card: CreditCard, context: FinanceContext, onClick: () -> Unit) {
    val invoice = card.currentInvoice
    SectionCard(
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.CreditCard,
        overline = stringResource(R.string.finance_card_named, card.name),
        title = invoiceTitle(invoice.dueOn),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                context.money(invoice.amountCents),
                style = PrumoType.dataDisplay,
                modifier = Modifier.weight(1f, fill = false),
            )
            StatusChip(invoice.status.label(), invoice.status.tone())
        }
        Text(invoiceDates(invoice, context), style = MaterialTheme.typography.bodyMedium)
        if (card.limitCents > 0) {
            LinearWavyProgressIndicator(
                progress = { (card.usedCents.toFloat() / card.limitCents).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                context.money(card.availableLimitCents),
                stringResource(R.string.finance_available_limit),
                Modifier.weight(1f),
            )
            KpiTile(context.money(card.limitCents), stringResource(R.string.finance_total_limit), Modifier.weight(1f))
        }
    }
}

/** "Fecha amanhã, 03/12 · vence 10/12" or, once closed, "Fechou 03/11 · vence 10/11". */
@Composable
internal fun invoiceDates(invoice: Invoice, context: FinanceContext): String {
    val closing = invoice.closingOn
    val closes = when {
        closing == context.today -> stringResource(R.string.finance_closes_today, Formats.shortDate(closing))

        closing == context.today.plusDays(
            1,
        ) -> stringResource(R.string.finance_closes_tomorrow, Formats.shortDate(closing))

        closing.isAfter(context.today) -> stringResource(R.string.finance_closes_on, Formats.shortDate(closing))

        else -> stringResource(R.string.finance_closed_on, Formats.shortDate(closing))
    }
    return closes + SEPARATOR + stringResource(R.string.finance_due_on, Formats.shortDate(invoice.dueOn))
}

@PhonePreviews
@Composable
private fun CardsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            Surface {
                CardsTab(
                    state = AccountsUiState(loading = false, cards = listOf(FinancePreview.card)),
                    events = CardsEvents({}, {}, {}, {}),
                )
            }
        }
    }
}
