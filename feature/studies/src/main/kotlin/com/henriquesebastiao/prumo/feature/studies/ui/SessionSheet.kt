package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.AutocompleteField
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Suggestion
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.DateTimeField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.SessionInput
import com.henriquesebastiao.prumo.feature.studies.model.SubjectRef
import com.henriquesebastiao.prumo.feature.studies.model.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val request: Long? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    /** The active subjects (and, editing, the session's own). */
    val subjects: List<SubjectRef> = emptyList(),
    val subjectId: String? = null,
    /** The chosen subject's topics (E10), suggested as the topic is typed. */
    val topics: List<Topic> = emptyList(),
    /** The session being edited, or null for a new one. */
    val sessionId: String? = null,
    /** The minutes, from a chip; null when "Outra" is picked (typed in the field). */
    val chip: Int? = DEFAULT_MINUTES,
    /** The start, local to the user. Until it's picked, it follows "terminei agora" (D24). */
    val startedAt: LocalDateTime = LocalDateTime.of(2026, 11, 17, 14, 0),
    val startPicked: Boolean = false,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: StudiesContext = StudiesContext(),
) {
    val subject: SubjectRef? get() = subjects.firstOrNull { it.id == subjectId }
}

/** "Registrar estudo" (E9, E10): a session of a subject, with a topic picked or typed. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: StudiesRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    /** The topic's text: a topic of the subject or a new one's name. */
    var topic by mutableStateOf("")
        private set

    /** The minutes typed with "Outra", as digits. */
    var customMinutes by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = mutableState.asStateFlow()

    private var wanted: String? = null

    fun start(request: Long, subjectId: String?, sessionId: String?) {
        if (mutableState.value.request == request) return
        topic = ""
        customMinutes = ""
        wanted = subjectId
        val context = account.preferences.value.studiesContext(clock)
        mutableState.value = SessionUiState(request = request, sessionId = sessionId, context = context)
        followNow()
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val subjects = when (val outcome = repository.subjects()) {
                is Outcome.Ok -> outcome.value.map { SubjectRef(it.id, it.name, it.color) }

                is Outcome.Failed -> return@launch mutableState.update {
                    it.copy(loading = false, error = outcome.error)
                }
            }
            val sessionId = mutableState.value.sessionId
            if (sessionId != null) {
                loadSession(sessionId, subjects)
            } else {
                val chosen = listOf(wanted, repository.lastSubjectId).firstNotNullOfOrNull { id ->
                    subjects.firstOrNull { it.id == id }
                } ?: subjects.firstOrNull()
                mutableState.update { it.copy(loading = false, subjects = subjects, subjectId = chosen?.id) }
                chosen?.let { loadTopics(it.id) }
            }
        }
    }

    private suspend fun loadSession(sessionId: String, subjects: List<SubjectRef>) {
        when (val outcome = repository.session(sessionId)) {
            is Outcome.Ok -> {
                val session = outcome.value
                topic = session.topic?.name.orEmpty()
                val minutes = session.durationMinutes
                if (minutes !in QUICK_MINUTES) customMinutes = minutes.toString()
                mutableState.update { state ->
                    state.copy(
                        loading = false,
                        subjects = if (subjects.any { it.id == session.subject.id }) {
                            subjects
                        } else {
                            subjects +
                                session.subject
                        },
                        subjectId = session.subject.id,
                        chip = minutes.takeIf { it in QUICK_MINUTES },
                        startedAt = LocalDateTime.ofInstant(session.startedAt, state.context.zone),
                        startPicked = true,
                    )
                }
                loadTopics(session.subject.id)
            }

            is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
        }
    }

    private suspend fun loadTopics(subjectId: String) {
        val topics = (repository.topics(subjectId) as? Outcome.Ok)?.value.orEmpty()
        mutableState.update { if (it.subjectId == subjectId) it.copy(topics = topics) else it }
    }

    fun selectSubject(subjectId: String) {
        if (subjectId == mutableState.value.subjectId) return
        topic = ""
        mutableState.update { it.copy(subjectId = subjectId, topics = emptyList(), saveError = null) }
        viewModelScope.launch { loadTopics(subjectId) }
    }

    fun onTopicChange(text: String) {
        topic = text
        mutableState.update { it.copy(saveError = null) }
    }

    /** A chip's minutes, or null for "Outra". */
    fun selectChip(minutes: Int?) {
        mutableState.update { it.copy(chip = minutes, saveError = null) }
        followNow()
    }

    fun onCustomMinutesChange(digits: String) {
        customMinutes = digits
        mutableState.update { it.copy(saveError = null) }
        followNow()
    }

    fun onStartChange(start: LocalDateTime) =
        mutableState.update { it.copy(startedAt = start, startPicked = true, saveError = null) }

    val minutes: Int?
        get() = mutableState.value.chip ?: Inputs.number(customMinutes)?.toInt()?.takeIf { it > 0 }

    val canSave: Boolean get() = mutableState.value.subjectId != null && minutes != null

    fun save() {
        val state = mutableState.value
        val subjectId = state.subjectId ?: return
        val minutes = minutes ?: return
        followNow()
        val start = mutableState.value.startedAt.atZone(state.context.zone).toInstant()
        val chosen = state.topics.firstOrNull { it.name.equals(topic.trim(), ignoreCase = true) }
        val input = SessionInput(
            subjectId = subjectId,
            topicId = chosen?.id,
            topicName = topic.trim().ifEmpty { null },
            startedAt = start,
            durationMinutes = minutes,
        )
        val sessionId = state.sessionId
        send { if (sessionId == null) repository.createSession(input) else repository.updateSession(sessionId, input) }
    }

    fun delete() {
        val sessionId = mutableState.value.sessionId ?: return
        send { repository.deleteSession(sessionId) }
    }

    /** "Terminei agora": until a start is picked, it is now less the duration (only the field's default). */
    private fun followNow() {
        val state = mutableState.value
        if (state.startPicked) return
        val now = LocalDateTime.ofInstant(clock.instant(), state.context.zone).withSecond(0).withNano(0)
        mutableState.update { it.copy(startedAt = now.minusMinutes((minutes ?: 0).toLong())) }
    }

    private fun send(block: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = block()) {
                is Outcome.Ok -> {
                    repository.lastSubjectId = mutableState.value.subjectId
                    mutableState.update { it.copy(saving = false, done = true) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }
}

/** "Registrar estudo" over the current screen, or the edit of [sessionId]. */
@Composable
internal fun SessionSheet(
    sessionId: String?,
    subjectId: String?,
    onDismiss: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, subjectId, sessionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SessionSheetContent(
            state = if (current) state else SessionUiState(),
            fields = SessionFields(viewModel.topic, viewModel.customMinutes, viewModel.canSave),
            events = SessionEvents(
                onSubject = viewModel::selectSubject,
                onTopicChange = viewModel::onTopicChange,
                onChip = viewModel::selectChip,
                onCustomMinutesChange = viewModel::onCustomMinutesChange,
                onStartChange = viewModel::onStartChange,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
                onRetry = viewModel::load,
            ),
        )
    }
}

class SessionEvents(
    val onSubject: (String) -> Unit,
    val onTopicChange: (String) -> Unit,
    val onChip: (Int?) -> Unit,
    val onCustomMinutesChange: (String) -> Unit,
    val onStartChange: (LocalDateTime) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onRetry: () -> Unit,
)

/** The view model's text, typed synchronously (CLAUDE.md, "Campos de texto"). */
class SessionFields(val topic: String, val customMinutes: String, val canSave: Boolean)

/** The content of the session sheet, without state (previews and tests call it directly). */
@Composable
fun SessionSheetContent(
    state: SessionUiState,
    fields: SessionFields,
    events: SessionEvents,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .navigationBarsPadding()
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            stringResource(if (state.sessionId == null) R.string.studies_action_log else R.string.studies_session_edit),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        val subject = state.subject
        when {
            state.loading -> LoadingState(stringResource(R.string.studies_loading), Modifier.heightIn(max = 200.dp))
            state.error != null -> ErrorState(state.error, onRetry = events.onRetry)
            subject == null -> Text(stringResource(R.string.studies_no_subjects))
            else -> Form(state, subject, fields, events, onDelete = { confirmDelete = true })
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.studies_delete_session_title),
            body = stringResource(R.string.studies_delete_session_body),
            confirmLabel = stringResource(R.string.studies_delete_session),
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
private fun Form(
    state: SessionUiState,
    subject: SubjectRef,
    fields: SessionFields,
    events: SessionEvents,
    onDelete: () -> Unit,
) {
    if (state.subjects.size > 1) {
        SubjectChips(state.subjects, subject.id, events.onSubject)
    } else {
        Text(subject.name, style = MaterialTheme.typography.titleMedium)
    }
    TopicField(state, fields, events)
    Text(stringResource(R.string.studies_duration), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        QUICK_MINUTES.forEach { minutes ->
            FilterChip(
                selected = state.chip == minutes,
                onClick = { events.onChip(minutes) },
                label = { Text(stringResource(R.string.studies_minutes, minutes)) },
            )
        }
        FilterChip(
            selected = state.chip == null,
            onClick = { events.onChip(null) },
            label = { Text(stringResource(R.string.studies_other_duration)) },
        )
    }
    if (state.chip == null) {
        ValueField(
            digits = fields.customMinutes,
            onDigitsChange = events.onCustomMinutesChange,
            label = stringResource(R.string.studies_duration),
            transformation = IntegerTransformation,
            suffix = stringResource(R.string.studies_minutes_suffix),
            error = fieldError(state.saveError, "duration_minutes"),
            maxDigits = MINUTES_DIGITS,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        DateTimeField(
            value = state.startedAt,
            onValueChange = events.onStartChange,
            label = stringResource(R.string.studies_start),
            today = state.context.today,
            asTag = true,
        )
        if (!state.startPicked) {
            Text(
                stringResource(R.string.studies_start_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    state.saveError?.takeIf { !it.isFieldError() }?.let {
        Text(studiesErrorText(it), color = MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = events.onSave,
        enabled = fields.canSave && !state.saving,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text(stringResource(R.string.studies_save), style = MaterialTheme.typography.titleMedium) }
    if (state.sessionId != null) {
        TextButton(
            onClick = onDelete,
            enabled = !state.saving,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(painterResource(PrumoIcons.Delete), contentDescription = null)
            Text(stringResource(R.string.studies_delete_session), Modifier.padding(start = Spacing.sm))
        }
    }
}

/** E10: the subject's topics as the text is typed, and "Criar tópico" for a new name. */
@Composable
private fun TopicField(state: SessionUiState, fields: SessionFields, events: SessionEvents) {
    val typed = fields.topic.trim()
    val matches = state.topics.filter { typed.isEmpty() || it.name.contains(typed, ignoreCase = true) }
    val exists = state.topics.any { it.name.equals(typed, ignoreCase = true) }
    AutocompleteField(
        value = fields.topic,
        onValueChange = events.onTopicChange,
        label = stringResource(R.string.studies_topic),
        suggestions = matches.map {
            Suggestion(
                it.name,
                pluralStringResource(R.plurals.studies_sessions_count, it.sessionsCount, it.sessionsCount),
            )
        },
        onPick = { index -> events.onTopicChange(matches[index].name) },
        createLabel = if (typed.isNotEmpty() && !exists) stringResource(R.string.studies_topic_create, typed) else null,
        error = fieldError(state.saveError, "topic_name"),
    )
}

private const val DEFAULT_MINUTES = 30

/** The chips of the duration (as the web's). */
internal val QUICK_MINUTES = listOf(15, 25, 30, 45, 60)
private const val MINUTES_DIGITS = 3

@PhonePreviews
@Composable
private fun SessionSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            SessionSheetContent(
                state = SessionUiState(
                    loading = false,
                    subjects = listOf(StudiesPreview.fastApi, StudiesPreview.english, StudiesPreview.calculus),
                    subjectId = StudiesPreview.fastApi.id,
                    topics = StudiesPreview.detail.topics,
                ),
                fields = SessionFields("Testes", "", canSave = true),
                events = SessionEvents({}, {}, {}, {}, {}, {}, {}, {}),
            )
        }
    }
}
