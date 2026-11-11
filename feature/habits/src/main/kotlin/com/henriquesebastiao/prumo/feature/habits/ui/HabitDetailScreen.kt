package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.CalendarCell
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.MonthCalendar
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PeriodSelector
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.relativeDateTime
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.CalendarMonth
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.Frequency
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyState
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitDetail
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitRef
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Pause
import com.henriquesebastiao.prumo.feature.habits.model.Relapse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitDetailUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val detail: HabitDetail? = null,
    /** The month shown, from the server: its days, today and the months around it. */
    val calendar: CalendarMonth? = null,
    val actionError: AppError? = null,
    val gone: Boolean = false,
    val context: HabitsContext = HabitsContext(),
)

/** A habit (H1–H7): the server's derived values, its month and its history. */
@HiltViewModel(assistedFactory = HabitDetailViewModel.Factory::class)
class HabitDetailViewModel @AssistedInject constructor(
    @Assisted private val habitId: String,
    private val repository: HabitsRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(HabitDetailUiState(context = account.preferences.value.habitsContext(clock)))
    val state: StateFlow<HabitDetailUiState> = mutableState.asStateFlow()

    private var month: YearMonth? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) load(refresh = true) } }
    }

    fun pull() {
        repository.expireCache()
        load(refresh = true)
    }

    fun load(refresh: Boolean = false) {
        mutableState.update {
            it.copy(
                loading = it.detail == null,
                refreshing = refresh && it.detail != null,
                error = null,
                context = account.preferences.value.habitsContext(clock),
            )
        }
        viewModelScope.launch {
            val detail = repository.habit(habitId)
            val calendar = (detail as? Outcome.Ok)?.value?.habit
                ?.takeIf { it.kind == HabitKind.Build }
                ?.let { (repository.calendar(habitId, month) as? Outcome.Ok)?.value }
            mutableState.update {
                when (detail) {
                    is Outcome.Ok -> it.copy(
                        loading = false,
                        refreshing = false,
                        detail = detail.value,
                        calendar = calendar,
                    )

                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = detail.error)
                }
            }
        }
    }

    /** Another month, by the server's previous and next. */
    fun showMonth(target: YearMonth?) {
        target ?: return
        month = target
        viewModelScope.launch {
            val outcome = repository.calendar(habitId, target)
            if (outcome is Outcome.Ok) mutableState.update { it.copy(calendar = outcome.value) }
        }
    }

    /** A day of the calendar of a `check`: marks it, or unmarks a done one. */
    fun toggleDay(day: LocalDate) {
        val done = mutableState.value.calendar?.days?.firstOrNull { it.on == day }?.state == DayState.Done
        act { if (done) repository.unmark(habitId, day) else repository.mark(habitId, day, 1) }
    }

    fun setArchived(archived: Boolean) = act { repository.setArchived(habitId, archived) }

    fun delete() = act(onDone = { mutableState.update { it.copy(gone = true) } }) { repository.deleteHabit(habitId) }

    fun deleteFrequency(frequency: Frequency) = act { repository.deleteFrequency(frequency.id) }

    /** Ends an open pause on the server's today. */
    fun endPause(pause: Pause) {
        val today = mutableState.value.calendar?.today ?: mutableState.value.context.today
        act { repository.updatePause(pause.id, pause.startsOn, today) }
    }

    fun deletePause(pause: Pause) = act { repository.deletePause(pause.id) }

    fun deleteRelapse(relapse: Relapse) = act { repository.deleteRelapse(relapse.id) }

    fun onActionErrorShown() = mutableState.update { it.copy(actionError = null) }

    private fun act(onDone: () -> Unit = {}, block: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            when (val outcome = block()) {
                is Outcome.Ok -> onDone()
                is Outcome.Failed -> mutableState.update { it.copy(actionError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(habitId: String): HabitDetailViewModel
    }
}

@Suppress("LongParameterList")
internal class HabitDetailEvents(
    val onEdit: () -> Unit,
    val onArchive: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onMonth: (YearMonth?) -> Unit,
    val onDay: (LocalDate) -> Unit,
    val onChangeFrequency: (Frequency?) -> Unit,
    val onDeleteFrequency: (Frequency) -> Unit,
    val onPause: () -> Unit,
    val onEndPause: (Pause) -> Unit,
    val onDeletePause: (Pause) -> Unit,
    val onDeleteRelapse: (Relapse) -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit,
    val onActionErrorShown: () -> Unit,
)

@Composable
internal fun HabitDetailRoute(
    habitId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = hiltViewModel<HabitDetailViewModel, HabitDetailViewModel.Factory>(
        key = "habit-$habitId",
        creationCallback = { it.create(habitId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    HabitDetailScreen(
        state = state,
        onBack = navigator::back,
        events = HabitDetailEvents(
            onEdit = { navigator.navigate(HabitEditorKey(habitId)) },
            onArchive = viewModel::setArchived,
            onDelete = viewModel::delete,
            onMonth = viewModel::showMonth,
            onDay = { day ->
                val habit = state.detail?.habit
                if (habit?.measure == Measure.Quantity) {
                    val value = state.calendar?.days?.firstOrNull { it.on == day }?.value
                    sheet = SheetRequest(SheetKind.Entry, habitId, day = day, value = value)
                } else {
                    viewModel.toggleDay(day)
                }
            },
            onChangeFrequency = { sheet = SheetRequest(SheetKind.Frequency, habitId, id = it?.id) },
            onDeleteFrequency = viewModel::deleteFrequency,
            onPause = { sheet = SheetRequest(SheetKind.Pause, habitId) },
            onEndPause = viewModel::endPause,
            onDeletePause = viewModel::deletePause,
            onDeleteRelapse = viewModel::deleteRelapse,
            onRetry = viewModel::pull,
            onRefresh = viewModel::pull,
            onActionErrorShown = viewModel::onActionErrorShown,
        ),
        onRelapse = { sheet = SheetRequest(SheetKind.Relapse, habitId) },
        modifier = modifier,
    )
    HabitsSheet(sheet, onDismiss = { sheet = null })
}

/**
 * A habit (D24). To practice: rate, streaks, the week in `weekly`, the month's calendar (a past day
 * is marked with a tap), the frequency versions and the pauses. To quit: the time without, the
 * longest stretch, the relapses of 30 days and the history, with "Registrar recaída".
 */
@Composable
internal fun HabitDetailScreen(
    state: HabitDetailUiState,
    onBack: () -> Unit,
    events: HabitDetailEvents,
    onRelapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val notice = state.actionError?.let { habitsErrorText(it) }
    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice)
            events.onActionErrorShown()
        }
    }
    val habit = state.detail?.habit
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(title = "", onBack = onBack) {
                if (habit != null) {
                    IconButton(onClick = events.onEdit) {
                        Icon(
                            painterResource(PrumoIcons.Edit),
                            contentDescription = stringResource(R.string.habits_editor_edit),
                        )
                    }
                    OverflowMenu(
                        listOf(
                            if (habit.archived) {
                                MenuOption(stringResource(R.string.habits_unarchive), PrumoIcons.Unarchive) {
                                    events.onArchive(false)
                                }
                            } else {
                                MenuOption(stringResource(R.string.habits_archive), PrumoIcons.Archive) {
                                    events.onArchive(true)
                                }
                            },
                            MenuOption(stringResource(R.string.habits_delete), PrumoIcons.Delete) {
                                confirmDelete = true
                            },
                        ),
                        label = stringResource(R.string.habits_more),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = TOOLBAR_SPACE)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val detail = state.detail
            when {
                detail == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

                detail == null -> LoadingState(stringResource(R.string.habits_loading))

                else -> {
                    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = events.onRefresh) {
                        HabitContent(state, detail, events)
                    }
                    if (detail.habit.kind == HabitKind.Quit && !detail.habit.archived) {
                        HabitsToolbar(
                            HabitsActions(
                                onPrimary = onRelapse,
                                primaryLabel = R.string.habits_log_relapse,
                                primaryIcon = PrumoIcons.History,
                            ),
                            Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg),
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete && habit != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.habits_delete_title),
            body = stringResource(R.string.habits_delete_body, habit.name),
            confirmLabel = stringResource(R.string.habits_delete),
            onConfirm = {
                confirmDelete = false
                events.onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
private fun HabitContent(state: HabitDetailUiState, detail: HabitDetail, events: HabitDetailEvents) {
    val habit = detail.habit
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
        verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
    ) {
        item { Header(habit) }
        if (habit.kind == HabitKind.Build) {
            item { BuildTotals(habit) }
            state.calendar?.let { calendar -> item { Calendar(habit, calendar, events) } }
            item {
                SectionHeader(stringResource(R.string.habits_frequencies), Modifier.padding(top = Spacing.md))
            }
            itemsIndexed(detail.frequencies, key = { _, f -> "f-" + f.id }) { index, frequency ->
                GroupedListItem(frequencyRow(frequency, events), index = index, count = detail.frequencies.size)
            }
            item {
                Column(
                    Modifier.padding(vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Hint(stringResource(R.string.habits_freq_locked))
                    FilledTonalButton(onClick = { events.onChangeFrequency(null) }) {
                        Text(stringResource(R.string.habits_change_frequency))
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.habits_pauses), Modifier.padding(top = Spacing.md)) }
            if (detail.pauses.isEmpty()) item { Hint(stringResource(R.string.habits_no_pauses)) }
            itemsIndexed(detail.pauses, key = { _, p -> "p-" + p.id }) { index, pause ->
                GroupedListItem(pauseRow(pause, events), index = index, count = detail.pauses.size)
            }
            item {
                FilledTonalButton(onClick = events.onPause, modifier = Modifier.padding(vertical = Spacing.sm)) {
                    Icon(painterResource(PrumoIcons.PauseCircle), contentDescription = null)
                    Text(stringResource(R.string.habits_add_pause), Modifier.padding(start = Spacing.sm))
                }
            }
        } else {
            item { QuitTotals(state, habit) }
            item { SectionHeader(stringResource(R.string.habits_relapses), Modifier.padding(top = Spacing.md)) }
            if (detail.relapses.isEmpty()) item { Hint(stringResource(R.string.habits_no_relapses)) }
            itemsIndexed(detail.relapses, key = { _, r -> "r-" + r.id }) { index, relapse ->
                GroupedListItem(relapseRow(relapse, state.context, events), index = index, count = detail.relapses.size)
            }
        }
        item {
            Hint(
                stringResource(R.string.habits_started, Formats.date(habit.startedOn)),
                Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Composable
private fun Header(habit: Habit) {
    Row(
        Modifier.padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        HabitAvatar(habit.color, habit.icon, size = 56)
        Column(Modifier.weight(1f)) {
            Text(
                habit.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                habit.frequency?.let { frequencyText(it) }
                    ?: stringResource(
                        if (habit.kind ==
                            HabitKind.Quit
                        ) {
                            R.string.habits_list_quit
                        } else {
                            R.string.habits_list_build
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            habit.archived -> Tag(stringResource(R.string.habits_archived), small = true)
            habit.paused -> Tag(stringResource(R.string.habits_paused), small = true)
        }
    }
}

@Composable
private fun BuildTotals(habit: Habit) {
    Column(Modifier.padding(bottom = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                value = habit.ratePercent?.let { "$it%" } ?: "—",
                label = stringResource(R.string.habits_rate_label),
                modifier = Modifier.weight(1f),
            )
            KpiTile(
                value = streakText(habit.currentStreak ?: 0, habit.streakInWeeks),
                label = stringResource(R.string.habits_streak_label),
                modifier = Modifier.weight(1f),
            )
            KpiTile(
                value = streakText(habit.bestStreak ?: 0, habit.streakInWeeks),
                label = stringResource(R.string.habits_best_label),
                modifier = Modifier.weight(1f),
            )
        }
        Hint(stringResource(if (habit.streakInWeeks) R.string.habits_rate_hint_weeks else R.string.habits_rate_hint))
        habit.week?.let { week ->
            KpiTile(
                value = stringResource(R.string.habits_today_week, week.done, week.target),
                label = stringResource(R.string.habits_week_label),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Calendar(habit: Habit, calendar: CalendarMonth, events: HabitDetailEvents) {
    val ref = HabitRef(habit.id, habit.name, habit.color, habit.icon, habit.measure, habit.targetValue, habit.unit)
    val cells = calendar.days.map { day ->
        val detail = if (habit.measure == Measure.Quantity && day.value != null) amountText(ref, day.value) else null
        CalendarCell(day.on, day.state.cell, listOfNotNull(day.state.label(), detail).joinToString(SEPARATOR))
    }
    SectionCard(modifier = Modifier.fillMaxWidth(), title = stringResource(R.string.habits_calendar)) {
        PeriodSelector(
            label = monthLabel(calendar.month),
            previousLabel = stringResource(R.string.habits_previous_month),
            nextLabel = stringResource(R.string.habits_next_month),
            onPrevious = { events.onMonth(calendar.previous) },
            onNext = calendar.next?.let { next -> { events.onMonth(next) } },
        )
        MonthCalendar(cells, today = calendar.today, onSelect = if (habit.archived) null else events.onDay)
    }
}

private val monthFormat = DateTimeFormatter.ofPattern("yyyy")

/** "Novembro de 2026". */
private fun monthLabel(month: YearMonth): String {
    val locale = Locale.forLanguageTag("pt-BR")
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    return "$name de ${month.atDay(1).format(monthFormat)}"
}

@Composable
private fun frequencyRow(frequency: Frequency, events: HabitDetailEvents): GroupedRow {
    val label = when (frequency.state) {
        FrequencyState.Current -> stringResource(R.string.habits_freq_current, Formats.date(frequency.validFrom))

        FrequencyState.Future -> stringResource(R.string.habits_freq_future, Formats.date(frequency.validFrom))

        FrequencyState.Past -> stringResource(
            R.string.habits_freq_past,
            Formats.date(frequency.validFrom),
            frequency.validUntil?.let(Formats::date).orEmpty(),
        )
    }
    return GroupedRow(
        title = frequencyText(frequency),
        summary = label,
        action = RowAction.Static(
            trailing = if (frequency.state == FrequencyState.Future) {
                {
                    Row {
                        IconButton(onClick = { events.onChangeFrequency(frequency) }) {
                            Icon(
                                painterResource(PrumoIcons.Edit),
                                contentDescription = stringResource(R.string.habits_edit_version),
                            )
                        }
                        IconButton(onClick = { events.onDeleteFrequency(frequency) }) {
                            Icon(
                                painterResource(PrumoIcons.Delete),
                                contentDescription = stringResource(R.string.habits_delete_version),
                            )
                        }
                    }
                }
            } else {
                null
            },
        ),
        icon = PrumoIcons.Schedule,
        neutral = frequency.state == FrequencyState.Past,
    )
}

@Composable
private fun pauseRow(pause: Pause, events: HabitDetailEvents): GroupedRow = GroupedRow(
    title = pause.endsOn?.let {
        stringResource(R.string.habits_pause_range, Formats.date(pause.startsOn), Formats.date(it))
    } ?: stringResource(R.string.habits_pause_open, Formats.date(pause.startsOn)),
    action = RowAction.Static(
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pause.endsOn == null) {
                    TextButton(onClick = {
                        events.onEndPause(pause)
                    }) { Text(stringResource(R.string.habits_end_pause)) }
                }
                IconButton(onClick = { events.onDeletePause(pause) }) {
                    Icon(
                        painterResource(PrumoIcons.Delete),
                        contentDescription = stringResource(R.string.habits_delete_pause),
                    )
                }
            }
        },
    ),
    icon = PrumoIcons.PauseCircle,
    neutral = true,
)

@Composable
private fun QuitTotals(state: HabitDetailUiState, habit: Habit) {
    Column(Modifier.padding(bottom = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            tone = SectionTone.Primary,
            hero = true,
            icon = PrumoIcons.LocalFireDepartment,
            overline = stringResource(R.string.habits_clean_label),
        ) {
            FitText(
                daysText(habit.cleanDays ?: 0),
                style = PrumoType.dataDisplay,
            )
            habit.cleanSince?.let {
                Text(
                    stringResource(
                        R.string.habits_clean_since,
                        relativeDateTime(it, state.context.zone, state.context.today),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                value = daysText(habit.bestCleanDays ?: 0),
                label = stringResource(R.string.habits_best_clean),
                modifier = Modifier.weight(1f),
            )
            KpiTile(
                value = (habit.relapses30d ?: 0).toString(),
                label = stringResource(R.string.habits_relapses_30d_label),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun relapseRow(relapse: Relapse, context: HabitsContext, events: HabitDetailEvents): GroupedRow = GroupedRow(
    title = relativeDateTime(relapse.occurredAt, context.zone, context.today),
    summary = relapse.note,
    action = RowAction.Static(
        trailing = {
            IconButton(onClick = { events.onDeleteRelapse(relapse) }) {
                Icon(
                    painterResource(PrumoIcons.Delete),
                    contentDescription = stringResource(R.string.habits_delete_relapse),
                )
            }
        },
    ),
    icon = PrumoIcons.History,
    neutral = true,
)

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@PhonePreviews
@Composable
private fun HabitDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            HabitDetailScreen(
                state = HabitDetailUiState(
                    loading = false,
                    detail = HabitsPreview.bibleDetail,
                    calendar = HabitsPreview.calendar,
                ),
                onBack = {},
                events = HabitsPreview.detailEvents,
                onRelapse = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun QuitDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            HabitDetailScreen(
                state = HabitDetailUiState(loading = false, detail = HabitsPreview.quitDetail),
                onBack = {},
                events = HabitsPreview.detailEvents,
                onRelapse = {},
            )
        }
    }
}
