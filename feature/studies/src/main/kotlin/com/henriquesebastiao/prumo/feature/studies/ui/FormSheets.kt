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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.ColorOption
import com.henriquesebastiao.prumo.core.designsystem.component.ColorPicker
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.rememberOpeningFocus
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.studies.model.Area
import com.henriquesebastiao.prumo.feature.studies.model.AreaInput
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import com.henriquesebastiao.prumo.feature.studies.model.SubjectInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The state both small forms share: loading what is edited, then saving it. */
data class FormUiState(
    val request: Long? = null,
    val editingId: String? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val color: StudyColor = StudyColor.Blue,
    /** The subject's area (and the areas to pick from). */
    val areas: List<Area> = emptyList(),
    val areaId: String? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
)

// ---------------------------------------------------------------- area

/** An area: its name and color (E2: the subjects inherit the color). */
@HiltViewModel
class AreaViewModel @Inject constructor(private val repository: StudiesRepository) : ViewModel() {
    var name by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(FormUiState())
    val state: StateFlow<FormUiState> = mutableState.asStateFlow()

    fun start(request: Long, areaId: String?) {
        if (mutableState.value.request == request) return
        name = ""
        mutableState.value = FormUiState(request = request, editingId = areaId, loading = areaId != null)
        if (areaId != null) load()
    }

    fun load() {
        val id = mutableState.value.editingId ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.areas(ArchivedFilter.All)) {
                is Outcome.Ok -> {
                    val area = outcome.value.firstOrNull { it.id == id }
                    name = area?.name.orEmpty()
                    mutableState.update { it.copy(loading = false, color = area?.color ?: it.color) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun onNameChange(text: String) {
        name = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onColorChange(color: StudyColor) = mutableState.update { it.copy(color = color) }

    fun save() {
        if (name.isBlank()) return
        val input = AreaInput(name.trim(), mutableState.value.color)
        val id = mutableState.value.editingId
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val outcome = if (id == null) repository.createArea(input) else repository.updateArea(id, input)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(saving = false, done = true)
                    is Outcome.Failed -> it.copy(saving = false, saveError = outcome.error)
                }
            }
        }
    }
}

@Composable
internal fun AreaSheet(areaId: String?, onDismiss: () -> Unit, viewModel: AreaViewModel = hiltViewModel()) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, areaId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        AreaSheetContent(
            state = if (current) state else FormUiState(loading = true),
            name = viewModel.name,
            onNameChange = viewModel::onNameChange,
            onColorChange = viewModel::onColorChange,
            onSave = viewModel::save,
            onRetry = viewModel::load,
        )
    }
}

/** "Nova área" or its edit, without state. */
@Composable
fun AreaSheetContent(
    state: FormUiState,
    name: String,
    onNameChange: (String) -> Unit,
    onColorChange: (StudyColor) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetColumn(
        stringResource(if (state.editingId == null) R.string.studies_new_area else R.string.studies_edit_area),
        modifier,
    ) {
        when {
            state.loading -> LoadingState(stringResource(R.string.studies_loading), Modifier.heightIn(max = 200.dp))

            state.error != null -> ErrorState(state.error, onRetry = onRetry)

            else -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.studies_area_name)) },
                    isError = fieldError(state.saveError, "name") != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rememberOpeningFocus(enabled = state.editingId == null)),
                )
                Text(stringResource(R.string.studies_color), style = MaterialTheme.typography.labelLarge)
                val colors = if (state.color in
                    StudyColor.Picker
                ) {
                    StudyColor.Picker
                } else {
                    StudyColor.Picker + state.color
                }
                ColorPicker(
                    options = colors.map { ColorOption(it.seed, it.label()) },
                    selected = colors.indexOf(state.color),
                    onSelect = { onColorChange(colors[it]) },
                )
                SaveFooter(state, enabled = name.isNotBlank(), onSave = onSave)
            }
        }
    }
}

// ---------------------------------------------------------------- subject

/** A subject: its area, name and the link of its note in Obsidian (E8). */
@HiltViewModel
class SubjectFormViewModel @Inject constructor(private val repository: StudiesRepository) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var uri by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(FormUiState())
    val state: StateFlow<FormUiState> = mutableState.asStateFlow()

    private var wantedArea: String? = null

    fun start(request: Long, subjectId: String?, areaId: String?) {
        if (mutableState.value.request == request) return
        name = ""
        uri = ""
        wantedArea = areaId
        mutableState.value = FormUiState(request = request, editingId = subjectId, loading = true)
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val areas = when (val outcome = repository.areas()) {
                is Outcome.Ok -> outcome.value

                is Outcome.Failed -> return@launch mutableState.update {
                    it.copy(loading = false, error = outcome.error)
                }
            }
            val id = mutableState.value.editingId
            if (id == null) {
                val area = areas.firstOrNull { it.id == wantedArea } ?: areas.firstOrNull()
                mutableState.update { it.copy(loading = false, areas = areas, areaId = area?.id) }
                return@launch
            }
            when (val outcome = repository.subject(id)) {
                is Outcome.Ok -> {
                    val subject = outcome.value.subject
                    name = subject.name
                    uri = subject.obsidianUri.orEmpty()
                    mutableState.update { it.copy(loading = false, areas = areas, areaId = subject.areaId) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun onNameChange(text: String) {
        name = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onUriChange(text: String) {
        uri = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onAreaChange(areaId: String) = mutableState.update { it.copy(areaId = areaId, saveError = null) }

    fun save() {
        val areaId = mutableState.value.areaId ?: return
        if (name.isBlank()) return
        val input = SubjectInput(areaId, name.trim(), uri.trim().ifEmpty { null })
        val id = mutableState.value.editingId
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val outcome = if (id == null) repository.createSubject(input) else repository.updateSubject(id, input)
            mutableState.update {
                when (outcome) {
                    is Outcome.Ok -> it.copy(saving = false, done = true)
                    is Outcome.Failed -> it.copy(saving = false, saveError = outcome.error)
                }
            }
        }
    }
}

@Composable
internal fun SubjectSheet(
    subjectId: String?,
    areaId: String?,
    onDismiss: () -> Unit,
    viewModel: SubjectFormViewModel = hiltViewModel(),
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, subjectId, areaId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SubjectSheetContent(
            state = if (current) state else FormUiState(loading = true),
            fields = SubjectFields(viewModel.name, viewModel.uri),
            events = SubjectFormEvents(
                onNameChange = viewModel::onNameChange,
                onUriChange = viewModel::onUriChange,
                onAreaChange = viewModel::onAreaChange,
                onSave = viewModel::save,
                onRetry = viewModel::load,
            ),
        )
    }
}

class SubjectFields(val name: String, val uri: String)

class SubjectFormEvents(
    val onNameChange: (String) -> Unit,
    val onUriChange: (String) -> Unit,
    val onAreaChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onRetry: () -> Unit,
)

/** "Nova matéria" or its edit, without state. */
@Composable
fun SubjectSheetContent(
    state: FormUiState,
    fields: SubjectFields,
    events: SubjectFormEvents,
    modifier: Modifier = Modifier,
) {
    SheetColumn(
        stringResource(if (state.editingId == null) R.string.studies_new_subject else R.string.studies_edit_subject),
        modifier,
    ) {
        when {
            state.loading -> LoadingState(stringResource(R.string.studies_loading), Modifier.heightIn(max = 200.dp))

            state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

            state.areas.isEmpty() -> Text(stringResource(R.string.studies_no_areas))

            else -> {
                Text(stringResource(R.string.studies_area), style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    state.areas.forEach { area ->
                        FilterChip(
                            selected = area.id == state.areaId,
                            onClick = { events.onAreaChange(area.id) },
                            label = { Text(area.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { StudyAvatar(area.color, size = 18) },
                        )
                    }
                }
                OutlinedTextField(
                    value = fields.name,
                    onValueChange = events.onNameChange,
                    label = { Text(stringResource(R.string.studies_subject_name)) },
                    isError = fieldError(state.saveError, "name") != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val uriError = fieldError(state.saveError, "obsidian_uri")
                OutlinedTextField(
                    value = fields.uri,
                    onValueChange = events.onUriChange,
                    label = { Text(stringResource(R.string.studies_obsidian)) },
                    placeholder = { Text("obsidian://open?vault=…") },
                    supportingText = { Text(uriError ?: stringResource(R.string.studies_obsidian_hint)) },
                    isError = uriError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SaveFooter(state, enabled = fields.name.isNotBlank() && state.areaId != null, onSave = events.onSave)
            }
        }
    }
}

// ---------------------------------------------------------------- shared

@Composable
private fun SheetColumn(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .navigationBarsPadding()
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun SaveFooter(state: FormUiState, enabled: Boolean, onSave: () -> Unit) {
    state.saveError?.takeIf { !it.isFieldError() }?.let {
        Text(studiesErrorText(it), color = MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = onSave,
        enabled = enabled && !state.saving,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text(stringResource(R.string.studies_save), style = MaterialTheme.typography.titleMedium) }
}

@PhonePreviews
@Composable
private fun AreaSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            AreaSheetContent(FormUiState(color = StudyColor.Orange), "Inglês", {}, {}, {}, {})
        }
    }
}

@PhonePreviews
@Composable
private fun SubjectSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            SubjectSheetContent(
                FormUiState(areas = StudiesPreview.areas, areaId = "a-dev"),
                SubjectFields("FastAPI", "obsidian://open?vault=Estudos&file=Dev%2FFastAPI"),
                SubjectFormEvents({}, {}, {}, {}, {}),
            )
        }
    }
}
