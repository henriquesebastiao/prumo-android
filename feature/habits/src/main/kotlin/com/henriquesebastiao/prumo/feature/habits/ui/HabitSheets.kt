package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.MultilineField
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.component.WeekdayChips
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.DateTimeField
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the small sheets of Hábitos share: the request they serve, saving and its error. */
data class SheetUiState(
    val request: Long? = null,
    val loading: Boolean = false,
    val habit: Habit? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: HabitsContext = HabitsContext(),
    // Frequency
    val frequencyId: String? = null,
    val kind: FrequencyKind = FrequencyKind.Daily,
    val timesPerWeek: Int = HabitEditorUiState.DEFAULT_TIMES,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.take(HabitEditorUiState.WORKDAYS).toSet(),
    val validFrom: LocalDate = LocalDate.of(2026, 11, 17),
    // Pause
    val startsOn: LocalDate = LocalDate.of(2026, 11, 17),
    val endsOn: LocalDate? = null,
    // Relapse
    val occurredAt: LocalDateTime = LocalDateTime.of(2026, 11, 17, 14, 30),
    // Entry
    val day: LocalDate? = null,
    val hadValue: Boolean = false,
)

/**
 * The small forms of a habit: a new frequency version (or the edit of a future one), a pause, a
 * relapse and a day's quantity. The server decides what is allowed (H2, H3, H7).
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class HabitSheetViewModel @Inject constructor(
    private val repository: HabitsRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    /** The relapse's note. */
    var note by mutableStateOf("")
        private set

    /** The day's quantity, as digits. */
    var value by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(SheetUiState())
    val state: StateFlow<SheetUiState> = mutableState.asStateFlow()

    private var habitId: String = ""

    fun start(
        request: Long,
        habitId: String,
        frequencyId: String? = null,
        day: LocalDate? = null,
        dayValue: Int? = null,
    ) {
        if (mutableState.value.request == request) return
        this.habitId = habitId
        note = ""
        value = dayValue?.toString().orEmpty()
        val context = account.preferences.value.habitsContext(clock)
        val now = LocalDateTime.ofInstant(clock.instant(), context.zone).withSecond(0).withNano(0)
        mutableState.value = SheetUiState(
            request = request,
            loading = true,
            context = context,
            frequencyId = frequencyId,
            validFrom = context.today,
            startsOn = context.today,
            occurredAt = now,
            day = day,
            hadValue = dayValue != null,
        )
        viewModelScope.launch {
            val outcome = repository.habit(habitId)
            mutableState.update { state ->
                val detail = (outcome as? Outcome.Ok)?.value ?: return@update state.copy(loading = false)
                val editing = detail.frequencies.firstOrNull { it.id == frequencyId }
                if (editing == null) {
                    state.copy(loading = false, habit = detail.habit)
                } else {
                    state.copy(
                        loading = false,
                        habit = detail.habit,
                        kind = editing.kind,
                        timesPerWeek = editing.timesPerWeek ?: state.timesPerWeek,
                        weekdays = editing.weekdays.toSet().ifEmpty { state.weekdays },
                        validFrom = editing.validFrom,
                    )
                }
            }
        }
    }

    fun onKind(kind: FrequencyKind) = mutableState.update { it.copy(kind = kind, saveError = null) }

    fun onTimes(times: Int) = mutableState.update { it.copy(timesPerWeek = times.coerceIn(1, DAYS_IN_WEEK)) }

    fun onWeekday(day: DayOfWeek) = mutableState.update {
        it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day, saveError = null)
    }

    fun onValidFrom(date: LocalDate) = mutableState.update { it.copy(validFrom = date, saveError = null) }

    fun onStartsOn(date: LocalDate) = mutableState.update { it.copy(startsOn = date, saveError = null) }

    fun onEndsOn(date: LocalDate?) = mutableState.update { it.copy(endsOn = date, saveError = null) }

    fun onOccurredAt(moment: LocalDateTime) = mutableState.update { it.copy(occurredAt = moment, saveError = null) }

    fun onNote(text: String) {
        note = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onValue(digits: String) {
        value = digits
        mutableState.update { it.copy(saveError = null) }
    }

    fun saveFrequency() {
        val state = mutableState.value
        val input = FrequencyInput(state.kind, state.timesPerWeek, state.weekdays.toList())
        val id = state.frequencyId
        send {
            if (id ==
                null
            ) {
                repository.createFrequency(habitId, state.validFrom, input)
            } else {
                repository.updateFrequency(id, input)
            }
        }
    }

    fun savePause() {
        val state = mutableState.value
        send { repository.createPause(habitId, state.startsOn, state.endsOn) }
    }

    fun saveRelapse() {
        val state = mutableState.value
        val moment = state.occurredAt.atZone(state.context.zone).toInstant()
        send { repository.logRelapse(habitId, moment, note.trim().ifEmpty { null }) }
    }

    fun saveEntry() {
        val day = mutableState.value.day ?: return
        val amount = Inputs.number(value)?.toInt() ?: 0
        send { repository.mark(habitId, day, amount) }
    }

    fun clearEntry() {
        val day = mutableState.value.day ?: return
        send { repository.unmark(habitId, day) }
    }

    private fun send(block: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = block()) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}

/** A sheet of this file over the current screen, bound to its own run of the view model. */
@Composable
private fun HabitSheetHost(
    onDismiss: () -> Unit,
    viewModel: HabitSheetViewModel,
    start: (Long) -> Unit,
    content: @Composable (SheetUiState) -> Unit,
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { start(request) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        content(if (current) state else SheetUiState(loading = true))
    }
}

@Composable
private fun SheetColumn(
    title: String,
    state: SheetUiState,
    onSave: () -> Unit,
    canSave: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        content()
        state.saveError?.takeIf { !it.isFieldError() }?.let {
            Text(habitsErrorText(it), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSave,
            enabled = canSave && !state.saving && !state.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.habits_save), style = MaterialTheme.typography.titleMedium) }
    }
}

// ---------------------------------------------------------------- frequency (H2)

@Composable
internal fun FrequencySheet(
    habitId: String,
    frequencyId: String?,
    onDismiss: () -> Unit,
    viewModel: HabitSheetViewModel = hiltViewModel(),
) {
    HabitSheetHost(onDismiss, viewModel, { viewModel.start(it, habitId, frequencyId = frequencyId) }) { state ->
        FrequencyContent(
            state,
            FrequencyEvents(
                viewModel::onKind,
                viewModel::onTimes,
                viewModel::onWeekday,
                viewModel::onValidFrom,
                viewModel::saveFrequency,
            ),
        )
    }
}

internal class FrequencyEvents(
    val onKind: (FrequencyKind) -> Unit,
    val onTimes: (Int) -> Unit,
    val onWeekday: (DayOfWeek) -> Unit,
    val onValidFrom: (LocalDate) -> Unit,
    val onSave: () -> Unit,
)

/** A new version of the frequency from a day (today or later), or the edit of a future one. */
@Composable
internal fun FrequencyContent(state: SheetUiState, events: FrequencyEvents) {
    val editing = state.frequencyId != null
    SheetColumn(
        stringResource(if (editing) R.string.habits_frequency_edit else R.string.habits_frequency_new),
        state,
        events.onSave,
        canSave = state.kind != FrequencyKind.Weekdays || state.weekdays.isNotEmpty(),
    ) {
        ChoiceGroup(options = FrequencyKind.entries, selected = state.kind, label = {
            it.label()
        }, onSelect = events.onKind)
        when (state.kind) {
            FrequencyKind.Weekly -> TimesStepper(state.timesPerWeek, events.onTimes)
            FrequencyKind.Weekdays -> WeekdayChips(state.weekdays, events.onWeekday)
            FrequencyKind.Daily -> Unit
        }
        if (!editing) {
            DateField(
                value = state.validFrom,
                onValueChange = events.onValidFrom,
                label = stringResource(R.string.habits_valid_from),
                supportingText =
                    fieldError(state.saveError, "valid_from") ?: stringResource(R.string.habits_valid_from_hint),
                today = state.context.today,
            )
        }
    }
}

// ---------------------------------------------------------------- pause

@Composable
internal fun PauseSheet(habitId: String, onDismiss: () -> Unit, viewModel: HabitSheetViewModel = hiltViewModel()) {
    HabitSheetHost(onDismiss, viewModel, { viewModel.start(it, habitId) }) { state ->
        PauseContent(state, viewModel::onStartsOn, viewModel::onEndsOn, viewModel::savePause)
    }
}

/** A pause from a day, with an end or open. */
@Composable
internal fun PauseContent(
    state: SheetUiState,
    onStartsOn: (LocalDate) -> Unit,
    onEndsOn: (LocalDate?) -> Unit,
    onSave: () -> Unit,
) {
    SheetColumn(stringResource(R.string.habits_pause_new), state, onSave, canSave = true) {
        DateField(
            value = state.startsOn,
            onValueChange = onStartsOn,
            label = stringResource(R.string.habits_starts_on),
            today = state.context.today,
        )
        DateField(
            value = state.endsOn,
            onValueChange = onEndsOn,
            label = stringResource(R.string.habits_ends_on),
            onClear = { onEndsOn(null) },
            supportingText = fieldError(state.saveError, "ends_on") ?: stringResource(R.string.habits_ends_on_hint),
            today = state.context.today,
        )
    }
}

// ---------------------------------------------------------------- relapse (H7)

@Composable
internal fun RelapseSheet(habitId: String, onDismiss: () -> Unit, viewModel: HabitSheetViewModel = hiltViewModel()) {
    HabitSheetHost(onDismiss, viewModel, { viewModel.start(it, habitId) }) { state ->
        RelapseContent(state, viewModel.note, viewModel::onOccurredAt, viewModel::onNote, viewModel::saveRelapse)
    }
}

/** A relapse: when (now by default) and, if wanted, what came before. */
@Composable
internal fun RelapseContent(
    state: SheetUiState,
    note: String,
    onOccurredAt: (LocalDateTime) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
) {
    SheetColumn(stringResource(R.string.habits_log_relapse), state, onSave, canSave = true) {
        DateTimeField(
            value = state.occurredAt,
            onValueChange = onOccurredAt,
            label = stringResource(R.string.habits_occurred_at),
            today = state.context.today,
        )
        MultilineField(
            value = note,
            onValueChange = onNote,
            label = stringResource(R.string.habits_note),
            error = fieldError(state.saveError, "note"),
        )
    }
}

// ---------------------------------------------------------------- a day's quantity (H1)

@Composable
internal fun EntrySheet(
    habitId: String,
    day: LocalDate,
    dayValue: Int?,
    onDismiss: () -> Unit,
    viewModel: HabitSheetViewModel = hiltViewModel(),
) {
    HabitSheetHost(onDismiss, viewModel, { viewModel.start(it, habitId, day = day, dayValue = dayValue) }) { state ->
        EntryContent(state, viewModel.value, viewModel::onValue, viewModel::saveEntry, viewModel::clearEntry)
    }
}

/** The quantity of one day (a past day from the calendar). */
@Composable
internal fun EntryContent(
    state: SheetUiState,
    value: String,
    onValue: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    val day = state.day
    val title = if (state.habit != null && day != null) {
        stringResource(R.string.habits_entry_title, state.habit.name, Formats.date(day))
    } else {
        stringResource(R.string.habits_value)
    }
    SheetColumn(title, state, onSave, canSave = true) {
        ValueField(
            digits = value,
            onDigitsChange = onValue,
            label = stringResource(R.string.habits_value),
            transformation = IntegerTransformation,
            suffix = state.habit?.unit,
            maxDigits = VALUE_DIGITS,
        )
        if (state.hadValue) {
            TextButton(onClick = onClear, enabled = !state.saving) { Text(stringResource(R.string.habits_clear)) }
        }
    }
}

private const val VALUE_DIGITS = 7
