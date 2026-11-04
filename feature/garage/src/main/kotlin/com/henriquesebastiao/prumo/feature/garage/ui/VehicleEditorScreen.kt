package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.VehicleInput
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
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

data class VehicleEditorUiState(
    /** Loading the vehicle to edit; a new one starts ready. */
    val loading: Boolean = false,
    val error: AppError? = null,
    /** The vehicle being edited; null for a new one. */
    val editing: Vehicle? = null,
    val kind: VehicleKind = VehicleKind.Car,
    val fuelType: FuelType? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    /** Saved or archived: the form closes. */
    val done: Boolean = false,
    val context: GarageContext = GarageContext(),
)

/** The texts typed in the vehicle form. */
data class VehicleFields(
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val km: String = "",
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val notes: String = "",
)

@HiltViewModel(assistedFactory = VehicleEditorViewModel.Factory::class)
class VehicleEditorViewModel @AssistedInject constructor(
    @Assisted private val vehicleId: String?,
    private val repository: GarageRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var fields by mutableStateOf(VehicleFields())
        private set

    private val mutableState = MutableStateFlow(
        VehicleEditorUiState(loading = vehicleId != null, context = account.preferences.value.garageContext(clock)),
    )
    val state: StateFlow<VehicleEditorUiState> = mutableState.asStateFlow()

    init {
        if (vehicleId != null) load()
    }

    fun load() {
        val id = vehicleId ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.vehicle(id)) {
                is Outcome.Ok -> {
                    val vehicle = outcome.value
                    fields = VehicleFields(
                        name = vehicle.name,
                        brand = vehicle.brand,
                        model = vehicle.model,
                        year = vehicle.year.toString(),
                        km = Inputs.digitsOf(vehicle.currentKm),
                        intervalKm = Inputs.digitsOf(vehicle.oilIntervalKm),
                        intervalMonths = vehicle.oilIntervalMonths?.toString().orEmpty(),
                        notes = vehicle.notes.orEmpty(),
                    )
                    mutableState.update {
                        it.copy(loading = false, editing = vehicle, kind = vehicle.kind, fuelType = vehicle.fuelType)
                    }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    /** One field typed: the others stay. */
    fun onFieldsChange(new: VehicleFields) {
        fields = new
        mutableState.update { it.copy(saveError = null) }
    }

    fun onKindChange(kind: VehicleKind) {
        mutableState.update { it.copy(kind = kind) }
    }

    fun onFuelChange(fuel: FuelType?) {
        mutableState.update { it.copy(fuelType = fuel) }
    }

    /** What the form sends, or null while a required field is missing (the ranges are the server's). */
    private fun input(): VehicleInput? {
        val current = fields
        val year = current.year.takeIf { it.length == YEAR_DIGITS }?.toIntOrNull() ?: return null
        val km = Inputs.number(current.km) ?: return null
        if (listOf(current.name, current.brand, current.model).any { it.isBlank() }) return null
        return VehicleInput(
            kind = mutableState.value.kind,
            name = current.name.trim(),
            brand = current.brand.trim(),
            model = current.model.trim(),
            year = year,
            fuelType = mutableState.value.fuelType,
            currentKm = km,
            oilIntervalKm = Inputs.number(current.intervalKm)?.takeIf { it > 0 },
            oilIntervalMonths = Inputs.number(current.intervalMonths)?.toInt()?.takeIf { it > 0 },
            notes = current.notes.trim().ifEmpty { null },
        )
    }

    val canSave: Boolean get() = input() != null

    fun save() {
        val input = input() ?: return
        submit {
            if (vehicleId ==
                null
            ) {
                repository.createVehicle(input)
            } else {
                repository.updateVehicle(vehicleId, input)
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val id = vehicleId ?: return
        submit { repository.setArchived(id, archived) }
    }

    private fun submit(call: suspend () -> Outcome<*>) {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = call()) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String?): VehicleEditorViewModel
    }
}

@Composable
internal fun VehicleEditorRoute(
    vehicleId: String?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: VehicleEditorViewModel = hiltViewModel<VehicleEditorViewModel, VehicleEditorViewModel.Factory>(
        creationCallback = { it.create(vehicleId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    VehicleEditorScreen(
        state = state,
        fields = viewModel.fields,
        canSave = viewModel.canSave,
        onFieldsChange = viewModel::onFieldsChange,
        onKindChange = viewModel::onKindChange,
        onFuelChange = viewModel::onFuelChange,
        onSave = viewModel::save,
        onArchive = viewModel::setArchived,
        onRetry = viewModel::load,
        onClose = navigator::back,
        modifier = modifier,
    )
}

/** A new vehicle or its edit (board 04), with the usual fuel and notes of the web. */
@Composable
internal fun VehicleEditorScreen(
    state: VehicleEditorUiState,
    fields: VehicleFields,
    canSave: Boolean,
    onFieldsChange: (VehicleFields) -> Unit,
    onKindChange: (VehicleKind) -> Unit,
    onFuelChange: (FuelType?) -> Unit,
    onSave: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state.editing
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                title = if (editing == null) {
                    stringResource(R.string.garage_vehicle_new)
                } else {
                    stringResource(R.string.garage_vehicle_edit, editing.name)
                },
                onBack = onClose,
                close = true,
                actions = {
                    if (editing != null) {
                        OverflowMenu(
                            listOf(
                                if (editing.archived) {
                                    MenuOption(stringResource(R.string.garage_unarchive), PrumoIcons.Unarchive) {
                                        onArchive(false)
                                    }
                                } else {
                                    MenuOption(stringResource(R.string.garage_archive), PrumoIcons.Archive) {
                                        onArchive(true)
                                    }
                                },
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.garage_loading))
                state.error != null -> ErrorState(state.error, onRetry = onRetry)
                else -> VehicleForm(state, fields, canSave, onFieldsChange, onKindChange, onFuelChange, onSave)
            }
        }
    }
}

@Composable
private fun VehicleForm(
    state: VehicleEditorUiState,
    fields: VehicleFields,
    canSave: Boolean,
    onFieldsChange: (VehicleFields) -> Unit,
    onKindChange: (VehicleKind) -> Unit,
    onFuelChange: (FuelType?) -> Unit,
    onSave: () -> Unit,
) {
    val error = state.saveError
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ChoiceGroup(
            options = VehicleKind.entries,
            selected = state.kind,
            label = { it.label() },
            onSelect = onKindChange,
            icon = { it.icon() },
        )
        TextField(
            fields.name,
            { onFieldsChange(fields.copy(name = it)) },
            stringResource(R.string.garage_vehicle_name),
            error = fieldError(error, "name"),
            supporting = stringResource(R.string.garage_vehicle_name_hint),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TextField(
                fields.brand,
                { onFieldsChange(fields.copy(brand = it)) },
                stringResource(R.string.garage_vehicle_brand),
                Modifier.weight(1f),
                error = fieldError(error, "brand"),
            )
            TextField(
                fields.model,
                { onFieldsChange(fields.copy(model = it)) },
                stringResource(R.string.garage_vehicle_model),
                Modifier.weight(1f),
                error = fieldError(error, "model"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ValueField(
                digits = fields.year,
                onDigitsChange = { onFieldsChange(fields.copy(year = it)) },
                label = stringResource(R.string.garage_vehicle_year),
                transformation = VisualTransformation.None,
                maxDigits = YEAR_DIGITS,
                error = fieldError(error, "year"),
                modifier = Modifier.weight(1f),
            )
            ValueField(
                digits = fields.km,
                onDigitsChange = { onFieldsChange(fields.copy(km = it)) },
                label = stringResource(R.string.garage_current_km_label),
                transformation = IntegerTransformation,
                suffix = stringResource(R.string.garage_km_unit),
                error = kmError(error, state.context, path = "current_km"),
                modifier = Modifier.weight(1f),
            )
        }
        Text(stringResource(R.string.garage_vehicle_fuel), style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterChip(
                selected = state.fuelType == null,
                onClick = { onFuelChange(null) },
                label = { Text(stringResource(R.string.garage_vehicle_no_fuel)) },
            )
            FuelType.entries.forEach { fuel ->
                FilterChip(
                    selected = fuel == state.fuelType,
                    onClick = { onFuelChange(fuel) },
                    label = { Text(fuel.shortLabel()) },
                )
            }
        }
        SectionHeader(stringResource(R.string.garage_oil_change))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ValueField(
                digits = fields.intervalKm,
                onDigitsChange = { onFieldsChange(fields.copy(intervalKm = it)) },
                label = stringResource(R.string.garage_vehicle_every),
                transformation = IntegerTransformation,
                suffix = stringResource(R.string.garage_km_unit),
                maxDigits = INTERVAL_KM_DIGITS,
                error = fieldError(error, "oil_interval_km"),
                modifier = Modifier.weight(1f),
            )
            ValueField(
                digits = fields.intervalMonths,
                onDigitsChange = { onFieldsChange(fields.copy(intervalMonths = it)) },
                label = stringResource(R.string.garage_vehicle_or_every),
                transformation = VisualTransformation.None,
                suffix = stringResource(R.string.garage_months_unit),
                maxDigits = MONTHS_DIGITS,
                error = fieldError(error, "oil_interval_months"),
                modifier = Modifier.weight(1f),
            )
        }
        GroupNote(stringResource(R.string.garage_vehicle_oil_note))
        OutlinedTextField(
            value = fields.notes,
            onValueChange = { onFieldsChange(fields.copy(notes = it)) },
            label = { Text(stringResource(R.string.garage_notes)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null && !error.isFieldError()) {
            Text(garageErrorText(error), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onSave, enabled = canSave && !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (state.editing ==
                        null
                    ) {
                        R.string.garage_vehicle_create
                    } else {
                        R.string.garage_vehicle_save
                    },
                ),
            )
        }
    }
}

@Composable
private fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

private const val YEAR_DIGITS = 4
private const val INTERVAL_KM_DIGITS = 7
private const val MONTHS_DIGITS = 3

@PhonePreviews
@Composable
private fun VehicleEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            VehicleEditorScreen(
                state = VehicleEditorUiState(kind = VehicleKind.Motorcycle, fuelType = FuelType.Gasoline),
                fields = VehicleFields("CG", "Honda", "CG 160 Fan", "2023", "18420", "1000", "3"),
                canSave = true,
                onFieldsChange = {},
                onKindChange = {},
                onFuelChange = {},
                onSave = {},
                onArchive = {},
                onRetry = {},
                onClose = {},
            )
        }
    }
}
