package com.henriquesebastiao.prumo.feature.finance.data

import app.cash.turbine.test
import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.ActiveSession
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.SessionGateway
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.core.network.testing.NoCache
import com.henriquesebastiao.prumo.feature.finance.model.CardInput
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.PaymentInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionFilter
import com.henriquesebastiao.prumo.feature.finance.model.TransactionInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinanceRepositoryTest {
    private val server = MockWebServer()
    private val changes = DataChanges()
    private lateinit var repository: DefaultFinanceRepository

    @Before
    fun setUp() {
        server.start()
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        repository = DefaultFinanceRepository(ServerCalls(ApiFactory(OkHttpClient()), gateway, NoCache), changes)
    }

    @After
    fun tearDown() = server.close()

    private fun json(status: Int, body: String) = MockResponse.Builder()
        .code(status)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun sentBody(): JsonObject = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    private fun first(page: String): String =
        Json.parseToJsonElement(page).jsonObject["items"]!!.jsonArray.first().toString()

    private val expense = TransactionInput(
        kind = TransactionKind.Expense,
        amountCents = 18_746,
        occurredOn = LocalDate.of(2026, 11, 17),
        occurredTime = LocalTime.of(14, 30),
        description = "Mercado Bom Preço",
        notes = null,
        accountId = null,
        destinationAccountId = null,
        creditCardId = "card",
        categoryId = "food",
        installments = 3,
    )

    @Test
    fun theFiltersGoAsQueryParameters() = runTest {
        server.enqueue(json(200, Fixtures.read("finance/transactions.json")))
        val filter = TransactionFilter(
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2026, 11, 30),
            kind = TransactionKind.Expense,
            categoryId = "food",
            query = "  212,40 ",
        )

        val page = (repository.transactions(filter, offset = 50) as Outcome.Ok).value

        assertEquals(65, page.total)
        val url = server.takeRequest().url
        assertEquals("/api/v1/finance/transactions", url.encodedPath)
        assertEquals("2026-11-01", url.queryParameter("from"))
        assertEquals("2026-11-30", url.queryParameter("to"))
        assertEquals("expense", url.queryParameter("kind"))
        assertEquals("food", url.queryParameter("category_id"))
        assertEquals("212,40", url.queryParameter("q"))
        assertEquals("50", url.queryParameter("offset"))
        assertEquals(null, url.queryParameter("account_id"))
    }

    @Test
    fun aCardPurchaseSendsItsInstallmentsAndNoInvoice() = runTest {
        server.enqueue(json(201, Fixtures.read("finance/transaction_installment.json")))

        repository.createTransaction(expense)

        val sent = sentBody()
        assertEquals("expense", sent["kind"]!!.jsonPrimitive.content)
        assertEquals("18746", sent["amount_cents"]!!.jsonPrimitive.content)
        assertEquals("14:30", sent["occurred_time"]!!.jsonPrimitive.content)
        assertEquals("3", sent["installments"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["account_id"])
        assertFalse("invoice_closing_on" in sent)
    }

    @Test
    fun anEditLeavesTheKindAndTheInvoiceOut() = runTest {
        server.enqueue(json(200, Fixtures.read("finance/transaction_installment.json")))

        repository.updateTransaction("t1", expense.copy(notes = null, occurredTime = null))

        val sent = sentBody()
        assertFalse("kind" in sent)
        assertFalse("installments" in sent)
        assertFalse("invoice_closing_on" in sent)
        assertEquals(JsonNull, sent["occurred_time"])
        assertEquals(JsonNull, sent["notes"])
    }

    @Test
    fun aTransferKeepsAnEmptyDescription() = runTest {
        server.enqueue(json(201, first(Fixtures.read("finance/transactions.json"))))
        val transfer = expense.copy(
            kind = TransactionKind.Transfer,
            description = "",
            accountId = "inter",
            destinationAccountId = "nubank",
            creditCardId = null,
            categoryId = null,
        )

        repository.createTransaction(transfer)

        val sent = sentBody()
        assertEquals("", sent["description"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["installments"])
        assertEquals("inter", repository.lastAccountId)
    }

    @Test
    fun deletingAnInstallmentCanTakeTheWholePurchase() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        server.enqueue(MockResponse.Builder().code(204).build())

        repository.deleteTransaction("t1")
        repository.deleteTransaction("t1", allInstallments = true)

        assertEquals("one", server.takeRequest().url.queryParameter("installments"))
        assertEquals("all", server.takeRequest().url.queryParameter("installments"))
    }

    @Test
    fun aCategoryInUseIsRefusedWithItsCount() = runTest {
        server.enqueue(json(422, Fixtures.read("finance/error_category_in_use.json")))

        val error = (repository.deleteCategory("food") as Outcome.Failed).error as AppError.Api

        assertEquals("finance.category_in_use", error.code)
        assertEquals("16", error.details["transactions_count"])
    }

    @Test
    fun aPaymentGoesToTheInvoiceAndRemembersTheAccount() = runTest {
        repository.changes.test {
            server.enqueue(json(201, first(Fixtures.read("finance/transactions.json"))))

            repository.payInvoice(
                "card",
                LocalDate.of(2026, 12, 3),
                PaymentInput(50_000, "inter", LocalDate.of(2026, 11, 17)),
            )

            awaitItem()
            val request = server.takeRequest()
            assertEquals("/api/v1/finance/credit-cards/card/invoices/2026-12-03/payments", request.url.encodedPath)
            val sent = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals("2026-11-17", sent["occurred_on"]!!.jsonPrimitive.content)
            assertEquals("inter", repository.lastAccountId)
        }
    }

    @Test
    fun aCardWithoutPaymentAccountSendsNull() = runTest {
        server.enqueue(json(200, Fixtures.read("finance/credit_card.json")))

        repository.updateCard("card", CardInput("Nubank", 500_000, 3, 10, null, FinanceColor.Violet))

        val sent = sentBody()
        assertEquals(JsonNull, sent["payment_account_id"])
        assertEquals("violet", sent["color"]!!.jsonPrimitive.content)
    }

    @Test
    fun theOverviewAsksForTheMonth() = runTest {
        server.enqueue(json(200, Fixtures.read("finance/overview.json")))

        assertTrue(repository.overview(YearMonth.of(2026, 10)) is Outcome.Ok)

        assertEquals("2026-10", server.takeRequest().url.queryParameter("month"))
    }
}
