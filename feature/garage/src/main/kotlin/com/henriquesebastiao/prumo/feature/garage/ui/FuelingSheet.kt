package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.DecimalField
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.component.rememberOpeningFocus
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.DateTimeField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.MoneyTransformation
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.FuelingInput
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FuelingUiState(
    val request: Long? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: String? = null,
    /** The fueling being edited; null for a new one. */
    val editing: Fueling? = null,
    val fuelType: FuelType = FuelType.Gasoline,
    val fullTank: Boolean = true,
    val fueledAt: LocalDateTime = LocalDateTime.of(2026, 11, 17, 14, 30),
    val saving: Boolean = false,
    val saveError: AppError? = null,
    /** Saved or deleted: the sheet closes. */
    val done: Boolean = false,
    val context: GarageContext = GarageContext(),
) {
    val vehicle: Vehicle? get() = vehicles.firstOrNull { it.id == vehicleId }
}

/** The texts typed in the fueling sheet. */
data class FuelingFields(val total: String = "", val litres: String = "", val km: String = "", val notes: String = "")

@HiltViewModel
class FuelingViewModel @Inject constructor(
    private val repository: GarageRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    var total by mutableStateOf("")
        private set
    var litres by mutableStateOf("")
        private set
    var km by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set

    private var kmTouched = false
    private var fuelTouched = false
    private var wantedVehicle: String? = null
    private var fuelingId: String? = null

    private val mutableState = MutableStateFlow(FuelingUiState())
    val state: StateFlow<FuelingUiState> = mutableState.asStateFlow()

    /** A new opening of the sheet: a new fueling (for [vehicleId], or to choose) or [fuelingId]'s edit. */
    fun start(request: Long, vehicleId: String?, fuelingId: String?) {
        if (mutableState.value.request == request) return
        total = ""
        litres = ""
        km = ""
        notes = ""
        kmTouched = false
        fuelTouched = false
        wantedVehicle = vehicleId
        this.fuelingId = fuelingId
        val context = account.preferences.value.garageContext(clock)
        val now = LocalDateTime.ofInstant(clock.instant(), context.zone).truncatedTo(ChronoUnit.MINUTES)
        mutableState.value = FuelingUiState(request = request, fueledAt = now, context = context)
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val vehicles = repository.vehicles()
            val editing = fuelingId?.let { id -> wantedVehicle?.let { repository.fueling(it, id) } }
            val error = (vehicles as? Outcome.Failed)?.error ?: (editing as? Outcome.Failed)?.error
            if (error != null) {
                mutableState.update { it.copy(loading = false, error = error) }
                return@launch
            }
            val list = vehicles.getOrNull().orEmpty()
            val fueling = editing?.getOrNull()
            if (fueling != null) fill(fueling, list) else choose(list)
        }
    }

    private fun fill(fueling: Fueling, vehicles: List<Vehicle>) {
        total = Inputs.digitsOf(fueling.totalCents)
        litres = Inputs.decimalOf(fueling.volumeMl, LITRE_SCALE)
        km = Inputs.digitsOf(fueling.km)
        notes = fueling.notes.orEmpty()
        mutableState.update {
            it.copy(
                loading = false,
                vehicles = vehicles,
                vehicleId = fueling.vehicle.id,
                editing = fueling,
                fuelType = fueling.fuelType,
                fullTank = fueling.fullTank,
                fueledAt = LocalDateTime.ofInstant(fueling.fueledAt, it.context.zone),
            )
        }
    }

    private fun choose(vehicles: List<Vehicle>) {
        val vehicle = chooseVehicle(vehicles, wantedVehicle, repository.lastVehicleId)
        mutableState.update { it.copy(loading = false, vehicles = vehicles) }
        vehicle?.let { select(it.id) }
    }

    /** The vehicle, its usual fuel and its km, unless they were changed by hand (C9). */
    fun select(vehicleId: String) {
        val vehicle = mutableState.value.vehicles.firstOrNull { it.id == vehicleId } ?: return
        if (!kmTouched) km = Inputs.digitsOf(vehicle.currentKm)
        mutableState.update {
            it.copy(
                vehicleId = vehicleId,
                fuelType = if (fuelTouched) it.fuelType else vehicle.fuelType ?: FuelType.Gasoline,
                saveError = null,
            )
        }
    }

    fun onTotalChange(text: String) {
        total = text
        clearError()
    }

    fun onLitresChange(text: String) {
        litres = text
        clearError()
    }

    fun onKmChange(text: String) {
        km = text
        kmTouched = true
        clearError()
    }

    fun onNotesChange(text: String) {
        notes = text
    }

    fun onFuelChange(fuel: FuelType) {
        fuelTouched = true
        mutableState.update { it.copy(fuelType = fuel, saveError = null) }
    }

    fun onFullTankChange(full: Boolean) {
        mutableState.update { it.copy(fullTank = full) }
    }

    fun onDateTimeChange(value: LocalDateTime) {
        mutableState.update { it.copy(fueledAt = value, saveError = null) }
    }

    private fun clearError() {
        mutableState.update { it.copy(saveError = null) }
    }

    /** Whether the three numbers the server requires were typed. */
    val canSave: Boolean get() = input(mutableState.value) != null

    private fun input(current: FuelingUiState): FuelingInput? {
        val totalCents = Inputs.number(total)?.takeIf { it > 0 } ?: return null
        val volumeMl = Inputs.scaled(litres, LITRE_SCALE)?.takeIf { it > 0 } ?: return null
        val odometer = Inputs.number(km) ?: return null
        return FuelingInput(
            fueledAt = current.fueledAt.atZone(current.context.zone).toInstant(),
            km = odometer,
            fuelType = current.fuelType,
            volumeMl = volumeMl,
            totalCents = totalCents,
            fullTank = current.fullTank,
            notes = notes.trim().ifEmpty { null },
        )
    }

    fun save() {
        val current = mutableState.value
        val vehicleId = current.vehicleId ?: return
        val input = input(current) ?: return
        if (current.saving) return
        val editing = current.editing
        submit {
            if (editing == null) {
                repository.createFueling(vehicleId, input)
            } else {
                repository.updateFueling(vehicleId, editing.id, input)
            }
        }
    }

    fun delete() {
        val editing = mutableState.value.editing ?: return
        submit { repository.deleteFueling(editing.vehicle.id, editing.id) }
    }

    private fun submit(call: suspend () -> Outcome<*>) {
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = call()) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    companion object {
        /** Litres are typed with up to three decimals: millilitres. */
        const val LITRE_SCALE = 3
    }
}

/** What the fueling sheet does when a value changes. */
class FuelingCallbacks(
    val onSelect: (String) -> Unit,
    val onTotalChange: (String) -> Unit,
    val onLitresChange: (String) -> Unit,
    val onKmChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onFuelChange: (FuelType) -> Unit,
    val onFullTankChange: (Boolean) -> Unit,
    val onDateTimeChange: (LocalDateTime) -> Unit,
)

/**
 * "Registrar abastecimento" (boards 08 and 09) over the current screen, or a fueling's detail and
 * edit ([fuelingId] of [vehicleId]).
 */
@Composable
internal fun FuelingSheet(
    vehicleId: String?,
    fuelingId: String?,
    onDismiss: () -> Unit,
    viewModel: FuelingViewModel = hiltViewModel(),
) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, vehicleId, fuelingId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.done) { if (current && state.done) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        FuelingSheetContent(
            state = if (current) state else FuelingUiState(),
            fields = FuelingFields(viewModel.total, viewModel.litres, viewModel.km, viewModel.notes),
            canSave = viewModel.canSave,
            callbacks = FuelingCallbacks(
                onSelect = viewModel::select,
                onTotalChange = viewModel::onTotalChange,
                onLitresChange = viewModel::onLitresChange,
                onKmChange = viewModel::onKmChange,
                onNotesChange = viewModel::onNotesChange,
                onFuelChange = viewModel::onFuelChange,
                onFullTankChange = viewModel::onFullTankChange,
                onDateTimeChange = viewModel::onDateTimeChange,
            ),
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onRetry = viewModel::load,
        )
    }
}

/** The content of the fueling sheet, without state. */
@Composable
fun FuelingSheetContent(
    state: FuelingUiState,
    fields: FuelingFields,
    canSave: Boolean,
    callbacks: FuelingCallbacks,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    if (state.editing ==
                        null
                    ) {
                        R.string.garage_fueling_title
                    } else {
                        R.string.garage_fueling_edit
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (state.editing != null) {
                IconButton(onClick = { confirmDelete = true }, enabled = !state.saving) {
                    Icon(
                        painterResource(PrumoIcons.Delete),
                        contentDescription = stringResource(R.string.garage_delete),
                    )
                }
            }
        }
        when {
            state.loading -> LoadingState(stringResource(R.string.garage_loading))
            state.error != null -> ErrorState(state.error, onRetry = onRetry)
            state.vehicle == null -> Text(stringResource(R.string.garage_no_vehicles_for_record))
            else -> FuelingForm(state, fields, canSave, callbacks, onSave)
        }
    }
    val editing = state.editing
    if (confirmDelete && editing != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.garage_fueling_delete_title),
            body = stringResource(
                R.string.garage_fueling_delete_body,
                whenText(editing.fueledAt, state.context),
                Formats.money(editing.totalCents, state.context.currency),
            ),
            confirmLabel = stringResource(R.string.garage_delete),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
private fun FuelingForm(
    state: FuelingUiState,
    fields: FuelingFields,
    canSave: Boolean,
    callbacks: FuelingCallbacks,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        FuelingFormFields(state, fields, canSave, callbacks, onSave)
    }
}

@Composable
private fun ColumnScope.FuelingFormFields(
    state: FuelingUiState,
    fields: FuelingFields,
    canSave: Boolean,
    callbacks: FuelingCallbacks,
    onSave: () -> Unit,
) {
    val currency = state.context.currency
    if (state.editing == null) VehicleChoice(state.vehicles, state.vehicleId, callbacks.onSelect)
    // Board 08: total and litres side by side as "Campo de valor"; a new fueling opens on the total,
    // with the keyboard up.
    val totalFocus = rememberOpeningFocus(enabled = state.editing == null)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ValueField(
            digits = fields.total,
            onDigitsChange = callbacks.onTotalChange,
            label = stringResource(R.string.garage_fueling_total),
            transformation = MoneyTransformation(currency),
            large = true,
            focusRequester = totalFocus,
            modifier = Modifier.weight(1.2f),
        )
        DecimalField(
            text = fields.litres,
            onTextChange = callbacks.onLitresChange,
            label = stringResource(R.string.garage_fueling_litres),
            scale = FuelingViewModel.LITRE_SCALE,
            suffix = stringResource(R.string.garage_litre_unit),
            large = true,
            modifier = Modifier.weight(1f),
        )
    }
    // C1 (D15): a preview to check what was typed; the price saved is the server's (G7).
    val cents = Inputs.number(fields.total) ?: 0L
    val ml = Inputs.scaled(fields.litres, FuelingViewModel.LITRE_SCALE) ?: 0L
    if (cents > 0 && ml > 0) {
        val price = cents / CENTS_PER_UNIT / (ml / ML_PER_LITRE)
        Tag(
            stringResource(R.string.garage_fueling_price_preview, Formats.unitPrice(price, currency)),
            icon = PrumoIcons.Calculate,
        )
    }
    Text(stringResource(R.string.garage_fuel), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FuelType.entries.forEach { fuel ->
            FilterChip(
                selected = fuel == state.fuelType,
                onClick = { callbacks.onFuelChange(fuel) },
                label = { Text(fuel.shortLabel()) },
            )
        }
    }
    // Board 08: date and km are defaults shown as tags that change with a tap; the km opens its
    // field (and stays open with a server error on it).
    val kmError = kmError(state.saveError, state.context)
    var editKm by rememberSaveable { mutableStateOf(false) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        DateTimeField(
            value = state.fueledAt,
            onValueChange = callbacks.onDateTimeChange,
            label = stringResource(R.string.garage_date_time),
            today = state.context.today,
            asTag = true,
        )
        if (!editKm && kmError == null && fields.km.isNotEmpty()) {
            Tag(
                Formats.km(Inputs.number(fields.km) ?: 0L),
                icon = PrumoIcons.Speed,
                onClick = { editKm = true },
            )
        }
    }
    if (editKm || kmError != null || fields.km.isEmpty()) {
        ValueField(
            digits = fields.km,
            onDigitsChange = callbacks.onKmChange,
            label = stringResource(R.string.garage_km_on_panel),
            transformation = IntegerTransformation,
            leadingIcon = PrumoIcons.Speed,
            error = kmError,
            focusRequester = rememberOpeningFocus(enabled = editKm),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilterChip(
            selected = state.fullTank,
            onClick = { callbacks.onFullTankChange(!state.fullTank) },
            label = { Text(stringResource(R.string.garage_full_tank)) },
            leadingIcon = if (state.fullTank) {
                { Icon(painterResource(PrumoIcons.Check), contentDescription = null) }
            } else {
                null
            },
        )
        Text(
            stringResource(if (state.fullTank) R.string.garage_full_tank_hint else R.string.garage_partial_tank_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    OutlinedTextField(
        value = fields.notes,
        onValueChange = callbacks.onNotesChange,
        label = { Text(stringResource(R.string.garage_notes)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    state.saveError?.takeIf { !it.isFieldError() }?.let {
        Text(garageErrorText(it), color = MaterialTheme.colorScheme.error)
    }
    Button(onClick = onSave, enabled = canSave && !state.saving, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.garage_fueling_save))
    }
}

private const val CENTS_PER_UNIT = 100.0
private const val ML_PER_LITRE = 1000.0

@PhonePreviews
@Composable
private fun FuelingSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            Surface {
                FuelingSheetContent(
                    state = FuelingUiState(
                        loading = false,
                        vehicles = GaragePreview.vehicles,
                        vehicleId = GaragePreview.cg.id,
                    ),
                    fields = FuelingFields(total = "4636", litres = "7,3", km = "18512"),
                    canSave = true,
                    callbacks = FuelingCallbacks({}, {}, {}, {}, {}, {}, {}, {}),
                    onSave = {},
                    onDelete = {},
                    onRetry = {},
                )
            }
        }
    }
}
