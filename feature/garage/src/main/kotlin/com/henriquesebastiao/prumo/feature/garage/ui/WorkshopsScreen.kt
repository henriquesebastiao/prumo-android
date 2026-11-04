package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The workshop dialog: a new one ([editing] null) or the edit of one. */
data class WorkshopDialog(val editing: Workshop? = null, val saving: Boolean = false, val error: AppError? = null)

data class WorkshopsUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val workshops: List<Workshop> = emptyList(),
    val dialog: WorkshopDialog? = null,
    /** Why a delete failed (`garage.workshop_in_use`), for a snackbar. */
    val actionError: AppError? = null,
    val context: GarageContext = GarageContext(),
)

@HiltViewModel
class WorkshopsViewModel @Inject constructor(
    private val repository: GarageRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set

    private val mutableState =
        MutableStateFlow(WorkshopsUiState(context = account.preferences.value.garageContext(clock)))
    val state: StateFlow<WorkshopsUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load() } }
    }

    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            when (val outcome = repository.workshops()) {
                is Outcome.Ok -> mutableState.update { it.copy(loading = false, workshops = outcome.value) }
                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun open(workshop: Workshop?) {
        name = workshop?.name.orEmpty()
        notes = workshop?.notes.orEmpty()
        mutableState.update { it.copy(dialog = WorkshopDialog(editing = workshop)) }
    }

    fun close() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun onNameChange(text: String) {
        name = text
        mutableState.update { it.copy(dialog = it.dialog?.copy(error = null)) }
    }

    fun onNotesChange(text: String) {
        notes = text
    }

    fun save() {
        val dialog = mutableState.value.dialog ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty() || dialog.saving) return
        val text = notes.trim().ifEmpty { null }
        mutableState.update { it.copy(dialog = dialog.copy(saving = true, error = null)) }
        viewModelScope.launch {
            val editing = dialog.editing
            val outcome = if (editing == null) {
                repository.createWorkshop(trimmed, text)
            } else {
                repository.updateWorkshop(editing.id, trimmed, text)
            }
            when (outcome) {
                is Outcome.Ok -> mutableState.update { it.copy(dialog = null) }

                is Outcome.Failed -> mutableState.update {
                    it.copy(dialog = it.dialog?.copy(saving = false, error = outcome.error))
                }
            }
        }
    }

    fun delete(workshop: Workshop) {
        mutableState.update { it.copy(dialog = null, actionError = null) }
        viewModelScope.launch {
            repository.deleteWorkshop(workshop.id).let { outcome ->
                if (outcome is Outcome.Failed) mutableState.update { it.copy(actionError = outcome.error) }
            }
        }
    }

    fun actionErrorShown() {
        mutableState.update { it.copy(actionError = null) }
    }
}

/** What the workshops screen does. */
internal class WorkshopsEvents(
    val onOpen: (Workshop?) -> Unit,
    val onClose: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onDelete: (Workshop) -> Unit,
    val onRetry: () -> Unit,
    val onActionErrorShown: () -> Unit,
)

@Composable
internal fun WorkshopsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkshopsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WorkshopsScreen(
        state = state,
        name = viewModel.name,
        notes = viewModel.notes,
        onBack = onBack,
        events = WorkshopsEvents(
            onOpen = viewModel::open,
            onClose = viewModel::close,
            onNameChange = viewModel::onNameChange,
            onNotesChange = viewModel::onNotesChange,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onRetry = viewModel::load,
            onActionErrorShown = viewModel::actionErrorShown,
        ),
        modifier = modifier,
    )
}

/** The workshops of the maintenances, as on the web: created here or when recording a maintenance. */
@Composable
internal fun WorkshopsScreen(
    state: WorkshopsUiState,
    name: String,
    notes: String,
    onBack: () -> Unit,
    events: WorkshopsEvents,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val actionError = state.actionError?.let { garageErrorText(it) }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbar.showSnackbar(actionError)
            events.onActionErrorShown()
        }
    }
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(stringResource(R.string.garage_workshops), onBack) {
                FilledTonalIconButton(onClick = { events.onOpen(null) }) {
                    Icon(
                        painterResource(PrumoIcons.Add),
                        contentDescription = stringResource(R.string.garage_workshop_new),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.garage_loading))

                state.error != null -> ErrorState(state.error, onRetry = events.onRetry)

                state.workshops.isEmpty() -> EmptyState(
                    icon = PrumoIcons.Storefront,
                    title = stringResource(R.string.garage_workshops_empty_title),
                    body = stringResource(R.string.garage_workshops_empty_body),
                )

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screen),
                ) {
                    itemsIndexed(state.workshops, key = { _, workshop -> workshop.id }) { index, workshop ->
                        GroupedListItem(
                            workshopRow(workshop, state.context) { events.onOpen(workshop) },
                            index = index,
                            count = state.workshops.size,
                            modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
                        )
                    }
                }
            }
        }
    }
    state.dialog?.let { WorkshopEditor(it, name, notes, events) }
}

@Composable
private fun workshopRow(workshop: Workshop, context: GarageContext, onClick: () -> Unit): GroupedRow {
    val count = workshop.maintenancesCount
    val last = workshop.lastMaintenanceAt?.let { Formats.date(it.atZone(context.zone).toLocalDate()) }
    return GroupedRow(
        title = workshop.name,
        summary = if (last != null && count > 0) {
            pluralStringResource(R.plurals.garage_workshop_stats, count, count, last)
        } else {
            stringResource(R.string.garage_workshop_unused)
        },
        icon = PrumoIcons.Storefront,
        action = RowAction.Click(onClick),
    )
}

@Composable
private fun WorkshopEditor(dialog: WorkshopDialog, name: String, notes: String, events: WorkshopsEvents) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val editing = dialog.editing
    AlertDialog(
        onDismissRequest = events.onClose,
        title = {
            Text(stringResource(if (editing == null) R.string.garage_workshop_new else R.string.garage_workshop_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = events.onNameChange,
                    label = { Text(stringResource(R.string.garage_workshop_name)) },
                    isError = dialog.error != null,
                    supportingText = dialog.error?.let { { Text(garageErrorText(it)) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = events.onNotesChange,
                    label = { Text(stringResource(R.string.garage_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (editing != null && editing.maintenancesCount == 0) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.garage_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = events.onSave, enabled = name.isNotBlank() && !dialog.saving) {
                Text(stringResource(R.string.garage_save))
            }
        },
        dismissButton = { TextButton(onClick = events.onClose) { Text(stringResource(R.string.garage_cancel)) } },
    )
    if (confirmDelete && editing != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.garage_workshop_delete_title),
            body = stringResource(R.string.garage_workshop_delete_body, editing.name),
            confirmLabel = stringResource(R.string.garage_delete),
            onConfirm = {
                confirmDelete = false
                events.onDelete(editing)
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@PhonePreviews
@Composable
private fun WorkshopsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            WorkshopsScreen(
                state = WorkshopsUiState(loading = false, workshops = GaragePreview.workshops),
                name = "",
                notes = "",
                onBack = {},
                events = WorkshopsEvents({}, {}, {}, {}, {}, {}, {}, {}),
            )
        }
    }
}
