package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.RowValue
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
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
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.StudySession
import com.henriquesebastiao.prumo.feature.studies.model.Subject
import com.henriquesebastiao.prumo.feature.studies.model.SubjectDetail
import com.henriquesebastiao.prumo.feature.studies.model.Topic
import com.henriquesebastiao.prumo.feature.studies.model.WeekMinutes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectDetailUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val detail: SubjectDetail? = null,
    /** An action the server refused (`studies.topic_in_use`…), shown once in a snackbar. */
    val actionError: AppError? = null,
    /** No app on the device opens `obsidian://` (D24): shown once in a snackbar. */
    val noObsidianApp: Boolean = false,
    /** The subject was deleted: the screen closes. */
    val gone: Boolean = false,
    val context: StudiesContext = StudiesContext(),
)

/** A subject (E7, E8, E10): its topics, minutes per week and latest sessions, from the server. */
@HiltViewModel(assistedFactory = SubjectDetailViewModel.Factory::class)
class SubjectDetailViewModel @AssistedInject constructor(
    @Assisted private val subjectId: String,
    private val repository: StudiesRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(SubjectDetailUiState(context = account.preferences.value.studiesContext(clock)))
    val state: StateFlow<SubjectDetailUiState> = mutableState.asStateFlow()

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
                context = account.preferences.value.studiesContext(clock),
            )
        }
        viewModelScope.launch {
            val outcome = repository.subject(subjectId)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(loading = false, refreshing = false, detail = outcome.value)
                    is Outcome.Failed -> it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    fun setArchived(archived: Boolean) = act { repository.setSubjectArchived(subjectId, archived) }

    fun delete() = act(onDone = {
        mutableState.update { it.copy(gone = true) }
    }) { repository.deleteSubject(subjectId) }

    fun deleteTopic(topic: Topic) = act { repository.deleteTopic(topic.id) }

    fun onNoObsidianApp() = mutableState.update { it.copy(noObsidianApp = true) }

    fun onNoticeShown() = mutableState.update { it.copy(actionError = null, noObsidianApp = false) }

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
        fun create(subjectId: String): SubjectDetailViewModel
    }
}

internal class SubjectDetailEvents(
    val onOpenObsidian: (String) -> Unit,
    val onEdit: () -> Unit,
    val onArchive: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onDeleteTopic: (Topic) -> Unit,
    val onSession: (StudySession) -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit,
    val onNoticeShown: () -> Unit,
)

@Composable
internal fun SubjectDetailRoute(
    subjectId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SubjectDetailViewModel = hiltViewModel<SubjectDetailViewModel, SubjectDetailViewModel.Factory>(
        key = "subject-$subjectId",
        creationCallback = { it.create(subjectId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    val context = LocalContext.current
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    SubjectDetailScreen(
        state = state,
        onBack = navigator::back,
        events = SubjectDetailEvents(
            onOpenObsidian = { uri -> if (!openObsidian(context, uri)) viewModel.onNoObsidianApp() },
            onEdit = { sheet = SheetRequest(SheetKind.Subject, id = subjectId) },
            onArchive = viewModel::setArchived,
            onDelete = viewModel::delete,
            onDeleteTopic = viewModel::deleteTopic,
            onSession = { sheet = SheetRequest(SheetKind.Session, id = it.id) },
            onRetry = viewModel::pull,
            onRefresh = viewModel::pull,
            onNoticeShown = viewModel::onNoticeShown,
        ),
        actions = StudiesActions(onLog = { sheet = SheetRequest(SheetKind.Session, parentId = subjectId) }),
        modifier = modifier,
    )
    StudiesSheet(sheet, onDismiss = { sheet = null })
}

/**
 * A subject (D24): its area and the link to its note in Obsidian, this week and the last session,
 * the minutes of the last 8 weeks, its topics and its latest sessions (tap to edit).
 */
@Composable
internal fun SubjectDetailScreen(
    state: SubjectDetailUiState,
    onBack: () -> Unit,
    events: SubjectDetailEvents,
    actions: StudiesActions,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val notice = when {
        state.noObsidianApp -> stringResource(R.string.studies_obsidian_no_app)
        state.actionError != null -> studiesErrorText(state.actionError)
        else -> null
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice)
            events.onNoticeShown()
        }
    }
    val subject = state.detail?.subject
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(title = "", onBack = onBack) {
                if (subject != null) {
                    IconButton(onClick = events.onEdit) {
                        Icon(
                            painterResource(PrumoIcons.Edit),
                            contentDescription = stringResource(R.string.studies_edit_subject),
                        )
                    }
                    OverflowMenu(
                        listOf(
                            if (subject.archived) {
                                MenuOption(stringResource(R.string.studies_unarchive), PrumoIcons.Unarchive) {
                                    events.onArchive(false)
                                }
                            } else {
                                MenuOption(stringResource(R.string.studies_archive), PrumoIcons.Archive) {
                                    events.onArchive(true)
                                }
                            },
                            MenuOption(stringResource(R.string.studies_delete), PrumoIcons.Delete) {
                                confirmDelete =
                                    true
                            },
                        ),
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

                detail == null -> LoadingState(stringResource(R.string.studies_loading))

                else -> {
                    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = events.onRefresh) {
                        SubjectContent(state, detail, events)
                    }
                    if (!detail.subject.archived) {
                        StudiesToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
                    }
                }
            }
        }
    }
    if (confirmDelete && subject != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.studies_delete_subject_title),
            body = stringResource(R.string.studies_delete_subject_body, subject.name),
            confirmLabel = stringResource(R.string.studies_delete),
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
private fun SubjectContent(state: SubjectDetailUiState, detail: SubjectDetail, events: SubjectDetailEvents) {
    val subject = detail.subject
    var deletingTopic by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
        verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap),
    ) {
        item { Header(subject, events) }
        item { Totals(state, subject) }
        item { Weekly(detail.weeklyMinutes) }
        item { SectionHeader(stringResource(R.string.studies_topics)) }
        if (detail.topics.isEmpty()) {
            item { Hint(stringResource(R.string.studies_no_topics)) }
        }
        itemsIndexed(detail.topics, key = { _, topic -> "topic-" + topic.id }) { index, topic ->
            GroupedListItem(topicRow(topic) { deletingTopic = topic.id }, index = index, count = detail.topics.size)
        }
        item { SectionHeader(stringResource(R.string.studies_recent)) }
        if (detail.recentSessions.isEmpty()) {
            item { Hint(stringResource(R.string.studies_no_recent)) }
        }
        itemsIndexed(detail.recentSessions, key = { _, session -> "session-" + session.id }) { index, session ->
            GroupedListItem(
                sessionRow(session, state.context) { events.onSession(session) },
                index = index,
                count = detail.recentSessions.size,
            )
        }
    }
    val topic = detail.topics.firstOrNull { it.id == deletingTopic }
    if (topic != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.studies_delete_topic_title),
            body = stringResource(R.string.studies_delete_topic_body, topic.name),
            confirmLabel = stringResource(R.string.studies_delete),
            onConfirm = {
                deletingTopic = null
                events.onDeleteTopic(topic)
            },
            onDismiss = { deletingTopic = null },
            destructive = true,
        )
    }
}

/** The area and the note in Obsidian (E8): a button when there is a link, a hint when there isn't. */
@Composable
private fun Header(subject: Subject, events: SubjectDetailEvents) {
    Column(Modifier.padding(vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StudyAvatar(subject.color, size = 56)
            Column(Modifier.weight(1f)) {
                Text(
                    subject.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subject.areaName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (subject.archived) Tag(stringResource(R.string.studies_archived), small = true)
        }
        val uri = subject.obsidianUri
        if (uri != null) {
            FilledTonalButton(
                onClick = { events.onOpenObsidian(uri) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(painterResource(PrumoIcons.OpenInNew), contentDescription = null)
                Text(stringResource(R.string.studies_open_obsidian), Modifier.padding(start = Spacing.sm))
            }
        } else {
            Hint(stringResource(R.string.studies_obsidian_missing))
        }
    }
}

@Composable
private fun Totals(state: SubjectDetailUiState, subject: Subject) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        KpiTile(
            value = minutesText(subject.minutesThisWeek),
            label = stringResource(R.string.studies_subject_week_label),
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            value = subject.lastSessionAt?.let { relativeDateTime(it, state.context.zone, state.context.today) }
                ?: stringResource(R.string.studies_subject_never),
            label = stringResource(R.string.studies_subject_last_label),
            modifier = Modifier.weight(1f),
        )
    }
}

/** E7: the minutes of each of the last 8 weeks, as the server counts them. */
@Composable
private fun Weekly(weeks: List<WeekMinutes>) {
    SectionCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
        icon = PrumoIcons.BarChart,
        title = stringResource(R.string.studies_weekly),
    ) {
        if (weeks.none { it.minutes > 0 }) {
            Hint(stringResource(R.string.studies_weekly_empty))
        } else {
            WeekBars(weeks)
        }
    }
}

/** One bar per week, as tall as its minutes against the tallest; read out as "semana de dd/mm: 1h". */
@Composable
private fun WeekBars(weeks: List<WeekMinutes>) {
    val most = weeks.maxOf { it.minutes }.coerceAtLeast(1)
    val description = weeks.map {
        stringResource(R.string.studies_weekly_label, Formats.shortDate(it.weekStart), minutesText(it.minutes))
    }.joinToString("; ")
    Row(
        Modifier.fillMaxWidth().height(BARS_HEIGHT).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeks.forEach { week ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                FitText(
                    if (week.minutes > 0) week.minutes.toString() else "",
                    style = PrumoType.dataSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(BAR_SHARE * week.minutes / most + MIN_BAR)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary),
                )
                // Eight dates side by side at 360 dp and a large font: they shrink further than a value.
                FitText(
                    Formats.shortDate(week.weekStart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    minFontSize = DATE_MIN,
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A topic and its sessions; one without sessions can be deleted (the server refuses the others). */
@Composable
private fun topicRow(topic: Topic, onDelete: () -> Unit): GroupedRow = GroupedRow(
    title = topic.name,
    summary = pluralStringResource(R.plurals.studies_sessions_count, topic.sessionsCount, topic.sessionsCount),
    action = RowAction.Static(
        trailing = if (topic.sessionsCount == 0) {
            {
                IconButton(onClick = onDelete) {
                    Icon(
                        painterResource(PrumoIcons.Delete),
                        contentDescription = stringResource(R.string.studies_delete_topic, topic.name),
                    )
                }
            }
        } else {
            null
        },
    ),
    icon = PrumoIcons.Notes,
    neutral = true,
)

/** "Hoje, 07:05" · the topic, with the minutes at the end. */
@Composable
private fun sessionRow(session: StudySession, context: StudiesContext, onClick: () -> Unit): GroupedRow = GroupedRow(
    title = relativeDateTime(session.startedAt, context.zone, context.today),
    summary = session.topic?.name,
    action = RowAction.Click(onClick, chevron = false, trailing = { RowValue(minutesText(session.durationMinutes)) }),
    icon = PrumoIcons.Timer,
)

private val BARS_HEIGHT = 120.dp
private val DATE_MIN = 6.sp
private const val BAR_SHARE = 0.6f
private const val MIN_BAR = 0.02f

@PhonePreviews
@Composable
private fun SubjectDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            SubjectDetailScreen(
                state = SubjectDetailUiState(loading = false, detail = StudiesPreview.detail),
                onBack = {},
                events = SubjectDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                actions = StudiesActions({}),
            )
        }
    }
}
