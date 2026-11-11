package com.henriquesebastiao.prumo.feature.studies

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The module's Home summary (schema StudiesSummary of the server), with the fields the card uses.

@Serializable
data class StudiesSummaryDto(
    val module: String = "studies",
    @SerialName("blocks_today") val blocksToday: List<SummaryBlockDto>,
    @SerialName("done_minutes_today") val doneMinutesToday: Int,
    @SerialName("planned_minutes_today") val plannedMinutesToday: Int,
    val week: SummaryWeekDto,
)

/** Schema SummaryBlock: a subject of today with its goal, minutes and status (E4). */
@Serializable
data class SummaryBlockDto(
    val subject: SummarySubjectDto,
    @SerialName("starts_at") val startsAt: String?,
    @SerialName("goal_minutes") val goalMinutes: Int,
    @SerialName("done_minutes") val doneMinutes: Int,
    val status: String,
)

/** Schema SubjectRef. */
@Serializable
data class SummarySubjectDto(val id: String, val name: String, val color: String)

/** Schema SummaryWeek (E6). */
@Serializable
data class SummaryWeekDto(
    @SerialName("days_met") val daysMet: Int,
    @SerialName("days_planned") val daysPlanned: Int,
    @SerialName("minutes_done") val minutesDone: Int,
    @SerialName("minutes_planned") val minutesPlanned: Int,
)
