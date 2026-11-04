package com.henriquesebastiao.prumo.feature.finance

import com.henriquesebastiao.prumo.core.network.NetworkJson
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceSummaryTest {
    private fun summary(): JsonObject = NetworkJson.parseToJsonElement(Fixtures.read("dashboard.json"))
        .jsonObject["modules"]!!.jsonArray
        .map { it.jsonObject }
        .first { it["module"]!!.jsonPrimitive.content == FinanceFeature.ID }

    @Test
    fun theRealSummaryDecodes() {
        val decoded = decodeOrNull(FinanceSummaryDto.serializer(), summary())!!
        assertEquals("2026-11", decoded.month)
        assertEquals(715_427L, decoded.totalBalanceCents)
        val invoice = decoded.nextInvoice!!
        assertEquals("Nubank", invoice.card.name)
        assertEquals(128_460L, invoice.invoice.amountCents)
    }

    @Test
    fun theDtosFollowTheContract() {
        OpenApiContract.assertReads(FinanceSummaryDto.serializer().descriptor, "FinanceSummary")
        OpenApiContract.assertReads(SummaryOpenInvoiceDto.serializer().descriptor, "OpenInvoice")
        OpenApiContract.assertReads(SummaryCardDto.serializer().descriptor, "CardRef")
        OpenApiContract.assertReads(SummaryInvoiceDto.serializer().descriptor, "InvoiceRead")
    }
}
