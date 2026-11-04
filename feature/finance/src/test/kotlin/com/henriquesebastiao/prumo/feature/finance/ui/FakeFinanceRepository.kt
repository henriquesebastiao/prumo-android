package com.henriquesebastiao.prumo.feature.finance.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.AccountInput
import com.henriquesebastiao.prumo.feature.finance.model.Accounts
import com.henriquesebastiao.prumo.feature.finance.model.CardInput
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryInput
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceDetail
import com.henriquesebastiao.prumo.feature.finance.model.Overview
import com.henriquesebastiao.prumo.feature.finance.model.PaymentInput
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionFilter
import com.henriquesebastiao.prumo.feature.finance.model.TransactionInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionPage
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableSharedFlow

/** Finanças in memory with the e2e server's data. [failure] makes the writes fail; what was sent is in [sent]. */
@Suppress("TooManyFunctions")
class FakeFinanceRepository(
    var accounts: List<Account> = FinancePreview.accounts,
    var cards: List<CreditCard> = listOf(FinancePreview.card),
    var categories: List<Category> = FinancePreview.expenseCategories + FinancePreview.incomeCategories,
    var transactions: List<Transaction> = FinancePreview.transactions + FinancePreview.installment,
    var overview: Overview = FinancePreview.overview,
) : FinanceRepository {
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val changes = mutableChanges

    override var lastAccountId: String? = null

    var failure: AppError? = null
    val sent = mutableListOf<Any>()
    val filters = mutableListOf<TransactionFilter>()
    val overviewMonths = mutableListOf<YearMonth?>()

    private fun <T> write(body: Any, value: () -> T): Outcome<T> {
        failure?.let { return Outcome.Failed(it) }
        sent += body
        mutableChanges.tryEmit(Unit)
        return Outcome.Ok(value())
    }

    override suspend fun overview(month: YearMonth?): Outcome<Overview> {
        overviewMonths += month
        return Outcome.Ok(month?.let { overview.copy(month = it) } ?: overview)
    }

    override suspend fun accounts(archived: Boolean) =
        Outcome.Ok(Accounts(accounts.filter { it.archived == archived }, accounts.sumOf { it.balanceCents }))

    override suspend fun account(id: String) = Outcome.Ok(accounts.first { it.id == id })

    override suspend fun createAccount(input: AccountInput) = write(input) { accounts.first() }

    override suspend fun updateAccount(id: String, input: AccountInput) = write(input) {
        accounts.first { it.id == id }
    }

    override suspend fun setAccountArchived(id: String, archived: Boolean) = write(id to archived) {
        accounts.first {
            it.id ==
                id
        }
    }

    override suspend fun deleteAccount(id: String) = write("delete $id") { }

    override suspend fun adjustBalance(id: String, balanceCents: Long, on: LocalDate) =
        write(Triple(id, balanceCents, on)) { transactions.first() }

    override suspend fun categories(kind: CategoryKind?, archived: Boolean, month: YearMonth?) =
        Outcome.Ok(categories.filter { (kind == null || it.kind == kind) && it.archived == archived })

    override suspend fun createCategory(input: CategoryInput) = write(input) { categories.first().copy(id = "new") }

    override suspend fun updateCategory(id: String, input: CategoryInput) = write(input) {
        categories.first {
            it.id ==
                id
        }
    }

    override suspend fun setCategoryArchived(id: String, archived: Boolean) = write(id to archived) {
        categories.first {
            it.id ==
                id
        }
    }

    override suspend fun deleteCategory(id: String) = write("delete $id") { }

    override suspend fun transactions(filter: TransactionFilter, offset: Int, limit: Int): Outcome<TransactionPage> {
        filters += filter
        val matching = transactions.filter { filter.accountId == null || it.account?.id == filter.accountId }
        return Outcome.Ok(TransactionPage(matching.drop(offset).take(limit), matching.size, 150_000, 78_361))
    }

    override suspend fun transaction(id: String) = Outcome.Ok(transactions.first { it.id == id })

    override suspend fun createTransaction(input: TransactionInput) = write(input) { transactions.first() }

    override suspend fun updateTransaction(id: String, input: TransactionInput) = write(input) {
        transactions.first {
            it.id ==
                id
        }
    }

    override suspend fun deleteTransaction(id: String, allInstallments: Boolean) = write(id to allInstallments) { }

    override suspend fun cards(archived: Boolean) = Outcome.Ok(cards.filter { it.archived == archived })

    override suspend fun card(id: String) = Outcome.Ok(cards.first { it.id == id })

    override suspend fun createCard(input: CardInput) = write(input) { cards.first() }

    override suspend fun updateCard(id: String, input: CardInput) = write(input) { cards.first { it.id == id } }

    override suspend fun setCardArchived(id: String, archived: Boolean) = write(id to archived) {
        cards.first {
            it.id ==
                id
        }
    }

    override suspend fun deleteCard(id: String) = write("delete $id") { }

    override suspend fun invoices(cardId: String) =
        Outcome.Ok(listOf(FinancePreview.invoice, FinancePreview.paidInvoice))

    override suspend fun invoice(cardId: String, closingOn: LocalDate): Outcome<InvoiceDetail> {
        val invoice = listOf(FinancePreview.invoice, FinancePreview.paidInvoice).first { it.closingOn == closingOn }
        return Outcome.Ok(
            InvoiceDetail(
                invoice,
                FinancePreview.card.ref,
                transactions.filter {
                    it.creditCard != null
                },
                emptyList(),
            ),
        )
    }

    override suspend fun payInvoice(cardId: String, closingOn: LocalDate, input: PaymentInput) =
        write(Triple(cardId, closingOn, input)) { transactions.first() }
}
