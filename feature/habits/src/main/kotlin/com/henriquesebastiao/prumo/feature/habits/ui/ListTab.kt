package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ReorderableColumn
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.habits.model.Cadence
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
    /** null: both kinds. */
    val kind: HabitKind? = null,
    /** null: every cadence. */
    val cadence: Cadence? = null,
    val actionError: AppError? = null,
)

/** The habits a list shows for [query] and the filters, in the server's order (H10). */
internal fun ListUiState.shown(query: String): List<Habit> {
    val wanted = searchable(query)
    return habits.orEmpty().filter { habit ->
        (wanted.isEmpty() || wanted in searchable(habit.name)) &&
            (kind == null || habit.kind == kind) &&
            (cadence == null || habit.frequency?.cadence == cadence)
    }
}

/** Hábitos: both kinds, with the derived values from the server; archived ones on demand. */
@HiltViewModel
class ListViewModel @Inject constructor(private val repository: HabitsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ListUiState())
    val state: StateFlow<ListUiState> = mutableState.asStateFlow()

    /** The search, typed synchronously (CLAUDE.md, "Campos de texto"). */
    var query by mutableStateOf("")
        private set

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

    fun onQuery(text: String) {
        query = text
    }

    fun onKind(kind: HabitKind?) = mutableState.update { it.copy(kind = kind) }

    fun onCadence(cadence: Cadence?) = mutableState.update { it.copy(cadence = cadence) }

    /** H10: the habits without a time dragged into a new order. */
    fun reorder(ids: List<String>) {
        viewModelScope.launch {
            val outcome = repository.reorder(ids)
            if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
        }
    }
}

@Suppress("LongParameterList")
internal class ListEvents(
    val onOpen: (Habit) -> Unit,
    val onNew: () -> Unit,
    val onToggleArchived: () -> Unit,
    val onRetry: () -> Unit,
    val onQuery: (String) -> Unit = {},
    val onKind: (HabitKind?) -> Unit = {},
    val onCadence: (Cadence?) -> Unit = {},
    val onReorder: (List<String>) -> Unit = {},
)

/**
 * Hábitos, the management list (D24, D48 of the server): search and filters, to practice with the
 * time, rate, streak and next day (those without a time dragged into order), to quit with the
 * time without.
 */
@Composable
internal fun ListTab(state: ListUiState, query: String, events: ListEvents, modifier: Modifier = Modifier) {
    val habits = state.habits
    when {
        habits == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        habits == null -> LoadingState(stringResource(R.string.habits_loading), modifier)

        else -> {
            val shown = state.shown(query)
            val timed = shown.filter { it.kind == HabitKind.Build && it.timeOfDay != null }
            val untimed = shown.filter { it.kind == HabitKind.Build && it.timeOfDay == null }
            val quit = shown.filter { it.kind == HabitKind.Quit }
            val filtering = searchable(query).isNotEmpty() || state.kind != null || state.cadence != null
            LazyColumn(
                modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
                verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
            ) {
                item(key = "filters") { Filters(state, query, events) }
                state.actionError?.let { error ->
                    item { Text(habitsErrorText(error), color = MaterialTheme.colorScheme.error) }
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
                } else if (shown.isEmpty()) {
                    item { Text(stringResource(R.string.habits_list_no_match)) }
                }
                if (timed.isNotEmpty() || untimed.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.habits_list_build)) }
                    itemsIndexed(timed, key = { _, habit -> habit.id }) { index, habit ->
                        GroupedListItem(habitRow(habit) { events.onOpen(habit) }, index = index, count = timed.size)
                    }
                    if (untimed.size > 1 && !filtering) {
                        item(key = "untimed") {
                            val byId = untimed.associateBy { it.id }
                            ReorderableColumn(
                                keys = untimed.map { it.id },
                                onReorder = events.onReorder,
                                label = { byId[it]?.name.orEmpty() },
                                modifier = Modifier.padding(top = if (timed.isEmpty()) 0.dp else Spacing.sm),
                            ) { id, handle ->
                                val habit = byId.getValue(id)
                                habitRow(habit) { events.onOpen(habit) }.copy(
                                    leading = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            handle()
                                            HabitAvatar(habit.color, habit.icon)
                                        }
                                    },
                                )
                            }
                        }
                        item { Hint(stringResource(R.string.habits_reorder_hint)) }
                    } else {
                        itemsIndexed(untimed, key = { _, habit -> habit.id }) { index, habit ->
                            GroupedListItem(
                                habitRow(habit) { events.onOpen(habit) },
                                index = index,
                                count = untimed.size,
                            )
                        }
                    }
                }
                if (quit.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.habits_list_quit), Modifier.padding(top = Spacing.md))
                    }
                    itemsIndexed(quit, key = { _, habit -> habit.id }) { index, habit ->
                        GroupedListItem(habitRow(habit) { events.onOpen(habit) }, index = index, count = quit.size)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(state: ListUiState, query: String, events: ListEvents) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(vertical = Spacing.sm)) {
        OutlinedTextField(
            value = query,
            onValueChange = events.onQuery,
            placeholder = { Text(stringResource(R.string.habits_search)) },
            leadingIcon = { Icon(painterResource(PrumoIcons.Search), contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(state.kind == null, { events.onKind(null) }, { Text(stringResource(R.string.habits_kind_all)) })
            FilterChip(
                state.kind == HabitKind.Build,
                { events.onKind(HabitKind.Build) },
                { Text(stringResource(R.string.habits_kind_build)) },
            )
            FilterChip(
                state.kind == HabitKind.Quit,
                { events.onKind(HabitKind.Quit) },
                { Text(stringResource(R.string.habits_kind_quit)) },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                state.cadence == null,
                { events.onCadence(null) },
                { Text(stringResource(R.string.habits_cadence_all)) },
            )
            Cadence.entries.forEach { cadence ->
                FilterChip(state.cadence == cadence, { events.onCadence(cadence) }, { Text(cadence.label()) })
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = events.onToggleArchived) {
                val label = if (state.showArchived) R.string.habits_hide_archived else R.string.habits_show_archived
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun habitRow(habit: Habit, onClick: () -> Unit): GroupedRow {
    val summary = if (habit.kind == HabitKind.Build) {
        listOfNotNull(
            habit.frequency?.let { frequencyText(it) },
            habit.ratePercent?.let { stringResource(R.string.habits_rate, it) }
                ?: stringResource(R.string.habits_no_rate),
            stringResource(R.string.habits_streak, streakText(habit.currentStreak ?: 0, habit.streakUnit)),
            habit.nextDue?.takeIf { !habit.archived }?.let { stringResource(R.string.habits_next_due, dueText(it)) },
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
    val time = habit.timeOfDay
    return GroupedRow(
        title = habit.name,
        summary = summary.joinToString(SEPARATOR),
        action = RowAction.Click(onClick, trailing = tag?.let { { Tag(it, small = true) } }),
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (time != null) Text(Formats.time(time), style = PrumoType.dataMedium)
                HabitAvatar(habit.color, habit.icon)
            }
        },
        neutral = habit.archived,
    )
}
