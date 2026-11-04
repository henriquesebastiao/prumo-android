package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.ui.errorText
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.model.AccountKind
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceStatus
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** What the screens of Finanças need from the account: the currency and the user's zone and today (S3, F9). */
data class FinanceContext(
    val currency: String = Preferences.Default.currency,
    val zone: ZoneId = Preferences.Default.zone,
    val today: LocalDate = LocalDate.of(2026, 11, 17),
) {
    val month: YearMonth get() = YearMonth.from(today)
}

internal fun Preferences?.financeContext(clock: Clock): FinanceContext {
    val prefs = this ?: Preferences.Default
    return FinanceContext(prefs.currency, prefs.zone, prefs.today(clock.instant()))
}

/** The icons the pickers offer, as the web's (C11, D16): Lucide names stored by the server. */
internal val AccountIcons = listOf("landmark", "vault", "wallet", "trending-up", "credit-card")

internal val CategoryIcons = listOf(
    "fuel",
    "utensils",
    "house",
    "ticket",
    "briefcase",
    "code-xml",
    "wrench",
    "bus",
    "heart-pulse",
    "graduation-cap",
    "shopping-bag",
    "repeat",
    "trending-up",
    "tag",
    "paw-print",
    "gift",
    "plane",
    "shirt",
    "banknote",
)

@Composable
@ReadOnlyComposable
internal fun iconLabel(name: String): String = stringResource(
    when (name) {
        "landmark" -> R.string.finance_icon_landmark
        "vault" -> R.string.finance_icon_vault
        "wallet" -> R.string.finance_icon_wallet
        "trending-up" -> R.string.finance_icon_trending_up
        "credit-card" -> R.string.finance_icon_credit_card
        "fuel" -> R.string.finance_icon_fuel
        "utensils" -> R.string.finance_icon_utensils
        "house" -> R.string.finance_icon_house
        "ticket" -> R.string.finance_icon_ticket
        "briefcase" -> R.string.finance_icon_briefcase
        "code-xml" -> R.string.finance_icon_code_xml
        "wrench" -> R.string.finance_icon_wrench
        "bus" -> R.string.finance_icon_bus
        "heart-pulse" -> R.string.finance_icon_heart_pulse
        "graduation-cap" -> R.string.finance_icon_graduation_cap
        "shopping-bag" -> R.string.finance_icon_shopping_bag
        "repeat" -> R.string.finance_icon_repeat
        "paw-print" -> R.string.finance_icon_paw_print
        "gift" -> R.string.finance_icon_gift
        "plane" -> R.string.finance_icon_plane
        "shirt" -> R.string.finance_icon_shirt
        "banknote" -> R.string.finance_icon_banknote
        else -> R.string.finance_icon_tag
    },
)

@Composable
@ReadOnlyComposable
internal fun FinanceColor.label(): String = stringResource(
    when (this) {
        FinanceColor.Green -> R.string.finance_color_green
        FinanceColor.Teal -> R.string.finance_color_teal
        FinanceColor.Cyan -> R.string.finance_color_cyan
        FinanceColor.Blue -> R.string.finance_color_blue
        FinanceColor.Indigo -> R.string.finance_color_indigo
        FinanceColor.Violet -> R.string.finance_color_violet
        FinanceColor.Pink -> R.string.finance_color_pink
        FinanceColor.Red -> R.string.finance_color_red
        FinanceColor.Orange -> R.string.finance_color_orange
        FinanceColor.Amber -> R.string.finance_color_amber
        FinanceColor.Gray -> R.string.finance_color_gray
    },
)

@Composable
@ReadOnlyComposable
internal fun AccountKind.label(): String = stringResource(
    when (this) {
        AccountKind.Checking -> R.string.finance_account_checking
        AccountKind.Savings -> R.string.finance_account_savings
        AccountKind.Cash -> R.string.finance_account_cash
        AccountKind.Investment -> R.string.finance_account_investment
        AccountKind.Other -> R.string.finance_account_other
    },
)

/** The kind in a chip: "Corrente". */
@Composable
@ReadOnlyComposable
internal fun AccountKind.shortLabel(): String = stringResource(
    when (this) {
        AccountKind.Checking -> R.string.finance_account_checking_short
        AccountKind.Savings -> R.string.finance_account_savings
        AccountKind.Cash -> R.string.finance_account_cash_short
        AccountKind.Investment -> R.string.finance_account_investment
        AccountKind.Other -> R.string.finance_account_other
    },
)

@Composable
@ReadOnlyComposable
internal fun TransactionKind.label(): String = stringResource(
    when (this) {
        TransactionKind.Income -> R.string.finance_kind_income
        TransactionKind.Expense -> R.string.finance_kind_expense
        TransactionKind.Transfer -> R.string.finance_kind_transfer
        TransactionKind.CardPayment -> R.string.finance_kind_card_payment
        TransactionKind.Adjustment -> R.string.finance_kind_adjustment
    },
)

@Composable
@ReadOnlyComposable
internal fun InvoiceStatus.label(): String = stringResource(
    when (this) {
        InvoiceStatus.Open -> R.string.finance_invoice_open
        InvoiceStatus.Closed -> R.string.finance_invoice_closed
        InvoiceStatus.Paid -> R.string.finance_invoice_paid
        InvoiceStatus.Overdue -> R.string.finance_invoice_overdue
    },
)

/** Board 13: open neutral, closed warning, paid success, overdue error. */
internal fun InvoiceStatus.tone(): StatusTone = when (this) {
    InvoiceStatus.Open -> StatusTone.Neutral
    InvoiceStatus.Closed -> StatusTone.Warning
    InvoiceStatus.Paid -> StatusTone.Success
    InvoiceStatus.Overdue -> StatusTone.Error
}

/** "Fatura de dezembro": invoices are named by the month they're due, as on the web. */
@Composable
@ReadOnlyComposable
internal fun invoiceTitle(dueOn: LocalDate): String = stringResource(R.string.finance_invoice_of, Formats.month(dueOn))

/** "Hoje", "Amanhã" or "03/11". */
@Composable
@ReadOnlyComposable
internal fun dayText(date: LocalDate, context: FinanceContext): String = when (date) {
    context.today -> stringResource(R.string.finance_today)
    context.today.plusDays(1) -> stringResource(R.string.finance_tomorrow)
    context.today.minusDays(1) -> stringResource(R.string.finance_yesterday)
    else -> Formats.shortDate(date)
}

/** The description, or the kind when the API keeps it empty (F16). */
@Composable
@ReadOnlyComposable
internal fun Transaction.title(): String = description.ifBlank { kind.label() }

/** How the amount reads in lists: `+R$ 850,00` income, `−R$ 187,46` expense, plain transfer (web's). */
@Composable
@ReadOnlyComposable
internal fun signedAmount(kind: TransactionKind, cents: Long, currency: String): String {
    val plain = Formats.money(kotlin.math.abs(cents), currency)
    return when {
        kind == TransactionKind.Income -> stringResource(R.string.finance_plus, plain)

        kind == TransactionKind.Expense || kind == TransactionKind.CardPayment -> stringResource(
            R.string.finance_minus,
            plain,
        )

        kind == TransactionKind.Adjustment && cents < 0 -> stringResource(R.string.finance_minus, plain)

        kind == TransactionKind.Adjustment -> stringResource(R.string.finance_plus, plain)

        else -> plain
    }
}

/** Income (and a positive adjustment) in the success color, the rest in the text color. */
@Composable
@ReadOnlyComposable
internal fun amountColor(kind: TransactionKind, cents: Long): Color =
    if (kind == TransactionKind.Income || (kind == TransactionKind.Adjustment && cents > 0)) {
        PrumoTheme.extendedColors.success
    } else {
        MaterialTheme.colorScheme.onSurface
    }

/** The icon of a transaction: its category's, or its kind's. */
internal fun Transaction.icon(): Int = when (kind) {
    TransactionKind.Transfer -> PrumoIcons.SwapHoriz
    TransactionKind.CardPayment -> PrumoIcons.CreditCard
    TransactionKind.Adjustment -> PrumoIcons.Tune
    else -> ServerIcons.of(category?.icon)
}

/** The seed of a transaction's avatar: its category's color, or the module's. */
internal fun Transaction.seed(): Int = category?.color?.seed ?: FinanceFeature.SEED

/**
 * The text of an error of Finanças: the module's codes (`docs/api.md` of the server, "Finanças"),
 * then the app's.
 */
@Composable
@ReadOnlyComposable
internal fun financeErrorText(error: AppError): String {
    val api = error as? AppError.Api ?: return errorText(error)
    val count = api.details["transactions_count"]?.toIntOrNull() ?: 0
    return when (api.code) {
        "finance.category_in_use" -> pluralStringResource(R.plurals.finance_error_category_in_use, count, count)
        "finance.account_in_use" -> pluralStringResource(R.plurals.finance_error_account_in_use, count, count)
        "finance.card_in_use" -> pluralStringResource(R.plurals.finance_error_card_in_use, count, count)
        "finance.category_exists" -> stringResource(R.string.finance_error_category_exists)
        "finance.invalid_amount" -> stringResource(R.string.finance_error_invalid_amount)
        "finance.category_kind_mismatch" -> stringResource(R.string.finance_error_category_kind)
        "finance.same_account" -> stringResource(R.string.finance_error_same_account)
        "finance.installments_require_card" -> stringResource(R.string.finance_error_installments)
        "finance.invalid_invoice" -> stringResource(R.string.finance_error_invalid_invoice)
        "finance.balance_unchanged" -> stringResource(R.string.finance_error_balance_unchanged)
        "finance.invalid_combination" -> stringResource(R.string.finance_error_invalid_combination)
        else -> errorText(error)
    }
}

/** "Confira este valor." when the server refused the field at [path] (`validation_error`). */
@Composable
@ReadOnlyComposable
internal fun fieldError(error: AppError?, path: String): String? {
    val api = error as? AppError.Api ?: return null
    val refused = api.fields.any { it.path == path || it.path.startsWith("$path.") }
    return if (refused) stringResource(R.string.finance_field_invalid) else null
}

/** Whether [error] is shown on a field rather than for the whole form. */
internal fun AppError.isFieldError(): Boolean = this is AppError.Api && fields.isNotEmpty()
