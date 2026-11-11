package com.henriquesebastiao.prumo.feature.habits.data

import com.henriquesebastiao.prumo.core.model.wireOf
import com.henriquesebastiao.prumo.feature.habits.model.CalendarDay
import com.henriquesebastiao.prumo.feature.habits.model.CalendarMonth
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.Frequency
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyState
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitDetail
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitRef
import com.henriquesebastiao.prumo.feature.habits.model.HabitsToday
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Pause
import com.henriquesebastiao.prumo.feature.habits.model.Relapse
import com.henriquesebastiao.prumo.feature.habits.model.TodayItem
import com.henriquesebastiao.prumo.feature.habits.model.TodayState
import com.henriquesebastiao.prumo.feature.habits.model.WeekProgress
import com.henriquesebastiao.prumo.feature.habits.model.weekdayOf
import com.henriquesebastiao.prumo.feature.habits.model.wire
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

internal fun color(wire: String) = wireOf(wire, HabitColor.Amber)

private fun measure(wire: String?) = wire?.let { wireOf(it, Measure.Check) }

internal fun FrequencyDto.toModel() = Frequency(
    id = id,
    validFrom = LocalDate.parse(validFrom),
    validUntil = validUntil?.let(LocalDate::parse),
    state = wireOf(state, FrequencyState.Past),
    kind = wireOf(kind, FrequencyKind.Daily),
    timesPerWeek = timesPerWeek,
    weekdays = weekdays.orEmpty().map(::weekdayOf),
)

internal fun HabitDto.toModel() = Habit(
    id = id,
    name = name,
    kind = wireOf(kind, HabitKind.Build),
    color = color(color),
    icon = icon,
    startedOn = LocalDate.parse(startedOn),
    measure = measure(measure),
    targetValue = targetValue,
    unit = unit,
    archived = archivedAt != null,
    frequency = frequency?.toModel(),
    paused = paused,
    ratePercent = ratePercent,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    streakInWeeks = streakUnit == "week",
    week = week?.let { WeekProgress(it.done, it.target) },
    today = today?.let { TodayState(it.scheduled, it.done, it.value) },
    cleanSince = cleanSince?.let(Instant::parse),
    cleanDays = cleanDays,
    bestCleanDays = bestCleanDays,
    relapses30d = relapses30d,
)

internal fun HabitDetailDto.toModel() = HabitDetail(
    habit = habit().toModel(),
    frequencies = frequencies.map { it.toModel() },
    pauses = pauses.map { Pause(it.id, LocalDate.parse(it.startsOn), it.endsOn?.let(LocalDate::parse)) },
    relapses = relapses.map { Relapse(it.id, Instant.parse(it.occurredAt), it.note) },
)

internal fun CalendarDto.toModel() = CalendarMonth(
    month = YearMonth.parse(month),
    today = LocalDate.parse(today),
    // A state a newer server adds reads as not scheduled: no color claims more than it knows.
    days = days.map { CalendarDay(LocalDate.parse(it.on), wireOf(it.status, DayState.Unscheduled), it.value) },
    previous = YearMonth.parse(previousMonth),
    next = nextMonth?.let(YearMonth::parse),
)

internal fun HabitRefDto.toModel() = HabitRef(id, name, color(color), icon, measure(measure), targetValue, unit)

internal fun TodayDto.toModel() = HabitsToday(
    today = LocalDate.parse(today),
    items = items.map {
        TodayItem(
            it.habit.toModel(),
            it.done,
            it.value,
            it.week?.let { w ->
                WeekProgress(w.done, w.target)
            },
            it.settled,
        )
    },
    done = done,
    scheduled = scheduled,
)

internal fun FrequencyInput.toFields() = FrequencyFieldsDto(
    kind = kind.wire,
    timesPerWeek = timesPerWeek.takeIf { kind == FrequencyKind.Weekly },
    weekdays = weekdays.map { it.wire }.sorted().takeIf { kind == FrequencyKind.Weekdays },
)

internal fun HabitInput.toCreateDto(): HabitCreateDto {
    val build = kind == HabitKind.Build
    val quantity = build && measure == Measure.Quantity
    return HabitCreateDto(
        name = name,
        kind = kind.wire,
        color = color.wire,
        icon = icon,
        startedOn = startedOn.toString(),
        measure = measure?.wire.takeIf { build },
        targetValue = targetValue.takeIf { quantity },
        unit = unit.takeIf { quantity },
        frequency = frequency?.toFields().takeIf { build },
    )
}

internal fun HabitInput.toUpdateDto(quantity: Boolean) = HabitUpdateDto(
    name = name,
    color = color.wire,
    icon = icon,
    startedOn = startedOn.toString(),
    targetValue = targetValue.takeIf { quantity },
    unit = unit.takeIf { quantity },
)
