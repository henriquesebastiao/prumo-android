package com.henriquesebastiao.prumo.feature.studies.data

import com.henriquesebastiao.prumo.core.model.wireOf
import com.henriquesebastiao.prumo.feature.studies.model.Area
import com.henriquesebastiao.prumo.feature.studies.model.AreaInput
import com.henriquesebastiao.prumo.feature.studies.model.BlockInput
import com.henriquesebastiao.prumo.feature.studies.model.Day
import com.henriquesebastiao.prumo.feature.studies.model.DayBlock
import com.henriquesebastiao.prumo.feature.studies.model.DayRow
import com.henriquesebastiao.prumo.feature.studies.model.DayStatus
import com.henriquesebastiao.prumo.feature.studies.model.Plan
import com.henriquesebastiao.prumo.feature.studies.model.PlanBlock
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.Plans
import com.henriquesebastiao.prumo.feature.studies.model.SessionInput
import com.henriquesebastiao.prumo.feature.studies.model.StudiesOverview
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import com.henriquesebastiao.prumo.feature.studies.model.StudySession
import com.henriquesebastiao.prumo.feature.studies.model.Subject
import com.henriquesebastiao.prumo.feature.studies.model.SubjectDetail
import com.henriquesebastiao.prumo.feature.studies.model.SubjectInput
import com.henriquesebastiao.prumo.feature.studies.model.SubjectRef
import com.henriquesebastiao.prumo.feature.studies.model.Topic
import com.henriquesebastiao.prumo.feature.studies.model.TopicRef
import com.henriquesebastiao.prumo.feature.studies.model.Week
import com.henriquesebastiao.prumo.feature.studies.model.WeekMinutes
import com.henriquesebastiao.prumo.feature.studies.model.WeekSubject
import com.henriquesebastiao.prumo.feature.studies.model.weekdayOf
import com.henriquesebastiao.prumo.feature.studies.model.wire
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal fun color(wire: String) = wireOf(wire, StudyColor.Blue)

internal fun SubjectRefDto.toModel() = SubjectRef(id, name, color(color))

internal fun AreaDto.toModel() = Area(
    id = id,
    name = name,
    color = color(color),
    archived = archivedAt != null,
    subjectsCount = subjectsCount,
)

internal fun SubjectDto.toModel() = Subject(
    id = id,
    areaId = areaId,
    areaName = areaName,
    name = name,
    color = color(color),
    obsidianUri = obsidianUri,
    archived = archivedAt != null,
    topicsCount = topicsCount,
    lastSessionAt = lastSessionAt?.let(Instant::parse),
    minutesThisWeek = minutesThisWeek,
)

internal fun SubjectDetailDto.toModel() = SubjectDetail(
    subject = SubjectDto(
        id = id,
        areaId = areaId,
        areaName = areaName,
        name = name,
        color = color,
        obsidianUri = obsidianUri,
        archivedAt = archivedAt,
        topicsCount = topicsCount,
        lastSessionAt = lastSessionAt,
        minutesThisWeek = minutesThisWeek,
    ).toModel(),
    topics = topics.map { it.toModel() },
    weeklyMinutes = weeklyMinutes.map { WeekMinutes(LocalDate.parse(it.weekStart), it.minutes) },
    recentSessions = recentSessions.map { it.toModel() },
)

internal fun TopicDto.toModel() = Topic(id, name, sessionsCount)

internal fun SessionDto.toModel() = StudySession(
    id = id,
    subject = subject.toModel(),
    topic = topic?.let { TopicRef(it.id, it.name) },
    startedAt = Instant.parse(startedAt),
    durationMinutes = durationMinutes,
)

internal fun BlockDto.toModel() = PlanBlock(
    id = id,
    subject = subject.toModel(),
    weekdays = weekdays.map(::weekdayOf),
    startsAt = startsAt?.let(LocalTime::parse),
    durationMinutes = durationMinutes,
)

internal fun PlanDto.toModel() = Plan(
    id = id,
    validFrom = LocalDate.parse(validFrom),
    validUntil = validUntil?.let(LocalDate::parse),
    state = wireOf(state, PlanState.Past),
    blocks = blocks.map { it.toModel() },
)

internal fun PlanListDto.toModel() = Plans(LocalDate.parse(today), items.map { it.toModel() })

internal fun DayDto.toModel() = Day(
    on = LocalDate.parse(on),
    today = LocalDate.parse(today),
    isToday = isToday,
    rows = rows.map { row ->
        DayRow(
            subject = row.subject.toModel(),
            goalMinutes = row.goalMinutes,
            doneMinutes = row.doneMinutes,
            // A status a newer server adds reads as "in progress": no color claims more than it knows.
            status = wireOf(row.status, DayStatus.InProgress),
            blocks = row.blocks.map { DayBlock(it.startsAt?.let(LocalTime::parse), it.durationMinutes) },
        )
    },
    doneMinutes = doneMinutes,
    plannedMinutes = plannedMinutes,
    met = met,
)

internal fun WeekDto.toModel() = Week(
    start = LocalDate.parse(start),
    end = LocalDate.parse(end),
    today = LocalDate.parse(today),
    days = days.map { it.toModel() },
    daysMet = daysMet,
    daysPlanned = daysPlanned,
    minutesDone = minutesDone,
    minutesPlanned = minutesPlanned,
    bySubject = bySubject.map { WeekSubject(it.subject.toModel(), it.minutesDone, it.minutesPlanned) },
    previousStart = LocalDate.parse(previousStart),
    nextStart = nextStart?.let(LocalDate::parse),
)

internal fun OverviewDto.toModel() = StudiesOverview(areasCount, subjectsCount, sessionsThisMonth, minutesThisMonth)

internal fun AreaInput.toCreateDto() = AreaCreateDto(name, color.wire)

internal fun AreaInput.toUpdateDto() = AreaUpdateDto(name, color.wire)

internal fun SubjectInput.toCreateDto() = SubjectCreateDto(areaId, name, obsidianUri)

internal fun SubjectInput.toUpdateDto() = SubjectUpdateDto(areaId, name, obsidianUri)

/** `19:00` (the API reads a local time with or without seconds). */
private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

internal fun BlockInput.toDto() = BlockWriteDto(
    subjectId = subjectId,
    weekdays = weekdays.map { it.wire }.sorted(),
    startsAt = startsAt?.format(hourMinute),
    durationMinutes = durationMinutes,
)

internal fun SessionInput.toCreateDto() = SessionCreateDto(
    subjectId = subjectId,
    topicId = topicId,
    topicName = topicName.takeIf { topicId == null },
    startedAt = startedAt.toString(),
    durationMinutes = durationMinutes,
)

internal fun SessionInput.toUpdateDto() = SessionUpdateDto(
    subjectId = subjectId,
    topicId = topicId,
    topicName = topicName.takeIf { topicId == null },
    startedAt = startedAt.toString(),
    durationMinutes = durationMinutes,
)
