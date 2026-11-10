package com.henriquesebastiao.prumo.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.Formats
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Finanças on the Home: the total balance (F2 of the server), the month's expenses and the next
 * invoice. As the hero (the first module of the order) it takes the full width and the big number.
 */
@Composable
internal fun FinanceHomeCard(
    summary: FinanceSummaryDto?,
    hero: Boolean,
    currency: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(R.string.finance_name)
    ModuleTheme(FinanceFeature.SEED) {
        SectionCard(
            modifier = if (hero) modifier else modifier.heightIn(min = TILE_MIN_HEIGHT),
            tone = SectionTone.Primary,
            hero = hero,
            icon = PrumoIcons.AccountBalanceWallet,
            overline = name,
            title = stringResource(R.string.finance_card_total_balance),
            trailing = if (hero) {
                { OpenButton(onOpen) }
            } else {
                null
            },
            onClick = onOpen,
        ) {
            if (summary != null) {
                FitText(
                    Formats.money(summary.totalBalanceCents, currency),
                    style = if (hero) PrumoType.dataDisplay else PrumoType.dataLarge,
                )
                if (hero) Details(summary, currency)
            }
        }
    }
}

@Composable
private fun Details(summary: FinanceSummaryDto, currency: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        KpiTile(
            Formats.money(summary.expenseCents, currency),
            stringResource(R.string.finance_card_month_expenses, monthName(summary.month)),
            Modifier.weight(1f),
        )
        val invoice = summary.nextInvoice
        if (invoice != null) {
            KpiTile(
                Formats.money(invoice.invoice.amountCents, currency),
                stringResource(R.string.finance_card_invoice, invoice.card.name),
                Modifier.weight(1f),
            )
        } else {
            KpiTile("—", stringResource(R.string.finance_card_no_invoice), Modifier.weight(1f))
        }
    }
}

@Composable
private fun OpenButton(onOpen: () -> Unit) {
    FilledIconButton(onClick = onOpen) {
        Icon(painterResource(PrumoIcons.ArrowForward), contentDescription = stringResource(R.string.finance_card_open))
    }
}

/** "novembro" for "2026-11"; the text itself when it isn't a month. */
private fun monthName(month: String): String = try {
    Formats.month(YearMonth.parse(month).atDay(1))
} catch (_: DateTimeParseException) {
    month
}

private val TILE_MIN_HEIGHT = 224.dp

@PhonePreviews
@Composable
private fun FinanceHomeCardPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            FinanceHomeCard(
                FinanceSummaryDto(
                    month = "2026-11",
                    totalBalanceCents = 874_235,
                    expenseCents = 148_290,
                    nextInvoice = SummaryOpenInvoiceDto(
                        SummaryCardDto("1", "Nubank"),
                        SummaryInvoiceDto("2026-12-03", 106_422, "open"),
                    ),
                ),
                hero = true,
                currency = "BRL",
                onOpen = {},
            )
        }
    }
}
