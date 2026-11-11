package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.DayStrip
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.PeriodSelector
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.RowValue
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.component.StripDay
import com.henriquesebastiao.prumo.core.designsystem.component.weekdayLong
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.Day
import com.henriquesebastiao.prumo.feature.studies.model.Week
import com.henriquesebastiao.prumo.feature.studies.model.WeekSubject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeekUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val week: Week? = null,
    /** The day whose subjects show under the strip: today in the current week, else Monday. */
    val selected: LocalDate? = null,
) {
    val selectedDay: Day? get() = week?.days?.firstOrNull { it.on == selected } ?: week?.days?.firstOrNull()
}

/** Semana: a week of the plan as the server reads it (E6), moving by the server's previous and next. */
@HiltViewModel
class WeekViewModel @Inject constructor(private val repository: StudiesRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(WeekUiState())
    val state: StateFlow<WeekUiState> = mutableState.asStateFlow()

    private var loading: Job? = null

    /** The week shown (any of its days), or null for the current one. */
    private var on: LocalDate? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load(refresh = true) } }
    }

    fun pull() {
        repository.expireCache()
        load(refresh = true)
    }

    fun load(refresh: Boolean = false) {
        loading?.cancel()
        mutableState.update {
            it.copy(loading = it.week == null, refreshing = refresh && it.week != null, error = null)
        }
        loading = viewModelScope.launch {
            val outcome = repository.week(on)
            mutableState.update { state ->
                when (outcome) {
                    is Outcome.Ok -> {
                        val week = outcome.value
                        val keep = state.selected?.takeIf { day -> week.days.any { it.on == day } }
                        val today = week.days.firstOrNull { it.isToday }?.on
                        state.copy(
                            loading = false,
                            refreshing = false,
                            week = week,
                            selected = keep ?: today ?: week.start,
                        )
                    }

                    is Outcome.Failed -> state.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    fun previous() = move(mutableState.value.week?.previousStart)

    fun next() = move(mutableState.value.week?.nextStart)

    fun select(day: LocalDate) = mutableState.update { it.copy(selected = day) }

    private fun move(start: LocalDate?) {
        start ?: return
        on = start
        mutableState.update { it.copy(selected = null) }
        load(refresh = true)
    }
}

internal class WeekEvents(
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onSelect: (LocalDate) -> Unit,
    val onOpenSubject: (String) -> Unit,
    val onRetry: () -> Unit,
)

/**
 * Semana (D24): the week's days met and minutes, a strip of the seven days with their status, the
 * subjects of the day picked on it and the minutes by subject. Every number is the server's.
 */
@Composable
internal fun WeekTab(state: WeekUiState, events: WeekEvents, modifier: Modifier = Modifier) {
    val week = state.week
    when {
        week == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        week == null -> LoadingState(stringResource(R.string.studies_loading), modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            item {
                PeriodSelector(
                    label = weekLabel(week),
                    previousLabel = stringResource(R.string.studies_week_previous),
                    nextLabel = stringResource(R.string.studies_week_next),
                    onPrevious = events.onPrevious,
                    onNext = week.nextStart?.let { { events.onNext() } },
                    modifier = Modifier.padding(vertical = Spacing.sm),
                )
            }
            item { WeekTotals(week) }
            item {
                DayStrip(
                    days = week.days.map { StripDay(it.on, it.tone(), it.isToday, it.toneLabel()) },
                    selected = state.selectedDay?.on,
                    onSelect = events.onSelect,
                    modifier = Modifier.padding(vertical = Spacing.md),
                )
            }
            state.selectedDay?.let { day ->
                item(key = "day-" + day.on) { DayHeader(day) }
                if (day.rows.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.studies_day_nothing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                        )
                    }
                }
                dayRows(day.rows, events.onOpenSubject)
            }
            if (week.bySubject.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.studies_week_by_subject), Modifier.padding(top = Spacing.md))
                }
                itemsIndexed(week.bySubject, key = { _, item -> "by-" + item.subject.id }) { index, item ->
                    GroupedListItem(subjectRow(item, events.onOpenSubject), index = index, count = week.bySubject.size)
                }
            } else if (week.days.all { it.rows.isEmpty() }) {
                item {
                    EmptyState(icon = PrumoIcons.ViewWeek, title = stringResource(R.string.studies_week_empty))
                }
            }
        }
    }
}

/** "Esta semana" when today is in it, else "09/11 a 15/11". */
@Composable
@ReadOnlyComposable
private fun weekLabel(week: Week): String = if (week.today in week.start..week.end) {
    stringResource(R.string.studies_week_this)
} else {
    stringResource(R.string.studies_week_of, Formats.shortDate(week.start), Formats.shortDate(week.end))
}

@Composable
private fun WeekTotals(week: Week) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        KpiTile(
            value = stringResource(R.string.studies_week_days_of, week.daysMet, week.daysPlanned),
            label = stringResource(R.string.studies_week_days_met),
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            value = stringResource(
                R.string.studies_week_minutes_of,
                minutesText(week.minutesDone),
                minutesText(week.minutesPlanned),
            ),
            label = stringResource(R.string.studies_week_minutes),
            modifier = Modifier.weight(1f),
        )
    }
}

/** The day picked on the strip: its name and status; its subjects follow as rows. */
@Composable
private fun DayHeader(day: Day) {
    Row(
        Modifier.fillMaxWidth().padding(start = Spacing.xs, top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            stringResource(R.string.studies_day_title, weekdayLong(day.on.dayOfWeek), Formats.shortDate(day.on)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        val tone = day.tone()
        if (tone != null) {
            StatusChip(day.toneLabel(), tone, small = true)
        } else {
            Text(
                day.toneLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun subjectRow(item: WeekSubject, onOpen: (String) -> Unit): GroupedRow = GroupedRow(
    title = item.subject.name,
    action = RowAction.Click(
        onClick = { onOpen(item.subject.id) },
        chevron = false,
        trailing = {
            RowValue(
                if (item.minutesPlanned > 0) {
                    stringResource(
                        R.string.studies_week_minutes_of,
                        minutesText(item.minutesDone),
                        minutesText(item.minutesPlanned),
                    )
                } else {
                    minutesText(item.minutesDone)
                },
            )
        },
    ),
    leading = { StudyAvatar(item.subject.color) },
)

/**
 * How a day of the week reads (E5 of the server, as the web's week): met, a day still open, or one
 * that ended unmet, partial when something was studied. Null for a day without goals.
 */
internal fun Day.tone(): StatusTone? = when {
    met == null -> null
    met -> StatusTone.Success
    isToday || on > today -> StatusTone.Neutral
    rows.any { it.doneMinutes > 0 } -> StatusTone.Warning
    else -> StatusTone.Error
}

@Composable
@ReadOnlyComposable
internal fun Day.toneLabel(): String = stringResource(
    when (tone()) {
        null -> R.string.studies_day_free
        StatusTone.Success -> R.string.studies_day_met
        StatusTone.Neutral -> R.string.studies_day_open
        StatusTone.Warning -> R.string.studies_day_partial
        StatusTone.Error -> R.string.studies_day_missed
    },
)
