package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.MonthDayChips
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.WeekdayChips
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.Period
import com.henriquesebastiao.prumo.feature.habits.model.weekdayOf
import com.henriquesebastiao.prumo.feature.habits.model.wire
import java.time.DayOfWeek

/**
 * The fields of a frequency (H2, D48 of the server), in the editor and the frequency sheet: fixed
 * dates (every day, weekdays every N weeks, days of the month or the Nth weekday every N months)
 * or N times in a week, a fortnight or a month, with an optional preferred day.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FrequencyFields(rule: FrequencyInput, onChange: (FrequencyInput) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(stringResource(R.string.habits_frequency), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FrequencyKind.Editable.forEach { kind ->
                FilterChip(
                    selected = rule.kind == kind,
                    onClick = { onChange(rule.copy(kind = kind)) },
                    label = { Text(kind.label()) },
                )
            }
        }
        when (rule.kind) {
            FrequencyKind.Weekdays -> {
                WeekdayChips(rule.weekdays, { day -> onChange(rule.copy(weekdays = rule.weekdays.toggle(day))) })
                CountStepper(
                    stringResource(R.string.habits_every_n_weeks),
                    rule.everyNWeeks,
                    1..FrequencyInput.MAX_EVERY_WEEKS,
                ) { onChange(rule.copy(everyNWeeks = it)) }
            }

            FrequencyKind.MonthDays -> {
                Text(stringResource(R.string.habits_month_days), style = MaterialTheme.typography.labelLarge)
                MonthDayChips(rule.monthDays, { day -> onChange(rule.copy(monthDays = rule.monthDays.toggle(day))) })
                Text(
                    stringResource(R.string.habits_month_days_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.habits_month_last_day),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(rule.monthLastDay, { onChange(rule.copy(monthLastDay = it)) })
                }
                EveryMonths(rule, onChange)
            }

            FrequencyKind.MonthWeekday -> {
                Text(stringResource(R.string.habits_week_of_month), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    WEEKS_OF_MONTH.forEach { week ->
                        FilterChip(
                            selected = rule.weekOfMonth == week,
                            onClick = { onChange(rule.copy(weekOfMonth = week)) },
                            label = { Text(nthText(week, rule.weekday)) },
                        )
                    }
                }
                Text(stringResource(R.string.habits_weekday), style = MaterialTheme.typography.labelLarge)
                WeekdayChips(setOf(rule.weekday), { day -> onChange(rule.copy(weekday = day)) })
                EveryMonths(rule, onChange)
            }

            FrequencyKind.PerPeriod, FrequencyKind.Weekly -> PerPeriodFields(rule, onChange)

            FrequencyKind.Daily -> Unit
        }
        if (rule.anchored) {
            Text(
                stringResource(R.string.habits_anchor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EveryMonths(rule: FrequencyInput, onChange: (FrequencyInput) -> Unit) {
    CountStepper(
        stringResource(R.string.habits_every_n_months),
        rule.everyNMonths,
        1..FrequencyInput.MAX_EVERY_MONTHS,
    ) {
        onChange(rule.copy(everyNMonths = it))
    }
}

@Composable
private fun PerPeriodFields(rule: FrequencyInput, onChange: (FrequencyInput) -> Unit) {
    ChoiceGroup(
        options = Period.entries,
        selected = rule.period,
        label = { it.label() },
        onSelect = { period ->
            // A preferred day of another kind of period would be refused: it starts over.
            val sameKind = (period == Period.Month) == (rule.period == Period.Month)
            onChange(
                rule.copy(
                    period = period,
                    times = rule.times.coerceAtMost(period.days),
                    preferredDay = rule.preferredDay.takeIf { sameKind },
                ),
            )
        },
    )
    CountStepper(stringResource(R.string.habits_times), rule.times, 1..rule.period.days) {
        onChange(rule.copy(times = it))
    }
    Text(stringResource(R.string.habits_preferred_day_label), style = MaterialTheme.typography.labelLarge)
    val preferred = rule.preferredDay
    if (rule.period == Period.Month) {
        MonthDayChips(setOfNotNull(preferred), { day ->
            onChange(rule.copy(preferredDay = day.takeIf { it != preferred }))
        })
    } else {
        WeekdayChips(
            setOfNotNull(preferred?.let(::weekdayOf)),
            { day -> onChange(rule.copy(preferredDay = day.wire.takeIf { it != preferred })) },
        )
    }
    Text(
        stringResource(R.string.habits_preferred_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A count with − and +, kept in [range]: "Vezes", "A cada quantas semanas". */
@Composable
internal fun CountStepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Icon(painterResource(PrumoIcons.DoNotDisturbOn), contentDescription = stringResource(R.string.habits_fewer))
        }
        FitText(value.toString(), style = PrumoType.dataLarge)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Icon(painterResource(PrumoIcons.Add), contentDescription = stringResource(R.string.habits_more_one))
        }
    }
}

/** Counting from the version's week or month: worth saying when it skips weeks or months. */
private val FrequencyInput.anchored: Boolean
    get() = when (kind) {
        FrequencyKind.Weekdays -> everyNWeeks > 1
        FrequencyKind.MonthDays, FrequencyKind.MonthWeekday -> everyNMonths > 1
        FrequencyKind.PerPeriod, FrequencyKind.Weekly -> period == Period.Fortnight
        FrequencyKind.Daily -> false
    }

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

private val WEEKS_OF_MONTH = listOf(1, 2, 3, 4, FrequencyInput.LAST)

@PhonePreviews
@Composable
private fun FrequencyFieldsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            Surface {
                var rule by remember {
                    mutableStateOf(
                        FrequencyInput(
                            kind = FrequencyKind.PerPeriod,
                            period = Period.Month,
                            times = 1,
                            preferredDay = 15,
                        ),
                    )
                }
                FrequencyFields(rule, { rule = it }, Modifier.padding(Spacing.screen))
            }
        }
    }
}

@PhonePreviews
@Composable
private fun FrequencyFieldsWeekdaysPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            Surface {
                FrequencyFields(
                    FrequencyInput(
                        kind = FrequencyKind.Weekdays,
                        weekdays = setOf(DayOfWeek.SATURDAY),
                        everyNWeeks = 2,
                    ),
                    {},
                    Modifier.padding(Spacing.screen),
                )
            }
        }
    }
}
