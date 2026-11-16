package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import com.henriquesebastiao.prumo.core.designsystem.component.ReorderableColumn
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.DayItem
import com.henriquesebastiao.prumo.feature.habits.model.HabitsDay
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Promotion
import com.henriquesebastiao.prumo.feature.habits.model.QuitItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A `check` just marked, offered to undo in a snackbar. */
data class UndoRequest(val habitId: String, val name: String, val on: LocalDate)

data class TodayUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val day: HabitsDay? = null,
    /** The day shown; null is today (the server's). */
    val selected: LocalDate? = null,
    /** Quantities being stepped, shown at once and sent after a short pause. */
    val drafts: Map<String, Int> = emptyMap(),
    /** A mark the server refused. */
    val actionError: AppError? = null,
    val undo: UndoRequest? = null,
)

/**
 * Hoje (H9): the day from the server, grouped and ordered (H10); marks by the server's day (H3),
 * earlier days too. Nothing about the sections is decided here.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: HabitsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    /** The search of "Marcar hábitos", typed synchronously (CLAUDE.md, "Campos de texto"). */
    var query by mutableStateOf("")
        private set

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
            it.copy(loading = it.day == null, refreshing = refresh && it.day != null, error = null)
        }
        loading = viewModelScope.launch {
            val outcome = repository.day(mutableState.value.selected)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, day = outcome.value)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    /** Another day: null (or today, or later) goes back to today. */
    fun onDay(day: LocalDate?) {
        val today = mutableState.value.day?.today
        val selected = day?.takeIf { today == null || it < today }
        if (selected == mutableState.value.selected) return
        mutableState.update { it.copy(selected = selected, day = null, loading = true) }
        load()
    }

    fun onQuery(text: String) {
        query = text
    }

    /** A `check`: marks or unmarks the day; a mark can be undone. */
    fun toggle(item: DayItem) {
        val on = mutableState.value.day?.on ?: return
        val id = item.habit.id
        mutableState.update { it.copy(actionError = null, undo = null) }
        viewModelScope.launch {
            val outcome = if (item.done) repository.unmark(id, on) else repository.mark(id, on, 1)
            mutableState.update {
                when {
                    outcome is Outcome.Failed -> it.copy(actionError = outcome.error)
                    item.done -> it
                    else -> it.copy(undo = UndoRequest(id, item.habit.name, on))
                }
            }
        }
    }

    fun undo() {
        val request = mutableState.value.undo ?: return
        mutableState.update { it.copy(undo = null) }
        viewModelScope.launch {
            val outcome = repository.unmark(request.habitId, request.on)
            if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
        }
    }

    fun onUndoShown() = mutableState.update { it.copy(undo = null) }

    /** A `quantity`: one step up or down, sent after a pause so a run of taps is one write. */
    fun step(item: DayItem, up: Boolean) {
        val id = item.habit.id
        val current = mutableState.value.drafts[id] ?: item.value ?: 0
        val next = (current + if (up) stepOf(item) else -stepOf(item)).coerceAtLeast(0)
        mutableState.update { it.copy(drafts = it.drafts + (id to next), actionError = null) }
        pending.remove(id)?.cancel()
        pending[id] = viewModelScope.launch {
            delay(STEP_PAUSE)
            pending.remove(id)
            val on = mutableState.value.day?.on ?: return@launch
            val outcome = repository.mark(id, on, next)
            mutableState.update {
                it.copy(drafts = it.drafts - id, actionError = (outcome as? Outcome.Failed)?.error ?: it.actionError)
            }
        }
    }

    /** H10: the habits without a time dragged into a new order. */
    fun reorder(ids: List<String>) {
        viewModelScope.launch {
            val outcome = repository.reorder(ids)
            if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
        }
    }

    fun onActionErrorShown() = mutableState.update { it.copy(actionError = null) }

    companion object {
        /** About an eighth of the target: 250 ml of 2 l; 1 for a small target. */
        fun stepOf(item: DayItem): Int = ((item.habit.targetValue ?: STEPS) / STEPS).coerceAtLeast(1)

        private const val STEPS = 8
        private const val STEP_PAUSE = 500L
    }
}

@Suppress("LongParameterList")
internal class TodayEvents(
    val onToggle: (DayItem) -> Unit,
    val onStep: (DayItem, Boolean) -> Unit,
    val onOpen: (String) -> Unit,
    val onRetry: () -> Unit,
    val onDay: (LocalDate?) -> Unit = {},
    val onReorder: (List<String>) -> Unit = {},
)

/**
 * Hoje (H9): the day in a hero (with its arrows to earlier days), the habits to quit in a strip,
 * the timeline with a mark of "now", those without a time (draggable), the periods still open and
 * what was met, the last two folded.
 */
@Composable
internal fun TodayTab(state: TodayUiState, events: TodayEvents, modifier: Modifier = Modifier) {
    val day = state.day
    var showPeriod by rememberSaveable { mutableStateOf(false) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    when {
        day == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        day == null -> LoadingState(stringResource(R.string.habits_loading), modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            state.actionError?.let { error ->
                item { Text(habitsErrorText(error), color = MaterialTheme.colorScheme.error) }
            }
            item(key = "hero") { DayHero(day, events.onDay) }
            if (day.quit.isNotEmpty()) item(key = "quit") { QuitStrip(day.quit, events.onOpen) }
            if (day.scheduled == 0 && day.period.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.Checklist,
                        title = stringResource(R.string.habits_today_empty),
                        body = stringResource(R.string.habits_today_empty_hint),
                    )
                }
            }
            if (day.timeline.isNotEmpty()) {
                item(key = "timeline") { SectionHeader(stringResource(R.string.habits_section_timeline)) }
                val now = day.now
                val before = day.timeline.filter { now == null || (it.habit.timeOfDay ?: now) <= now }
                val after = day.timeline - before.toSet()
                dayRows("timeline", before, state.drafts, events)
                if (now != null) item(key = "now") { NowMark(Formats.time(now)) }
                dayRows("timeline", after, state.drafts, events)
            }
            if (day.untimed.isNotEmpty()) {
                item(key = "untimed") { SectionHeader(stringResource(R.string.habits_section_untimed)) }
                item(key = "untimed-list") { UntimedRows(day.untimed, state.drafts, events) }
            }
            if (day.period.isNotEmpty()) {
                item(key = "period") {
                    FoldHeader(stringResource(R.string.habits_section_period, day.period.size), showPeriod) {
                        showPeriod = !showPeriod
                    }
                }
                if (showPeriod) dayRows("period", day.period, state.drafts, events)
            }
            if (day.doneItems.isNotEmpty()) {
                item(key = "done") {
                    val label = if (day.isToday) R.string.habits_section_done else R.string.habits_section_done_past
                    FoldHeader(stringResource(label, day.doneItems.size), showDone) { showDone = !showDone }
                }
                if (showDone) dayRows("done", day.doneItems, state.drafts, events)
            }
        }
    }
}

@Composable
private fun DayHero(day: HabitsDay, onDay: (LocalDate?) -> Unit) {
    val allDone = day.scheduled > 0 && day.open == 0
    SectionCard(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.md),
        tone = SectionTone.Primary,
        hero = true,
        icon = PrumoIcons.Checklist,
        overline = Formats.weekdayAndDate(day.on),
        celebrating = allDone && day.isToday,
    ) {
        if (day.scheduled > 0) {
            FitText(
                stringResource(
                    if (day.isToday) R.string.habits_today_progress else R.string.habits_day_progress,
                    day.done,
                    day.scheduled,
                ),
                style = PrumoType.dataHeadline,
            )
            LinearWavyProgressIndicator(
                progress = { day.done / day.scheduled.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (allDone) {
            Text(
                stringResource(R.string.habits_today_all_done),
                style = MaterialTheme.typography.titleMedium,
                color = PrumoTheme.extendedColors.success,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDay(day.on.minusDays(1)) }) {
                Icon(painterResource(PrumoIcons.ChevronLeft), stringResource(R.string.habits_previous_day))
            }
            DateField(
                value = day.on,
                onValueChange = onDay,
                label = stringResource(R.string.habits_pick_day),
                today = day.today,
                asTag = true,
            )
            IconButton(onClick = { onDay(day.on.plusDays(1)) }, enabled = !day.isToday) {
                Icon(painterResource(PrumoIcons.ChevronRight), stringResource(R.string.habits_next_day))
            }
            if (!day.isToday) {
                TextButton(onClick = { onDay(null) }) { Text(stringResource(R.string.habits_back_to_today)) }
            }
        }
    }
}

/** The habits to quit, one chip each with the days without; a tap opens the habit. */
@Composable
private fun QuitStrip(quit: List<QuitItem>, onOpen: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(quit, key = { it.id }) { habit ->
            AssistChip(
                onClick = { onOpen(habit.id) },
                label = {
                    Text(
                        stringResource(
                            R.string.habits_join,
                            habit.name,
                            stringResource(R.string.habits_clean, daysText(habit.cleanDays)),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { HabitAvatar(habit.color, habit.icon, size = 24) },
            )
        }
    }
}

/** "agora · 14:30" between the past and the coming times of the timeline. */
@Composable
private fun NowMark(time: String) {
    val color = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = color)
        Text(stringResource(R.string.habits_now, time), style = MaterialTheme.typography.labelMedium, color = color)
        HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = color)
    }
}

/** A folded section ("Neste período · 2"), opened with a tap. */
@Composable
private fun FoldHeader(title: String, open: Boolean, onToggle: () -> Unit) {
    TextButton(onClick = onToggle, modifier = Modifier.padding(top = Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Icon(
            painterResource(PrumoIcons.ChevronRight),
            contentDescription = null,
            modifier = Modifier.size(20.dp).rotate(if (open) QUARTER_TURN else 0f),
        )
    }
}

@Composable
private fun UntimedRows(items: List<DayItem>, drafts: Map<String, Int>, events: TodayEvents) {
    val byId = items.associateBy { it.habit.id }
    if (items.size < 2) {
        items.forEachIndexed { index, item ->
            GroupedListItem(dayRow(item, drafts[item.habit.id], events), index = index, count = items.size)
        }
        return
    }
    ReorderableColumn(
        keys = items.map { it.habit.id },
        onReorder = events.onReorder,
        label = { byId[it]?.habit?.name.orEmpty() },
    ) { id, handle ->
        val item = byId.getValue(id)
        val row = dayRow(item, drafts[id], events)
        row.copy(
            leading = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    handle()
                    HabitAvatar(item.habit.color, item.habit.icon)
                }
            },
        )
    }
}

/** Rows of a section of the day, keyed by section so a mark moves the row with an animation. */
internal fun LazyListScope.dayRows(
    section: String,
    items: List<DayItem>,
    drafts: Map<String, Int>,
    events: TodayEvents,
) {
    itemsIndexed(items, key = { _, item -> "$section-${item.habit.id}" }) { index, item ->
        GroupedListItem(
            dayRow(item, drafts[item.habit.id], events),
            index = index,
            count = items.size,
            modifier = Modifier.animateItem(),
        )
    }
}

/** A habit of the day: its time, amount, period and why it rose to the day; a tap or a counter. */
@Composable
internal fun dayRow(item: DayItem, draft: Int?, events: TodayEvents): GroupedRow {
    val quantity = item.habit.measure == Measure.Quantity
    val parts = listOfNotNull(
        item.habit.timeOfDay?.let(Formats::time),
        amountText(item.habit, draft ?: item.value).takeIf { quantity },
        item.period?.takeIf { !item.done }?.let { periodText(it) },
        when (item.promoted) {
            Promotion.PreferredDay -> stringResource(R.string.habits_preferred_day)
            Promotion.DueSoon -> dueSoonText(item.dueInDays ?: 0)
            null -> null
        },
    )
    val badge: (@Composable () -> Unit)? = when {
        item.late -> {
            { Tag(stringResource(R.string.habits_late), small = true) }
        }

        item.period != null && !item.done && item.settled -> {
            { Tag(stringResource(R.string.habits_period_done), small = true) }
        }

        else -> null
    }
    val action = if (quantity) {
        RowAction.Static(trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                badge?.invoke()
                Stepper(item, draft ?: item.value ?: 0, events)
            }
        })
    } else {
        RowAction.Click(
            onClick = { events.onToggle(item) },
            chevron = false,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    badge?.invoke()
                    Checkbox(checked = item.done, onCheckedChange = null)
                }
            },
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
private fun dueSoonText(days: Int): String = if (days <=
    0
) {
    stringResource(R.string.habits_due_today)
} else {
    pluralStringResource(R.plurals.habits_due_in, days, days)
}

@Composable
private fun Stepper(item: DayItem, value: Int, events: TodayEvents) {
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
private const val QUARTER_TURN = 90f

/** "Marcar hábitos" over the current screen (the Home's quick action, the app shortcut). */
@Composable
internal fun CheckInSheet(onDismiss: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        CheckInContent(
            state = state,
            query = viewModel.query,
            onQuery = viewModel::onQuery,
            events = TodayEvents(viewModel::toggle, viewModel::step, onOpen = {}, onRetry = viewModel::load),
        )
    }
}

/**
 * The content of the check-in sheet, without state: every habit of today (the periods still open
 * included), with a search by name that ignores accents and case.
 */
@Composable
internal fun CheckInContent(
    state: TodayUiState,
    query: String,
    onQuery: (String) -> Unit,
    events: TodayEvents,
    modifier: Modifier = Modifier,
) {
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
        val day = state.day
        when {
            day == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

            day == null -> LoadingState(stringResource(R.string.habits_loading), Modifier.padding(Spacing.xl))

            day.all.isEmpty() -> Text(stringResource(R.string.habits_today_empty_hint))

            else -> {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text(stringResource(R.string.habits_search)) },
                    leadingIcon = { Icon(painterResource(PrumoIcons.Search), contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                )
                val wanted = searchable(query)
                val items = day.all.filter { wanted.isEmpty() || wanted in searchable(it.habit.name) }
                if (items.isEmpty()) Text(stringResource(R.string.habits_search_empty))
                items.forEachIndexed { index, item ->
                    GroupedListItem(
                        dayRow(item, state.drafts[item.habit.id], events),
                        index = index,
                        count = items.size,
                    )
                }
            }
        }
        state.actionError?.let { Text(habitsErrorText(it), color = MaterialTheme.colorScheme.error) }
    }
}
