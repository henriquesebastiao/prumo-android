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
        assertEquals(1, decoded.doneToday)
        assertEquals(6, decoded.scheduledToday)
        assertEquals("2026-11-17", decoded.today)
        assertEquals(listOf("Alongar", "Regar as plantas", "Tarefa do mês"), decoded.pending.map { it.name })
        assertEquals(2, decoded.pendingMore)
        // The next open ones in the order of the day, to mark from the card (D48 of the server).
        assertEquals(
            listOf("Alongar", "Regar as plantas", "Tarefa do mês", "Anki", "Beber água"),
            decoded.upcoming.map { it.habit.name },
        )
        assertEquals("07:00", decoded.upcoming.first().habit.timeOfDay)
        assertEquals(true, decoded.upcoming.first().late)
        assertEquals(8, decoded.quit.single().cleanDays)
    }

    @Test
    fun theDtosFollowTheContract() {
        OpenApiContract.assertReads(HabitsSummaryDto.serializer().descriptor, "HabitsSummary")
        OpenApiContract.assertReads(SummaryHabitDto.serializer().descriptor, "HabitRef")
        OpenApiContract.assertReads(SummaryQuitDto.serializer().descriptor, "HabitsSummaryQuit")
        OpenApiContract.assertReads(
            com.henriquesebastiao.prumo.feature.habits.data.DayItemDto.serializer().descriptor,
            "HabitDayItem",
        )
    }
}
