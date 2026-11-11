package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
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
import com.henriquesebastiao.prumo.feature.studies.model.Plan
import com.henriquesebastiao.prumo.feature.studies.model.PlanBlock
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.Plans
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val plans: Plans? = null,
    /** The version shown: the one in force by default. */
    val selectedId: String? = null,
    val actionError: AppError? = null,
) {
    val plan: Plan?
        get() = plans?.items?.let { items ->
            items.firstOrNull { it.id == selectedId }
                ?: items.firstOrNull { it.state == PlanState.Current }
                ?: items.firstOrNull()
        }

    /** E1: only a version that starts today or later changes; one already in force is kept (as the web). */
    val editable: Boolean get() = plan?.let { plan -> plans?.let { plan.validFrom >= it.today } } ?: false
}

/** Plano: the versions of the weekly plan (E1). */
@HiltViewModel
class PlanViewModel @Inject constructor(private val repository: StudiesRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = mutableState.asStateFlow()

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
            it.copy(loading = it.plans == null, refreshing = refresh && it.plans != null, error = null)
        }
        loading = viewModelScope.launch {
            val outcome = repository.plans()
            mutableState.update { state ->
                when (outcome) {
                    is Outcome.Ok -> state.copy(
                        loading = false,
                        refreshing = false,
                        plans = outcome.value,
                        selectedId = state.selectedId?.takeIf { id -> outcome.value.items.any { it.id == id } },
                    )

                    is Outcome.Failed -> state.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    fun select(id: String) = mutableState.update { it.copy(selectedId = id, actionError = null) }

    fun delete(plan: Plan) {
        mutableState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            when (val outcome = repository.deletePlan(plan.id)) {
                is Outcome.Ok -> mutableState.update { it.copy(selectedId = null) }
                is Outcome.Failed -> mutableState.update { it.copy(actionError = outcome.error) }
            }
        }
    }
}

internal class PlanEvents(
    val onSelect: (String) -> Unit,
    val onNew: () -> Unit,
    val onEdit: (Plan) -> Unit,
    val onDelete: (Plan) -> Unit,
    val onRetry: () -> Unit,
)

/**
 * Plano (D24): the versions in chips (the one in force first picked), what to study on each day of
 * the version, and its actions: a version not yet in force is edited or deleted, a new one changes
 * the plan from a date (E1).
 */
@Composable
internal fun PlanTab(state: PlanUiState, events: PlanEvents, modifier: Modifier = Modifier) {
    val plans = state.plans
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    when {
        plans == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        plans == null -> LoadingState(stringResource(R.string.studies_loading), modifier)

        plans.items.isEmpty() -> EmptyState(
            icon = PrumoIcons.CalendarMonth,
            title = stringResource(R.string.studies_plan_empty),
            body = stringResource(R.string.studies_plan_empty_hint),
            modifier = modifier.padding(horizontal = Spacing.screen),
            action = {
                FilledTonalButton(onClick = events.onNew, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.studies_plan_new))
                }
            },
        )

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            val plan = state.plan ?: return@LazyColumn
            item { Versions(plans, plan, events.onSelect) }
            item { Actions(state, plan, events, onDelete = { confirmDelete = true }) }
            if (plan.blocks.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.studies_plan_no_blocks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DayOfWeek.entries.forEach { weekday ->
                val blocks = plan.blocks.filter { weekday in it.weekdays }
                    .sortedBy { it.startsAt ?: LocalTime.MAX }
                item(key = "day-${weekday.value}") {
                    SectionHeader(
                        weekdayLong(weekday).replaceFirstChar { it.titlecase() },
                        trailing = if (blocks.isEmpty()) stringResource(R.string.studies_day_free) else null,
                    )
                }
                blocks.forEachIndexed { index, block ->
                    item(key = "block-${weekday.value}-${block.id}") {
                        GroupedListItem(blockRow(block), index = index, count = blocks.size)
                    }
                }
            }
        }
    }
    val plan = state.plan
    if (confirmDelete && plan != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.studies_plan_delete_title),
            body = stringResource(R.string.studies_plan_delete_body, Formats.date(plan.validFrom)),
            confirmLabel = stringResource(R.string.studies_delete),
            onConfirm = {
                confirmDelete = false
                events.onDelete(plan)
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
private fun Versions(plans: Plans, plan: Plan, onSelect: (String) -> Unit) {
    if (plans.items.size < 2) {
        Text(
            versionLabel(plan),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
        return
    }
    val list = rememberLazyListState(plans.items.indexOf(plan).coerceAtLeast(0))
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        state = list,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(plans.items, key = { it.id }) { version ->
            FilterChip(
                selected = version.id == plan.id,
                onClick = { onSelect(version.id) },
                label = { Text(versionLabel(version), maxLines = 1) },
                leadingIcon = if (version.state == PlanState.Current) {
                    { Icon(painterResource(PrumoIcons.CalendarMonth), contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun Actions(state: PlanUiState, plan: Plan, events: PlanEvents, onDelete: () -> Unit) {
    Column(Modifier.padding(bottom = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (!state.editable) {
            Text(
                stringResource(R.string.studies_plan_locked_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilledTonalButton(onClick = events.onNew) {
                Icon(painterResource(PrumoIcons.Add), contentDescription = null)
                Text(stringResource(R.string.studies_plan_change), Modifier.padding(start = Spacing.sm))
            }
            if (state.editable) {
                OutlinedButton(onClick = { events.onEdit(plan) }) {
                    Icon(painterResource(PrumoIcons.Edit), contentDescription = null)
                    Text(stringResource(R.string.studies_plan_edit_version), Modifier.padding(start = Spacing.sm))
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.studies_delete)) }
            }
        }
        state.actionError?.let { Text(studiesErrorText(it), color = MaterialTheme.colorScheme.error) }
    }
}

/** "Vigente desde 02/11/2026", "Começa em 23/11/2026", "05/10/2026 a 01/11/2026". */
@Composable
@ReadOnlyComposable
internal fun versionLabel(plan: Plan): String = when (plan.state) {
    PlanState.Current -> stringResource(R.string.studies_plan_current_since, Formats.date(plan.validFrom))

    PlanState.Future -> stringResource(R.string.studies_plan_future_from, Formats.date(plan.validFrom))

    PlanState.Past -> stringResource(
        R.string.studies_plan_past_range,
        Formats.date(plan.validFrom),
        plan.validUntil?.let(Formats::date).orEmpty(),
    )
}

/** A block on its day: the subject, then "19:00 · 1h" (or the duration alone). */
@Composable
private fun blockRow(block: PlanBlock): GroupedRow = GroupedRow(
    title = block.subject.name,
    summary = listOfNotNull(block.startsAt?.let(Formats::time), minutesText(block.durationMinutes))
        .joinToString(SEPARATOR),
    action = RowAction.Static(),
    leading = { StudyAvatar(block.subject.color) },
)
