package com.henriquesebastiao.prumo.feature.finance.model

import com.henriquesebastiao.prumo.core.model.Wire
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** The module's id on the server (`/api/v1/modules`, the Home's summaries). */
const val FINANCE_MODULE_ID = "finance"

// Finanças as the API gives it (docs/dominio.md of the server, "Finanças"). Balances, invoices,
// limits, the month's totals and shares come ready from the server (D02); nothing is computed here.

enum class AccountKind(override val wire: String) : Wire {
    Checking("checking"),
    Savings("savings"),
    Cash("cash"),
    Investment("investment"),
    Other("other"),
}

enum class CategoryKind(override val wire: String) : Wire {
    Expense("expense"),
    Income("income"),
}

enum class TransactionKind(override val wire: String) : Wire {
    Income("income"),
    Expense("expense"),
    Transfer("transfer"),
    CardPayment("card_payment"),
    Adjustment("adjustment"),
}

/** F5: open before closing; closed with something left; paid; overdue after the due date. */
enum class InvoiceStatus(override val wire: String) : Wire {
    Open("open"),
    Closed("closed"),
    Paid("paid"),
    Overdue("overdue"),
}

/**
 * A color of the server's palette (D33 of the server). Each one is drawn with the web's hex as the
 * seed of a tonal scheme (C10, D16).
 */
enum class FinanceColor(override val wire: String, val seed: Int) : Wire {
    Green("green", 0xFF24A06B.toInt()),
    Teal("teal", 0xFF16917F.toInt()),
    Cyan("cyan", 0xFF1F9DB3.toInt()),
    Blue("blue", 0xFF2F6FDE.toInt()),
    Indigo("indigo", 0xFF4F5AD1.toInt()),
    Violet("violet", 0xFF6F57E8.toInt()),
    Pink("pink", 0xFFD2508A.toInt()),
    Red("red", 0xFFC2362B.toInt()),
    Orange("orange", 0xFFE4672E.toInt()),
    Amber("amber", 0xFFC98A12.toInt()),
    Gray("gray", 0xFF67646C.toInt()),
    ;

    companion object {
        /** The colors the pickers offer, as the web's (D16); the current one is added when it's another. */
        val Picker = listOf(Blue, Violet, Orange, Green, Pink, Cyan, Gray)
    }
}

data class AccountRef(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val color: FinanceColor,
    val icon: String,
)

data class CategoryRef(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val color: FinanceColor,
    val icon: String,
)

data class CardRef(val id: String, val name: String, val color: FinanceColor)

data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val initialBalanceCents: Long,
    val color: FinanceColor,
    val icon: String,
    val includeInTotal: Boolean,
    val archivedAt: Instant?,
    /** F1: as of today, in the user's zone. */
    val balanceCents: Long,
    val lastTransactionOn: LocalDate?,
    val monthTransactionsCount: Int,
    val transactionsCount: Int,
) {
    val ref: AccountRef get() = AccountRef(id, name, kind, color, icon)
    val archived: Boolean get() = archivedAt != null
}

/** The accounts and F2's total balance. */
data class Accounts(val items: List<Account>, val totalBalanceCents: Long)

data class Category(
    val id: String,
    val kind: CategoryKind,
    val name: String,
    val color: FinanceColor,
    val icon: String,
    val archivedAt: Instant?,
    val transactionsCount: Int,
    val monthTransactionsCount: Int,
    val monthTotalCents: Long,
) {
    val ref: CategoryRef get() = CategoryRef(id, name, kind, color, icon)
    val archived: Boolean get() = archivedAt != null
}

/** An invoice (F4, F5): its period, values and status, all from the server. */
data class Invoice(
    val closingOn: LocalDate,
    val dueOn: LocalDate,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val amountCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
    val status: InvoiceStatus,
    val transactionsCount: Int,
    val lastPaymentOn: LocalDate?,
    val lastPaymentAccount: AccountRef?,
)

/** An invoice with its purchases and payments. */
data class InvoiceDetail(
    val invoice: Invoice,
    val card: CardRef,
    val transactions: List<Transaction>,
    val payments: List<Transaction>,
)

data class CreditCard(
    val id: String,
    val name: String,
    val limitCents: Long,
    val closingDay: Int,
    val dueDay: Int,
    val paymentAccount: AccountRef?,
    val color: FinanceColor,
    val archivedAt: Instant?,
    val usedCents: Long,
    /** F6, with future installments. */
    val availableLimitCents: Long,
    val currentInvoice: Invoice,
    val transactionsCount: Int,
) {
    val ref: CardRef get() = CardRef(id, name, color)
    val archived: Boolean get() = archivedAt != null
}

data class Transaction(
    val id: String,
    val kind: TransactionKind,
    val amountCents: Long,
    val occurredOn: LocalDate,
    val occurredTime: LocalTime?,
    /** Empty on transfers, payments and adjustments (F16): they're labelled by kind. */
    val description: String,
    val notes: String?,
    val account: AccountRef?,
    val destinationAccount: AccountRef?,
    val creditCard: CardRef?,
    val category: CategoryRef?,
    val invoiceClosingOn: LocalDate?,
    val installmentGroupId: String?,
    val installmentNumber: Int?,
    val installmentCount: Int?,
)

/** A page of transactions and the totals of the whole filter (D34 of the server). */
data class TransactionPage(val items: List<Transaction>, val total: Int, val incomeCents: Long, val expenseCents: Long)

/** What the transactions are filtered by; null is "all". */
data class TransactionFilter(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val kind: TransactionKind? = null,
    val accountId: String? = null,
    val creditCardId: String? = null,
    val categoryId: String? = null,
    val query: String? = null,
)

data class AccountBalance(val account: AccountRef, val balanceCents: Long)

/** Income and expense of a month of the 6-month series (F12). */
data class MonthPoint(val month: YearMonth, val incomeCents: Long, val expenseCents: Long)

/** A category's share of the month (A7: the server's whole percent). */
data class CategoryShare(
    val category: CategoryRef,
    val amountCents: Long,
    val percent: Int,
    val transactionsCount: Int,
)

data class OpenInvoice(val card: CardRef, val limitCents: Long, val availableLimitCents: Long, val invoice: Invoice)

/** The month's overview (F12). */
data class Overview(
    val month: YearMonth,
    /** The day the balances are of: today in the current month, else the month's last day. */
    val until: LocalDate,
    val totalBalanceCents: Long,
    val accountsCount: Int,
    val accounts: List<AccountBalance>,
    val incomeCents: Long,
    val expenseCents: Long,
    /** The expenses as a share of the income, or null without income. */
    val expensePercent: Int?,
    val resultCents: Long,
    val series: List<MonthPoint>,
    val expensesByCategory: List<CategoryShare>,
    val incomeByCategory: List<CategoryShare>,
    val openInvoices: List<OpenInvoice>,
)

// What the forms send.

data class AccountInput(
    val name: String,
    val kind: AccountKind,
    val initialBalanceCents: Long,
    val color: FinanceColor,
    val icon: String,
    val includeInTotal: Boolean,
)

data class CategoryInput(val kind: CategoryKind, val name: String, val color: FinanceColor, val icon: String)

data class CardInput(
    val name: String,
    val limitCents: Long,
    val closingDay: Int,
    val dueDay: Int,
    val paymentAccountId: String?,
    val color: FinanceColor,
)

/**
 * A transaction as the form has it. Which references go with each kind is the server's table
 * (docs/dominio.md); [installments] only for a card expense.
 */
data class TransactionInput(
    val kind: TransactionKind,
    val amountCents: Long,
    val occurredOn: LocalDate,
    val occurredTime: LocalTime?,
    val description: String,
    val notes: String?,
    val accountId: String?,
    val destinationAccountId: String?,
    val creditCardId: String?,
    val categoryId: String?,
    val installments: Int? = null,
)

data class PaymentInput(val amountCents: Long, val accountId: String, val occurredOn: LocalDate)
