package com.henriquesebastiao.prumo.feature.studies.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.component.WeekdayChips
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.TimeField
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.BlockInput
import com.henriquesebastiao.prumo.feature.studies.model.PlanBlock
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.SubjectRef
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A block being edited: [duration] is the typed minutes, as digits. */
data class BlockDraft(
    val key: Long,
    val subjectId: String?,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val startsAt: LocalTime? = null,
    val duration: String = "",
) {
    fun toInput(): BlockInput? {
        val minutes = Inputs.number(duration)?.toInt()?.takeIf { it > 0 } ?: return null
        val subject = subjectId ?: return null
        if (weekdays.isEmpty()) return null
        return BlockInput(subject, weekdays.sortedBy { it.value }, startsAt, minutes)
    }
}

data class PlanEditorUiState(
    /** The version edited (not yet in force), or null for a new one. */
    val editingId: String? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    val subjects: List<SubjectRef> = emptyList(),
    val today: LocalDate? = null,
    val validFrom: LocalDate? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
)

/**
 * A version of the plan (E1, E3): from a day (today or later; the next Monday by default) with its
 * blocks. A new version starts from the blocks of the one in force.
 */
@HiltViewModel(assistedFactory = PlanEditorViewModel.Factory::class)
class PlanEditorViewModel @AssistedInject constructor(
    @Assisted private val planId: String?,
    private val repository: StudiesRepository,
) : ViewModel() {
    /** The blocks, edited synchronously (their minutes are typed: CLAUDE.md, "Campos de texto"). */
    val blocks: SnapshotStateList<BlockDraft> = mutableStateListOf()

    private val mutableState = MutableStateFlow(PlanEditorUiState(editingId = planId))
    val state: StateFlow<PlanEditorUiState> = mutableState.asStateFlow()

    private var nextKey = 0L

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val plans = repository.plans()
            val subjects = repository.subjects()
            when {
                plans is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = plans.error) }

                subjects is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = subjects.error) }

                else -> {
                    val list = (plans as Outcome.Ok).value
                    val refs = (subjects as Outcome.Ok).value.map { SubjectRef(it.id, it.name, it.color) }
                    val base = list.items.firstOrNull { it.id == planId }
                        ?: list.items.firstOrNull { it.state == PlanState.Current }
                        ?: list.items.firstOrNull()
                    blocks.clear()
                    base?.blocks?.forEach { blocks += draftOf(it) }
                    if (blocks.isEmpty()) addBlock(refs.firstOrNull()?.id)
                    mutableState.update {
                        it.copy(
                            loading = false,
                            subjects = (
                                refs + base?.blocks.orEmpty().map(
                                    PlanBlock::subject,
                                )
                                ).distinctBy(SubjectRef::id),
                            today = list.today,
                            validFrom = if (planId !=
                                null
                            ) {
                                base?.validFrom
                            } else {
                                list.today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                            },
                        )
                    }
                }
            }
        }
    }

    private fun draftOf(block: PlanBlock) = BlockDraft(
        key = nextKey++,
        subjectId = block.subject.id,
        weekdays = block.weekdays.toSet(),
        startsAt = block.startsAt,
        duration = block.durationMinutes.toString(),
    )

    fun addBlock(subjectId: String? = mutableState.value.subjects.firstOrNull()?.id) {
        blocks += BlockDraft(key = nextKey++, subjectId = subjectId)
        mutableState.update { it.copy(saveError = null) }
    }

    fun removeBlock(key: Long) {
        blocks.removeAll { it.key == key }
        mutableState.update { it.copy(saveError = null) }
    }

    fun changeBlock(key: Long, change: (BlockDraft) -> BlockDraft) {
        val index = blocks.indexOfFirst { it.key == key }
        if (index < 0) return
        blocks[index] = change(blocks[index])
        mutableState.update { it.copy(saveError = null) }
    }

    fun onValidFromChange(date: LocalDate) = mutableState.update { it.copy(validFrom = date, saveError = null) }

    val canSave: Boolean
        get() = blocks.isNotEmpty() && blocks.all { it.toInput() != null } && mutableState.value.validFrom != null

    fun save() {
        if (!canSave) return
        val inputs = blocks.mapNotNull { it.toInput() }
        val validFrom = mutableState.value.validFrom ?: return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val outcome = if (planId ==
                null
            ) {
                repository.createPlan(validFrom, inputs)
            } else {
                repository.updatePlan(planId, inputs)
            }
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(saving = false, done = true)
                    is Outcome.Failed -> it.copy(saving = false, saveError = outcome.error)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(planId: String?): PlanEditorViewModel
    }
}

internal class PlanEditorEvents(
    val onValidFromChange: (LocalDate) -> Unit,
    val onAddBlock: () -> Unit,
    val onRemoveBlock: (Long) -> Unit,
    val onChangeBlock: (Long, (BlockDraft) -> BlockDraft) -> Unit,
    val onSave: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal fun PlanEditorRoute(
    planId: String?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: PlanEditorViewModel = hiltViewModel<PlanEditorViewModel, PlanEditorViewModel.Factory>(
        key = "plan-editor-$planId",
        creationCallback = { it.create(planId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    PlanEditorScreen(
        state = state,
        blocks = viewModel.blocks,
        canSave = viewModel.canSave,
        events = PlanEditorEvents(
            onValidFromChange = viewModel::onValidFromChange,
            onAddBlock = { viewModel.addBlock() },
            onRemoveBlock = viewModel::removeBlock,
            onChangeBlock = viewModel::changeBlock,
            onSave = viewModel::save,
            onRetry = viewModel::load,
        ),
        onClose = navigator::back,
        modifier = modifier,
    )
}

/**
 * A version of the plan (D24): the day it starts (a new version only) and its blocks, each with a
 * subject, the days of the week, an optional time and the minutes.
 */
@Composable
internal fun PlanEditorScreen(
    state: PlanEditorUiState,
    blocks: List<BlockDraft>,
    canSave: Boolean,
    events: PlanEditorEvents,
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
                        R.string.studies_plan_editor_new
                    } else {
                        R.string.studies_plan_editor_edit
                    },
                ),
                onBack = onClose,
                close = true,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.studies_loading))

                state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

                state.subjects.isEmpty() -> Text(
                    stringResource(R.string.studies_plan_no_subjects),
                    Modifier.padding(Spacing.screen),
                )

                else -> Form(state, blocks, canSave, events)
            }
        }
    }
}

@Composable
private fun Form(state: PlanEditorUiState, blocks: List<BlockDraft>, canSave: Boolean, events: PlanEditorEvents) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.editingId == null) {
            DateField(
                value = state.validFrom,
                onValueChange = events.onValidFromChange,
                label = stringResource(R.string.studies_plan_valid_from),
                supportingText = stringResource(R.string.studies_plan_valid_from_hint),
                today = state.today,
            )
            fieldError(state.saveError, "valid_from")?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.studies_plan_blocks), style = MaterialTheme.typography.titleMedium)
        blocks.forEachIndexed { index, block ->
            BlockCard(index, block, state, events)
        }
        if (blocks.isEmpty()) {
            Text(
                stringResource(R.string.studies_plan_no_blocks_form),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = events.onAddBlock, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(painterResource(PrumoIcons.Add), contentDescription = null)
            Text(stringResource(R.string.studies_plan_add_block), Modifier.padding(start = Spacing.sm))
        }
        state.saveError?.takeIf { !it.isFieldError() }?.let {
            Text(studiesErrorText(it), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = events.onSave,
            enabled = canSave && !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.studies_plan_save), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun BlockCard(index: Int, block: BlockDraft, state: PlanEditorUiState, events: PlanEditorEvents) {
    val change: ((BlockDraft) -> BlockDraft) -> Unit = { events.onChangeBlock(block.key, it) }
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.studies_plan_block, index + 1),
        trailing = {
            IconButton(onClick = { events.onRemoveBlock(block.key) }) {
                Icon(
                    painterResource(PrumoIcons.Delete),
                    contentDescription = stringResource(R.string.studies_plan_remove_block, index + 1),
                )
            }
        },
    ) {
        SubjectChips(state.subjects, block.subjectId, onSelect = { id -> change { it.copy(subjectId = id) } })
        Text(stringResource(R.string.studies_plan_weekdays), style = MaterialTheme.typography.labelLarge)
        WeekdayChips(
            selected = block.weekdays,
            onToggle = { day ->
                change {
                    it.copy(
                        weekdays = if (day in
                            it.weekdays
                        ) {
                            it.weekdays - day
                        } else {
                            it.weekdays + day
                        },
                    )
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
            TimeField(
                value = block.startsAt,
                onValueChange = { time -> change { it.copy(startsAt = time) } },
                onClear = { change { it.copy(startsAt = null) } },
                label = stringResource(R.string.studies_plan_starts_at),
                modifier = Modifier.weight(1f),
            )
            ValueField(
                digits = block.duration,
                onDigitsChange = { digits -> change { it.copy(duration = digits) } },
                label = stringResource(R.string.studies_plan_duration),
                transformation = IntegerTransformation,
                suffix = stringResource(R.string.studies_minutes_suffix),
                modifier = Modifier.weight(1f),
                error = fieldError(state.saveError, "blocks.$index.duration_minutes"),
                maxDigits = DURATION_DIGITS,
            )
        }
    }
}

private const val DURATION_DIGITS = 3

@PhonePreviews
@Composable
private fun PlanEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            PlanEditorScreen(
                state = PlanEditorUiState(
                    loading = false,
                    subjects = listOf(StudiesPreview.english, StudiesPreview.calculus, StudiesPreview.fastApi),
                    today = LocalDate.of(2026, 11, 17),
                    validFrom = LocalDate.of(2026, 11, 23),
                ),
                blocks = listOf(
                    BlockDraft(
                        1,
                        StudiesPreview.english.id,
                        DayOfWeek.entries.take(5).toSet(),
                        LocalTime.of(7, 0),
                        "30",
                    ),
                    BlockDraft(2, StudiesPreview.calculus.id, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), null, "60"),
                ),
                canSave = true,
                events = PlanEditorEvents({}, {}, {}, { _, _ -> }, {}, {}),
                onClose = {},
            )
        }
    }
}
