package com.henriquesebastiao.prumo.feature.studies

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

class StudiesSummaryTest {
    private fun summary(): JsonObject = NetworkJson.parseToJsonElement(Fixtures.read("dashboard.json"))
        .jsonObject["modules"]!!.jsonArray
        .map { it.jsonObject }
        .first { it["module"]!!.jsonPrimitive.content == StudiesFeature.ID }

    @Test
    fun theRealSummaryDecodes() {
        val decoded = decodeOrNull(StudiesSummaryDto.serializer(), summary())!!
        assertEquals(30, decoded.doneMinutesToday)
        assertEquals(75, decoded.plannedMinutesToday)
        assertEquals(listOf("Inglês", "FastAPI"), decoded.blocksToday.map { it.subject.name })
        assertEquals(listOf("met", "in_progress"), decoded.blocksToday.map { it.status })
        assertEquals(1, decoded.week.daysMet)
        assertEquals(2, decoded.week.daysPlanned)
    }

    @Test
    fun theDtosFollowTheContract() {
        OpenApiContract.assertReads(StudiesSummaryDto.serializer().descriptor, "StudiesSummary")
        OpenApiContract.assertReads(SummaryBlockDto.serializer().descriptor, "SummaryBlock")
        OpenApiContract.assertReads(SummarySubjectDto.serializer().descriptor, "SubjectRef")
        OpenApiContract.assertReads(SummaryWeekDto.serializer().descriptor, "SummaryWeek")
    }
}
