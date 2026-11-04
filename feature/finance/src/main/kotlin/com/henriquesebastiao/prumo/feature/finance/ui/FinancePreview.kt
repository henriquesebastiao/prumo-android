package com.henriquesebastiao.prumo.feature.finance.ui

import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.AccountBalance
import com.henriquesebastiao.prumo.feature.finance.model.AccountKind
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.CategoryShare
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.Invoice
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceStatus
import com.henriquesebastiao.prumo.feature.finance.model.MonthPoint
import com.henriquesebastiao.prumo.feature.finance.model.OpenInvoice
import com.henriquesebastiao.prumo.feature.finance.model.Overview
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** The data of the e2e server (17/11/2026), for previews and tests. */
internal object FinancePreview {
    val context = FinanceContext()

    private fun account(id: String, name: String, kind: AccountKind, color: FinanceColor, icon: String, cents: Long) =
        Account(id, name, kind, 0, color, icon, true, null, cents, LocalDate.of(2026, 11, 16), 8, 20)

    val nubank = account("a1", "Nubank", AccountKind.Checking, FinanceColor.Violet, "landmark", 481_237)
    val inter = account("a2", "Inter", AccountKind.Checking, FinanceColor.Orange, "landmark", 215_690)
    val wallet = account("a3", "Carteira", AccountKind.Cash, FinanceColor.Green, "wallet", 18_500)
    val accounts = listOf(nubank, inter, wallet)

    private fun category(
        id: String,
        name: String,
        color: FinanceColor,
        icon: String,
        month: Long,
        kind: CategoryKind = CategoryKind.Expense,
    ) = Category(id, kind, name, color, icon, null, 10, 4, month)

    val food = category("c1", "Alimentação", FinanceColor.Orange, "utensils", 74_285)
    val home = category("c2", "Moradia", FinanceColor.Blue, "house", 138_630)
    val fuel = category("c3", "Combustível", FinanceColor.Gray, "fuel", 27_104)
    val leisure = category("c4", "Lazer", FinanceColor.Pink, "ticket", 16_400)
    val salary = category("c5", "Salário", FinanceColor.Green, "briefcase", 500_000, CategoryKind.Income)
    val freelance = category("c6", "Freelance", FinanceColor.Teal, "code-xml", 150_000, CategoryKind.Income)
    val expenseCategories = listOf(home, food, fuel, leisure)
    val incomeCategories = listOf(salary, freelance)

    val invoice = Invoice(
        closingOn = LocalDate.of(2026, 12, 3),
        dueOn = LocalDate.of(2026, 12, 10),
        startsOn = LocalDate.of(2026, 11, 3),
        endsOn = LocalDate.of(2026, 12, 2),
        amountCents = 128_460,
        paidCents = 0,
        remainingCents = 128_460,
        status = InvoiceStatus.Open,
        transactionsCount = 12,
        lastPaymentOn = null,
        lastPaymentAccount = null,
    )

    val paidInvoice = invoice.copy(
        closingOn = LocalDate.of(2026, 11, 3),
        dueOn = LocalDate.of(2026, 11, 10),
        startsOn = LocalDate.of(2026, 10, 3),
        endsOn = LocalDate.of(2026, 11, 2),
        amountCents = 193_218,
        paidCents = 193_218,
        remainingCents = 0,
        status = InvoiceStatus.Paid,
        lastPaymentOn = LocalDate.of(2026, 11, 10),
        lastPaymentAccount = nubank.ref,
    )

    val card = CreditCard(
        id = "k1",
        name = "Nubank",
        limitCents = 500_000,
        closingDay = 3,
        dueDay = 10,
        paymentAccount = nubank.ref,
        color = FinanceColor.Violet,
        archivedAt = null,
        usedCents = 128_460,
        availableLimitCents = 371_540,
        currentInvoice = invoice,
        transactionsCount = 27,
    )

    private fun transaction(
        id: String,
        kind: TransactionKind,
        cents: Long,
        day: Int,
        description: String,
        category: Category? = null,
        account: Account? = null,
        card: Boolean = false,
    ) = Transaction(
        id = id,
        kind = kind,
        amountCents = cents,
        occurredOn = LocalDate.of(2026, 11, day),
        occurredTime = LocalTime.of(14, 30).takeIf { day == 17 },
        description = description,
        notes = null,
        account = account?.ref,
        destinationAccount = null,
        creditCard = if (card) this.card.ref else null,
        category = category?.ref,
        invoiceClosingOn = if (card) invoice.closingOn else null,
        installmentGroupId = null,
        installmentNumber = null,
        installmentCount = null,
    )

    val transactions = listOf(
        transaction("t1", TransactionKind.Expense, 1_850, 17, "Padaria", food, wallet),
        transaction("t2", TransactionKind.Expense, 4_636, 16, "Posto · CG 160", fuel, card = true),
        transaction("t3", TransactionKind.Expense, 21_240, 16, "Supermercado", food, nubank),
        transaction("t4", TransactionKind.Income, 150_000, 12, "Site institucional", freelance, inter),
        transaction(
            "t5",
            TransactionKind.Transfer,
            50_000,
            12,
            "",
            account = inter,
        ).copy(destinationAccount = nubank.ref),
    )

    val installment = transaction("t6", TransactionKind.Expense, 32_635, 20, "Celular", leisure, card = true)
        .copy(installmentGroupId = "g1", installmentNumber = 4, installmentCount = 4)

    val overview = Overview(
        month = YearMonth.of(2026, 11),
        until = LocalDate.of(2026, 11, 17),
        totalBalanceCents = 715_427,
        accountsCount = 3,
        accounts = accounts.map { AccountBalance(it.ref, it.balanceCents) },
        incomeCents = 565_000,
        expenseCents = 273_581,
        expensePercent = 48,
        resultCents = 291_419,
        series = (6 downTo 1).map {
            val month = YearMonth.of(2026, 11).minusMonths(it - 1L)
            MonthPoint(month, 560_000 + it * 10_000L, 250_000 + it * 15_000L)
        },
        expensesByCategory = listOf(
            CategoryShare(home.ref, 138_630, 54, 8),
            CategoryShare(food.ref, 74_285, 29, 16),
            CategoryShare(fuel.ref, 27_104, 11, 8),
            CategoryShare(leisure.ref, 16_400, 6, 9),
        ),
        incomeByCategory = listOf(
            CategoryShare(salary.ref, 415_000, 73, 1),
            CategoryShare(freelance.ref, 150_000, 27, 1),
        ),
        openInvoices = listOf(OpenInvoice(card.ref, card.limitCents, card.availableLimitCents, invoice)),
    )
}
