package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.CalendarDayStatus
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SeededAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.component.weekdayShort
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.ui.errorText
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.Frequency
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitRef
import java.io.Serializable
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** The user's zone and "today" in it (S3 of the server): only the defaults of date fields. */
data class HabitsContext(
    val zone: ZoneId = Preferences.Default.zone,
    val today: LocalDate = LocalDate.of(2026, 11, 17),
)

internal fun Preferences?.habitsContext(clock: Clock): HabitsContext {
    val prefs = this ?: Preferences.Default
    return HabitsContext(prefs.zone, prefs.today(clock.instant()))
}

/** A habit: its icon (a server name, D33) in its color. */
@Composable
internal fun HabitAvatar(color: HabitColor, icon: String, modifier: Modifier = Modifier, size: Int = 40) {
    SeededAvatar(color.seed, ServerIcons.of(icon), modifier = modifier, size = size.dp, iconSize = (size * 0.6f).dp)
}

@Composable
@ReadOnlyComposable
internal fun HabitColor.label(): String = stringResource(
    when (this) {
        HabitColor.Green -> R.string.habits_color_green
        HabitColor.Teal -> R.string.habits_color_teal
        HabitColor.Cyan -> R.string.habits_color_cyan
        HabitColor.Blue -> R.string.habits_color_blue
        HabitColor.Indigo -> R.string.habits_color_indigo
        HabitColor.Violet -> R.string.habits_color_violet
        HabitColor.Pink -> R.string.habits_color_pink
        HabitColor.Red -> R.string.habits_color_red
        HabitColor.Orange -> R.string.habits_color_orange
        HabitColor.Amber -> R.string.habits_color_amber
        HabitColor.Gray -> R.string.habits_color_gray
    },
)

@Composable
@ReadOnlyComposable
internal fun FrequencyKind.label(): String = stringResource(
    when (this) {
        FrequencyKind.Daily -> R.string.habits_freq_kind_daily
        FrequencyKind.Weekly -> R.string.habits_freq_kind_weekly
        FrequencyKind.Weekdays -> R.string.habits_freq_kind_weekdays
    },
)

/** "Todo dia", "3× por semana", "Dias úteis", "Seg, Qua e Sex" (as the web's). */
@Composable
internal fun frequencyText(kind: FrequencyKind, timesPerWeek: Int?, weekdays: List<DayOfWeek>): String = when (kind) {
    FrequencyKind.Daily -> stringResource(R.string.habits_freq_daily)

    FrequencyKind.Weekly -> stringResource(R.string.habits_freq_weekly, timesPerWeek ?: 1)

    FrequencyKind.Weekdays -> {
        val workdays = weekdays.toSet() == DayOfWeek.entries.take(WORKDAYS).toSet()
        val names = weekdays.sortedBy { it.value }.map { weekdayShortName(it) }
        when {
            workdays -> stringResource(R.string.habits_freq_workdays)

            names.size > 1 -> stringResource(
                R.string.habits_freq_list,
                names.dropLast(1).joinToString(", "),
                names.last(),
            )

            else -> names.firstOrNull().orEmpty()
        }
    }
}

@Composable
internal fun frequencyText(frequency: Frequency): String =
    frequencyText(frequency.kind, frequency.timesPerWeek, frequency.weekdays)

@Composable
private fun weekdayShortName(day: DayOfWeek): String = weekdayShort(day)

private const val WORKDAYS = 5

/** "12 dias", "3 semanas". */
@Composable
@ReadOnlyComposable
internal fun streakText(value: Int, weeks: Boolean): String =
    pluralStringResource(if (weeks) R.plurals.habits_weeks else R.plurals.habits_days, value, value)

@Composable
@ReadOnlyComposable
internal fun daysText(value: Int): String = pluralStringResource(R.plurals.habits_days, value, value)

/** "1.250 / 2.000 ml". */
@Composable
@ReadOnlyComposable
internal fun amountText(habit: HabitRef, value: Int?): String = stringResource(
    R.string.habits_amount,
    Formats.integer((value ?: 0).toLong()),
    Formats.integer((habit.targetValue ?: 0).toLong()),
    habit.unit.orEmpty(),
).trim()

@Composable
@ReadOnlyComposable
internal fun DayState.label(): String = stringResource(
    when (this) {
        DayState.Done -> R.string.habits_day_done
        DayState.Partial -> R.string.habits_day_partial
        DayState.Missed -> R.string.habits_day_missed
        DayState.Pending -> R.string.habits_day_pending
        DayState.Paused -> R.string.habits_day_paused
        DayState.Unscheduled -> R.string.habits_day_unscheduled
        DayState.Future -> R.string.habits_day_future
    },
)

internal val DayState.cell: CalendarDayStatus
    get() = when (this) {
        DayState.Done -> CalendarDayStatus.Done
        DayState.Partial -> CalendarDayStatus.Partial
        DayState.Missed -> CalendarDayStatus.Missed
        DayState.Pending -> CalendarDayStatus.Pending
        DayState.Paused -> CalendarDayStatus.Paused
        DayState.Unscheduled -> CalendarDayStatus.Unscheduled
        DayState.Future -> CalendarDayStatus.Future
    }

/** Which sheet of Hábitos is open over a screen, and for what. Kept across a rotation. */
data class SheetRequest(
    val kind: SheetKind,
    val habitId: String? = null,
    /** The frequency being edited. */
    val id: String? = null,
    /** The day of a quantity, and its value. */
    val day: LocalDate? = null,
    val value: Int? = null,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

enum class SheetKind { CheckIn, Relapse, Frequency, Pause, Entry }

/** The sheet of [request], in the module's colors (also over the Home). */
@Composable
internal fun HabitsSheet(request: SheetRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    ModuleTheme(HabitsFeature.SEED) {
        when (request.kind) {
            SheetKind.CheckIn -> CheckInSheet(onDismiss)

            SheetKind.Relapse -> RelapseSheet(request.habitId.orEmpty(), onDismiss)

            SheetKind.Frequency -> FrequencySheet(request.habitId.orEmpty(), request.id, onDismiss)

            SheetKind.Pause -> PauseSheet(request.habitId.orEmpty(), onDismiss)

            SheetKind.Entry -> EntrySheet(
                request.habitId.orEmpty(),
                request.day ?: LocalDate.MIN,
                request.value,
                onDismiss,
            )
        }
    }
}

/** What the toolbar records: [onPrimary] with [primaryIcon]; [onNewHabit] null leaves the button out. */
internal class HabitsActions(
    val onPrimary: () -> Unit,
    val primaryLabel: Int = R.string.habits_action_check_in,
    val primaryIcon: Int = PrumoIcons.Checklist,
    val onNewHabit: (() -> Unit)? = null,
)

/** The floating toolbar of Hábitos (D24): the FAB marks the day, a button adds a habit. */
@Composable
internal fun HabitsToolbar(actions: HabitsActions, modifier: Modifier = Modifier) {
    val icon: @Composable () -> Unit = {
        Icon(painterResource(actions.primaryIcon), contentDescription = stringResource(actions.primaryLabel))
    }
    val onNew = actions.onNewHabit
    if (onNew == null) {
        FloatingActionButton(
            onClick = actions.onPrimary,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            content = icon,
        )
        return
    }
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = actions.onPrimary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                content = icon,
            )
        },
        // A module's toolbar is the standard one (the Home's is vibrant), its FAB in primary container.
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        modifier = modifier,
    ) {
        IconButton(onClick = onNew) {
            Icon(painterResource(PrumoIcons.Add), contentDescription = stringResource(R.string.habits_new_habit))
        }
    }
}

/** Room under a list for the floating toolbar. */
internal val TOOLBAR_SPACE = 112.dp

/** The text of an error of Hábitos: the module's codes (`docs/api.md` of the server), then the app's. */
@Composable
@ReadOnlyComposable
internal fun habitsErrorText(error: AppError): String = when ((error as? AppError.Api)?.code) {
    "habits.future_date" -> stringResource(R.string.habits_error_future_date)
    "habits.before_start" -> stringResource(R.string.habits_error_before_start)
    "habits.not_build" -> stringResource(R.string.habits_error_not_build)
    "habits.not_quit" -> stringResource(R.string.habits_error_not_quit)
    FREQUENCY_LOCKED -> stringResource(R.string.habits_error_frequency_locked)
    FREQUENCY_DATE_TAKEN -> stringResource(R.string.habits_error_frequency_date_taken)
    "habits.pause_overlap" -> stringResource(R.string.habits_error_pause_overlap)
    INVALID_MEASURE -> stringResource(R.string.habits_error_invalid_measure)
    else -> errorText(error)
}

/** The message under the field at [path] when the server refused it, or null. */
@Composable
@ReadOnlyComposable
internal fun fieldError(error: AppError?, path: String): String? {
    val api = error as? AppError.Api ?: return null
    if (api.code == FREQUENCY_DATE_TAKEN && path == "valid_from") {
        return stringResource(R.string.habits_error_frequency_date_taken)
    }
    if (api.code == INVALID_MEASURE && path in setOf("target_value", "unit")) {
        return stringResource(R.string.habits_error_invalid_measure)
    }
    val refused = api.fields.any { it.path == path || it.path.startsWith("$path.") }
    return if (refused) stringResource(R.string.habits_field_invalid) else null
}

/** Whether [error] belongs to a field (shown there) rather than to the whole form. */
internal fun AppError.isFieldError(): Boolean =
    this is AppError.Api && (code == FREQUENCY_DATE_TAKEN || code == INVALID_MEASURE || fields.isNotEmpty())

private const val FREQUENCY_LOCKED = "habits.frequency_locked"
private const val FREQUENCY_DATE_TAKEN = "habits.frequency_date_taken"
private const val INVALID_MEASURE = "habits.invalid_measure"
