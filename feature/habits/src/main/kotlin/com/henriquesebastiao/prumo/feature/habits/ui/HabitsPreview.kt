package com.henriquesebastiao.prumo.feature.habits.ui

import com.henriquesebastiao.prumo.feature.habits.model.CalendarDay
import com.henriquesebastiao.prumo.feature.habits.model.CalendarMonth
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.Frequency
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyState
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitDetail
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitRef
import com.henriquesebastiao.prumo.feature.habits.model.HabitsToday
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Pause
import com.henriquesebastiao.prumo.feature.habits.model.Relapse
import com.henriquesebastiao.prumo.feature.habits.model.TodayItem
import com.henriquesebastiao.prumo.feature.habits.model.TodayState
import com.henriquesebastiao.prumo.feature.habits.model.WeekProgress
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Hábitos as the demo server has it on 17/11/2026 (backend/scripts/e2e_server.py), for previews and tests. */
internal object HabitsPreview {
    private val day17 = LocalDate.of(2026, 11, 17)

    private fun frequency(
        id: String,
        from: LocalDate,
        kind: FrequencyKind,
        times: Int? = null,
        days: List<DayOfWeek> = emptyList(),
        state: FrequencyState = FrequencyState.Current,
    ) = Frequency(id, from, null, state, kind, times, days)

    @Suppress("LongParameterList")
    private fun build(
        id: String,
        name: String,
        color: HabitColor,
        icon: String,
        frequency: Frequency,
        rate: Int?,
        streak: Int,
        best: Int,
        measure: Measure = Measure.Check,
        target: Int? = null,
        unit: String? = null,
        week: WeekProgress? = null,
        value: Int? = null,
    ) = Habit(
        id = id, name = name, kind = HabitKind.Build, color = color, icon = icon, startedOn = frequency.validFrom,
        measure = measure, targetValue = target, unit = unit, archived = false, frequency = frequency, paused = false,
        ratePercent = rate, currentStreak = streak, bestStreak = best,
        streakInWeeks =
            frequency.kind == FrequencyKind.Weekly,
        week = week, today = TodayState(true, false, value), cleanSince = null, cleanDays = null, bestCleanDays = null,
        relapses30d = null,
    )

    val gym = build(
        "h-gym", "Academia", HabitColor.Orange, "dumbbell",
        frequency(
            "f-gym",
            LocalDate.of(2026, 10, 5),
            FrequencyKind.Weekly,
            times = 3,
        ),
        75, 3, 3, week = WeekProgress(1, 3),
    )
    val anki = build(
        "h-anki",
        "Anki",
        HabitColor.Blue,
        "graduation-cap",
        frequency("f-anki", LocalDate.of(2026, 11, 1), FrequencyKind.Weekdays, days = DayOfWeek.entries.take(5)),
        83,
        1,
        9,
    )
    val water = build(
        "h-water", "Beber água", HabitColor.Cyan, "droplet",
        frequency("f-water", LocalDate.of(2026, 10, 15), FrequencyKind.Daily), 69, 1, 3,
        measure = Measure.Quantity, target = 2000, unit = "ml", value = 1250,
    )
    val bible = build(
        "h-bible",
        "Ler a Bíblia",
        HabitColor.Amber,
        "book-open",
        frequency("f-bible", LocalDate.of(2026, 9, 1), FrequencyKind.Daily),
        100,
        40,
        40,
    )
    val social = Habit(
        id = "h-social", name = "Redes sociais antes de dormir", kind = HabitKind.Quit, color = HabitColor.Red,
        icon = "smartphone", startedOn = LocalDate.of(2026, 8, 1), measure = null, targetValue = null, unit = null,
        archived = false, frequency = null, paused = false, ratePercent = null, currentStreak = null, bestStreak = null,
        streakInWeeks = false, week = null, today = null, cleanSince = Instant.parse("2026-11-09T02:30:00Z"),
        cleanDays = 8, bestCleanDays = 42, relapses30d = 1,
    )

    val habits = listOf(gym, anki, water, bible, social)

    private fun ref(habit: Habit) =
        HabitRef(habit.id, habit.name, habit.color, habit.icon, habit.measure, habit.targetValue, habit.unit)

    val today = HabitsToday(
        today = day17,
        items = listOf(
            TodayItem(ref(gym), done = false, value = null, week = WeekProgress(1, 3), settled = false),
            TodayItem(ref(anki), done = false, value = null, week = null, settled = false),
            TodayItem(ref(water), done = false, value = 1250, week = null, settled = false),
            TodayItem(ref(bible), done = true, value = 1, week = null, settled = true),
        ),
        done = 1,
        scheduled = 4,
    )

    val bibleDetail = HabitDetail(
        habit = bible,
        frequencies = listOf(bible.frequency!!),
        pauses = listOf(Pause("p1", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 24))),
        relapses = emptyList(),
    )

    val quitDetail = HabitDetail(
        habit = social,
        frequencies = emptyList(),
        pauses = emptyList(),
        relapses = listOf(
            Relapse("r3", Instant.parse("2026-11-09T02:30:00Z"), "Domingo entediado."),
            Relapse("r2", Instant.parse("2026-10-04T01:30:00Z"), "Noite de prova, fiquei ansioso."),
            Relapse("r1", Instant.parse("2026-09-13T02:00:00Z"), null),
        ),
    )

    val calendar = CalendarMonth(
        month = YearMonth.of(2026, 11),
        today = day17,
        days = (1..30).map { day ->
            val on = LocalDate.of(2026, 11, day)
            val state = when {
                day > 17 -> DayState.Future
                day == 17 -> DayState.Pending
                else -> DayState.Done
            }
            CalendarDay(on, state, if (state == DayState.Done) 1 else null)
        },
        previous = YearMonth.of(2026, 10),
        next = null,
    )

    val todayEvents = TodayEvents({}, { _, _ -> }, {}, {})
    val listEvents = ListEvents({}, {}, {}, {})
    val detailEvents = HabitDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    val editorEvents = HabitEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}
