package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.Day
import com.henriquesebastiao.prumo.feature.studies.model.DayRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val day: Day? = null,
)

/** Hoje: the day of the plan in force, as the server reads it (E4, E5). */
@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: StudiesRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    private var loading: Job? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load(refresh = true) } }
    }

    /** Pull to refresh: asks the server first, not the device's copy (D21). */
    fun pull() {
        repository.expireCache()
        load(refresh = true)
    }

    fun load(refresh: Boolean = false) {
        loading?.cancel()
        mutableState.update { it.copy(loading = it.day == null, refreshing = refresh && it.day != null, error = null) }
        loading = viewModelScope.launch {
            val outcome = repository.day()
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, day = outcome.value)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }
}

internal class TodayEvents(val onOpenSubject: (String) -> Unit, val onPlan: () -> Unit, val onRetry: () -> Unit)

/**
 * Hoje (D24): the minutes done against the day's plan in a hero, then each subject of the day with
 * its goal, minutes and status, all from the server.
 */
@Composable
internal fun TodayTab(state: TodayUiState, events: TodayEvents, modifier: Modifier = Modifier) {
    val day = state.day
    when {
        day == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        day == null -> LoadingState(stringResource(R.string.studies_loading), modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            item { TodayHero(day) }
            if (day.rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.CalendarMonth,
                        title = stringResource(R.string.studies_today_no_plan),
                        body = stringResource(R.string.studies_today_no_plan_hint),
                        action = {
                            FilledTonalButton(onClick = events.onPlan, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.studies_plan_new))
                            }
                        },
                    )
                }
            } else {
                item { SectionHeader(stringResource(R.string.studies_today_rows)) }
                dayRows(day.rows, events.onOpenSubject)
            }
        }
    }
}

@Composable
private fun TodayHero(day: Day) {
    SectionCard(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.md),
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.School,
        overline = Formats.weekdayAndDate(day.on),
        celebrating = day.met == true,
    ) {
        FitText(minutesText(day.doneMinutes), style = PrumoType.dataDisplay)
        if (day.plannedMinutes > 0) {
            Text(
                stringResource(R.string.studies_today_progress, minutesText(day.plannedMinutes)),
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearWavyProgressIndicator(
                progress = { day.doneMinutes.coerceAtMost(day.plannedMinutes) / day.plannedMinutes.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (day.met == true) {
            Text(
                stringResource(R.string.studies_today_met),
                style = MaterialTheme.typography.titleMedium,
                color = PrumoTheme.extendedColors.success,
            )
        }
    }
}

/** The subjects of a day as rows: goal, minutes and status (E4), with the blocks' times. */
internal fun LazyListScope.dayRows(rows: List<DayRow>, onOpen: (String) -> Unit) {
    itemsIndexed(rows, key = { _, row -> row.subject.id }) { index, row ->
        GroupedListItem(dayRow(row) { onOpen(row.subject.id) }, index = index, count = rows.size)
    }
}

@Composable
internal fun dayRow(row: DayRow, onClick: () -> Unit): GroupedRow = GroupedRow(
    title = row.subject.name,
    summary = rowSummary(row),
    action = RowAction.Click(
        onClick = onClick,
        chevron = false,
        trailing = { StatusChip(row.status.label(), row.status.tone, small = true) },
    ),
    leading = { StudyAvatar(row.subject.color) },
)

/** "às 19:00 · meta 1h · nada ainda". */
@Composable
private fun rowSummary(row: DayRow): String {
    val times = row.blocks.mapNotNull { it.startsAt }.map(Formats::time)
    val goal = if (row.goalMinutes > 0) {
        stringResource(R.string.studies_row_goal, minutesText(row.goalMinutes))
    } else {
        stringResource(R.string.studies_row_no_goal)
    }
    val done = if (row.doneMinutes > 0) {
        stringResource(R.string.studies_row_done, minutesText(row.doneMinutes))
    } else {
        stringResource(R.string.studies_row_nothing)
    }
    val parts = listOfNotNull(
        times.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.studies_row_at, it.joinToString(", ")) },
        goal,
        done,
    )
    return parts.joinToString(SEPARATOR)
}

internal const val SEPARATOR = " · "
