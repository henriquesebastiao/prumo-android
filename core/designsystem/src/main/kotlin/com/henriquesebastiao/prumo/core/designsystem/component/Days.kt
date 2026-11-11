package com.henriquesebastiao.prumo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.prumo.core.designsystem.R
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate

/** "Seg", "Ter"… for [day] (pt-BR only, D07). */
@Composable
fun weekdayShort(day: DayOfWeek): String = stringArrayResource(R.array.designsystem_weekdays_short)[day.value - 1]

/** "segunda-feira"… for [day]. */
@Composable
fun weekdayLong(day: DayOfWeek): String = stringArrayResource(R.array.designsystem_weekdays_long)[day.value - 1]

/**
 * The days of the week, Monday first, as seven toggle buttons side by side (D24): a block of a
 * plan, a habit's days. Each one reads as the day's full name and whether it's on.
 */
@Composable
fun WeekdayChips(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shorts = stringArrayResource(R.array.designsystem_weekdays_short)
    val longs = stringArrayResource(R.array.designsystem_weekdays_long)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        DayOfWeek.entries.forEach { day ->
            val checked = day in selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { onToggle(day) },
                enabled = enabled,
                // A day off still reads as a button, also on a card's container.
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = longs[day.value - 1] },
            ) {
                Text(
                    shorts[day.value - 1],
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 1.sp),
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

/** One day of a [DayStrip]: its [tone] (null for a day without goals) and what it reads as. */
data class StripDay(
    val date: LocalDate,
    val tone: StatusTone?,
    val today: Boolean,
    /** "Dia cumprido", "Livre"…: read after the date. */
    val description: String,
)

/**
 * The seven days of a week in a row (D24): the weekday, the day of the month and a mark in the
 * status's color (a check, a warning, an error, a dot for one still open, a ring for a free day).
 * Today has its number in the primary container; [selected] is outlined. Tapping a day picks it.
 */
@Composable
fun DayStrip(
    days: List<StripDay>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shorts = stringArrayResource(R.array.designsystem_weekdays_short)
    val longs = stringArrayResource(R.array.designsystem_weekdays_long)
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        days.forEach { day ->
            val index = day.date.dayOfWeek.value - 1
            val isSelected = day.date == selected
            val scheme = MaterialTheme.colorScheme
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isSelected) scheme.surfaceContainerHighest else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(day.date) })
                    .heightIn(min = 48.dp)
                    .padding(vertical = Spacing.sm)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${longs[index]}, ${day.date.dayOfMonth}"
                        stateDescription = day.description
                        role = Role.Tab
                        this.selected = isSelected
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    shorts[index],
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp, stepSize = 1.sp),
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (day.today) scheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.date.dayOfMonth.toString(),
                        style = PrumoType.dataMedium,
                        color = if (day.today) scheme.onPrimaryContainer else scheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                DayMark(day.tone)
            }
        }
    }
}

@Composable
private fun DayMark(tone: StatusTone?) {
    val extended = PrumoTheme.extendedColors
    val scheme = MaterialTheme.colorScheme
    val size = Modifier.size(18.dp)
    when (tone) {
        null -> Box(size.padding(3.dp).border(1.5.dp, scheme.outlineVariant, CircleShape))

        StatusTone.Neutral -> Box(size, contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).background(scheme.outline, CircleShape))
        }

        else -> Icon(
            painterResource(
                when (tone) {
                    StatusTone.Success -> PrumoIcons.CheckCircleFilled
                    StatusTone.Warning -> PrumoIcons.WarningFilled
                    else -> PrumoIcons.ErrorFilled
                },
            ),
            contentDescription = null,
            tint = when (tone) {
                StatusTone.Success -> extended.success
                StatusTone.Warning -> extended.warning
                else -> scheme.error
            },
            modifier = size,
        )
    }
}

@PhonePreviews
@Composable
private fun WeekdayChipsPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            var days by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)) }
            WeekdayChips(days, { day -> days = if (day in days) days - day else days + day }, Modifier.padding(16.dp))
        }
    }
}

@PhonePreviews
@Composable
private fun DayStripPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            val monday = LocalDate.of(2026, 11, 16)
            val tones = listOf(StatusTone.Success, StatusTone.Neutral, null, null, null, null, null)
            DayStrip(
                days = tones.mapIndexed { index, tone ->
                    StripDay(monday.plusDays(index.toLong()), tone, today = index == 1, description = "")
                },
                selected = monday.plusDays(1),
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
