package com.henriquesebastiao.prumo.feature.habits.data

import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyState
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hand-written DTOs of Hábitos against the server's openapi.json (D04). */
class HabitsContractTest {
    @Test
    fun theReadDtosFollowTheContract() {
        OpenApiContract.assertReads(WeekDto.serializer().descriptor, "HabitWeek")
        OpenApiContract.assertReads(TodayStateDto.serializer().descriptor, "HabitTodayState")
        OpenApiContract.assertReads(FrequencyDto.serializer().descriptor, "HabitFrequencyRead")
        OpenApiContract.assertReads(HabitDto.serializer().descriptor, "HabitRead")
        OpenApiContract.assertReads(PauseDto.serializer().descriptor, "HabitPauseRead")
        OpenApiContract.assertReads(RelapseDto.serializer().descriptor, "RelapseRead")
        OpenApiContract.assertReads(HabitDetailDto.serializer().descriptor, "HabitDetail")
        OpenApiContract.assertReads(CalendarDayDto.serializer().descriptor, "HabitCalendarDay")
        OpenApiContract.assertReads(CalendarDto.serializer().descriptor, "HabitCalendar")
        OpenApiContract.assertReads(HabitRefDto.serializer().descriptor, "HabitRef")
        OpenApiContract.assertReads(TodayItemDto.serializer().descriptor, "HabitTodayItem")
        OpenApiContract.assertReads(TodayDto.serializer().descriptor, "HabitsToday")
    }

    @Test
    fun theWriteDtosFollowTheContract() {
        OpenApiContract.assertWrites(FrequencyFieldsDto.serializer().descriptor, "HabitFrequencyFields")
        OpenApiContract.assertWrites(HabitCreateDto.serializer().descriptor, "HabitCreate")
        OpenApiContract.assertWrites(HabitUpdateDto.serializer().descriptor, "HabitUpdate")
        OpenApiContract.assertWrites(FrequencyCreateDto.serializer().descriptor, "HabitFrequencyCreate")
        OpenApiContract.assertWrites(FrequencyUpdateDto.serializer().descriptor, "HabitFrequencyUpdate")
        OpenApiContract.assertWrites(EntryWriteDto.serializer().descriptor, "HabitEntryWrite")
        OpenApiContract.assertWrites(PauseCreateDto.serializer().descriptor, "HabitPauseCreate")
        OpenApiContract.assertWrites(PauseUpdateDto.serializer().descriptor, "HabitPauseUpdate")
        OpenApiContract.assertWrites(RelapseCreateDto.serializer().descriptor, "RelapseCreate")
    }

    @Test
    fun theEnumsAreTheServers() {
        assertEquals(OpenApiContract.enumValues("CalendarStatus"), DayState.entries.map { it.wire }.toSet())
        assertEquals(OpenApiContract.enumValues("HabitFrequencyKind"), FrequencyKind.entries.map { it.wire }.toSet())
        assertEquals(
            OpenApiContract.enumValues("HabitFrequencyRead", "state"),
            FrequencyState.entries.map {
                it.wire
            }.toSet(),
        )
        assertEquals(OpenApiContract.enumValues("HabitCreate", "kind"), HabitKind.entries.map { it.wire }.toSet())
        assertEquals(OpenApiContract.enumValues("HabitCreate", "color"), HabitColor.entries.map { it.wire }.toSet())
        // `measure` is optional (null in a habit to quit): its enum sits in the first `anyOf`.
        val measure = OpenApiContract.schema(
            "HabitCreate",
        )["properties"]!!.jsonObject["measure"]!!.jsonObject["anyOf"]!!
            .jsonArray[0].jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(measure, Measure.entries.map { it.wire }.toSet())
    }
}
