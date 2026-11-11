package com.henriquesebastiao.prumo.feature.habits

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

class HabitsSummaryTest {
    private fun summary(): JsonObject = NetworkJson.parseToJsonElement(Fixtures.read("dashboard.json"))
        .jsonObject["modules"]!!.jsonArray
        .map { it.jsonObject }
        .first { it["module"]!!.jsonPrimitive.content == HabitsFeature.ID }

    @Test
    fun theRealSummaryDecodes() {
        val decoded = decodeOrNull(HabitsSummaryDto.serializer(), summary())!!
        assertEquals(0, decoded.doneToday)
        assertEquals(4, decoded.scheduledToday)
        assertEquals(listOf("Academia", "Anki", "Beber água"), decoded.pending.map { it.name })
        assertEquals(1, decoded.pendingMore)
        assertEquals(8, decoded.quit.single().cleanDays)
    }

    @Test
    fun theDtosFollowTheContract() {
        OpenApiContract.assertReads(HabitsSummaryDto.serializer().descriptor, "HabitsSummary")
        OpenApiContract.assertReads(SummaryHabitDto.serializer().descriptor, "HabitRef")
        OpenApiContract.assertReads(SummaryQuitDto.serializer().descriptor, "HabitsSummaryQuit")
    }
}
