package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val habits: List<Habit>? = null,
    val showArchived: Boolean = false,
) {
    val build: List<Habit> get() = habits.orEmpty().filter { it.kind == HabitKind.Build }
    val quit: List<Habit> get() = habits.orEmpty().filter { it.kind == HabitKind.Quit }
}

/** Hábitos: both kinds, with the derived values from the server; archived ones on demand. */
@HiltViewModel
class ListViewModel @Inject constructor(private val repository: HabitsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ListUiState())
    val state: StateFlow<ListUiState> = mutableState.asStateFlow()

    private var loading: Job? = null

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
            it.copy(loading = it.habits == null, refreshing = refresh && it.habits != null, error = null)
        }
        loading = viewModelScope.launch {
            val filter = if (mutableState.value.showArchived) ArchivedFilter.All else ArchivedFilter.Active
            val outcome = repository.habits(filter)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, habits = outcome.value)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    fun toggleArchived() {
        mutableState.update { it.copy(showArchived = !it.showArchived) }
        load(refresh = true)
    }
}

internal class ListEvents(
    val onOpen: (Habit) -> Unit,
    val onNew: () -> Unit,
    val onToggleArchived: () -> Unit,
    val onRetry: () -> Unit,
)

/** Hábitos (D24): to practice with the rate and streak, to quit with the time without. */
@Composable
internal fun ListTab(state: ListUiState, events: ListEvents, modifier: Modifier = Modifier) {
    val habits = state.habits
    when {
        habits == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        habits == null -> LoadingState(stringResource(R.string.habits_loading), modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = events.onToggleArchived) {
                        val label = when (state.showArchived) {
                            true -> R.string.habits_hide_archived
                            false -> R.string.habits_show_archived
                        }
                        Text(stringResource(label))
                    }
                }
            }
            if (habits.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.Checklist,
                        title = stringResource(R.string.habits_list_empty),
                        body = stringResource(R.string.habits_list_empty_hint),
                        action = {
                            FilledTonalButton(onClick = events.onNew) {
                                Text(stringResource(R.string.habits_new_habit))
                            }
                        },
                    )
                }
            }
            if (state.build.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.habits_list_build)) }
                itemsIndexed(state.build, key = { _, habit -> habit.id }) { index, habit ->
                    GroupedListItem(habitRow(habit) { events.onOpen(habit) }, index = index, count = state.build.size)
                }
            }
            if (state.quit.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.habits_list_quit), Modifier.padding(top = Spacing.md)) }
                itemsIndexed(state.quit, key = { _, habit -> habit.id }) { index, habit ->
                    GroupedListItem(habitRow(habit) { events.onOpen(habit) }, index = index, count = state.quit.size)
                }
            }
        }
    }
}

@Composable
private fun habitRow(habit: Habit, onClick: () -> Unit): GroupedRow {
    val summary = if (habit.kind == HabitKind.Build) {
        listOfNotNull(
            habit.frequency?.let { frequencyText(it) },
            habit.ratePercent?.let { stringResource(R.string.habits_rate, it) }
                ?: stringResource(R.string.habits_no_rate),
            stringResource(R.string.habits_streak, streakText(habit.currentStreak ?: 0, habit.streakInWeeks)),
        )
    } else {
        val relapses = habit.relapses30d ?: 0
        listOf(
            stringResource(R.string.habits_clean, daysText(habit.cleanDays ?: 0)),
            if (relapses == 0) {
                stringResource(R.string.habits_no_relapses_30d)
            } else {
                pluralStringResource(R.plurals.habits_relapses_30d, relapses, relapses)
            },
        )
    }
    val tag = when {
        habit.archived -> stringResource(R.string.habits_archived)
        habit.paused -> stringResource(R.string.habits_paused)
        else -> null
    }
    return GroupedRow(
        title = habit.name,
        summary = summary.joinToString(SEPARATOR),
        action = RowAction.Click(onClick, trailing = tag?.let { { Tag(it, small = true) } }),
        leading = { HabitAvatar(habit.color, habit.icon) },
        neutral = habit.archived,
    )
}
