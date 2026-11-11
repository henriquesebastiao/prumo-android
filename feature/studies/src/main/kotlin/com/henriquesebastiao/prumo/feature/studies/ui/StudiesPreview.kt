package com.henriquesebastiao.prumo.feature.studies.ui

import com.henriquesebastiao.prumo.feature.studies.model.Area
import com.henriquesebastiao.prumo.feature.studies.model.Day
import com.henriquesebastiao.prumo.feature.studies.model.DayBlock
import com.henriquesebastiao.prumo.feature.studies.model.DayRow
import com.henriquesebastiao.prumo.feature.studies.model.DayStatus
import com.henriquesebastiao.prumo.feature.studies.model.Plan
import com.henriquesebastiao.prumo.feature.studies.model.PlanBlock
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.Plans
import com.henriquesebastiao.prumo.feature.studies.model.StudiesOverview
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import com.henriquesebastiao.prumo.feature.studies.model.StudySession
import com.henriquesebastiao.prumo.feature.studies.model.Subject
import com.henriquesebastiao.prumo.feature.studies.model.SubjectDetail
import com.henriquesebastiao.prumo.feature.studies.model.SubjectRef
import com.henriquesebastiao.prumo.feature.studies.model.Topic
import com.henriquesebastiao.prumo.feature.studies.model.TopicRef
import com.henriquesebastiao.prumo.feature.studies.model.Week
import com.henriquesebastiao.prumo.feature.studies.model.WeekMinutes
import com.henriquesebastiao.prumo.feature.studies.model.WeekSubject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Estudos as the demo server has it on 17/11/2026 (backend/scripts/e2e_server.py), for previews and tests. */
internal object StudiesPreview {
    private val tuesday = LocalDate.of(2026, 11, 17)
    private val monday = LocalDate.of(2026, 11, 16)

    val english = SubjectRef("s-en", "Inglês", StudyColor.Orange)
    val fastApi = SubjectRef("s-api", "FastAPI", StudyColor.Green)
    val calculus = SubjectRef("s-calc", "Cálculo II", StudyColor.Blue)

    val today = Day(
        on = tuesday,
        today = tuesday,
        isToday = true,
        rows = listOf(
            DayRow(english, 30, 30, DayStatus.Met, listOf(DayBlock(LocalTime.of(7, 0), 30))),
            DayRow(fastApi, 45, 0, DayStatus.InProgress, listOf(DayBlock(LocalTime.of(20, 0), 45))),
        ),
        doneMinutes = 30,
        plannedMinutes = 75,
        met = false,
    )

    private val mondayDay = Day(
        on = monday,
        today = tuesday,
        isToday = false,
        rows = listOf(
            DayRow(english, 30, 30, DayStatus.Met, listOf(DayBlock(LocalTime.of(7, 0), 30))),
            DayRow(calculus, 60, 60, DayStatus.Met, listOf(DayBlock(LocalTime.of(19, 0), 60))),
        ),
        doneMinutes = 90,
        plannedMinutes = 90,
        met = true,
    )

    val week = Week(
        start = monday,
        end = monday.plusDays(6),
        today = tuesday,
        days = listOf(mondayDay, today) + (2L..6L).map { offset ->
            val day = monday.plusDays(offset)
            Day(day, tuesday, false, emptyList(), 0, 0, null)
        },
        daysMet = 1,
        daysPlanned = 2,
        minutesDone = 120,
        minutesPlanned = 165,
        bySubject = listOf(
            WeekSubject(english, 60, 150),
            WeekSubject(calculus, 60, 120),
            WeekSubject(fastApi, 0, 90),
        ),
        previousStart = monday.minusWeeks(1),
        nextStart = monday.plusWeeks(1),
    )

    val areas = listOf(
        Area("a-uni", "Faculdade", StudyColor.Blue, archived = false, subjectsCount = 2),
        Area("a-dev", "Desenvolvimento", StudyColor.Green, archived = false, subjectsCount = 2),
        Area("a-en", "Inglês", StudyColor.Orange, archived = false, subjectsCount = 1),
    )

    private fun subject(ref: SubjectRef, area: Area, topics: Int, week: Int, uri: String? = null) = Subject(
        id = ref.id,
        areaId = area.id,
        areaName = area.name,
        name = ref.name,
        color = ref.color,
        obsidianUri = uri,
        archived = false,
        topicsCount = topics,
        lastSessionAt = Instant.parse("2026-11-16T22:00:00Z"),
        minutesThisWeek = week,
    )

    val subjects = listOf(
        subject(calculus, areas[0], 3, 60),
        subject(SubjectRef("s-ds", "Estruturas de Dados", StudyColor.Blue), areas[0], 2, 0),
        subject(fastApi, areas[1], 4, 0, uri = "obsidian://open?vault=Estudos&file=Dev%2FFastAPI"),
        subject(SubjectRef("s-kt", "Kotlin e Compose", StudyColor.Green), areas[1], 0, 0).copy(lastSessionAt = null),
        subject(english, areas[2], 2, 60),
    )

    val overview = StudiesOverview(areasCount = 3, subjectsCount = 5, sessionsThisMonth = 18, minutesThisMonth = 860)

    val detail = SubjectDetail(
        subject = subjects[2],
        topics = listOf(
            Topic("t1", "Dependências", 2),
            Topic("t2", "Pydantic", 3),
            Topic("t3", "Testes", 1),
        ),
        weeklyMinutes = (7 downTo 0).map { WeekMinutes(monday.minusWeeks(it.toLong()), (it * 25) % 135) },
        recentSessions = listOf(
            StudySession("ss1", fastApi, TopicRef("t3", "Testes"), Instant.parse("2026-11-12T23:00:00Z"), 45),
            StudySession("ss2", fastApi, null, Instant.parse("2026-11-10T23:00:00Z"), 50),
        ),
    )

    private val blocks = listOf(
        PlanBlock("b1", english, DayOfWeek.entries.take(5), LocalTime.of(7, 0), 30),
        PlanBlock("b2", calculus, listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(19, 0), 60),
        PlanBlock("b3", fastApi, listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), LocalTime.of(20, 0), 45),
    )

    val plans = Plans(
        today = tuesday,
        items = listOf(
            Plan("p3", LocalDate.of(2026, 11, 23), null, PlanState.Future, blocks.take(2)),
            Plan("p2", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 22), PlanState.Current, blocks),
            Plan("p1", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 1), PlanState.Past, blocks.take(1)),
        ),
    )

    val events = StudiesTabEvents(
        today = TodayEvents({}, {}, {}),
        week = WeekEvents({}, {}, {}, {}, {}),
        subjects = SubjectsEvents({}, {}, {}, {}, { _, _ -> }, {}, {}, {}),
        plan = PlanEvents({}, {}, {}, {}, {}),
    )
}
