package com.henriquesebastiao.prumo.feature.reading

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

class ReadingSummaryTest {
    private fun summary(): JsonObject = NetworkJson.parseToJsonElement(Fixtures.read("dashboard.json"))
        .jsonObject["modules"]!!.jsonArray
        .map { it.jsonObject }
        .first { it["module"]!!.jsonPrimitive.content == ReadingFeature.ID }

    @Test
    fun theRealSummaryDecodes() {
        val decoded = decodeOrNull(ReadingSummaryDto.serializer(), summary())!!
        val current = decoded.current!!
        assertEquals("Código Limpo", current.book.title)
        assertEquals(212, current.currentPage)
        assertEquals(50, current.progressPercent)
    }

    @Test
    fun theDtosFollowTheContract() {
        OpenApiContract.assertReads(ReadingSummaryDto.serializer().descriptor, "ReadingSummary")
        OpenApiContract.assertReads(CurrentBookDto.serializer().descriptor, "CurrentBook")
        OpenApiContract.assertReads(BookRefDto.serializer().descriptor, "BookRef")
    }
}
