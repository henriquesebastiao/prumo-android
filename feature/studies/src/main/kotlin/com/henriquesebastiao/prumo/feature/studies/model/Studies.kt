package com.henriquesebastiao.prumo.feature.studies.model

import com.henriquesebastiao.prumo.core.model.Wire
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** The module's id on the server (`/api/v1/modules`, the Home's summaries). */
const val STUDIES_MODULE_ID = "studies"

// Estudos as the API gives it (docs/dominio.md of the server, "Estudos", E1–E11). Every derived
// number (goals, minutes done, statuses, the week, minutes per week) comes ready from the server (D02).

/** An area's color: a subject inherits it (E2). The same keys as Finanças's colors. */
enum class StudyColor(override val wire: String, val seed: Int) : Wire {
    Green("green", 0xFF24A06B.toInt()),
    Teal("teal", 0xFF16917F.toInt()),
    Cyan("cyan", 0xFF1F9DB3.toInt()),
    Blue("blue", 0xFF2F6FDE.toInt()),
    Indigo("indigo", 0xFF4F5AD1.toInt()),
    Violet("violet", 0xFF6F57E8.toInt()),
    Pink("pink", 0xFFD2508A.toInt()),
    Red("red", 0xFFC2362B.toInt()),
    Orange("orange", 0xFFE4672E.toInt()),
    Amber("amber", 0xFFC98A12.toInt()),
    Gray("gray", 0xFF67646C.toInt()),
    ;

    companion object {
        /** The colors the picker offers, as the web's; the current one is added when it's another. */
        val Picker = listOf(Blue, Violet, Orange, Green, Pink, Cyan, Gray)
    }
}

/** E4: how a subject went on a day, as the server says. */
enum class DayStatus(override val wire: String) : Wire {
    Met("met"),
    Partial("partial"),
    Missed("missed"),
    InProgress("in_progress"),
    Extra("extra"),
}

/** E1: a version of the plan already over, the one in force or one that starts later. */
enum class PlanState(override val wire: String) : Wire {
    Past("past"),
    Current("current"),
    Future("future"),
}

/** Which areas and subjects a list shows (`archived` of the API). */
enum class ArchivedFilter(override val wire: String) : Wire {
    Active("false"),
    All("all"),
}

/** A subject as other records name it (schema `SubjectRef`): its color is the area's. */
data class SubjectRef(val id: String, val name: String, val color: StudyColor)

/** Schema `AreaRead`. */
data class Area(val id: String, val name: String, val color: StudyColor, val archived: Boolean, val subjectsCount: Int)

/** Schema `SubjectRead`: [minutesThisWeek] is Monday to today (E6). */
data class Subject(
    val id: String,
    val areaId: String,
    val areaName: String,
    val name: String,
    val color: StudyColor,
    /** E8: the `obsidian://` link of the subject's note, opened by another app. */
    val obsidianUri: String?,
    val archived: Boolean,
    val topicsCount: Int,
    val lastSessionAt: Instant?,
    val minutesThisWeek: Int,
)

/** Schema `TopicRead` (E10). */
data class Topic(val id: String, val name: String, val sessionsCount: Int)

/** Schema `TopicRef`. */
data class TopicRef(val id: String, val name: String)

/** Schema `StudySessionRead`. */
data class StudySession(
    val id: String,
    val subject: SubjectRef,
    val topic: TopicRef?,
    val startedAt: Instant,
    val durationMinutes: Int,
)

/** E7: the minutes of one week, from its Monday. */
data class WeekMinutes(val weekStart: LocalDate, val minutes: Int)

/** Schema `SubjectDetail`: the last 8 weeks and the last 10 sessions. */
data class SubjectDetail(
    val subject: Subject,
    val topics: List<Topic>,
    val weeklyMinutes: List<WeekMinutes>,
    val recentSessions: List<StudySession>,
)

/** A block of a plan (schema `BlockRead`): [startsAt] only orders the day (E3). */
data class PlanBlock(
    val id: String,
    val subject: SubjectRef,
    val weekdays: List<DayOfWeek>,
    val startsAt: LocalTime?,
    val durationMinutes: Int,
)

/** A version of the plan (schema `PlanRead`, E1). Only one that hasn't started can change. */
data class Plan(
    val id: String,
    val validFrom: LocalDate,
    val validUntil: LocalDate?,
    val state: PlanState,
    val blocks: List<PlanBlock>,
)

/** Schema `PlanList`: newest version first, with the user's today. */
data class Plans(val today: LocalDate, val items: List<Plan>)

/** Schema `DayBlock`. */
data class DayBlock(val startsAt: LocalTime?, val durationMinutes: Int)

/** Schema `DayRow`: a subject on a day, with its goal, minutes and status (E4). */
data class DayRow(
    val subject: SubjectRef,
    val goalMinutes: Int,
    val doneMinutes: Int,
    val status: DayStatus,
    val blocks: List<DayBlock>,
)

/** Schema `DayRead`: [met] is E5, null for a day without goals. */
data class Day(
    val on: LocalDate,
    val today: LocalDate,
    val isToday: Boolean,
    val rows: List<DayRow>,
    val doneMinutes: Int,
    val plannedMinutes: Int,
    val met: Boolean?,
)

/** Schema `WeekSubject`. */
data class WeekSubject(val subject: SubjectRef, val minutesDone: Int, val minutesPlanned: Int)

/** Schema `WeekRead` (E6): [nextStart] is null past the week after the current one. */
data class Week(
    val start: LocalDate,
    val end: LocalDate,
    val today: LocalDate,
    val days: List<Day>,
    val daysMet: Int,
    val daysPlanned: Int,
    val minutesDone: Int,
    val minutesPlanned: Int,
    val bySubject: List<WeekSubject>,
    val previousStart: LocalDate,
    val nextStart: LocalDate?,
)

/** Schema `StudiesOverview`. */
data class StudiesOverview(
    val areasCount: Int,
    val subjectsCount: Int,
    val sessionsThisMonth: Int,
    val minutesThisMonth: Int,
)

/** What the area form sends. */
data class AreaInput(val name: String, val color: StudyColor)

/** What the subject form sends; a null [obsidianUri] clears the link. */
data class SubjectInput(val areaId: String, val name: String, val obsidianUri: String?)

/** A block of the plan editor. */
data class BlockInput(
    val subjectId: String,
    val weekdays: List<DayOfWeek>,
    val startsAt: LocalTime?,
    val durationMinutes: Int,
)

/**
 * What "Registrar estudo" sends: a topic chosen ([topicId]) or typed ([topicName], E10: the server
 * creates it or finds it by name), or none.
 */
data class SessionInput(
    val subjectId: String,
    val topicId: String?,
    val topicName: String?,
    val startedAt: Instant,
    val durationMinutes: Int,
)

/** The API's weekday: 0 is Monday … 6 is Sunday. */
internal val DayOfWeek.wire: Int get() = value - 1

internal fun weekdayOf(wire: Int): DayOfWeek = DayOfWeek.of(wire + 1)
