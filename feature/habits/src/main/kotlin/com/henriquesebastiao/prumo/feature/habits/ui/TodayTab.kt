package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.HabitsToday
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.TodayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val today: HabitsToday? = null,
    /** Quantities being stepped, shown at once and sent after a short pause. */
    val drafts: Map<String, Int> = emptyMap(),
    /** A mark the server refused. */
    val actionError: AppError? = null,
)

/** Hoje: what today asks, from the server (H2); marks by the server's today (H3). */
@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: HabitsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    private var loading: Job? = null
    private val pending = mutableMapOf<String, Job>()

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
            it.copy(loading = it.today == null, refreshing = refresh && it.today != null, error = null)
        }
        loading = viewModelScope.launch {
            val outcome = repository.today()
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, today = outcome.value)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    /** A `check`: marks or unmarks today. */
    fun toggle(item: TodayItem) {
        val today = mutableState.value.today?.today ?: return
        send { if (item.done) repository.unmark(item.habit.id, today) else repository.mark(item.habit.id, today, 1) }
    }

    /** A `quantity`: one step up or down, sent after a pause so a run of taps is one write. */
    fun step(item: TodayItem, up: Boolean) {
        val id = item.habit.id
        val current = mutableState.value.drafts[id] ?: item.value ?: 0
        val next = (current + if (up) stepOf(item) else -stepOf(item)).coerceAtLeast(0)
        mutableState.update { it.copy(drafts = it.drafts + (id to next), actionError = null) }
        pending.remove(id)?.cancel()
        pending[id] = viewModelScope.launch {
            delay(STEP_PAUSE)
            pending.remove(id)
            val today = mutableState.value.today?.today ?: return@launch
            val outcome = repository.mark(id, today, next)
            mutableState.update {
                it.copy(
                    drafts = it.drafts - id,
                    actionError = (outcome as? Outcome.Failed)?.error ?: it.actionError,
                )
            }
        }
    }

    fun onActionErrorShown() = mutableState.update { it.copy(actionError = null) }

    private fun send(block: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val outcome = block()
            if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
        }
    }

    companion object {
        /** About an eighth of the target: 250 ml of 2 l; 1 for a small target. */
        fun stepOf(item: TodayItem): Int = ((item.habit.targetValue ?: STEPS) / STEPS).coerceAtLeast(1)

        private const val STEPS = 8
        private const val STEP_PAUSE = 500L
    }
}

internal class TodayEvents(
    val onToggle: (TodayItem) -> Unit,
    val onStep: (TodayItem, Boolean) -> Unit,
    val onOpen: (String) -> Unit,
    val onRetry: () -> Unit,
)

/** Hoje (D24): today done over scheduled in a hero, then each habit with its tap or counter. */
@Composable
internal fun TodayTab(state: TodayUiState, events: TodayEvents, modifier: Modifier = Modifier) {
    val today = state.today
    when {
        today == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        today == null -> LoadingState(stringResource(R.string.habits_loading), modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            state.actionError?.let { error ->
                item { Text(habitsErrorText(error), color = MaterialTheme.colorScheme.error) }
            }
            if (today.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.Checklist,
                        title = stringResource(R.string.habits_today_empty),
                        body = stringResource(R.string.habits_today_empty_hint),
                    )
                }
            } else {
                item { TodayHero(today) }
                todayRows(today.items, state.drafts, events)
            }
        }
    }
}

@Composable
private fun TodayHero(today: HabitsToday) {
    SectionCard(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.md),
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.Checklist,
        overline = Formats.weekdayAndDate(today.today),
        celebrating = today.scheduled > 0 && today.done == today.scheduled,
    ) {
        FitText(
            stringResource(R.string.habits_today_progress, today.done, today.scheduled),
            style = PrumoType.dataHeadline,
        )
        LinearWavyProgressIndicator(
            progress = { if (today.scheduled == 0) 0f else today.done / today.scheduled.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        if (today.scheduled > 0 && today.done == today.scheduled) {
            Text(
                stringResource(R.string.habits_today_all_done),
                style = MaterialTheme.typography.titleMedium,
                color = PrumoTheme.extendedColors.success,
            )
        }
    }
}

/** The habits of today as rows (Hoje and the "Marcar hábitos" sheet); a quantity shows its amount in the row. */
internal fun LazyListScope.todayRows(items: List<TodayItem>, drafts: Map<String, Int>, events: TodayEvents) {
    itemsIndexed(items, key = { _, item -> item.habit.id }) { index, item ->
        GroupedListItem(todayRow(item, drafts[item.habit.id], events), index = index, count = items.size)
    }
}

@Composable
internal fun todayRow(item: TodayItem, draft: Int?, events: TodayEvents): GroupedRow {
    val quantity = item.habit.measure == Measure.Quantity
    val parts = listOfNotNull(
        amountText(item.habit, draft ?: item.value).takeIf { quantity },
        item.week?.let { stringResource(R.string.habits_today_week, it.done, it.target) },
        stringResource(R.string.habits_today_week_done).takeIf { item.week != null && !item.done && item.settled },
    )
    val action = if (quantity) {
        RowAction.Static(trailing = { Stepper(item, draft ?: item.value ?: 0, events) })
    } else {
        RowAction.Click(
            onClick = { events.onToggle(item) },
            chevron = false,
            trailing = { Checkbox(checked = item.done, onCheckedChange = null) },
        )
    }
    return GroupedRow(
        title = item.habit.name,
        summary = parts.joinToString(SEPARATOR).ifEmpty { null },
        action = action,
        leading = { HabitAvatar(item.habit.color, item.habit.icon) },
    )
}

@Composable
private fun Stepper(item: TodayItem, value: Int, events: TodayEvents) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { events.onStep(item, false) }, enabled = value > 0) {
            Icon(
                painterResource(PrumoIcons.DoNotDisturbOn),
                contentDescription = stringResource(R.string.habits_decrease, item.habit.name),
            )
        }
        IconButton(onClick = { events.onStep(item, true) }) {
            Icon(
                painterResource(PrumoIcons.Add),
                contentDescription = stringResource(R.string.habits_increase, item.habit.name),
            )
        }
    }
}

internal const val SEPARATOR = " · "

/** "Marcar hábitos" over the current screen (the Home's quick action): today's habits. */
@Composable
internal fun CheckInSheet(onDismiss: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        CheckInContent(
            state = state,
            events = TodayEvents(viewModel::toggle, viewModel::step, onOpen = {}, onRetry = viewModel::load),
        )
    }
}

/** The content of the check-in sheet, without state. */
@Composable
internal fun CheckInContent(state: TodayUiState, events: TodayEvents, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .navigationBarsPadding()
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
    ) {
        Text(
            stringResource(R.string.habits_action_check_in),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = Spacing.md).semantics { heading() },
        )
        val today = state.today
        when {
            today == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

            today == null -> LoadingState(stringResource(R.string.habits_loading), Modifier.padding(Spacing.xl))

            today.items.isEmpty() -> Text(stringResource(R.string.habits_today_empty_hint))

            else -> {
                Text(
                    stringResource(R.string.habits_today_progress, today.done, today.scheduled),
                    style = MaterialTheme.typography.titleMedium,
                )
                today.items.forEachIndexed { index, item ->
                    GroupedListItem(
                        todayRow(item, state.drafts[item.habit.id], events),
                        index = index,
                        count = today.items.size,
                    )
                }
            }
        }
        state.actionError?.let { Text(habitsErrorText(it), color = MaterialTheme.colorScheme.error) }
    }
}
