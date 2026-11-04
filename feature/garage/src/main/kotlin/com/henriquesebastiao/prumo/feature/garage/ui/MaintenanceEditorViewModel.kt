package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.model.ItemInput
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OilChangeSuggestion
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaintenanceEditorUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val oilChange: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: String? = null,
    /** The maintenance being edited; null for a new one. */
    val editing: Maintenance? = null,
    val performedAt: LocalDateTime = LocalDateTime.of(2026, 11, 17, 14, 30),
    val workshops: List<Workshop> = emptyList(),
    /** The workshop picked from the list; a typed name that matches none is created on save (G10). */
    val workshopId: String? = null,
    /** The server's next oil change for this km and date (G5), and the oils used before (G14). */
    val suggestion: OilChangeSuggestion? = null,
    val nextOn: LocalDate? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: GarageContext = GarageContext(),
) {
    val vehicle: Vehicle? get() = vehicles.firstOrNull { it.id == vehicleId }
}

/** One part of the list (C2: with quantity); [key] keeps the row while others are removed. */
data class PartText(val key: Long, val description: String = "", val quantity: String = "1", val price: String = "") {
    val isEmpty: Boolean get() = description.isBlank() && price.isEmpty()
}

/** The texts typed in the maintenance form. */
data class MaintenanceFields(
    val km: String = "",
    val workshop: String = "",
    val labor: String = "",
    val notes: String = "",
    val oilType: String = "",
    val nextKm: String = "",
    val parts: List<PartText> = listOf(PartText(0)),
)

@Suppress("TooManyFunctions")
@HiltViewModel(assistedFactory = MaintenanceEditorViewModel.Factory::class)
class MaintenanceEditorViewModel @AssistedInject constructor(
    @Assisted private val key: MaintenanceEditor,
    private val repository: GarageRepository,
    account: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    var fields by mutableStateOf(MaintenanceFields())
        private set

    private val context = account.preferences.value.garageContext(clock)
    private val mutableState = MutableStateFlow(
        MaintenanceEditorUiState(
            oilChange = key.oilChange,
            performedAt = LocalDateTime.ofInstant(clock.instant(), context.zone).truncatedTo(ChronoUnit.MINUTES),
            context = context,
        ),
    )
    val state: StateFlow<MaintenanceEditorUiState> = mutableState.asStateFlow()

    private var kmTouched = false
    private var nextKmTouched = false
    private var nextOnTouched = false
    private var nextPartKey = 1L
    private var suggestionJob: Job? = null

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val vehicles = repository.vehicles()
            val workshops = repository.workshops()
            val editing = key.maintenanceId?.let { id -> key.vehicleId?.let { repository.maintenance(it, id) } }
            val error = (vehicles as? Outcome.Failed)?.error ?: (editing as? Outcome.Failed)?.error
            if (error != null) {
                mutableState.update { it.copy(loading = false, error = error) }
                return@launch
            }
            mutableState.update {
                it.copy(
                    loading = false,
                    vehicles = vehicles.getOrNull().orEmpty(),
                    workshops = workshops.getOrNull().orEmpty(),
                )
            }
            val maintenance = editing?.getOrNull()
            if (maintenance != null) {
                fill(maintenance)
            } else {
                chooseVehicle(mutableState.value.vehicles, key.vehicleId, repository.lastVehicleId)?.let {
                    select(it.id)
                }
            }
        }
    }

    private fun fill(maintenance: Maintenance) {
        kmTouched = true
        nextKmTouched = true
        nextOnTouched = true
        fields = MaintenanceFields(
            km = Inputs.digitsOf(maintenance.km),
            workshop = maintenance.workshop?.name.orEmpty(),
            labor = Inputs.digitsOf(maintenance.laborCostCents.takeIf { it > 0 }),
            notes = maintenance.notes.orEmpty(),
            oilType = maintenance.oilType.orEmpty(),
            nextKm = Inputs.digitsOf(maintenance.nextOilChangeKm),
            parts = maintenance.items.map {
                PartText(nextPartKey++, it.description, it.quantity.toString(), Inputs.digitsOf(it.unitPriceCents))
            }.ifEmpty { listOf(PartText(nextPartKey++)) },
        )
        mutableState.update {
            it.copy(
                editing = maintenance,
                oilChange = maintenance.kind == MaintenanceKind.OilChange,
                vehicleId = maintenance.vehicle.id,
                performedAt = LocalDateTime.ofInstant(maintenance.performedAt, it.context.zone),
                workshopId = maintenance.workshop?.id,
                nextOn = maintenance.nextOilChangeOn,
            )
        }
        suggest(immediately = true)
    }

    /** The vehicle and, unless typed by hand, its km (C9). */
    fun select(vehicleId: String) {
        val vehicle = mutableState.value.vehicles.firstOrNull { it.id == vehicleId } ?: return
        if (!kmTouched) fields = fields.copy(km = Inputs.digitsOf(vehicle.currentKm))
        mutableState.update { it.copy(vehicleId = vehicleId, saveError = null) }
        suggest(immediately = true)
    }

    fun onFieldsChange(new: MaintenanceFields) {
        val kmChanged = new.km != fields.km
        if (kmChanged) kmTouched = true
        if (new.nextKm != fields.nextKm) nextKmTouched = true
        // Typing another name than the picked workshop's makes it a new one.
        if (new.workshop != fields.workshop) mutableState.update { it.copy(workshopId = null) }
        fields = new
        mutableState.update { it.copy(saveError = null) }
        if (kmChanged) suggest()
    }

    fun onDateTimeChange(value: LocalDateTime) {
        mutableState.update { it.copy(performedAt = value, saveError = null) }
        suggest()
    }

    fun pickWorkshop(workshop: Workshop) {
        fields = fields.copy(workshop = workshop.name)
        mutableState.update { it.copy(workshopId = workshop.id, saveError = null) }
    }

    fun onNextOnChange(date: LocalDate?) {
        nextOnTouched = true
        mutableState.update { it.copy(nextOn = date) }
    }

    fun addPart() {
        fields = fields.copy(parts = fields.parts + PartText(nextPartKey++))
    }

    fun onPartChange(part: PartText) {
        fields = fields.copy(parts = fields.parts.map { if (it.key == part.key) part else it })
        mutableState.update { it.copy(saveError = null) }
    }

    fun removePart(key: Long) {
        fields = fields.copy(parts = fields.parts.filterNot { it.key == key })
    }

    /**
     * Asks the server for the next oil change of an oil change at this km and date (G5), after the
     * typing stops. Values typed by hand stay.
     */
    private fun suggest(immediately: Boolean = false) {
        val current = mutableState.value
        if (!current.oilChange) return
        val vehicleId = current.vehicleId ?: return
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            if (!immediately) delay(SUGGESTION_DELAY_MS)
            val km = Inputs.number(fields.km)
            val on = mutableState.value.performedAt.toLocalDate()
            val suggestion = repository.oilChangeSuggestion(vehicleId, km, on).getOrNull() ?: return@launch
            if (!nextKmTouched) fields = fields.copy(nextKm = Inputs.digitsOf(suggestion.nextKm))
            mutableState.update {
                it.copy(suggestion = suggestion, nextOn = if (nextOnTouched) it.nextOn else suggestion.nextOn)
            }
        }
    }

    /** What the form sends, or null while the km is missing or a part has a value but no name. */
    private fun input(): MaintenanceInput? {
        val current = mutableState.value
        val km = Inputs.number(fields.km) ?: return null
        val parts = fields.parts.filterNot { it.isEmpty }
        if (parts.any { it.description.isBlank() || (Inputs.number(it.quantity) ?: 0L) < 1 }) return null
        val workshop = fields.workshop.trim()
        val picked = current.workshops.firstOrNull { it.id == current.workshopId }?.takeIf { it.name == workshop }
        return MaintenanceInput(
            kind = if (current.oilChange) MaintenanceKind.OilChange else MaintenanceKind.General,
            performedAt = current.performedAt.atZone(current.context.zone).toInstant(),
            km = km,
            workshopId = picked?.id,
            workshopName = workshop.ifEmpty { null },
            laborCostCents = Inputs.number(fields.labor) ?: 0L,
            notes = fields.notes.trim().ifEmpty { null },
            items = parts.map {
                ItemInput(
                    it.description.trim(),
                    Inputs.number(it.quantity)?.toInt() ?: 1,
                    Inputs.number(it.price) ?: 0L,
                )
            },
            oilType = fields.oilType.trim().ifEmpty { null },
            nextOilChangeKm = Inputs.number(fields.nextKm),
            nextOilChangeOn = current.nextOn,
        )
    }

    val canSave: Boolean get() = mutableState.value.vehicleId != null && input() != null

    fun save() {
        val current = mutableState.value
        val vehicleId = current.vehicleId ?: return
        val input = input() ?: return
        if (current.saving) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val editing = current.editing
            val outcome = if (editing == null) {
                repository.createMaintenance(vehicleId, input)
            } else {
                repository.updateMaintenance(vehicleId, editing.id, input)
            }
            when (outcome) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(key: MaintenanceEditor): MaintenanceEditorViewModel
    }

    private companion object {
        const val SUGGESTION_DELAY_MS = 400L
    }
}

/**
 * The total of the form as typed (C3, D15): parts × quantity + labour. Only a preview; the total
 * saved and shown afterwards is the server's (G4).
 */
internal fun MaintenanceFields.previewTotals(): Pair<Long, Long> {
    val parts = parts.sumOf { (Inputs.number(it.quantity) ?: 0L) * (Inputs.number(it.price) ?: 0L) }
    return parts to (Inputs.number(labor) ?: 0L)
}
