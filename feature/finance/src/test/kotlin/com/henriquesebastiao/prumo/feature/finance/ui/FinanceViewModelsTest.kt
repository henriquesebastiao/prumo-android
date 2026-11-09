package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.paging.PagingSource
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.AccountInput
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.PaymentInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionFilter
import com.henriquesebastiao.prumo.feature.finance.model.TransactionInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FinanceViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    // 17/11/2026 at 14:30 in São Paulo.
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)
    private val account = FakeAccountRepository()
    private val repository = FakeFinanceRepository()

    @Test
    fun theOverviewMovesBetweenMonths() = runTest {
        val vm = OverviewViewModel(repository, account, clock)
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
        assertEquals(715_427L, vm.state.value.overview!!.totalBalanceCents)

        vm.shiftMonth(-1)

        assertEquals(YearMonth.of(2026, 10), repository.overviewMonths.last())
        assertEquals(YearMonth.of(2026, 10), vm.state.value.overview!!.month)
    }

    @Test
    fun thePagesFollowTheServersOffsetsAndBringTheTotals() = runTest {
        var totals: TransactionTotals? = null
        val source = TransactionPagingSource(repository, TransactionFilter()) { totals = it }

        val first = source.load(PagingSource.LoadParams.Refresh(null, 4, false)) as PagingSource.LoadResult.Page
        assertEquals(4, first.data.size)
        assertEquals(4, first.nextKey)
        assertEquals(TransactionTotals(6, 150_000, 78_361), totals)

        val last = source.load(PagingSource.LoadParams.Append(4, 4, false)) as PagingSource.LoadResult.Page
        assertEquals(2, last.data.size)
        assertNull(last.nextKey)
    }

    @Test
    fun aFailedPageCarriesTheAppError() = runTest {
        val failing = object : FinanceRepository by repository {
            override suspend fun transactions(filter: TransactionFilter, offset: Int, limit: Int) =
                Outcome.Failed(AppError.Unexpected(500))
        }
        val source = TransactionPagingSource(failing, TransactionFilter()) {}

        val result = source.load(PagingSource.LoadParams.Refresh(null, 4, false)) as PagingSource.LoadResult.Error

        assertEquals(AppError.Unexpected(500), (result.throwable as PagingError).error)
    }

    @Test
    fun theFiltersOfADestinationAreTheAccountsAndEveryMonth() = runTest {
        val vm = TransactionsViewModel(repository, account, clock)
        assertEquals(YearMonth.of(2026, 11), vm.state.value.filters.month)

        vm.show(accountId = "a2", cardId = null, categoryId = null)

        val filters = vm.state.value.filters
        assertNull(filters.month)
        assertEquals("Inter", filters.account!!.name)
        assertEquals("a2", filters.toFilter("  ").accountId)
        assertNull(filters.toFilter("  ").query)
        assertEquals(LocalDate.of(2026, 11, 1), TransactionFilters(month = YearMonth.of(2026, 11)).toFilter("").from)
    }

    @Test
    fun anExpenseNeedsADescriptionACategoryAndASource() = runTest {
        repository.lastAccountId = "a2"
        val vm = TransactionEditorViewModel(TransactionEditor(EntryKind.Expense), repository, account, clock)
        assertEquals(Source.OfAccount("a2"), vm.state.value.source)
        assertEquals(LocalDate.of(2026, 11, 17), vm.state.value.date)

        vm.onFieldsChange(TransactionFields(amount = "18746"))
        assertFalse(vm.canSave)
        vm.onFieldsChange(TransactionFields(amount = "18746", description = "Mercado"))
        assertFalse(vm.canSave)
        vm.onCategoryChange("c1")
        vm.onSourceChange(Source.OfCard("k1"))
        vm.onInstallmentsChange(3)
        assertTrue(vm.canSave)
        vm.save()

        val sent = repository.sent.single() as TransactionInput
        assertEquals(TransactionKind.Expense, sent.kind)
        assertEquals("k1", sent.creditCardId)
        assertNull(sent.accountId)
        assertEquals(3, sent.installments)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun switchingToIncomeDropsTheCardAndTheExpenseCategory() = runTest {
        val vm = TransactionEditorViewModel(TransactionEditor(EntryKind.Expense), repository, account, clock)
        vm.onSourceChange(Source.OfCard("k1"))
        vm.onCategoryChange("c1")

        vm.onKindChange(EntryKind.Income)

        assertTrue(vm.state.value.source is Source.OfAccount)
        assertNull(vm.state.value.categoryId)
    }

    @Test
    fun aTransferGoesBetweenTwoDifferentAccounts() = runTest {
        val vm = TransactionEditorViewModel(TransactionEditor(EntryKind.Transfer), repository, account, clock)
        assertEquals("a1", vm.state.value.fromId)
        assertEquals("a2", vm.state.value.toId)

        vm.onFieldsChange(TransactionFields(amount = "50000"))
        vm.onToChange("a1")
        assertFalse(vm.canSave)
        vm.onToChange("a3")
        vm.save()

        val sent = repository.sent.single() as TransactionInput
        assertEquals(TransactionKind.Transfer, sent.kind)
        assertEquals("", sent.description)
        assertEquals("a3", sent.destinationAccountId)
    }

    @Test
    fun editingKeepsTheKindAndSendsNoInstallments() = runTest {
        val vm = TransactionEditorViewModel(TransactionEditor(transactionId = "t6"), repository, account, clock)
        assertEquals(EntryKind.Expense, vm.state.value.kind)
        assertEquals("32635", vm.fields.amount)

        vm.onKindChange(EntryKind.Income)
        assertEquals(EntryKind.Expense, vm.state.value.kind)
        vm.save()

        assertNull((repository.sent.single() as TransactionInput).installments)
    }

    @Test
    fun aPartialPaymentSendsWhatWasTyped() = runTest {
        val vm = PayInvoiceViewModel(repository, account, clock)
        vm.start(request = 1, cardId = null, closingOn = null)
        assertEquals(128_460L, vm.paying)
        assertEquals("a1", vm.state.value.accountId)

        vm.onTotalChange(false)
        assertEquals("", vm.amount)
        vm.onAmountChange("50000")
        vm.onAccountChange("a2")
        vm.pay()

        val (card, closing, input) = repository.sent.single() as Triple<*, *, *>
        assertEquals("k1", card)
        assertEquals(LocalDate.of(2026, 12, 3), closing)
        assertEquals(PaymentInput(50_000, "a2", LocalDate.of(2026, 11, 17)), input)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun anAdjustmentSendsTheSignedRealBalance() = runTest {
        val vm = AccountDetailViewModel("a1", repository, account, clock)
        vm.openAdjust()
        assertEquals("481237", vm.balance)

        vm.onBalanceChange("2500")
        vm.onNegativeChange(true)
        vm.adjust()

        assertEquals(Triple("a1", -2_500L, LocalDate.of(2026, 11, 17)), repository.sent.single())
        assertFalse(vm.state.value.adjusting)
    }

    @Test
    fun anAccountInUseRefusesTheDeleteWithAMessage() = runTest {
        val vm = AccountDetailViewModel("a1", repository, account, clock)
        repository.failure = AppError.Api(422, "finance.account_in_use", details = mapOf("transactions_count" to "26"))

        vm.delete()

        assertEquals("finance.account_in_use", (vm.state.value.actionError as AppError.Api).code)
        assertFalse(vm.state.value.gone)
    }

    @Test
    fun aNewAccountSendsItsSignedBalanceAndColor() = runTest {
        val vm = AccountEditorViewModel(null, repository, account, clock)
        vm.onNameChange(" Carteira ")
        vm.onBalanceChange("25000")
        vm.onNegativeChange(true)
        vm.onColorChange(FinanceColor.Green)
        vm.onIconChange("wallet")
        vm.save()

        val sent = repository.sent.single() as AccountInput
        assertEquals("Carteira", sent.name)
        assertEquals(-25_000L, sent.initialBalanceCents)
        assertEquals(FinanceColor.Green, sent.color)
        assertEquals("wallet", sent.icon)
    }

    @Test
    fun aCardNeedsValidDays() = runTest {
        val vm = CardEditorViewModel(null, repository, account, clock)
        vm.onFieldsChange(CardFields("Nubank", "500000", "32", "10"))
        assertFalse(vm.canSave)
        vm.onFieldsChange(CardFields("Nubank", "500000", "3", "10"))
        assertTrue(vm.canSave)
    }

    @Test
    fun theCardMovesBetweenItsInvoices() = runTest {
        val vm = CardDetailViewModel("k1", repository, account, clock)
        assertEquals(LocalDate.of(2026, 12, 3), vm.state.value.closingOn)
        assertEquals(
            listOf(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 3)),
            vm.state.value.invoices.map {
                it.closingOn
            },
        )

        vm.move(-1)

        assertEquals(LocalDate.of(2026, 11, 3), vm.state.value.detail!!.invoice.closingOn)
    }
}
