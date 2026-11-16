package com.henriquesebastiao.prumo.feature.habits.ui

import com.henriquesebastiao.prumo.feature.habits.model.Cadence
import com.henriquesebastiao.prumo.feature.habits.model.CalendarDay
import com.henriquesebastiao.prumo.feature.habits.model.CalendarMonth
import com.henriquesebastiao.prumo.feature.habits.model.DayItem
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.Frequency
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyState
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitDetail
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitRef
import com.henriquesebastiao.prumo.feature.habits.model.HabitsDay
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Pause
import com.henriquesebastiao.prumo.feature.habits.model.Period
import com.henriquesebastiao.prumo.feature.habits.model.PeriodProgress
import com.henriquesebastiao.prumo.feature.habits.model.Promotion
import com.henriquesebastiao.prumo.feature.habits.model.QuitItem
import com.henriquesebastiao.prumo.feature.habits.model.Relapse
import com.henriquesebastiao.prumo.feature.habits.model.StreakUnit
import com.henriquesebastiao.prumo.feature.habits.model.TodayState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Hábitos as the demo server has it on 17/11/2026 (backend/scripts/e2e_server.py), for previews and tests. */
internal object HabitsPreview {
    private val day17 = LocalDate.of(2026, 11, 17)

    private fun frequency(id: String, from: LocalDate, rule: FrequencyInput, cadence: Cadence) =
        Frequency(id, from, null, FrequencyState.Current, rule, cadence)

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
        value: Int? = null,
        time: LocalTime? = null,
        unitOfStreak: StreakUnit = StreakUnit.Day,
        period: PeriodProgress? = null,
        nextDue: LocalDate? = day17,
    ) = Habit(
        id = id, name = name, kind = HabitKind.Build, color = color, icon = icon, startedOn = frequency.validFrom,
        measure = measure, targetValue = target, unit = unit, archived = false, timeOfDay = time, position = 0,
        frequency = frequency, paused = false, ratePercent = rate, currentStreak = streak, bestStreak = best,
        streakUnit = unitOfStreak, period = period, nextDue = nextDue, today = TodayState(true, false, value),
        cleanSince = null, cleanDays = null, bestCleanDays = null, relapses30d = null,
    )

    private val gymWeek =
        PeriodProgress(Period.Week, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22), 1, 3, 6, false)

    val gym = build(
        "h-gym", "Academia", HabitColor.Orange, "dumbbell",
        frequency(
            "f-gym",
            LocalDate.of(2026, 10, 5),
            FrequencyInput(kind = FrequencyKind.PerPeriod, times = 3, period = Period.Week),
            Cadence.Weekly,
        ),
        75, 3, 3, unitOfStreak = StreakUnit.Week, period = gymWeek,
    )
    val anki = build(
        "h-anki",
        "Anki",
        HabitColor.Blue,
        "graduation-cap",
        frequency("f-anki", LocalDate.of(2026, 11, 1), FrequencyInput(kind = FrequencyKind.Weekdays), Cadence.Weekly),
        83,
        1,
        9,
    )
    val water = build(
        "h-water", "Beber água", HabitColor.Cyan, "droplet",
        frequency("f-water", LocalDate.of(2026, 10, 15), FrequencyInput(), Cadence.Daily), 69, 1, 3,
        measure = Measure.Quantity, target = 2000, unit = "ml", value = 1250,
    )
    val bible = build(
        "h-bible",
        "Ler a Bíblia",
        HabitColor.Amber,
        "book-open",
        frequency("f-bible", LocalDate.of(2026, 9, 1), FrequencyInput(), Cadence.Daily),
        100,
        41,
        41,
        time = LocalTime.of(5, 15),
        nextDue = LocalDate.of(2026, 11, 18),
    )
    val stretch = build(
        "h-stretch", "Alongar", HabitColor.Teal, "heart-pulse",
        frequency("f-stretch", LocalDate.of(2026, 11, 1), FrequencyInput(), Cadence.Daily), 94, 6, 9,
        time = LocalTime.of(7, 0),
    )
    val sheets = build(
        "h-sheets", "Trocar roupa de cama", HabitColor.Pink, "moon",
        frequency(
            "f-sheets",
            LocalDate.of(2026, 10, 10),
            FrequencyInput(kind = FrequencyKind.Weekdays, weekdays = setOf(DayOfWeek.SATURDAY), everyNWeeks = 2),
            Cadence.Fortnightly,
        ),
        100, 3, 3, unitOfStreak = StreakUnit.Occurrence, nextDue = LocalDate.of(2026, 11, 21),
    )
    val chore = build(
        "h-chore", "Tarefa do mês", HabitColor.Green, "sparkles",
        frequency(
            "f-chore",
            LocalDate.of(2026, 9, 1),
            FrequencyInput(kind = FrequencyKind.PerPeriod, times = 1, period = Period.Month, preferredDay = 15),
            Cadence.Monthly,
        ),
        100, 2, 2, unitOfStreak = StreakUnit.Month,
        period = PeriodProgress(Period.Month, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 0, 1, 14, false),
    )
    val social = Habit(
        id = "h-social", name = "Redes sociais antes de dormir", kind = HabitKind.Quit, color = HabitColor.Red,
        icon = "smartphone", startedOn = LocalDate.of(2026, 8, 1), measure = null, targetValue = null, unit = null,
        archived = false, timeOfDay = null, position = 9, frequency = null, paused = false, ratePercent = null,
        currentStreak = null, bestStreak = null, streakUnit = StreakUnit.Day, period = null, nextDue = null,
        today = null, cleanSince = Instant.parse("2026-11-09T02:30:00Z"), cleanDays = 8, bestCleanDays = 42,
        relapses30d = 1,
    )

    val habits = listOf(bible, stretch, chore, gym, anki, water, sheets, social)

    private fun ref(habit: Habit) = HabitRef(
        habit.id,
        habit.name,
        habit.color,
        habit.icon,
        habit.measure,
        habit.targetValue,
        habit.unit,
        habit.timeOfDay,
    )

    val day = HabitsDay(
        on = day17,
        today = day17,
        now = LocalTime.of(14, 30),
        done = 1,
        scheduled = 5,
        timeline = listOf(DayItem(ref(stretch), done = false, value = null, settled = false, late = true)),
        untimed = listOf(
            DayItem(
                ref(chore),
                done = false,
                value = null,
                settled = false,
                period = chore.period,
                promoted = Promotion.PreferredDay,
            ),
            DayItem(ref(anki), done = false, value = null, settled = false),
            DayItem(ref(water), done = false, value = 1250, settled = false),
        ),
        period = listOf(DayItem(ref(gym), done = false, value = null, settled = false, period = gymWeek)),
        doneItems = listOf(DayItem(ref(bible), done = true, value = 1, settled = true)),
        quit = listOf(QuitItem(social.id, social.name, social.color, social.icon, 8)),
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
    val editorEvents = HabitEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}
