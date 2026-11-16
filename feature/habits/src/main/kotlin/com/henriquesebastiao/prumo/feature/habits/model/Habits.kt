package com.henriquesebastiao.prumo.feature.habits.model

import com.henriquesebastiao.prumo.core.model.Wire
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** The module's id on the server (`/api/v1/modules`, the Home's summaries). */
const val HABITS_MODULE_ID = "habits"

// Hábitos as the API gives it (docs/dominio.md of the server, "Hábitos", H1–H9). Every derived
// value (a day met, what is scheduled, streaks, the rate, the week, the time without) comes ready
// from the server (D02).

/** `build`: a habit to practice; `quit`: one to stop. It never changes. */
enum class HabitKind(override val wire: String) : Wire {
    Build("build"),
    Quit("quit"),
}

/** How a habit to practice is measured; it never changes. */
enum class Measure(override val wire: String) : Wire {
    Check("check"),
    Quantity("quantity"),
}

/**
 * How a frequency schedules (H2, D48 of the server): fixed dates ([Daily], [Weekdays],
 * [MonthDays], [MonthWeekday]) or [PerPeriod]. [Weekly] is only what older servers sent; the app
 * reads it as [PerPeriod] weekly and never sends it.
 */
enum class FrequencyKind(override val wire: String) : Wire {
    Daily("daily"),
    Weekly("weekly"),
    Weekdays("weekdays"),
    MonthDays("month_days"),
    MonthWeekday("month_weekday"),
    PerPeriod("per_period"),
    ;

    companion object {
        /** What the editor offers. */
        val Editable = listOf(Daily, Weekdays, MonthDays, MonthWeekday, PerPeriod)
    }
}

/** The window of [FrequencyKind.PerPeriod], with the most times it takes. */
enum class Period(override val wire: String, val days: Int) : Wire {
    Week("week", 7),
    Fortnight("fortnight", 14),
    Month("month", 28),
}

/** How often a frequency comes back (`cadence` of the server): filters and labels. */
enum class Cadence(override val wire: String) : Wire {
    Daily("daily"),
    Weekly("weekly"),
    Fortnightly("fortnightly"),
    Monthly("monthly"),
    Longer("longer"),
}

/** The unit of the streaks (H4): days, sparse dates, or the period of `per_period`. */
enum class StreakUnit(override val wire: String) : Wire {
    Day("day"),
    Occurrence("occurrence"),
    Week("week"),
    Fortnight("fortnight"),
    Month("month"),
}

/** Why a `per_period` habit rose to the day (H9). */
enum class Promotion(override val wire: String) : Wire {
    PreferredDay("preferred_day"),
    DueSoon("due_soon"),
}

/** H2: a version of the frequency already over, the one in force, or one that starts later. */
enum class FrequencyState(override val wire: String) : Wire {
    Past("past"),
    Current("current"),
    Future("future"),
}

/** A day of a habit's calendar, as the server reads it. */
enum class DayState(override val wire: String) : Wire {
    Done("done"),
    Partial("partial"),
    Missed("missed"),
    Pending("pending"),
    Paused("paused"),
    Unscheduled("unscheduled"),
    Future("future"),
}

/** Which habits a list shows (`archived` of the API). */
enum class ArchivedFilter(override val wire: String) : Wire {
    Active("false"),
    All("all"),
}

/** A habit's color (a key of the server's palette, D33). The same keys as the other modules'. */
enum class HabitColor(override val wire: String, val seed: Int) : Wire {
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
        val Picker = listOf(Amber, Orange, Green, Blue, Violet, Pink, Cyan, Gray)
    }
}

/** The icons offered for a habit: free names (D33 of the server), the web's list. */
val HABIT_ICONS = listOf(
    "sparkles", "book-open", "book", "droplet", "dumbbell", "bike", "heart-pulse", "brain", "apple",
    "utensils", "coffee", "beer", "smartphone", "tv", "gamepad-2", "music", "palette", "camera", "pill",
    "toothbrush", "graduation-cap", "code", "briefcase", "piggy-bank", "paw-print", "church", "flame",
    "moon", "sun", "trophy",
)

/** H6: the `per_period` period in progress, from a reference day. */
data class PeriodProgress(
    val period: Period,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val done: Int,
    val target: Int,
    val daysLeft: Int,
    /** H2a: out of the rate; breaks no streak. */
    val neutral: Boolean,
)

/** What today asks of a habit to practice: its day, or the period in progress. */
data class TodayState(val scheduled: Boolean, val done: Boolean, val value: Int?)

/** A version of the frequency (H2): [rule] holds the fields of its kind. */
data class Frequency(
    val id: String,
    val validFrom: LocalDate,
    val validUntil: LocalDate?,
    val state: FrequencyState,
    val rule: FrequencyInput,
    val cadence: Cadence,
)

/** Schema `HabitRead`: the fields of the other kind come null. */
data class Habit(
    val id: String,
    val name: String,
    val kind: HabitKind,
    val color: HabitColor,
    val icon: String,
    val startedOn: LocalDate,
    val measure: Measure?,
    val targetValue: Int?,
    val unit: String?,
    val archived: Boolean,
    /** Wall-clock time in the user's zone (H10), or null. */
    val timeOfDay: LocalTime?,
    val position: Int,
    val frequency: Frequency?,
    val paused: Boolean,
    /** H5: rounded; null with nothing scheduled. */
    val ratePercent: Int?,
    val currentStreak: Int?,
    val bestStreak: Int?,
    val streakUnit: StreakUnit,
    /** H6, in `per_period`. */
    val period: PeriodProgress?,
    /** H6a: the next day that asks something; null under an open pause. */
    val nextDue: LocalDate?,
    val today: TodayState?,
    val cleanSince: Instant?,
    val cleanDays: Int?,
    val bestCleanDays: Int?,
    val relapses30d: Int?,
)

data class Pause(val id: String, val startsOn: LocalDate, val endsOn: LocalDate?)

data class Relapse(val id: String, val occurredAt: Instant, val note: String?)

/** Schema `HabitDetail`: the versions newest first, the pauses, the last 20 relapses. */
data class HabitDetail(
    val habit: Habit,
    val frequencies: List<Frequency>,
    val pauses: List<Pause>,
    val relapses: List<Relapse>,
)

data class CalendarDay(val on: LocalDate, val state: DayState, val value: Int?)

/** Schema `HabitCalendar`: [next] is null in the current month. */
data class CalendarMonth(
    val month: YearMonth,
    val today: LocalDate,
    val days: List<CalendarDay>,
    val previous: YearMonth,
    val next: YearMonth?,
)

/** A habit as other records name it (schema `HabitRef`). */
data class HabitRef(
    val id: String,
    val name: String,
    val color: HabitColor,
    val icon: String,
    val measure: Measure?,
    val targetValue: Int?,
    val unit: String?,
    val timeOfDay: LocalTime? = null,
)

/** Schema `HabitDayItem`: a habit of a day (H9), with why a period rose to it. */
data class DayItem(
    val habit: HabitRef,
    val done: Boolean,
    val value: Int?,
    /** The day or the period already met. */
    val settled: Boolean,
    /** Open with a time already past (only today). */
    val late: Boolean = false,
    val period: PeriodProgress? = null,
    val promoted: Promotion? = null,
    val dueInDays: Int? = null,
)

/** A habit to quit in the day, with its days without (H7). */
data class QuitItem(val id: String, val name: String, val color: HabitColor, val icon: String, val cleanDays: Int)

/** Schema `HabitsDay` (H9): the sections come grouped and ordered by the server (H10). */
data class HabitsDay(
    val on: LocalDate,
    val today: LocalDate,
    /** The user's wall-clock time, only when [on] is today. */
    val now: LocalTime?,
    val done: Int,
    val scheduled: Int,
    val timeline: List<DayItem>,
    val untimed: List<DayItem>,
    val period: List<DayItem>,
    val doneItems: List<DayItem>,
    val quit: List<QuitItem>,
) {
    val isToday: Boolean get() = on == today

    /** Every habit of the day: what "Marcar hábitos" searches. */
    val all: List<DayItem> get() = timeline + untimed + period + doneItems

    val open: Int get() = timeline.size + untimed.size
}

/**
 * A frequency's fields (H2): the version read from the server, or what the editor sends. Only the
 * fields of [kind] matter.
 */
data class FrequencyInput(
    val kind: FrequencyKind = FrequencyKind.Daily,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.take(WORKDAYS).toSet(),
    val everyNWeeks: Int = 1,
    val monthDays: Set<Int> = setOf(1),
    val monthLastDay: Boolean = false,
    val everyNMonths: Int = 1,
    /** 1 to 4, or [LAST]. */
    val weekOfMonth: Int = 1,
    val weekday: DayOfWeek = DayOfWeek.SUNDAY,
    val times: Int = DEFAULT_TIMES,
    val period: Period = Period.Week,
    /** A weekday's wire (0–6) in weeks and fortnights, a day (1–31) in months. */
    val preferredDay: Int? = null,
) {
    /** Enough to send: a weekday, or a day of the month. */
    val valid: Boolean
        get() = when (kind) {
            FrequencyKind.Weekdays -> weekdays.isNotEmpty()
            FrequencyKind.MonthDays -> monthDays.isNotEmpty() || monthLastDay
            else -> true
        }

    companion object {
        const val LAST = -1
        const val DEFAULT_TIMES = 3
        const val MAX_EVERY_WEEKS = 52
        const val MAX_EVERY_MONTHS = 12
        private const val WORKDAYS = 5
    }
}

/** What the habit form sends; the kind and the measure only on a new habit. */
data class HabitInput(
    val name: String,
    val kind: HabitKind,
    val color: HabitColor,
    val icon: String,
    val startedOn: LocalDate,
    val measure: Measure? = null,
    val targetValue: Int? = null,
    val unit: String? = null,
    val frequency: FrequencyInput? = null,
    val timeOfDay: LocalTime? = null,
)

/** The API's weekday: 0 is Monday … 6 is Sunday. */
internal val DayOfWeek.wire: Int get() = value - 1

internal fun weekdayOf(wire: Int): DayOfWeek = DayOfWeek.of(wire + 1)
