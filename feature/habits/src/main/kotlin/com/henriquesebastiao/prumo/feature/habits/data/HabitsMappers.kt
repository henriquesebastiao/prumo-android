package com.henriquesebastiao.prumo.feature.habits.data

import com.henriquesebastiao.prumo.core.model.wireOf
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
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
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
import com.henriquesebastiao.prumo.feature.habits.model.weekdayOf
import com.henriquesebastiao.prumo.feature.habits.model.wire
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale

internal fun color(wire: String) = wireOf(wire, HabitColor.Amber)

private fun measure(wire: String?) = wire?.let { wireOf(it, Measure.Check) }

private fun time(wire: String?): LocalTime? = wire?.let(LocalTime::parse)

/** The fields of a version; `weekly` (older servers) reads as a weekly period. */
internal fun FrequencyDto.rule(): FrequencyInput {
    val defaults = FrequencyInput()
    val legacy = kind == FrequencyKind.Weekly.wire
    return FrequencyInput(
        kind = if (legacy) FrequencyKind.PerPeriod else wireOf(kind, FrequencyKind.Daily),
        weekdays = weekdays?.map(::weekdayOf)?.toSet() ?: defaults.weekdays,
        everyNWeeks = everyNWeeks ?: 1,
        monthDays = monthDays?.toSet() ?: defaults.monthDays,
        monthLastDay = monthLastDay ?: false,
        everyNMonths = everyNMonths ?: 1,
        weekOfMonth = weekOfMonth ?: 1,
        weekday = weekday?.let(::weekdayOf) ?: DayOfWeek.SUNDAY,
        times = times ?: timesPerWeek ?: FrequencyInput.DEFAULT_TIMES,
        period = period?.let { wireOf(it, Period.Week) } ?: Period.Week,
        preferredDay = preferredDay,
    )
}

internal fun FrequencyDto.toModel() = Frequency(
    id = id,
    validFrom = LocalDate.parse(validFrom),
    validUntil = validUntil?.let(LocalDate::parse),
    state = wireOf(state, FrequencyState.Past),
    rule = rule(),
    cadence = wireOf(cadence, Cadence.Longer),
)

internal fun PeriodDto.toModel() = PeriodProgress(
    period = wireOf(period, Period.Week),
    startsOn = LocalDate.parse(startsOn),
    endsOn = LocalDate.parse(endsOn),
    done = done,
    target = target,
    daysLeft = daysLeft,
    neutral = neutral,
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
    timeOfDay = time(timeOfDay),
    position = position,
    frequency = frequency?.toModel(),
    paused = paused,
    ratePercent = ratePercent,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    streakUnit = streakUnit?.let { wireOf(it, StreakUnit.Day) } ?: StreakUnit.Day,
    period = period?.toModel(),
    nextDue = nextDue?.let(LocalDate::parse),
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

internal fun HabitRefDto.toModel() =
    HabitRef(id, name, color(color), icon, measure(measure), targetValue, unit, time(timeOfDay))

internal fun DayItemDto.toModel() = DayItem(
    habit = habit.toModel(),
    done = done,
    value = value,
    settled = settled,
    late = late,
    period = period?.toModel(),
    // A reason a newer server adds reads as none: the item still shows, without the hint.
    promoted = promoted?.let { wire -> Promotion.entries.firstOrNull { it.wire == wire } },
    dueInDays = dueInDays,
)

internal fun QuitItemDto.toModel() = QuitItem(id, name, color(color), icon, cleanDays)

internal fun DayDto.toModel() = HabitsDay(
    on = LocalDate.parse(on),
    today = LocalDate.parse(today),
    now = time(now),
    done = done,
    scheduled = scheduled,
    timeline = timeline.map { it.toModel() },
    untimed = untimed.map { it.toModel() },
    period = period.map { it.toModel() },
    doneItems = doneItems.map { it.toModel() },
    quit = quit.map { it.toModel() },
)

/** The fields of [FrequencyInput.kind] only; `weekly` is never sent. */
internal fun FrequencyInput.toFields(): FrequencyFieldsDto = when (kind) {
    FrequencyKind.Weekdays -> FrequencyFieldsDto(
        kind = kind.wire,
        weekdays = weekdays.map { it.wire }.sorted(),
        everyNWeeks = everyNWeeks,
    )

    FrequencyKind.MonthDays -> FrequencyFieldsDto(
        kind = kind.wire,
        monthDays = monthDays.sorted(),
        monthLastDay = monthLastDay,
        everyNMonths = everyNMonths,
    )

    FrequencyKind.MonthWeekday -> FrequencyFieldsDto(
        kind = kind.wire,
        weekOfMonth = weekOfMonth,
        weekday = weekday.wire,
        everyNMonths = everyNMonths,
    )

    FrequencyKind.PerPeriod, FrequencyKind.Weekly -> FrequencyFieldsDto(
        kind = FrequencyKind.PerPeriod.wire,
        times = times.coerceIn(1, period.days),
        period = period.wire,
        preferredDay = preferredDay,
    )

    FrequencyKind.Daily -> FrequencyFieldsDto(kind = kind.wire)
}

internal fun FrequencyFieldsDto.createDto(validFrom: LocalDate) = FrequencyCreateDto(
    validFrom = validFrom.toString(),
    kind = kind,
    weekdays = weekdays,
    everyNWeeks = everyNWeeks,
    monthDays = monthDays,
    monthLastDay = monthLastDay,
    weekOfMonth = weekOfMonth,
    weekday = weekday,
    everyNMonths = everyNMonths,
    times = times,
    period = period,
    preferredDay = preferredDay,
)

private fun LocalTime.wire(): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

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
        timeOfDay = timeOfDay?.wire(),
    )
}

internal fun HabitInput.toUpdateDto(quantity: Boolean) = HabitUpdateDto(
    name = name,
    color = color.wire,
    icon = icon,
    startedOn = startedOn.toString(),
    targetValue = targetValue.takeIf { quantity },
    unit = unit.takeIf { quantity },
    timeOfDay = timeOfDay?.wire(),
)
