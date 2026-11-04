package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.IntegerTransformation
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OdometerUiState(
    /** Which opening of the sheet this state is for (a new one starts empty). */
    val request: Long? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: String? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val saved: Boolean = false,
    val context: GarageContext = GarageContext(),
) {
    val vehicle: Vehicle? get() = vehicles.firstOrNull { it.id == vehicleId }
}

@HiltViewModel
class OdometerViewModel @Inject constructor(
    private val repository: GarageRepository,
    private val account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    /** The km typed, as digits. */
    var km by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(OdometerUiState())
    val state: StateFlow<OdometerUiState> = mutableState.asStateFlow()

    /** A new opening of the sheet ([request]); the same one again (a rotation) keeps what was typed. */
    fun start(request: Long, vehicleId: String?) {
        if (mutableState.value.request == request) return
        km = ""
        wanted = vehicleId
        mutableState.value =
            OdometerUiState(request = request, context = account.preferences.value.garageContext(clock))
        load()
    }

    private var wanted: String? = null

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.vehicles()) {
                is Outcome.Ok -> mutableState.update {
                    val vehicle = chooseVehicle(outcome.value, wanted, repository.lastVehicleId)
                    it.copy(loading = false, vehicles = outcome.value, vehicleId = vehicle?.id)
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun select(vehicleId: String) {
        mutableState.update { it.copy(vehicleId = vehicleId, saveError = null) }
    }

    fun onKmChange(text: String) {
        km = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun save() {
        val vehicleId = mutableState.value.vehicleId ?: return
        val value = Inputs.number(km) ?: return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = repository.addReading(vehicleId, value)) {
                is Outcome.Ok -> {
                    repository.lastVehicleId = vehicleId
                    mutableState.update { it.copy(saving = false, saved = true) }
                }

                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }
}

/** "Atualizar km" (board 06) over the current screen: the panel's km for a vehicle. */
@Composable
internal fun OdometerSheet(vehicleId: String?, onDismiss: () -> Unit, viewModel: OdometerViewModel = hiltViewModel()) {
    val request = rememberSaveable { Random.nextLong() }
    LaunchedEffect(request) { viewModel.start(request, vehicleId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.request == request
    LaunchedEffect(current, state.saved) { if (current && state.saved) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        OdometerSheetContent(
            state = if (current) state else OdometerUiState(),
            km = viewModel.km,
            onSelect = viewModel::select,
            onKmChange = viewModel::onKmChange,
            onSave = viewModel::save,
            onRetry = viewModel::load,
        )
    }
}

/** The content of the km sheet, without state (previews and tests call it directly). */
@Composable
fun OdometerSheetContent(
    state: OdometerUiState,
    km: String,
    onSelect: (String) -> Unit,
    onKmChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            stringResource(R.string.garage_action_odometer),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        val vehicle = state.vehicle
        when {
            state.loading -> LoadingState(stringResource(R.string.garage_loading))

            state.error != null -> ErrorState(state.error, onRetry = onRetry)

            vehicle == null -> Text(stringResource(R.string.garage_no_vehicles_for_record))

            else -> {
                VehicleChoice(state.vehicles, state.vehicleId, onSelect)
                Tag(
                    stringResource(
                        R.string.garage_current_km,
                        Formats.km(vehicle.currentKm),
                        whenText(vehicle.currentKmAt, state.context),
                    ),
                    icon = PrumoIcons.History,
                )
                val typed = Inputs.number(km)
                val delta = typed?.minus(vehicle.currentKm)?.takeIf { it > 0 }
                ValueField(
                    digits = km,
                    onDigitsChange = onKmChange,
                    label = stringResource(R.string.garage_km_on_panel),
                    transformation = IntegerTransformation,
                    large = true,
                    leadingIcon = PrumoIcons.Speed,
                    supportingText = delta?.let { stringResource(R.string.garage_km_since_last, Formats.km(it)) },
                    error = kmError(state.saveError, state.context),
                    imeAction = ImeAction.Done,
                )
                state.saveError?.takeIf { !it.isFieldError() }?.let {
                    Text(garageErrorText(it), color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = onSave,
                    enabled = typed != null && !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.garage_save))
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun OdometerSheetPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            Surface {
                OdometerSheetContent(
                    state = OdometerUiState(
                        loading = false,
                        vehicles = GaragePreview.vehicles,
                        vehicleId = GaragePreview.cg.id,
                    ),
                    km = "18512",
                    onSelect = {},
                    onKmChange = {},
                    onSave = {},
                    onRetry = {},
                )
            }
        }
    }
}
