package com.henriquesebastiao.prumo.feature.finance.data

import com.henriquesebastiao.prumo.core.model.wireOf
import com.henriquesebastiao.prumo.core.network.parseInstant
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.AccountBalance
import com.henriquesebastiao.prumo.feature.finance.model.AccountInput
import com.henriquesebastiao.prumo.feature.finance.model.AccountKind
import com.henriquesebastiao.prumo.feature.finance.model.AccountRef
import com.henriquesebastiao.prumo.feature.finance.model.Accounts
import com.henriquesebastiao.prumo.feature.finance.model.CardInput
import com.henriquesebastiao.prumo.feature.finance.model.CardRef
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryInput
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.CategoryRef
import com.henriquesebastiao.prumo.feature.finance.model.CategoryShare
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.Invoice
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceDetail
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceStatus
import com.henriquesebastiao.prumo.feature.finance.model.MonthPoint
import com.henriquesebastiao.prumo.feature.finance.model.OpenInvoice
import com.henriquesebastiao.prumo.feature.finance.model.Overview
import com.henriquesebastiao.prumo.feature.finance.model.PaymentInput
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import com.henriquesebastiao.prumo.feature.finance.model.TransactionPage
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// DTOs stay in the data package: the screens get the models of `model/`.

internal fun color(wire: String) = wireOf(wire, FinanceColor.Gray)

internal fun AccountRefDto.toModel() = AccountRef(id, name, wireOf(kind, AccountKind.Other), color(color), icon)

internal fun CategoryRefDto.toModel() = CategoryRef(id, name, wireOf(kind, CategoryKind.Expense), color(color), icon)

internal fun CardRefDto.toModel() = CardRef(id, name, color(color))

internal fun AccountDto.toModel() = Account(
    id = id,
    name = name,
    kind = wireOf(kind, AccountKind.Other),
    initialBalanceCents = initialBalanceCents,
    color = color(color),
    icon = icon,
    includeInTotal = includeInTotal,
    archivedAt = archivedAt?.let(::parseInstant),
    balanceCents = balanceCents,
    lastTransactionOn = lastTransactionOn?.let(LocalDate::parse),
    monthTransactionsCount = monthTransactionsCount,
    transactionsCount = transactionsCount,
)

internal fun AccountListDto.toModel() = Accounts(items.map { it.toModel() }, totalBalanceCents)

internal fun CategoryDto.toModel() = Category(
    id = id,
    kind = wireOf(kind, CategoryKind.Expense),
    name = name,
    color = color(color),
    icon = icon,
    archivedAt = archivedAt?.let(::parseInstant),
    transactionsCount = transactionsCount,
    monthTransactionsCount = monthTransactionsCount,
    monthTotalCents = monthTotalCents,
)

internal fun InvoiceDto.toModel() = Invoice(
    closingOn = LocalDate.parse(closingOn),
    dueOn = LocalDate.parse(dueOn),
    startsOn = LocalDate.parse(startsOn),
    endsOn = LocalDate.parse(endsOn),
    amountCents = amountCents,
    paidCents = paidCents,
    remainingCents = remainingCents,
    status = wireOf(status, InvoiceStatus.Open),
    transactionsCount = transactionsCount,
    lastPaymentOn = lastPaymentOn?.let(LocalDate::parse),
    lastPaymentAccount = lastPaymentAccount?.toModel(),
)

internal fun InvoiceDetailDto.toModel() = InvoiceDetail(
    invoice = InvoiceDto(
        closingOn, dueOn, startsOn, endsOn, amountCents, paidCents, remainingCents, status, transactionsCount,
        lastPaymentOn, lastPaymentAccount,
    ).toModel(),
    card = card.toModel(),
    transactions = transactions.map { it.toModel() },
    payments = payments.map { it.toModel() },
)

internal fun CreditCardDto.toModel() = CreditCard(
    id = id,
    name = name,
    limitCents = limitCents,
    closingDay = closingDay,
    dueDay = dueDay,
    paymentAccount = paymentAccount?.toModel(),
    color = color(color),
    archivedAt = archivedAt?.let(::parseInstant),
    usedCents = usedCents,
    availableLimitCents = availableLimitCents,
    currentInvoice = currentInvoice.toModel(),
    transactionsCount = transactionsCount,
)

internal fun TransactionDto.toModel() = Transaction(
    id = id,
    kind = wireOf(kind, TransactionKind.Expense),
    amountCents = amountCents,
    occurredOn = LocalDate.parse(occurredOn),
    occurredTime = occurredTime?.let(LocalTime::parse),
    description = description,
    notes = notes,
    account = account?.toModel(),
    destinationAccount = destinationAccount?.toModel(),
    creditCard = creditCard?.toModel(),
    category = category?.toModel(),
    invoiceClosingOn = invoiceClosingOn?.let(LocalDate::parse),
    installmentGroupId = installmentGroupId,
    installmentNumber = installmentNumber,
    installmentCount = installmentCount,
)

internal fun TransactionPageDto.toModel() = TransactionPage(
    items.map {
        it.toModel()
    },
    total,
    incomeCents,
    expenseCents,
)

private fun CategoryShareDto.toModel() = CategoryShare(category.toModel(), amountCents, percent, transactionsCount)

internal fun OverviewDto.toModel() = Overview(
    month = YearMonth.parse(month),
    until = LocalDate.parse(until),
    totalBalanceCents = totalBalanceCents,
    accountsCount = accountsCount,
    accounts = accounts.map { AccountBalance(it.account.toModel(), it.balanceCents) },
    incomeCents = incomeCents,
    expenseCents = expenseCents,
    expensePercent = expensePercent,
    resultCents = resultCents,
    series = series.map { MonthPoint(YearMonth.parse(it.month), it.incomeCents, it.expenseCents) },
    expensesByCategory = expensesByCategory.map { it.toModel() },
    incomeByCategory = incomeByCategory.map { it.toModel() },
    openInvoices = openInvoices.map {
        OpenInvoice(it.card.toModel(), it.limitCents, it.availableLimitCents, it.invoice.toModel())
    },
)

internal fun AccountInput.toDto() =
    AccountWriteDto(name, kind.wire, initialBalanceCents, color.wire, icon, includeInTotal)

internal fun CategoryInput.toDto() = CategoryWriteDto(kind.wire, name, color.wire, icon)

internal fun CardInput.toDto() = CreditCardWriteDto(name, limitCents, closingDay, dueDay, paymentAccountId, color.wire)

internal fun TransactionInput.toCreateDto() = TransactionCreateDto(
    kind = kind.wire,
    amountCents = amountCents,
    occurredOn = occurredOn.toString(),
    occurredTime = occurredTime?.toString(),
    description = description,
    notes = notes,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    creditCardId = creditCardId,
    categoryId = categoryId,
    installments = installments?.takeIf { it > 1 && creditCardId != null && kind == TransactionKind.Expense },
)

internal fun TransactionInput.toUpdateDto() = TransactionUpdateDto(
    amountCents = amountCents,
    occurredOn = occurredOn.toString(),
    occurredTime = occurredTime?.toString(),
    description = description,
    notes = notes,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    creditCardId = creditCardId,
    categoryId = categoryId,
)

internal fun PaymentInput.toDto() = PaymentCreateDto(amountCents, accountId, occurredOn.toString())
