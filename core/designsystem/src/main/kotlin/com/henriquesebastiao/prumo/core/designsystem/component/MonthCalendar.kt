package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.R
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import java.time.LocalDate

/** How a day of a [MonthCalendar] is drawn: the API's status of the day (a habit's month). */
enum class CalendarDayStatus { Done, Partial, Missed, Pending, Paused, Unscheduled, Future }

/** One day of a [MonthCalendar]; [description] is read after the date ("Cumprido · 1.250 ml"). */
data class CalendarCell(val date: LocalDate, val status: CalendarDayStatus, val description: String)

/**
 * A month as a grid of days, Monday first (D24): done in the primary color, partial in its
 * container, missed outlined in the error color, a pause on a neutral surface, today ringed. With
 * [onSelect], every day up to today can be tapped (to mark a past day).
 */
@Composable
fun MonthCalendar(
    days: List<CalendarCell>,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    onSelect: ((LocalDate) -> Unit)? = null,
) {
    val shorts = stringArrayResource(R.array.designsystem_weekdays_short)
    val longs = stringArrayResource(R.array.designsystem_weekdays_long)
    val offset = days.firstOrNull()?.date?.dayOfWeek?.value?.minus(1) ?: 0
    val cells: List<CalendarCell?> = List(offset) { null } + days
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            shorts.forEach { short ->
                Text(
                    short,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp, stepSize = 1.sp),
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                for (index in 0 until DAYS_IN_WEEK) {
                    val cell = week.getOrNull(index)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (cell != null) {
                            Day(cell, cell.date == today, longs[cell.date.dayOfWeek.value - 1], onSelect)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(cell: CalendarCell, isToday: Boolean, weekday: String, onSelect: ((LocalDate) -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    val (background, content) = when (cell.status) {
        CalendarDayStatus.Done -> scheme.primary to scheme.onPrimary
        CalendarDayStatus.Partial -> scheme.primaryContainer to scheme.onPrimaryContainer
        CalendarDayStatus.Paused -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        CalendarDayStatus.Missed -> Color.Transparent to scheme.error
        CalendarDayStatus.Pending -> Color.Transparent to scheme.onSurface
        CalendarDayStatus.Unscheduled, CalendarDayStatus.Future -> Color.Transparent to scheme.onSurfaceVariant
    }
    val border = when (cell.status) {
        CalendarDayStatus.Missed -> scheme.error
        CalendarDayStatus.Partial, CalendarDayStatus.Pending -> scheme.primary
        else -> Color.Transparent
    }
    val tappable = onSelect != null && cell.status != CalendarDayStatus.Future
    Box(
        Modifier
            .widthIn(max = 44.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (isToday) Modifier.border(2.dp, scheme.primary, CircleShape).padding(3.dp) else Modifier)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, border, CircleShape)
            .then(if (tappable) Modifier.clickable(role = Role.Button) { onSelect?.invoke(cell.date) } else Modifier)
            .alpha(if (cell.status == CalendarDayStatus.Future) FUTURE_ALPHA else 1f)
            .semantics(mergeDescendants = true) {
                contentDescription = "$weekday, ${cell.date.dayOfMonth}"
                stateDescription = cell.description
                if (tappable) role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            cell.date.dayOfMonth.toString(),
            style = PrumoType.dataSmall,
            color = content,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

private const val DAYS_IN_WEEK = 7
private const val FUTURE_ALPHA = 0.5f

@PhonePreviews
@Composable
private fun MonthCalendarPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            val first = LocalDate.of(2026, 11, 1)
            MonthCalendar(
                days = (0 until 30).map { index ->
                    val day = first.plusDays(index.toLong())
                    val status = when {
                        day.dayOfMonth > 17 -> CalendarDayStatus.Future
                        day.dayOfMonth == 17 -> CalendarDayStatus.Pending
                        day.dayOfMonth in 5..8 -> CalendarDayStatus.Paused
                        day.dayOfMonth == 12 -> CalendarDayStatus.Missed
                        day.dayOfMonth == 14 -> CalendarDayStatus.Partial
                        else -> CalendarDayStatus.Done
                    }
                    CalendarCell(day, status, "")
                },
                today = LocalDate.of(2026, 11, 17),
                modifier = Modifier.padding(16.dp),
                onSelect = {},
            )
        }
    }
}
