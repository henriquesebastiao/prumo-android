package com.henriquesebastiao.prumo.feature.finance.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The finance part of the contract (openapi.json), written by hand with the fields the app uses
// (docs/decisoes.md, D04). FinanceContractTest checks them against the server's schemas. Enums and
// colors travel as strings; an unknown value maps to a fallback (a newer server).

/** Schema `AccountRef`. */
@Serializable
data class AccountRefDto(val id: String, val name: String, val kind: String, val color: String, val icon: String)

/** Schema `CategoryRef`. */
@Serializable
data class CategoryRefDto(val id: String, val name: String, val kind: String, val color: String, val icon: String)

/** Schema `CardRef`. */
@Serializable
data class CardRefDto(val id: String, val name: String, val color: String)

/** Schema `AccountRead`. */
@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val kind: String,
    @SerialName("initial_balance_cents") val initialBalanceCents: Long,
    val color: String,
    val icon: String,
    @SerialName("include_in_total") val includeInTotal: Boolean,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("balance_cents") val balanceCents: Long,
    @SerialName("last_transaction_on") val lastTransactionOn: String?,
    @SerialName("month_transactions_count") val monthTransactionsCount: Int,
    @SerialName("transactions_count") val transactionsCount: Int,
)

/** Schema `AccountList`. */
@Serializable
data class AccountListDto(val items: List<AccountDto>, @SerialName("total_balance_cents") val totalBalanceCents: Long)

/** `POST /finance/accounts` (schema `AccountCreate`) and, with the same fields, `AccountUpdate`. */
@Serializable
data class AccountWriteDto(
    val name: String,
    val kind: String,
    @SerialName("initial_balance_cents") val initialBalanceCents: Long,
    val color: String,
    val icon: String,
    @SerialName("include_in_total") val includeInTotal: Boolean,
)

/** `POST /finance/accounts/{id}/adjustments` (schema `AdjustmentCreate`): the real balance (F11). */
@Serializable
data class AdjustmentCreateDto(
    @SerialName("balance_cents") val balanceCents: Long,
    @SerialName("occurred_on") val occurredOn: String,
)

/** Schema `CategoryRead`. */
@Serializable
data class CategoryDto(
    val id: String,
    val kind: String,
    val name: String,
    val color: String,
    val icon: String,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("transactions_count") val transactionsCount: Int,
    @SerialName("month_transactions_count") val monthTransactionsCount: Int,
    @SerialName("month_total_cents") val monthTotalCents: Long,
)

/** Schema `CategoryList`. */
@Serializable
data class CategoryListDto(val month: String, val items: List<CategoryDto>)

/** Schema `CategoryCreate` and, with the same fields, `CategoryUpdate`. */
@Serializable
data class CategoryWriteDto(val kind: String, val name: String, val color: String, val icon: String)

/** Schema `InvoiceRead`. */
@Serializable
data class InvoiceDto(
    @SerialName("closing_on") val closingOn: String,
    @SerialName("due_on") val dueOn: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("paid_cents") val paidCents: Long,
    @SerialName("remaining_cents") val remainingCents: Long,
    val status: String,
    @SerialName("transactions_count") val transactionsCount: Int,
    @SerialName("last_payment_on") val lastPaymentOn: String?,
    @SerialName("last_payment_account") val lastPaymentAccount: AccountRefDto?,
)

/** Schema `InvoiceList`. */
@Serializable
data class InvoiceListDto(val items: List<InvoiceDto>)

/** Schema `InvoiceDetail`: the invoice's fields, its card, purchases and payments. */
@Serializable
data class InvoiceDetailDto(
    @SerialName("closing_on") val closingOn: String,
    @SerialName("due_on") val dueOn: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("paid_cents") val paidCents: Long,
    @SerialName("remaining_cents") val remainingCents: Long,
    val status: String,
    @SerialName("transactions_count") val transactionsCount: Int,
    @SerialName("last_payment_on") val lastPaymentOn: String?,
    @SerialName("last_payment_account") val lastPaymentAccount: AccountRefDto?,
    val card: CardRefDto,
    val transactions: List<TransactionDto>,
    val payments: List<TransactionDto>,
)

/** `POST …/invoices/{closing_on}/payments` (schema `InvoicePaymentCreate`). */
@Serializable
data class PaymentCreateDto(
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("account_id") val accountId: String,
    @SerialName("occurred_on") val occurredOn: String,
)

/** Schema `CreditCardRead`. */
@Serializable
data class CreditCardDto(
    val id: String,
    val name: String,
    @SerialName("limit_cents") val limitCents: Long,
    @SerialName("closing_day") val closingDay: Int,
    @SerialName("due_day") val dueDay: Int,
    @SerialName("payment_account") val paymentAccount: AccountRefDto?,
    val color: String,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("used_cents") val usedCents: Long,
    @SerialName("available_limit_cents") val availableLimitCents: Long,
    @SerialName("current_invoice") val currentInvoice: InvoiceDto,
    @SerialName("transactions_count") val transactionsCount: Int,
)

/** Schema `CreditCardList`. */
@Serializable
data class CreditCardListDto(val items: List<CreditCardDto>)

/** Schema `CreditCardCreate` and, with the same fields, `CreditCardUpdate`. */
@Serializable
data class CreditCardWriteDto(
    val name: String,
    @SerialName("limit_cents") val limitCents: Long,
    @SerialName("closing_day") val closingDay: Int,
    @SerialName("due_day") val dueDay: Int,
    @SerialName("payment_account_id") val paymentAccountId: String?,
    val color: String,
)

/** Schema `TransactionRead`. */
@Serializable
data class TransactionDto(
    val id: String,
    val kind: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("occurred_on") val occurredOn: String,
    @SerialName("occurred_time") val occurredTime: String?,
    val description: String,
    val notes: String?,
    val account: AccountRefDto?,
    @SerialName("destination_account") val destinationAccount: AccountRefDto?,
    @SerialName("credit_card") val creditCard: CardRefDto?,
    val category: CategoryRefDto?,
    @SerialName("invoice_closing_on") val invoiceClosingOn: String?,
    @SerialName("installment_group_id") val installmentGroupId: String?,
    @SerialName("installment_number") val installmentNumber: Int?,
    @SerialName("installment_count") val installmentCount: Int?,
)

/** Schema `TransactionPage`: the standard page and the totals of the whole filter (D34 of the server). */
@Serializable
data class TransactionPageDto(
    val items: List<TransactionDto>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    @SerialName("income_cents") val incomeCents: Long,
    @SerialName("expense_cents") val expenseCents: Long,
)

/**
 * Schema `TransactionCreate`. `invoice_closing_on` stays out: the app records card payments through
 * the invoice (`…/payments`), and an expense's invoice is the server's (F4).
 */
@Serializable
data class TransactionCreateDto(
    val kind: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("occurred_on") val occurredOn: String,
    @SerialName("occurred_time") val occurredTime: String?,
    val description: String,
    val notes: String?,
    @SerialName("account_id") val accountId: String?,
    @SerialName("destination_account_id") val destinationAccountId: String?,
    @SerialName("credit_card_id") val creditCardId: String?,
    @SerialName("category_id") val categoryId: String?,
    val installments: Int?,
)

/**
 * Schema `TransactionUpdate`: the kind never changes (F16), and `invoice_closing_on` stays out (the
 * server refuses it, even `null`, for anything but a card payment).
 */
@Serializable
data class TransactionUpdateDto(
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("occurred_on") val occurredOn: String,
    @SerialName("occurred_time") val occurredTime: String?,
    val description: String,
    val notes: String?,
    @SerialName("account_id") val accountId: String?,
    @SerialName("destination_account_id") val destinationAccountId: String?,
    @SerialName("credit_card_id") val creditCardId: String?,
    @SerialName("category_id") val categoryId: String?,
)

/** Schema `AccountBalance`. */
@Serializable
data class AccountBalanceDto(val account: AccountRefDto, @SerialName("balance_cents") val balanceCents: Long)

/** Schema `MonthPoint`. */
@Serializable
data class MonthPointDto(
    val month: String,
    @SerialName("income_cents") val incomeCents: Long,
    @SerialName("expense_cents") val expenseCents: Long,
)

/** Schema `CategoryShare`. */
@Serializable
data class CategoryShareDto(
    val category: CategoryRefDto,
    @SerialName("amount_cents") val amountCents: Long,
    val percent: Int,
    @SerialName("transactions_count") val transactionsCount: Int,
)

/** Schema `OpenInvoice`. */
@Serializable
data class OpenInvoiceDto(
    val card: CardRefDto,
    @SerialName("limit_cents") val limitCents: Long,
    @SerialName("available_limit_cents") val availableLimitCents: Long,
    val invoice: InvoiceDto,
)

/** Schema `FinanceOverview`. */
@Serializable
data class OverviewDto(
    val month: String,
    val until: String,
    @SerialName("total_balance_cents") val totalBalanceCents: Long,
    @SerialName("accounts_count") val accountsCount: Int,
    val accounts: List<AccountBalanceDto>,
    @SerialName("income_cents") val incomeCents: Long,
    @SerialName("expense_cents") val expenseCents: Long,
    @SerialName("expense_percent") val expensePercent: Int?,
    @SerialName("result_cents") val resultCents: Long,
    val series: List<MonthPointDto>,
    @SerialName("expenses_by_category") val expensesByCategory: List<CategoryShareDto>,
    @SerialName("income_by_category") val incomeByCategory: List<CategoryShareDto>,
    @SerialName("open_invoices") val openInvoices: List<OpenInvoiceDto>,
)
