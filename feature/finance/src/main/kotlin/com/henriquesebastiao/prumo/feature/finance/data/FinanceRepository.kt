package com.henriquesebastiao.prumo.feature.finance.data

import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.requestBody
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.AccountInput
import com.henriquesebastiao.prumo.feature.finance.model.Accounts
import com.henriquesebastiao.prumo.feature.finance.model.CardInput
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryInput
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.FINANCE_MODULE_ID
import com.henriquesebastiao.prumo.feature.finance.model.Invoice
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceDetail
import com.henriquesebastiao.prumo.feature.finance.model.Overview
import com.henriquesebastiao.prumo.feature.finance.model.PaymentInput
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionFilter
import com.henriquesebastiao.prumo.feature.finance.model.TransactionInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionPage
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Finanças on the server. Every write tells [changes] (and the Home, through [DataChanges]), so open
 * screens load again; balances, invoices and totals are the server's (D02).
 */
@Suppress("TooManyFunctions")
interface FinanceRepository {
    /** Emits after each successful write of Finanças. */
    val changes: Flow<Unit>

    /** Pull to refresh: the next reads ask the server before the device's copy (D21). */
    fun expireCache()

    /** The account last used in a form during this run of the app (A9, D16), or null. */
    var lastAccountId: String?

    /** The month's overview (F12); [month] null is the current one. */
    suspend fun overview(month: YearMonth? = null): Outcome<Overview>

    suspend fun accounts(archived: Boolean = false): Outcome<Accounts>

    suspend fun account(id: String): Outcome<Account>

    suspend fun createAccount(input: AccountInput): Outcome<Account>

    suspend fun updateAccount(id: String, input: AccountInput): Outcome<Account>

    suspend fun setAccountArchived(id: String, archived: Boolean): Outcome<Account>

    suspend fun deleteAccount(id: String): Outcome<Unit>

    /** F11: the account's real balance on [on]; the server records the difference. */
    suspend fun adjustBalance(id: String, balanceCents: Long, on: LocalDate): Outcome<Transaction>

    /** The categories of [kind] (both when null), with the uses of [month] (the current one when null). */
    suspend fun categories(
        kind: CategoryKind? = null,
        archived: Boolean = false,
        month: YearMonth? = null,
    ): Outcome<List<Category>>

    suspend fun createCategory(input: CategoryInput): Outcome<Category>

    suspend fun updateCategory(id: String, input: CategoryInput): Outcome<Category>

    suspend fun setCategoryArchived(id: String, archived: Boolean): Outcome<Category>

    suspend fun deleteCategory(id: String): Outcome<Unit>

    /** A page of transactions, newest first, with the totals of the whole [filter]. */
    suspend fun transactions(
        filter: TransactionFilter,
        offset: Int = 0,
        limit: Int = PAGE_SIZE,
    ): Outcome<TransactionPage>

    suspend fun transaction(id: String): Outcome<Transaction>

    suspend fun createTransaction(input: TransactionInput): Outcome<Transaction>

    suspend fun updateTransaction(id: String, input: TransactionInput): Outcome<Transaction>

    /** [allInstallments] removes the whole installment purchase (F10). */
    suspend fun deleteTransaction(id: String, allInstallments: Boolean = false): Outcome<Unit>

    suspend fun cards(archived: Boolean = false): Outcome<List<CreditCard>>

    suspend fun card(id: String): Outcome<CreditCard>

    suspend fun createCard(input: CardInput): Outcome<CreditCard>

    suspend fun updateCard(id: String, input: CardInput): Outcome<CreditCard>

    suspend fun setCardArchived(id: String, archived: Boolean): Outcome<CreditCard>

    suspend fun deleteCard(id: String): Outcome<Unit>

    /** The card's invoices: open, closed and paid, newest first. */
    suspend fun invoices(cardId: String): Outcome<List<Invoice>>

    suspend fun invoice(cardId: String, closingOn: LocalDate): Outcome<InvoiceDetail>

    suspend fun payInvoice(cardId: String, closingOn: LocalDate, input: PaymentInput): Outcome<Transaction>

    companion object {
        /** A page of transactions (the server's default). */
        const val PAGE_SIZE = 50
    }
}

@Suppress("TooManyFunctions")
@Singleton
class DefaultFinanceRepository @Inject constructor(
    private val calls: ServerCalls,
    private val dataChanges: DataChanges,
) : FinanceRepository {
    override val changes: Flow<Unit> = dataChanges.changes.filter { it == FINANCE_MODULE_ID }.map { }

    override fun expireCache() = calls.expireCache()

    @Volatile
    override var lastAccountId: String? = null

    private suspend fun <T> api(block: suspend FinanceApi.() -> T): Outcome<T> =
        calls.module(FinanceApi::class.java, block)

    /** A write: tells the screens and the Home when it worked. */
    private suspend fun <T> write(block: suspend FinanceApi.() -> T): Outcome<T> =
        api(block).onOk { dataChanges.changed(FINANCE_MODULE_ID) }

    override suspend fun overview(month: YearMonth?) = api { overview(month?.toString()) }.map { it.toModel() }

    override suspend fun accounts(archived: Boolean) = api { accounts(archived) }.map { it.toModel() }

    override suspend fun account(id: String) = api { account(id) }.map { it.toModel() }

    override suspend fun createAccount(input: AccountInput) =
        write { createAccount(requestBody(AccountWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun updateAccount(id: String, input: AccountInput) =
        write { updateAccount(id, requestBody(AccountWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun setAccountArchived(id: String, archived: Boolean) =
        write { if (archived) archiveAccount(id) else unarchiveAccount(id) }.map { it.toModel() }

    override suspend fun deleteAccount(id: String) = write { deleteAccount(id) }

    override suspend fun adjustBalance(id: String, balanceCents: Long, on: LocalDate) = write {
        adjustAccount(
            id,
            requestBody(AdjustmentCreateDto.serializer(), AdjustmentCreateDto(balanceCents, on.toString())),
        )
    }.map { it.toModel() }

    override suspend fun categories(kind: CategoryKind?, archived: Boolean, month: YearMonth?) =
        api { categories(kind?.wire, archived, month?.toString()) }.map { list -> list.items.map { it.toModel() } }

    override suspend fun createCategory(input: CategoryInput) =
        write { createCategory(requestBody(CategoryWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun updateCategory(id: String, input: CategoryInput) =
        write { updateCategory(id, requestBody(CategoryWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun setCategoryArchived(id: String, archived: Boolean) =
        write { if (archived) archiveCategory(id) else unarchiveCategory(id) }.map { it.toModel() }

    override suspend fun deleteCategory(id: String) = write { deleteCategory(id) }

    override suspend fun transactions(filter: TransactionFilter, offset: Int, limit: Int) = api {
        transactions(
            from = filter.from?.toString(),
            to = filter.to?.toString(),
            kind = filter.kind?.wire,
            accountId = filter.accountId,
            creditCardId = filter.creditCardId,
            categoryId = filter.categoryId,
            query = filter.query?.trim()?.takeIf { it.isNotEmpty() },
            limit = limit,
            offset = offset,
        )
    }.map { it.toModel() }

    override suspend fun transaction(id: String) = api { transaction(id) }.map { it.toModel() }

    override suspend fun createTransaction(input: TransactionInput) =
        write { createTransaction(requestBody(TransactionCreateDto.serializer(), input.toCreateDto())) }
            .map { it.toModel() }
            .onOk { input.accountId?.let { lastAccountId = it } }

    override suspend fun updateTransaction(id: String, input: TransactionInput) =
        write { updateTransaction(id, requestBody(TransactionUpdateDto.serializer(), input.toUpdateDto())) }
            .map { it.toModel() }

    override suspend fun deleteTransaction(id: String, allInstallments: Boolean) =
        write { deleteTransaction(id, if (allInstallments) "all" else "one") }

    override suspend fun cards(archived: Boolean) = api {
        cards(archived)
    }.map { list -> list.items.map { it.toModel() } }

    override suspend fun card(id: String) = api { card(id) }.map { it.toModel() }

    override suspend fun createCard(input: CardInput) =
        write { createCard(requestBody(CreditCardWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun updateCard(id: String, input: CardInput) =
        write { updateCard(id, requestBody(CreditCardWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun setCardArchived(id: String, archived: Boolean) =
        write { if (archived) archiveCard(id) else unarchiveCard(id) }.map { it.toModel() }

    override suspend fun deleteCard(id: String) = write { deleteCard(id) }

    override suspend fun invoices(cardId: String) = api {
        invoices(cardId)
    }.map { list -> list.items.map { it.toModel() } }

    override suspend fun invoice(cardId: String, closingOn: LocalDate) =
        api { invoice(cardId, closingOn.toString()) }.map { it.toModel() }

    override suspend fun payInvoice(cardId: String, closingOn: LocalDate, input: PaymentInput) =
        write { payInvoice(cardId, closingOn.toString(), requestBody(PaymentCreateDto.serializer(), input.toDto())) }
            .map { it.toModel() }
            .onOk { lastAccountId = input.accountId }
}
