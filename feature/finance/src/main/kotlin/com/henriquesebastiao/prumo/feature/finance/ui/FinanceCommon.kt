package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.RowValue
import com.henriquesebastiao.prumo.core.designsystem.component.SeededAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import java.io.Serializable
import java.time.LocalDate

/** An account's or category's avatar: its icon (a server name) in the tonal scheme of its color (C10, C11). */
@Composable
internal fun FinanceAvatar(color: FinanceColor, icon: String, modifier: Modifier = Modifier, cookie: Boolean = false) {
    SeededAvatar(
        color.seed,
        ServerIcons.of(icon),
        modifier = modifier,
        cookie = cookie,
        size = if (cookie) 56.dp else 40.dp,
    )
}

/**
 * A transaction: its category's avatar, the description, "Alimentação · Cartão Nubank · 2/4" and the
 * signed amount. [showDate] adds the day, for lists not grouped by day.
 */
@Composable
internal fun transactionRow(
    transaction: Transaction,
    context: FinanceContext,
    onClick: () -> Unit,
    showDate: Boolean = false,
): GroupedRow {
    val parts = buildList {
        if (showDate) add(dayText(transaction.occurredOn, context))
        transaction.category?.let { add(it.name) }
        transaction.creditCard?.let { add(stringResource(R.string.finance_card_named, it.name)) }
        val source = transaction.account?.name
        val destination = transaction.destinationAccount?.name
        when {
            source != null && destination != null -> add(stringResource(R.string.finance_from_to, source, destination))
            source != null -> add(source)
        }
        val number = transaction.installmentNumber
        val count = transaction.installmentCount
        if (number != null && count != null) add(stringResource(R.string.finance_installment, number, count))
    }
    return GroupedRow(
        title = transaction.title(),
        summary = parts.joinToString(SEPARATOR),
        action = RowAction.Click(
            onClick = onClick,
            chevron = false,
            trailing = {
                RowValue(
                    signedAmount(transaction.kind, transaction.amountCents, context.currency),
                    color = amountColor(transaction.kind, transaction.amountCents),
                )
            },
        ),
        leading = { SeededAvatar(transaction.seed(), transaction.icon()) },
    )
}

internal const val SEPARATOR = " · "

/** What the toolbar of Finanças records (board 01). */
internal class FinanceActions(
    val onExpense: () -> Unit,
    val onIncome: () -> Unit,
    val onTransfer: () -> Unit,
    val onPayInvoice: () -> Unit,
)

/** The floating toolbar: new income, new transfer and pay an invoice; the FAB records an expense. */
@Composable
internal fun FinanceToolbar(actions: FinanceActions, modifier: Modifier = Modifier) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = actions.onExpense,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    painterResource(PrumoIcons.Payments),
                    contentDescription = stringResource(R.string.finance_new_expense),
                )
            }
        },
        // A module's toolbar is the standard one (the Home's is vibrant), its FAB in primary container.
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        modifier = modifier,
    ) {
        IconButton(onClick = actions.onIncome) {
            Icon(painterResource(PrumoIcons.Savings), contentDescription = stringResource(R.string.finance_new_income))
        }
        IconButton(onClick = actions.onTransfer) {
            Icon(
                painterResource(PrumoIcons.SwapHoriz),
                contentDescription = stringResource(R.string.finance_new_transfer),
            )
        }
        IconButton(onClick = actions.onPayInvoice) {
            Icon(
                painterResource(PrumoIcons.CreditCard),
                contentDescription = stringResource(R.string.finance_pay_invoice),
            )
        }
    }
}

/** Which sheet of Finanças is open over a screen, and for what. Kept across a rotation. */
sealed interface FinanceSheet : Serializable {
    /**
     * Pay an invoice (board 14): [cardId]'s invoice closing on [closingOn], or, with nulls, one of
     * the open invoices to choose.
     */
    data class PayInvoice(val cardId: String? = null, val closingOn: LocalDate? = null) : FinanceSheet {
        private companion object {
            private const val serialVersionUID = 1L
        }
    }

    /** A new category of [expense] kind, or the edit of [categoryId] (board 11). */
    data class Category(val categoryId: String? = null, val expense: Boolean = true) : FinanceSheet {
        private companion object {
            private const val serialVersionUID = 1L
        }
    }
}

/** The sheet of [sheet], in the module's colors (also over the Home). */
@Composable
internal fun FinanceSheetHost(sheet: FinanceSheet?, onDismiss: () -> Unit, onCategorySaved: (String) -> Unit = {}) {
    if (sheet == null) return
    ModuleTheme(FinanceFeature.SEED) {
        when (sheet) {
            is FinanceSheet.PayInvoice -> PayInvoiceSheet(sheet.cardId, sheet.closingOn, onDismiss)
            is FinanceSheet.Category -> CategorySheet(sheet.categoryId, sheet.expense, onDismiss, onCategorySaved)
        }
    }
}

/** "R$ 1.064,22" in the user's currency. */
internal fun FinanceContext.money(cents: Long): String = Formats.money(cents, currency)

/** One choice of a filter chip's menu. */
internal data class Choice(val label: String, val onClick: () -> Unit)

/** A filter chip that opens its choices in a menu. */
@Composable
internal fun MenuChip(label: String, selected: Boolean, choices: List<Choice>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = selected,
            onClick = { open = true },
            label = { Text(label) },
            trailingIcon = { Icon(painterResource(PrumoIcons.ArrowDropDown), contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        open = false
                        choice.onClick()
                    },
                )
            }
        }
    }
}
