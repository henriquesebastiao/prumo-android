package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.relativeDate
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.studies.model.Area
import com.henriquesebastiao.prumo.feature.studies.model.StudiesOverview
import com.henriquesebastiao.prumo.feature.studies.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val areas: List<Area>? = null,
    val subjects: List<Subject> = emptyList(),
    val overview: StudiesOverview? = null,
    val showArchived: Boolean = false,
    /** An archive or a delete the server refused (`studies.area_in_use`). */
    val actionError: AppError? = null,
    val context: StudiesContext = StudiesContext(),
) {
    fun subjectsOf(area: Area): List<Subject> = subjects.filter { it.areaId == area.id }
}

/** Matérias: the areas and their subjects, archived ones on demand. */
@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repository: StudiesRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(SubjectsUiState(context = account.preferences.value.studiesContext(clock)))
    val state: StateFlow<SubjectsUiState> = mutableState.asStateFlow()

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
            it.copy(
                loading = it.areas == null,
                refreshing = refresh && it.areas != null,
                error = null,
                context = account.preferences.value.studiesContext(clock),
            )
        }
        loading = viewModelScope.launch {
            val filter = if (mutableState.value.showArchived) ArchivedFilter.All else ArchivedFilter.Active
            val areas = repository.areas(filter)
            val subjects = repository.subjects(filter)
            val overview = repository.overview()
            mutableState.update { state ->
                when {
                    areas is Outcome.Failed -> state.copy(loading = false, refreshing = false, error = areas.error)

                    subjects is Outcome.Failed -> state.copy(
                        loading = false,
                        refreshing = false,
                        error = subjects.error,
                    )

                    else -> state.copy(
                        loading = false,
                        refreshing = false,
                        areas = (areas as Outcome.Ok).value,
                        subjects = (subjects as Outcome.Ok).value,
                        overview = (overview as? Outcome.Ok)?.value,
                    )
                }
            }
        }
    }

    fun toggleArchived() {
        mutableState.update { it.copy(showArchived = !it.showArchived) }
        load(refresh = true)
    }

    fun setArchived(area: Area, archived: Boolean) = act { repository.setAreaArchived(area.id, archived) }

    fun delete(area: Area) = act { repository.deleteArea(area.id) }

    private fun act(block: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val outcome = block()
            if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
        }
    }
}

internal class SubjectsEvents(
    val onOpen: (Subject) -> Unit,
    val onNewArea: () -> Unit,
    val onEditArea: (Area) -> Unit,
    val onNewSubject: (Area) -> Unit,
    val onArchiveArea: (Area, Boolean) -> Unit,
    val onDeleteArea: (Area) -> Unit,
    val onToggleArchived: () -> Unit,
    val onRetry: () -> Unit,
)

/**
 * Matérias (D24): the month's overview, then each area with its subjects (topics, minutes this week
 * or the last session), its ⋮ menu and, on demand, the archived ones.
 */
@Composable
internal fun SubjectsTab(state: SubjectsUiState, events: SubjectsEvents, modifier: Modifier = Modifier) {
    val areas = state.areas
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    when {
        areas == null && state.error != null -> ErrorState(state.error, onRetry = events.onRetry, modifier = modifier)

        areas == null -> LoadingState(stringResource(R.string.studies_loading), modifier)

        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
            verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
        ) {
            item { Header(state, events) }
            state.actionError?.let { error ->
                item { Text(studiesErrorText(error), color = MaterialTheme.colorScheme.error) }
            }
            if (areas.isEmpty()) {
                item {
                    EmptyState(
                        icon = PrumoIcons.School,
                        title = stringResource(R.string.studies_subjects_empty),
                        body = stringResource(R.string.studies_subjects_empty_hint),
                        action = {
                            FilledTonalButton(onClick = events.onNewArea) {
                                Text(stringResource(R.string.studies_new_area))
                            }
                        },
                    )
                }
            }
            areas.forEach { area ->
                item(key = "area-" + area.id) { AreaHeader(area, events, onDelete = { deleting = area.id }) }
                val subjects = state.subjectsOf(area)
                if (subjects.isEmpty()) {
                    item(key = "empty-" + area.id) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.studies_area_no_subjects),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            if (!area.archived) {
                                TextButton(onClick = { events.onNewSubject(area) }) {
                                    Text(stringResource(R.string.studies_new_subject))
                                }
                            }
                        }
                    }
                }
                itemsIndexed(subjects, key = { _, subject -> subject.id }) { index, subject ->
                    GroupedListItem(
                        subjectRow(subject, state.context) { events.onOpen(subject) },
                        index = index,
                        count = subjects.size,
                    )
                }
            }
        }
    }
    val area = areas?.firstOrNull { it.id == deleting }
    if (area != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.studies_delete_area_title),
            body = stringResource(R.string.studies_delete_area_body, area.name),
            confirmLabel = stringResource(R.string.studies_delete),
            onConfirm = {
                deleting = null
                events.onDeleteArea(area)
            },
            onDismiss = { deleting = null },
            destructive = true,
        )
    }
}

/** "5 matérias em 3 áreas · 4h 20 este mês", the archived toggle and "Nova área". */
@Composable
private fun Header(state: SubjectsUiState, events: SubjectsEvents) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Without areas the empty state below says it all, with its own "Nova área".
        state.overview?.takeIf { it.areasCount > 0 }?.let { overview ->
            val subjects = if (overview.subjectsCount == 0) {
                stringResource(R.string.studies_subjects_none)
            } else {
                pluralStringResource(R.plurals.studies_subjects_count, overview.subjectsCount, overview.subjectsCount)
            }
            Text(
                stringResource(
                    R.string.studies_overview,
                    subjects,
                    pluralStringResource(
                        R.plurals.studies_areas_count,
                        overview.areasCount,
                        overview.areasCount,
                    ),
                    minutesText(overview.minutesThisMonth),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (!state.areas.isNullOrEmpty()) {
                OutlinedButton(onClick = events.onNewArea) {
                    Icon(painterResource(PrumoIcons.Add), contentDescription = null)
                    Text(stringResource(R.string.studies_new_area), Modifier.padding(start = Spacing.sm))
                }
            }
            TextButton(onClick = events.onToggleArchived) {
                Text(
                    stringResource(
                        if (state.showArchived) R.string.studies_hide_archived else R.string.studies_show_archived,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AreaHeader(area: Area, events: SubjectsEvents, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StudyAvatar(area.color, size = 32)
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                area.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            )
            if (area.archived) Tag(stringResource(R.string.studies_archived), small = true)
        }
        OverflowMenu(
            listOfNotNull(
                MenuOption(stringResource(R.string.studies_edit), PrumoIcons.Edit) { events.onEditArea(area) },
                MenuOption(stringResource(R.string.studies_new_subject), PrumoIcons.Add) {
                    events.onNewSubject(area)
                }.takeIf { !area.archived },
                if (area.archived) {
                    MenuOption(stringResource(R.string.studies_unarchive), PrumoIcons.Unarchive) {
                        events.onArchiveArea(area, false)
                    }
                } else {
                    MenuOption(stringResource(R.string.studies_archive), PrumoIcons.Archive) {
                        events.onArchiveArea(area, true)
                    }
                },
                MenuOption(stringResource(R.string.studies_delete), PrumoIcons.Delete, onDelete),
            ),
            label = stringResource(R.string.studies_area_actions, area.name),
        )
    }
}

/** A subject: its topics and the minutes of this week, or when it was last studied. */
@Composable
private fun subjectRow(subject: Subject, context: StudiesContext, onClick: () -> Unit): GroupedRow {
    val topics = subject.topicsCount.takeIf { it > 0 }?.let {
        pluralStringResource(R.plurals.studies_topics_count, it, it)
    }
    val last = subject.lastSessionAt
    val time = when {
        subject.archived -> stringResource(R.string.studies_archived)

        subject.minutesThisWeek > 0 -> stringResource(
            R.string.studies_subject_this_week,
            minutesText(subject.minutesThisWeek),
        )

        last != null -> stringResource(
            R.string.studies_subject_last,
            relativeDate(last.atZone(context.zone).toLocalDate(), context.today).lowercase(),
        )

        else -> stringResource(R.string.studies_subject_no_sessions)
    }
    return GroupedRow(
        title = subject.name,
        summary = listOfNotNull(topics, time).joinToString(SEPARATOR),
        action = RowAction.Click(onClick),
        leading = { StudyAvatar(subject.color) },
        neutral = subject.archived,
    )
}
