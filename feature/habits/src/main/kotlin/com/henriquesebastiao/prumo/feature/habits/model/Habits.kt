package com.henriquesebastiao.prumo.feature.habits.model

import com.henriquesebastiao.prumo.core.model.Wire
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
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

enum class FrequencyKind(override val wire: String) : Wire {
    Daily("daily"),
    Weekly("weekly"),
    Weekdays("weekdays"),
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

/** H6: days met this week over what the week asks. */
data class WeekProgress(val done: Int, val target: Int)

/** What today asks of a habit to practice: its day, or the week in progress. */
data class TodayState(val scheduled: Boolean, val done: Boolean, val value: Int?)

/** A version of the frequency (H2): [weekdays] in `weekdays`, [timesPerWeek] in `weekly`. */
data class Frequency(
    val id: String,
    val validFrom: LocalDate,
    val validUntil: LocalDate?,
    val state: FrequencyState,
    val kind: FrequencyKind,
    val timesPerWeek: Int?,
    val weekdays: List<DayOfWeek>,
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
    val frequency: Frequency?,
    val paused: Boolean,
    /** H5: rounded; null with nothing scheduled. */
    val ratePercent: Int?,
    val currentStreak: Int?,
    val bestStreak: Int?,
    /** The unit of the streaks: weeks in `weekly`, else days. */
    val streakInWeeks: Boolean,
    val week: WeekProgress?,
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
)

/** Schema `HabitTodayItem`: [settled] is today met, or the week already met. */
data class TodayItem(
    val habit: HabitRef,
    val done: Boolean,
    val value: Int?,
    val week: WeekProgress?,
    val settled: Boolean,
)

/** Schema `HabitsToday`. */
data class HabitsToday(val today: LocalDate, val items: List<TodayItem>, val done: Int, val scheduled: Int)

/** What the frequency fields send. */
data class FrequencyInput(
    val kind: FrequencyKind,
    val timesPerWeek: Int? = null,
    val weekdays: List<DayOfWeek> = emptyList(),
)

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
)

/** The API's weekday: 0 is Monday … 6 is Sunday. */
internal val DayOfWeek.wire: Int get() = value - 1

internal fun weekdayOf(wire: Int): DayOfWeek = DayOfWeek.of(wire + 1)
