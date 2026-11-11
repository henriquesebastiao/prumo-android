package com.henriquesebastiao.prumo.feature.studies.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The wire format of Estudos (openapi/openapi.json of the server). Only the fields the app uses; the
// contract test checks them against the schema (D04).

/** A page of a list. */
@Serializable
data class PageDto<T>(val items: List<T>, val total: Int, val limit: Int, val offset: Int)

/** A small collection, not paginated (`{items}`). */
@Serializable
data class ItemsDto<T>(val items: List<T>)

/** Schema `SubjectRef`: the color is the area's. */
@Serializable
data class SubjectRefDto(val id: String, val name: String, val color: String)

/** Schema `AreaRead`. */
@Serializable
data class AreaDto(
    val id: String,
    val name: String,
    val color: String,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("subjects_count") val subjectsCount: Int,
)

/** Schema `SubjectRead`. */
@Serializable
data class SubjectDto(
    val id: String,
    @SerialName("area_id") val areaId: String,
    @SerialName("area_name") val areaName: String,
    val name: String,
    val color: String,
    @SerialName("obsidian_uri") val obsidianUri: String?,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("topics_count") val topicsCount: Int,
    @SerialName("last_session_at") val lastSessionAt: String?,
    @SerialName("minutes_this_week") val minutesThisWeek: Int,
)

/** Schema `SubjectDetail`: a subject with its topics, weeks (E7) and latest sessions. */
@Serializable
data class SubjectDetailDto(
    val id: String,
    @SerialName("area_id") val areaId: String,
    @SerialName("area_name") val areaName: String,
    val name: String,
    val color: String,
    @SerialName("obsidian_uri") val obsidianUri: String?,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("topics_count") val topicsCount: Int,
    @SerialName("last_session_at") val lastSessionAt: String?,
    @SerialName("minutes_this_week") val minutesThisWeek: Int,
    val topics: List<TopicDto>,
    @SerialName("weekly_minutes") val weeklyMinutes: List<WeekMinutesDto>,
    @SerialName("recent_sessions") val recentSessions: List<SessionDto>,
)

/** Schema `TopicRef`. */
@Serializable
data class TopicRefDto(val id: String, val name: String)

/** Schema `TopicRead`. */
@Serializable
data class TopicDto(val id: String, val name: String, @SerialName("sessions_count") val sessionsCount: Int)

/** Schema `WeekMinutes`. */
@Serializable
data class WeekMinutesDto(@SerialName("week_start") val weekStart: String, val minutes: Int)

/** Schema `StudySessionRead`. */
@Serializable
data class SessionDto(
    val id: String,
    val subject: SubjectRefDto,
    val topic: TopicRefDto?,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
)

/** Schema `BlockRead`: weekdays 0 (Monday) to 6, a local time or null. */
@Serializable
data class BlockDto(
    val id: String,
    val subject: SubjectRefDto,
    val weekdays: List<Int>,
    @SerialName("starts_at") val startsAt: String?,
    @SerialName("duration_minutes") val durationMinutes: Int,
)

/** Schema `PlanRead`. */
@Serializable
data class PlanDto(
    val id: String,
    @SerialName("valid_from") val validFrom: String,
    @SerialName("valid_until") val validUntil: String?,
    val state: String,
    val blocks: List<BlockDto>,
)

/** Schema `PlanList`. */
@Serializable
data class PlanListDto(val today: String, val items: List<PlanDto>)

/** Schema `DayBlock`. */
@Serializable
data class DayBlockDto(
    @SerialName("starts_at") val startsAt: String?,
    @SerialName("duration_minutes") val durationMinutes: Int,
)

/** Schema `DayRow`. */
@Serializable
data class DayRowDto(
    val subject: SubjectRefDto,
    @SerialName("goal_minutes") val goalMinutes: Int,
    @SerialName("done_minutes") val doneMinutes: Int,
    val status: String,
    val blocks: List<DayBlockDto>,
)

/** Schema `DayRead`. */
@Serializable
data class DayDto(
    val on: String,
    val today: String,
    @SerialName("is_today") val isToday: Boolean,
    val rows: List<DayRowDto>,
    @SerialName("done_minutes") val doneMinutes: Int,
    @SerialName("planned_minutes") val plannedMinutes: Int,
    val met: Boolean?,
)

/** Schema `WeekSubject`. */
@Serializable
data class WeekSubjectDto(
    val subject: SubjectRefDto,
    @SerialName("minutes_done") val minutesDone: Int,
    @SerialName("minutes_planned") val minutesPlanned: Int,
)

/** Schema `WeekRead`. */
@Serializable
data class WeekDto(
    val start: String,
    val end: String,
    val today: String,
    val days: List<DayDto>,
    @SerialName("days_met") val daysMet: Int,
    @SerialName("days_planned") val daysPlanned: Int,
    @SerialName("minutes_done") val minutesDone: Int,
    @SerialName("minutes_planned") val minutesPlanned: Int,
    @SerialName("by_subject") val bySubject: List<WeekSubjectDto>,
    @SerialName("previous_start") val previousStart: String,
    @SerialName("next_start") val nextStart: String?,
)

/** Schema `StudiesOverview`. */
@Serializable
data class OverviewDto(
    @SerialName("areas_count") val areasCount: Int,
    @SerialName("subjects_count") val subjectsCount: Int,
    @SerialName("sessions_this_month") val sessionsThisMonth: Int,
    @SerialName("minutes_this_month") val minutesThisMonth: Int,
)

/** `POST /studies/areas` (schema `AreaCreate`). */
@Serializable
data class AreaCreateDto(val name: String, val color: String)

/** `PATCH /studies/areas/{id}` (schema `AreaUpdate`). */
@Serializable
data class AreaUpdateDto(val name: String, val color: String)

/** `POST /studies/subjects` (schema `SubjectCreate`). */
@Serializable
data class SubjectCreateDto(
    @SerialName("area_id") val areaId: String,
    val name: String,
    @SerialName("obsidian_uri") val obsidianUri: String?,
)

/** `PATCH /studies/subjects/{id}` (schema `SubjectUpdate`): a null link clears it. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SubjectUpdateDto(
    @SerialName("area_id") val areaId: String,
    val name: String,
    @SerialName("obsidian_uri") @EncodeDefault(EncodeDefault.Mode.ALWAYS) val obsidianUri: String? = null,
)

/** Schema `BlockWrite`. */
@Serializable
data class BlockWriteDto(
    @SerialName("subject_id") val subjectId: String,
    val weekdays: List<Int>,
    @SerialName("starts_at") val startsAt: String?,
    @SerialName("duration_minutes") val durationMinutes: Int,
)

/** `POST /studies/plans` (schema `PlanWrite`): a new version from [validFrom] (today or later, E1). */
@Serializable
data class PlanWriteDto(@SerialName("valid_from") val validFrom: String, val blocks: List<BlockWriteDto>)

/** `PUT /studies/plans/{id}` (schema `PlanBlocksWrite`): the blocks of a version not yet in force. */
@Serializable
data class PlanBlocksWriteDto(val blocks: List<BlockWriteDto>)

/** `POST /studies/sessions` (schema `StudySessionCreate`): a topic by id or by name (E10). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SessionCreateDto(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("topic_id") @EncodeDefault(EncodeDefault.Mode.NEVER) val topicId: String? = null,
    @SerialName("topic_name") @EncodeDefault(EncodeDefault.Mode.NEVER) val topicName: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
)

/**
 * `PATCH /studies/sessions/{id}` (schema `StudySessionUpdate`). Without a topic the session keeps
 * none: [topicId] goes as null to clear it, unless a [topicName] is sent.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SessionUpdateDto(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("topic_id") @EncodeDefault(EncodeDefault.Mode.ALWAYS) val topicId: String? = null,
    @SerialName("topic_name") @EncodeDefault(EncodeDefault.Mode.NEVER) val topicName: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
)
