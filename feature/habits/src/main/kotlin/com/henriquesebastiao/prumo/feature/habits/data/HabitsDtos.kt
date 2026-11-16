package com.henriquesebastiao.prumo.feature.habits.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The wire format of Hábitos (openapi/openapi.json of the server). Only the fields the app uses; the
// contract test checks them against the schema (D04).

/** A page of a list. */
@Serializable
data class PageDto<T>(val items: List<T>, val total: Int, val limit: Int, val offset: Int)

/** Schema `HabitTodayState`. */
@Serializable
data class TodayStateDto(val scheduled: Boolean, val done: Boolean, val value: Int?)

/** Schema `HabitPeriod`. */
@Serializable
data class PeriodDto(
    val period: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String,
    val done: Int,
    val target: Int,
    @SerialName("days_left") val daysLeft: Int,
    val neutral: Boolean,
)

/** Schema `HabitFrequencyRead`: the fields of the other kinds come null. */
@Serializable
data class FrequencyDto(
    val id: String,
    @SerialName("valid_from") val validFrom: String,
    @SerialName("valid_until") val validUntil: String?,
    val state: String,
    val kind: String,
    val cadence: String = "daily",
    val weekdays: List<Int>? = null,
    @SerialName("every_n_weeks") val everyNWeeks: Int? = null,
    @SerialName("month_days") val monthDays: List<Int>? = null,
    @SerialName("month_last_day") val monthLastDay: Boolean? = null,
    @SerialName("week_of_month") val weekOfMonth: Int? = null,
    val weekday: Int? = null,
    @SerialName("every_n_months") val everyNMonths: Int? = null,
    val times: Int? = null,
    val period: String? = null,
    @SerialName("preferred_day") val preferredDay: Int? = null,
    @SerialName("times_per_week") val timesPerWeek: Int? = null,
)

/** Schema `HabitRead`. */
@Serializable
data class HabitDto(
    val id: String,
    val name: String,
    val kind: String,
    val color: String,
    val icon: String,
    @SerialName("started_on") val startedOn: String,
    val measure: String?,
    @SerialName("target_value") val targetValue: Int?,
    val unit: String?,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    val position: Int = 0,
    val frequency: FrequencyDto?,
    val paused: Boolean,
    @SerialName("rate_percent") val ratePercent: Int?,
    @SerialName("current_streak") val currentStreak: Int?,
    @SerialName("best_streak") val bestStreak: Int?,
    @SerialName("streak_unit") val streakUnit: String?,
    val period: PeriodDto? = null,
    @SerialName("next_due") val nextDue: String? = null,
    val today: TodayStateDto?,
    @SerialName("clean_since") val cleanSince: String?,
    @SerialName("clean_days") val cleanDays: Int?,
    @SerialName("best_clean_days") val bestCleanDays: Int?,
    @SerialName("relapses_30d") val relapses30d: Int?,
)

/** Schema `HabitPauseRead`. */
@Serializable
data class PauseDto(
    val id: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String?,
)

/** Schema `RelapseRead`. */
@Serializable
data class RelapseDto(val id: String, @SerialName("occurred_at") val occurredAt: String, val note: String?)

/** Schema `HabitDetail`: a habit with its versions, pauses and latest relapses. */
@Serializable
data class HabitDetailDto(
    val id: String,
    val name: String,
    val kind: String,
    val color: String,
    val icon: String,
    @SerialName("started_on") val startedOn: String,
    val measure: String?,
    @SerialName("target_value") val targetValue: Int?,
    val unit: String?,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    val position: Int = 0,
    val frequency: FrequencyDto?,
    val paused: Boolean,
    @SerialName("rate_percent") val ratePercent: Int?,
    @SerialName("current_streak") val currentStreak: Int?,
    @SerialName("best_streak") val bestStreak: Int?,
    @SerialName("streak_unit") val streakUnit: String?,
    val period: PeriodDto? = null,
    @SerialName("next_due") val nextDue: String? = null,
    val today: TodayStateDto?,
    @SerialName("clean_since") val cleanSince: String?,
    @SerialName("clean_days") val cleanDays: Int?,
    @SerialName("best_clean_days") val bestCleanDays: Int?,
    @SerialName("relapses_30d") val relapses30d: Int?,
    val frequencies: List<FrequencyDto>,
    val pauses: List<PauseDto>,
    val relapses: List<RelapseDto>,
) {
    fun habit() = HabitDto(
        id, name, kind, color, icon, startedOn, measure, targetValue, unit, archivedAt, timeOfDay, position,
        frequency, paused, ratePercent, currentStreak, bestStreak, streakUnit, period, nextDue, today, cleanSince,
        cleanDays, bestCleanDays, relapses30d,
    )
}

/** Schema `HabitCalendarDay`. */
@Serializable
data class CalendarDayDto(val on: String, val status: String, val value: Int?)

/** Schema `HabitCalendar`. */
@Serializable
data class CalendarDto(
    val month: String,
    val today: String,
    val days: List<CalendarDayDto>,
    @SerialName("previous_month") val previousMonth: String,
    @SerialName("next_month") val nextMonth: String?,
)

/** Schema `HabitRef`. */
@Serializable
data class HabitRefDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val measure: String?,
    @SerialName("target_value") val targetValue: Int?,
    val unit: String?,
    @SerialName("time_of_day") val timeOfDay: String? = null,
)

/** Schema `HabitDayItem`. */
@Serializable
data class DayItemDto(
    val habit: HabitRefDto,
    val done: Boolean,
    val value: Int?,
    val settled: Boolean,
    val late: Boolean = false,
    val period: PeriodDto? = null,
    val promoted: String? = null,
    @SerialName("due_in_days") val dueInDays: Int? = null,
)

/** Schema `HabitsSummaryQuit`. */
@Serializable
data class QuitItemDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    @SerialName("clean_days") val cleanDays: Int,
)

/** Schema `HabitsDay` (H9): the sections, grouped and ordered by the server. */
@Serializable
data class DayDto(
    val on: String,
    val today: String,
    val now: String?,
    val done: Int,
    val scheduled: Int,
    val timeline: List<DayItemDto>,
    val untimed: List<DayItemDto>,
    val period: List<DayItemDto>,
    @SerialName("done_items") val doneItems: List<DayItemDto>,
    val quit: List<QuitItemDto>,
)

/** `PUT /habits/order` (schema `HabitOrder`). */
@Serializable
data class OrderDto(val ids: List<String>)

/**
 * Schema `HabitFrequencyFields` (and `HabitFrequencyUpdate`): the fields of [kind]; the others go
 * as null and the server ignores them.
 */
@Serializable
data class FrequencyFieldsDto(
    val kind: String,
    val weekdays: List<Int>? = null,
    @SerialName("every_n_weeks") val everyNWeeks: Int? = null,
    @SerialName("month_days") val monthDays: List<Int>? = null,
    @SerialName("month_last_day") val monthLastDay: Boolean? = null,
    @SerialName("week_of_month") val weekOfMonth: Int? = null,
    val weekday: Int? = null,
    @SerialName("every_n_months") val everyNMonths: Int? = null,
    val times: Int? = null,
    val period: String? = null,
    @SerialName("preferred_day") val preferredDay: Int? = null,
)

/** `POST /habits` (schema `HabitCreate`): measure and frequency only in `build`. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HabitCreateDto(
    val name: String,
    val kind: String,
    val color: String,
    val icon: String,
    @SerialName("started_on") val startedOn: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val measure: String? = null,
    @SerialName("target_value") @EncodeDefault(EncodeDefault.Mode.NEVER) val targetValue: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val unit: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val frequency: FrequencyFieldsDto? = null,
    @SerialName("time_of_day") @EncodeDefault(EncodeDefault.Mode.NEVER) val timeOfDay: String? = null,
)

/** `PATCH /habits/{id}` (schema `HabitUpdate`): target and unit only in a quantity. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HabitUpdateDto(
    val name: String,
    val color: String,
    val icon: String,
    @SerialName("started_on") val startedOn: String,
    @SerialName("target_value") @EncodeDefault(EncodeDefault.Mode.NEVER) val targetValue: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val unit: String? = null,
    /** Always sent: null clears the time. */
    @SerialName("time_of_day") @EncodeDefault(EncodeDefault.Mode.ALWAYS) val timeOfDay: String? = null,
)

/** `POST /habits/{id}/frequencies` (schema `HabitFrequencyCreate`). */
@Serializable
data class FrequencyCreateDto(
    @SerialName("valid_from") val validFrom: String,
    val kind: String,
    val weekdays: List<Int>? = null,
    @SerialName("every_n_weeks") val everyNWeeks: Int? = null,
    @SerialName("month_days") val monthDays: List<Int>? = null,
    @SerialName("month_last_day") val monthLastDay: Boolean? = null,
    @SerialName("week_of_month") val weekOfMonth: Int? = null,
    val weekday: Int? = null,
    @SerialName("every_n_months") val everyNMonths: Int? = null,
    val times: Int? = null,
    val period: String? = null,
    @SerialName("preferred_day") val preferredDay: Int? = null,
)

/** `PUT /habits/{id}/entries/{on}` (schema `HabitEntryWrite`). */
@Serializable
data class EntryWriteDto(val value: Int)

/** `POST /habits/{id}/pauses` (schema `HabitPauseCreate`). */
@Serializable
data class PauseCreateDto(@SerialName("starts_on") val startsOn: String, @SerialName("ends_on") val endsOn: String?)

/** `PATCH /habits/pauses/{id}` (schema `HabitPauseUpdate`): a null end reopens it. */
@Serializable
data class PauseUpdateDto(@SerialName("starts_on") val startsOn: String, @SerialName("ends_on") val endsOn: String?)

/** `POST /habits/{id}/relapses` (schema `RelapseCreate`). */
@Serializable
data class RelapseCreateDto(@SerialName("occurred_at") val occurredAt: String, val note: String?)
