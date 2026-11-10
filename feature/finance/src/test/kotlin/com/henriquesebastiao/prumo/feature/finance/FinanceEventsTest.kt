package com.henriquesebastiao.prumo.feature.finance

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.network.NetworkJson
import com.henriquesebastiao.prumo.core.network.dto.DashboardDto
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.core.network.toModel
import com.henriquesebastiao.prumo.feature.finance.ui.CardDetail
import com.henriquesebastiao.prumo.feature.finance.ui.TransactionDetail
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** How the recorded items of Finanças (fixtures/dashboard.json, the e2e server) read on the Home. */
@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class FinanceEventsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dashboard: Dashboard =
        NetworkJson.decodeFromString(DashboardDto.serializer(), Fixtures.read("dashboard.json")).toModel()

    private val context = HomeContext("BRL", LocalDate.of(2026, 11, 17), ZoneId.of("America/Sao_Paulo"))

    private fun String?.plain() = this?.replace(' ', ' ')

    @Test
    fun invoicesReadWithTheServersValues() {
        val due = dashboard.upcoming.first { it.kind == "finance.invoice_due" }
        val overdue = due.copy(
            data = due.data + mapOf(
                "status" to JsonPrimitive("overdue"),
                "paid_cents" to JsonPrimitive(28460),
                "remaining_cents" to JsonPrimitive(100000),
            ),
        )
        // A server from before remaining_cents: the amount, never a subtraction in the app.
        val older = overdue.copy(data = overdue.data - "remaining_cents")
        var events: List<HomeEvent?> = emptyList()
        compose.setContent {
            events = (dashboard.upcoming.filter { it.module == "finance" } + overdue + older)
                .map { financeUpcoming(it, context) }
        }
        compose.waitForIdle()

        val closing = events[0]!!
        val open = events[1]!!
        val late = events[2]!!
        val old = events[3]!!
        assertEquals("Fatura Nubank fecha", closing.title)
        assertEquals("Limite disponível R$ 3.715,40", closing.subtitle.plain())
        assertEquals(CardDetail("01a1ea85-ce05-77f7-b6ef-0ee13f0bd35d"), closing.target)
        assertEquals("Fatura Nubank vence", open.title)
        assertEquals("R$ 1.284,60 até agora", open.subtitle.plain())
        assertEquals("Vencida · faltam R$ 1.000,00", late.subtitle.plain())
        assertEquals("R$ 1.284,60 até agora", old.subtitle.plain())
    }

    @Test
    fun transactionsReadSignedWithWhereTheyWent() {
        var events: List<HomeEvent> = emptyList()
        compose.setContent {
            events = dashboard.activity.filter { it.module == "finance" }.mapNotNull { financeActivity(it, context) }
        }
        compose.waitForIdle()

        val bakery = events.first { it.title.startsWith("Padaria ·") }
        assertEquals("Padaria · Alimentação", bakery.title)
        assertEquals("Hoje, 08:10 · Carteira", bakery.subtitle)
        assertEquals("−R$ 18,50", bakery.value.plain())
        assertFalse(bakery.positive)
        assertEquals(TransactionDetail("01a1ea85-ce66-777d-b905-ca3e9079aeae"), bakery.target)
        val fuel = events.first { it.title.startsWith("Posto") }
        assertEquals("Ontem, 18:10 · Cartão Nubank", fuel.subtitle)
        // Without a time of its own, only the day.
        assertEquals("15/11 · Cartão Nubank", events.first { it.title.startsWith("Streaming") }.subtitle)
    }

    @Test
    fun transfersAndIncomesReadLikeTheWeb() {
        val base = dashboard.activity.first { it.kind == "finance.transaction" }
        val transfer = base.copy(
            subject = "",
            data = mapOf(
                "transaction_kind" to JsonPrimitive("transfer"),
                "amount_cents" to JsonPrimitive(50000),
                "account" to JsonPrimitive("Inter"),
                "destination_account" to JsonPrimitive("Nubank"),
            ),
        )
        val income = base.copy(
            subject = "Salário",
            data = mapOf("transaction_kind" to JsonPrimitive("income"), "amount_cents" to JsonPrimitive(530000)),
        )
        var events: List<HomeEvent?> = emptyList()
        compose.setContent { events = listOf(transfer, income).map { financeActivity(it, context) } }
        compose.waitForIdle()

        assertEquals("Transferência", events[0]!!.title)
        assertEquals("Hoje · Inter → Nubank", events[0]!!.subtitle)
        assertEquals("R$ 500,00", events[0]!!.value.plain())
        assertEquals("+R$ 5.300,00", events[1]!!.value.plain())
        assertTrue(events[1]!!.positive)
    }
}
