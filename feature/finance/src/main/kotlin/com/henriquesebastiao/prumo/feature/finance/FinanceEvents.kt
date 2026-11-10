package com.henriquesebastiao.prumo.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.UpcomingItem
import com.henriquesebastiao.prumo.core.model.long
import com.henriquesebastiao.prumo.core.model.text
import com.henriquesebastiao.prumo.core.model.wireOf
import com.henriquesebastiao.prumo.core.ui.relativeDate
import com.henriquesebastiao.prumo.core.ui.relativeDateTime
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import com.henriquesebastiao.prumo.feature.finance.ui.CardDetail
import com.henriquesebastiao.prumo.feature.finance.ui.SEPARATOR
import com.henriquesebastiao.prumo.feature.finance.ui.TransactionDetail
import com.henriquesebastiao.prumo.feature.finance.ui.label
import com.henriquesebastiao.prumo.feature.finance.ui.signedAmount

// How the items of Finanças read on the Home (docs/api.md of the server, "Itens do Início"), as
// the web's finance/events.ts writes them. Every value comes from the server (F5, F15).

/** "Vem aí": an invoice that closes (while open) or is due, opening its card. */
@Composable
@ReadOnlyComposable
internal fun financeUpcoming(item: UpcomingItem, context: HomeContext): HomeEvent? {
    val data = item.data
    val money = { cents: Long -> Formats.money(cents, context.currency) }
    val target = item.entityId?.let(::CardDetail)
    return when (item.kind) {
        "finance.invoice_closing" -> HomeEvent(
            title = stringResource(R.string.finance_event_invoice_closing, item.subject),
            subtitle = data.long("available_limit_cents")?.let {
                stringResource(R.string.finance_event_available_limit, money(it))
            },
            target = target,
        )

        "finance.invoice_due" -> {
            // What is left comes from the server (F5); without it (an older server), the amount.
            val remaining = data.long("remaining_cents")
            val amount = data.long("amount_cents") ?: 0
            val subtitle = when {
                remaining != null && data.text("status") == "overdue" ->
                    stringResource(R.string.finance_event_overdue, money(remaining))

                remaining != null && (data.long("paid_cents") ?: 0) > 0 ->
                    stringResource(R.string.finance_event_due_remaining, money(remaining))

                else -> stringResource(R.string.finance_event_due_amount, money(amount))
            }
            HomeEvent(
                title = stringResource(R.string.finance_event_invoice_due, item.subject),
                subtitle = subtitle,
                target = target,
            )
        }

        else -> null
    }
}

/** "Atividade recente": a transaction, signed by its kind, opening its detail. */
@Composable
@ReadOnlyComposable
internal fun financeActivity(item: ActivityItem, context: HomeContext): HomeEvent? {
    if (item.kind != "finance.transaction") return null
    val data = item.data
    val kind = wireOf(data.text("transaction_kind"), TransactionKind.Expense)
    val cents = data.long("amount_cents") ?: 0
    val description = item.subject.ifBlank { kind.label() }
    // Without a time of its own, the moment is the day's: only the day is written (web's).
    val whenText = if (data.text("occurred_time") != null) {
        relativeDateTime(item.occurredAt, context.zone, context.today)
    } else {
        relativeDate(item.occurredAt.atZone(context.zone).toLocalDate(), context.today)
    }
    val account = data.text("account")
    val destination = data.text("destination_account")
    val card = data.text("credit_card")
    val where = when {
        kind == TransactionKind.Transfer && account != null && destination != null ->
            stringResource(R.string.finance_from_to, account, destination)

        card != null -> stringResource(R.string.finance_card_named, card)

        else -> account
    }
    return HomeEvent(
        title = data.text("category")?.let { stringResource(R.string.finance_event_transaction, description, it) }
            ?: description,
        subtitle = listOfNotNull(whenText, where).joinToString(SEPARATOR),
        value = signedAmount(kind, cents, context.currency),
        positive = kind == TransactionKind.Income || (kind == TransactionKind.Adjustment && cents > 0),
        icon = when (kind) {
            TransactionKind.Income -> PrumoIcons.Savings
            TransactionKind.Expense -> PrumoIcons.Payments
            TransactionKind.Transfer -> PrumoIcons.SwapHoriz
            TransactionKind.CardPayment -> PrumoIcons.CreditCard
            TransactionKind.Adjustment -> PrumoIcons.Tune
        },
        target = item.entityId?.let(::TransactionDetail),
    )
}
