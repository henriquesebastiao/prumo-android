package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.ColorOption
import com.henriquesebastiao.prumo.core.designsystem.component.ColorPicker
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.IconOption
import com.henriquesebastiao.prumo.core.designsystem.component.IconPicker
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.component.WeekdayChips
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.HABIT_ICONS
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitEditorUiState(
    val editingId: String? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val kind: HabitKind = HabitKind.Build,
    val measure: Measure = Measure.Check,
    val color: HabitColor = HabitColor.Amber,
    val icon: String = HABIT_ICONS.first(),
    val startedOn: LocalDate = LocalDate.of(2026, 11, 17),
    val frequency: FrequencyKind = FrequencyKind.Daily,
    val timesPerWeek: Int = DEFAULT_TIMES,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.take(WORKDAYS).toSet(),
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val today: LocalDate = LocalDate.of(2026, 11, 17),
) {
    val isBuild: Boolean get() = kind == HabitKind.Build
    val isQuantity: Boolean get() = isBuild && measure == Measure.Quantity

    companion object {
        const val DEFAULT_TIMES = 3
        const val WORKDAYS = 5
    }
}

/** A new habit or its edit: the kind and the measure only when it is created. */
@HiltViewModel(assistedFactory = HabitEditorViewModel.Factory::class)
class HabitEditorViewModel @AssistedInject constructor(
    @Assisted private val habitId: String?,
    private val repository: HabitsRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var target by mutableStateOf("")
        private set
    var unit by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(
        account.preferences.value.habitsContext(clock).let { context ->
            HabitEditorUiState(
                editingId = habitId,
                loading = habitId != null,
                startedOn = context.today,
                today = context.today,
            )
        },
    )
    val state: StateFlow<HabitEditorUiState> = mutableState.asStateFlow()

    init {
        if (habitId != null) load()
    }

    fun load() {
        val id = habitId ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.habit(id)) {
                is Outcome.Ok -> {
                    val habit = outcome.value.habit
                    name = habit.name
                    target = habit.targetValue?.toString().orEmpty()
                    unit = habit.unit.orEmpty()
                    mutableState.update {
                        it.copy(
                            loading = false,
                            kind = habit.kind,
                            measure = habit.measure ?: Measure.Check,
                            color = habit.color,
                            icon = habit.icon,
                            startedOn = habit.startedOn,
                        )
                    }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun onNameChange(text: String) = edit { name = text }

    fun onTargetChange(digits: String) = edit { target = digits }

    fun onUnitChange(text: String) = edit { unit = text }

    fun onKind(kind: HabitKind) = mutableState.update { it.copy(kind = kind, saveError = null) }

    fun onMeasure(measure: Measure) = mutableState.update { it.copy(measure = measure, saveError = null) }

    fun onColor(color: HabitColor) = mutableState.update { it.copy(color = color) }

    fun onIcon(icon: String) = mutableState.update { it.copy(icon = icon) }

    fun onStartedOn(date: LocalDate) = mutableState.update { it.copy(startedOn = date, saveError = null) }

    fun onFrequency(kind: FrequencyKind) = mutableState.update { it.copy(frequency = kind, saveError = null) }

    fun onTimes(times: Int) = mutableState.update { it.copy(timesPerWeek = times.coerceIn(1, DAYS_IN_WEEK)) }

    fun onWeekday(day: DayOfWeek) = mutableState.update {
        it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day, saveError = null)
    }

    val canSave: Boolean
        get() {
            val state = mutableState.value
            val quantityOk = !state.isQuantity || ((Inputs.number(target) ?: 0L) > 0 && unit.isNotBlank())
            val daysOk = state.editingId != null || !state.isBuild || state.frequency != FrequencyKind.Weekdays ||
                state.weekdays.isNotEmpty()
            return name.isNotBlank() && quantityOk && daysOk
        }

    fun save() {
        if (!canSave) return
        val state = mutableState.value
        val input = HabitInput(
            name = name.trim(),
            kind = state.kind,
            color = state.color,
            icon = state.icon,
            startedOn = state.startedOn,
            measure = state.measure.takeIf { state.isBuild },
            targetValue = Inputs.number(target)?.toInt().takeIf { state.isQuantity },
            unit = unit.trim().takeIf { state.isQuantity },
            frequency = FrequencyInput(state.frequency, state.timesPerWeek, state.weekdays.toList()).takeIf {
                state.isBuild
            },
        )
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val id = state.editingId
            val outcome = if (id ==
                null
            ) {
                repository.createHabit(input)
            } else {
                repository.updateHabit(id, input, state.isQuantity)
            }
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(saving = false, done = true)
                    is Outcome.Failed -> it.copy(saving = false, saveError = outcome.error)
                }
            }
        }
    }

    private fun edit(change: () -> Unit) {
        change()
        mutableState.update { it.copy(saveError = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(habitId: String?): HabitEditorViewModel
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}

@Suppress("LongParameterList")
internal class HabitEditorEvents(
    val onName: (String) -> Unit,
    val onTarget: (String) -> Unit,
    val onUnit: (String) -> Unit,
    val onKind: (HabitKind) -> Unit,
    val onMeasure: (Measure) -> Unit,
    val onColor: (HabitColor) -> Unit,
    val onIcon: (String) -> Unit,
    val onStartedOn: (LocalDate) -> Unit,
    val onFrequency: (FrequencyKind) -> Unit,
    val onTimes: (Int) -> Unit,
    val onWeekday: (DayOfWeek) -> Unit,
    val onSave: () -> Unit,
    val onRetry: () -> Unit,
)

/** The view model's text, typed synchronously (CLAUDE.md, "Campos de texto"). */
internal class HabitEditorFields(val name: String, val target: String, val unit: String, val canSave: Boolean)

@Composable
internal fun HabitEditorRoute(
    habitId: String?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: HabitEditorViewModel = hiltViewModel<HabitEditorViewModel, HabitEditorViewModel.Factory>(
        key = "habit-editor-$habitId",
        creationCallback = { it.create(habitId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    HabitEditorScreen(
        state = state,
        fields = HabitEditorFields(viewModel.name, viewModel.target, viewModel.unit, viewModel.canSave),
        events = HabitEditorEvents(
            onName = viewModel::onNameChange,
            onTarget = viewModel::onTargetChange,
            onUnit = viewModel::onUnitChange,
            onKind = viewModel::onKind,
            onMeasure = viewModel::onMeasure,
            onColor = viewModel::onColor,
            onIcon = viewModel::onIcon,
            onStartedOn = viewModel::onStartedOn,
            onFrequency = viewModel::onFrequency,
            onTimes = viewModel::onTimes,
            onWeekday = viewModel::onWeekday,
            onSave = viewModel::save,
            onRetry = viewModel::load,
        ),
        onClose = navigator::back,
        modifier = modifier,
    )
}

/**
 * A habit (D24): the kind (to practice or to quit, chosen once), name, icon, color; to practice,
 * how it is measured and how often; when it started.
 */
@Composable
internal fun HabitEditorScreen(
    state: HabitEditorUiState,
    fields: HabitEditorFields,
    events: HabitEditorEvents,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                stringResource(
                    if (state.editingId ==
                        null
                    ) {
                        R.string.habits_editor_new
                    } else {
                        R.string.habits_editor_edit
                    },
                ),
                onBack = onClose,
                close = true,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.habits_loading))
                state.error != null -> ErrorState(state.error, onRetry = events.onRetry)
                else -> Form(state, fields, events)
            }
        }
    }
}

@Composable
private fun Form(state: HabitEditorUiState, fields: HabitEditorFields, events: HabitEditorEvents) {
    val creating = state.editingId == null
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (creating) {
            Label(stringResource(R.string.habits_kind))
            ChoiceGroup(
                options = HabitKind.entries,
                selected = state.kind,
                label = {
                    stringResource(
                        if (it ==
                            HabitKind.Build
                        ) {
                            R.string.habits_kind_build
                        } else {
                            R.string.habits_kind_quit
                        },
                    )
                },
                onSelect = events.onKind,
            )
            Text(
                stringResource(if (state.isBuild) R.string.habits_kind_build_hint else R.string.habits_kind_quit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = fields.name,
            onValueChange = events.onName,
            label = { Text(stringResource(R.string.habits_habit_name)) },
            isError = fieldError(state.saveError, "name") != null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Label(stringResource(R.string.habits_icon))
        val labels = stringArrayResource(R.array.habits_icon_labels)
        IconPicker(
            options = HABIT_ICONS.mapIndexed { index, name ->
                IconOption(ServerIcons.of(name), labels.getOrElse(index) { name })
            },
            selected = HABIT_ICONS.indexOf(state.icon),
            onSelect = { events.onIcon(HABIT_ICONS[it]) },
        )
        Label(stringResource(R.string.habits_color))
        val colors = if (state.color in HabitColor.Picker) HabitColor.Picker else HabitColor.Picker + state.color
        ColorPicker(
            options = colors.map { ColorOption(it.seed, it.label()) },
            selected = colors.indexOf(state.color),
            onSelect = { events.onColor(colors[it]) },
        )
        if (state.isBuild && creating) {
            Label(stringResource(R.string.habits_measure))
            ChoiceGroup(
                options = Measure.entries,
                selected = state.measure,
                label = {
                    stringResource(
                        if (it ==
                            Measure.Check
                        ) {
                            R.string.habits_measure_check
                        } else {
                            R.string.habits_measure_quantity
                        },
                    )
                },
                onSelect = events.onMeasure,
            )
        }
        if (state.isQuantity) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ValueField(
                    digits = fields.target,
                    onDigitsChange = events.onTarget,
                    label = stringResource(R.string.habits_target),
                    transformation = IntegerTransformation,
                    modifier = Modifier.weight(1f),
                    error = fieldError(state.saveError, "target_value"),
                    maxDigits = TARGET_DIGITS,
                )
                OutlinedTextField(
                    value = fields.unit,
                    onValueChange = { if (it.length <= UNIT_MAX) events.onUnit(it) },
                    label = { Text(stringResource(R.string.habits_unit)) },
                    supportingText = { Text(stringResource(R.string.habits_unit_hint)) },
                    isError = fieldError(state.saveError, "unit") != null,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.isBuild && creating) {
            Label(stringResource(R.string.habits_frequency))
            ChoiceGroup(
                options = FrequencyKind.entries,
                selected = state.frequency,
                label = { it.label() },
                onSelect = events.onFrequency,
            )
            when (state.frequency) {
                FrequencyKind.Weekly -> TimesStepper(state.timesPerWeek, events.onTimes)
                FrequencyKind.Weekdays -> WeekdayChips(state.weekdays, events.onWeekday)
                FrequencyKind.Daily -> Unit
            }
        }
        DateField(
            value = state.startedOn,
            onValueChange = events.onStartedOn,
            label = stringResource(R.string.habits_started_on),
            supportingText = fieldError(state.saveError, "started_on"),
            today = state.today,
        )
        state.saveError?.takeIf { !it.isFieldError() }?.let {
            Text(habitsErrorText(it), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = events.onSave,
            enabled = fields.canSave && !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.habits_save), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

/** "Vezes por semana": 1 to 7, with − and +. */
@Composable
internal fun TimesStepper(times: Int, onTimes: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            stringResource(R.string.habits_times_per_week),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onTimes(times - 1) }, enabled = times > 1) {
            Icon(
                painterResource(PrumoIcons.DoNotDisturbOn),
                contentDescription = stringResource(R.string.habits_fewer_times),
            )
        }
        FitText(times.toString(), style = PrumoType.dataLarge)
        IconButton(onClick = { onTimes(times + 1) }, enabled = times < MAX_TIMES) {
            Icon(painterResource(PrumoIcons.Add), contentDescription = stringResource(R.string.habits_more_times))
        }
    }
}

private const val TARGET_DIGITS = 6
private const val UNIT_MAX = 12
private const val MAX_TIMES = 7

@PhonePreviews
@Composable
private fun HabitEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            HabitEditorScreen(
                state = HabitEditorUiState(measure = Measure.Quantity, icon = "droplet", color = HabitColor.Cyan),
                fields = HabitEditorFields("Beber água", "2000", "ml", canSave = true),
                events = HabitsPreview.editorEvents,
                onClose = {},
            )
        }
    }
}
