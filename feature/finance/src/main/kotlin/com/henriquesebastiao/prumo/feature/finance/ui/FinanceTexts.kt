package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.annotation.StringRes
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

/** A group of the category icons' chooser: its title and the icons' names, in the web's order (D23). */
internal data class IconGroup(@param:StringRes val title: Int, val names: List<String>)

internal val CategoryIconGroups = listOf(
    IconGroup(
        R.string.finance_icons_home,
        listOf(
            "house",
            "key",
            "building",
            "zap",
            "droplet",
            "flame",
            "wifi",
            "smartphone",
            "tv",
            "sofa",
            "hammer",
            "spray-can",
            "washing-machine",
        ),
    ),
    IconGroup(
        R.string.finance_icons_food,
        listOf(
            "utensils",
            "shopping-cart",
            "coffee",
            "pizza",
            "croissant",
            "apple",
            "beer",
            "ice-cream-cone",
            "cake",
        ),
    ),
    IconGroup(
        R.string.finance_icons_transport,
        listOf(
            "car",
            "fuel",
            "bus",
            "train-front",
            "car-taxi-front",
            "bike",
            "square-parking",
            "wrench",
            "plane",
            "map",
        ),
    ),
    IconGroup(
        R.string.finance_icons_health,
        listOf(
            "heart-pulse",
            "pill",
            "stethoscope",
            "toothbrush",
            "dumbbell",
            "brain",
            "scissors",
            "sparkles",
            "glasses",
        ),
    ),
    IconGroup(
        R.string.finance_icons_leisure,
        listOf(
            "ticket",
            "film",
            "music",
            "gamepad-2",
            "book",
            "party-popper",
            "trophy",
            "tent",
            "tree-palm",
            "camera",
            "palette",
        ),
    ),
    IconGroup(
        R.string.finance_icons_personal,
        listOf(
            "shirt",
            "gem",
            "baby",
            "users",
            "paw-print",
            "gift",
            "heart-handshake",
            "church",
        ),
    ),
    IconGroup(
        R.string.finance_icons_work,
        listOf(
            "briefcase",
            "laptop",
            "code-xml",
            "graduation-cap",
            "book-open",
        ),
    ),
    IconGroup(
        R.string.finance_icons_money,
        listOf(
            "banknote",
            "trending-up",
            "piggy-bank",
            "coins",
            "receipt",
            "credit-card",
            "repeat",
            "scale",
            "percent",
            "shield",
            "file-text",
            "shopping-bag",
            "store",
            "package",
            "tag",
        ),
    ),
)

internal val CategoryIcons: List<String> = CategoryIconGroups.flatMap { it.names }

/** The icons the category sheet shows before "Mais ícones": the most common expenses and incomes. */
internal val QuickCategoryIcons =
    listOf("utensils", "shopping-cart", "house", "car", "heart-pulse", "ticket", "banknote")

private val IconLabels = mapOf(
    "apple" to R.string.finance_icon_apple,
    "baby" to R.string.finance_icon_baby,
    "banknote" to R.string.finance_icon_banknote,
    "beer" to R.string.finance_icon_beer,
    "bike" to R.string.finance_icon_bike,
    "book" to R.string.finance_icon_book,
    "book-open" to R.string.finance_icon_book_open,
    "brain" to R.string.finance_icon_brain,
    "briefcase" to R.string.finance_icon_briefcase,
    "building" to R.string.finance_icon_building,
    "bus" to R.string.finance_icon_bus,
    "cake" to R.string.finance_icon_cake,
    "camera" to R.string.finance_icon_camera,
    "car" to R.string.finance_icon_car,
    "car-taxi-front" to R.string.finance_icon_car_taxi_front,
    "church" to R.string.finance_icon_church,
    "code-xml" to R.string.finance_icon_code_xml,
    "coffee" to R.string.finance_icon_coffee,
    "coins" to R.string.finance_icon_coins,
    "credit-card" to R.string.finance_icon_credit_card,
    "croissant" to R.string.finance_icon_croissant,
    "droplet" to R.string.finance_icon_droplet,
    "dumbbell" to R.string.finance_icon_dumbbell,
    "file-text" to R.string.finance_icon_file_text,
    "film" to R.string.finance_icon_film,
    "flame" to R.string.finance_icon_flame,
    "fuel" to R.string.finance_icon_fuel,
    "gamepad-2" to R.string.finance_icon_gamepad_2,
    "gem" to R.string.finance_icon_gem,
    "gift" to R.string.finance_icon_gift,
    "glasses" to R.string.finance_icon_glasses,
    "graduation-cap" to R.string.finance_icon_graduation_cap,
    "hammer" to R.string.finance_icon_hammer,
    "heart-handshake" to R.string.finance_icon_heart_handshake,
    "heart-pulse" to R.string.finance_icon_heart_pulse,
    "house" to R.string.finance_icon_house,
    "ice-cream-cone" to R.string.finance_icon_ice_cream_cone,
    "key" to R.string.finance_icon_key,
    "landmark" to R.string.finance_icon_landmark,
    "laptop" to R.string.finance_icon_laptop,
    "map" to R.string.finance_icon_map,
    "music" to R.string.finance_icon_music,
    "package" to R.string.finance_icon_package,
    "palette" to R.string.finance_icon_palette,
    "party-popper" to R.string.finance_icon_party_popper,
    "paw-print" to R.string.finance_icon_paw_print,
    "percent" to R.string.finance_icon_percent,
    "piggy-bank" to R.string.finance_icon_piggy_bank,
    "pill" to R.string.finance_icon_pill,
    "pizza" to R.string.finance_icon_pizza,
    "plane" to R.string.finance_icon_plane,
    "receipt" to R.string.finance_icon_receipt,
    "repeat" to R.string.finance_icon_repeat,
    "scale" to R.string.finance_icon_scale,
    "scissors" to R.string.finance_icon_scissors,
    "shield" to R.string.finance_icon_shield,
    "shirt" to R.string.finance_icon_shirt,
    "shopping-bag" to R.string.finance_icon_shopping_bag,
    "shopping-cart" to R.string.finance_icon_shopping_cart,
    "smartphone" to R.string.finance_icon_smartphone,
    "sofa" to R.string.finance_icon_sofa,
    "sparkles" to R.string.finance_icon_sparkles,
    "spray-can" to R.string.finance_icon_spray_can,
    "square-parking" to R.string.finance_icon_square_parking,
    "stethoscope" to R.string.finance_icon_stethoscope,
    "store" to R.string.finance_icon_store,
    "tag" to R.string.finance_icon_tag,
    "tent" to R.string.finance_icon_tent,
    "ticket" to R.string.finance_icon_ticket,
    "toothbrush" to R.string.finance_icon_toothbrush,
    "train-front" to R.string.finance_icon_train_front,
    "tree-palm" to R.string.finance_icon_tree_palm,
    "trending-up" to R.string.finance_icon_trending_up,
    "trophy" to R.string.finance_icon_trophy,
    "tv" to R.string.finance_icon_tv,
    "users" to R.string.finance_icon_users,
    "utensils" to R.string.finance_icon_utensils,
    "vault" to R.string.finance_icon_vault,
    "wallet" to R.string.finance_icon_wallet,
    "washing-machine" to R.string.finance_icon_washing_machine,
    "wifi" to R.string.finance_icon_wifi,
    "wrench" to R.string.finance_icon_wrench,
    "zap" to R.string.finance_icon_zap,
)

/** The icon's name in pt-BR ("Mercado"), for the chooser's search and screen readers. */
@Composable
@ReadOnlyComposable
internal fun iconLabel(name: String): String = stringResource(IconLabels[name] ?: R.string.finance_icon_tag)

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
