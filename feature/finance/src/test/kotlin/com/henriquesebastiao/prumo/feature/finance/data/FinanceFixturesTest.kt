package com.henriquesebastiao.prumo.feature.finance.data

import com.henriquesebastiao.prumo.core.network.NetworkJson
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.feature.finance.model.AccountKind
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceStatus
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real responses of the server (scripts/record-fixtures.py, from e2e_server.py) read as the app's models. */
class FinanceFixturesTest {
    private fun <T> read(name: String, serializer: KSerializer<T>): T =
        NetworkJson.decodeFromString(serializer, Fixtures.read("finance/$name"))

    @Test
    fun theOverviewHasTheMonthAsTheServerComputedIt() {
        val overview = read("overview.json", OverviewDto.serializer()).toModel()

        assertEquals(YearMonth.of(2026, 11), overview.month)
        assertEquals(LocalDate.of(2026, 11, 17), overview.until)
        assertEquals(715_427L, overview.totalBalanceCents)
        assertEquals(48, overview.expensePercent)
        assertEquals(6, overview.series.size)
        assertEquals("Moradia", overview.expensesByCategory.first().category.name)
        assertEquals(54, overview.expensesByCategory.first().percent)
        assertEquals("Nubank", overview.openInvoices.single().card.name)
    }

    @Test
    fun theAccountsHaveTheirBalancesAndColors() {
        val accounts = read("accounts.json", AccountListDto.serializer()).toModel()

        assertEquals(715_427L, accounts.totalBalanceCents)
        val nubank = accounts.items.first()
        assertEquals("Nubank", nubank.name)
        assertEquals(AccountKind.Checking, nubank.kind)
        assertEquals(FinanceColor.Violet, nubank.color)
        assertEquals("landmark", nubank.icon)
        assertEquals(481_237L, nubank.balanceCents)
        assertEquals(nubank, read("account.json", AccountDto.serializer()).toModel())
    }

    @Test
    fun theCategoriesHaveTheMonthsUse() {
        val list = read("categories_expense.json", CategoryListDto.serializer())
        assertEquals("2026-11", list.month)
        val food = list.items.map { it.toModel() }.first { it.name == "Alimentação" }
        assertEquals(74_285L, food.monthTotalCents)
        assertEquals(FinanceColor.Orange, food.color)
        assertTrue(read("categories_income.json", CategoryListDto.serializer()).items.any { it.name == "Salário" })
    }

    @Test
    fun theTransactionsComeWithTheFiltersTotals() {
        val page = read("transactions.json", TransactionPageDto.serializer()).toModel()

        assertEquals(65, page.total)
        assertEquals(3_130_000L, page.incomeCents)
        assertEquals(1_999_054L, page.expenseCents)
        assertTrue(page.items.any { it.kind == TransactionKind.Transfer && it.description.isEmpty() })
        val installment = read("transaction_installment.json", TransactionDto.serializer()).toModel()
        assertEquals(4, installment.installmentCount)
        assertNotNull(installment.creditCard)
        assertNotNull(installment.invoiceClosingOn)
    }

    @Test
    fun theCardHasItsInvoiceAndLimit() {
        val card = read("credit_card.json", CreditCardDto.serializer()).toModel()
        assertEquals(3, card.closingDay)
        assertEquals(10, card.dueDay)
        assertEquals(371_540L, card.availableLimitCents)
        assertEquals(InvoiceStatus.Open, card.currentInvoice.status)
        assertEquals(LocalDate.of(2026, 12, 3), card.currentInvoice.closingOn)

        val invoices = read("invoices.json", InvoiceListDto.serializer()).items.map { it.toModel() }
        assertTrue(invoices.any { it.status == InvoiceStatus.Paid })
        val detail = read("invoice.json", InvoiceDetailDto.serializer()).toModel()
        assertEquals(card.currentInvoice.amountCents, detail.invoice.amountCents)
        assertEquals(detail.invoice.transactionsCount, detail.transactions.size)
    }
}
