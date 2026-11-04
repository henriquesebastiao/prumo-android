package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.data.Page
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OdometerReading
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
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

/** The tabs of a vehicle's history (board 05). */
enum class HistoryTab { Maintenances, Fuelings, OilChanges, Km }

/** One tab's records, loaded page by page. */
data class HistoryList<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: AppError? = null,
) {
    val hasMore: Boolean get() = items.size < total
}

data class VehicleDetailUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val vehicle: Vehicle? = null,
    val tab: HistoryTab = HistoryTab.Maintenances,
    val maintenances: HistoryList<Maintenance> = HistoryList(),
    val fuelings: HistoryList<Fueling> = HistoryList(),
    val oilChanges: HistoryList<Maintenance> = HistoryList(),
    val readings: HistoryList<OdometerReading> = HistoryList(),
    /** Archiving, deleting: the menu waits. */
    val busy: Boolean = false,
    /** Why the last action (archive, delete, delete a reading) failed, for a snackbar. */
    val actionError: AppError? = null,
    /** The vehicle was deleted: the screen closes. */
    val gone: Boolean = false,
    val context: GarageContext = GarageContext(),
)

@HiltViewModel(assistedFactory = VehicleDetailViewModel.Factory::class)
class VehicleDetailViewModel @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    private val repository: GarageRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        VehicleDetailUiState(context = account.preferences.value.garageContext(clock)),
    )
    val state: StateFlow<VehicleDetailUiState> = mutableState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) refresh() } }
    }

    /** The vehicle (its oil change and stats come from the server) and the open tab's first page. */
    fun refresh() {
        // The other tabs load again when opened: a new record may be in any of them.
        mutableState.update {
            val tab = it.tab
            it.copy(
                error = null,
                maintenances = if (tab == HistoryTab.Maintenances) it.maintenances else HistoryList(),
                fuelings = if (tab == HistoryTab.Fuelings) it.fuelings else HistoryList(),
                oilChanges = if (tab == HistoryTab.OilChanges) it.oilChanges else HistoryList(),
                readings = if (tab == HistoryTab.Km) it.readings else HistoryList(),
            )
        }
        viewModelScope.launch {
            when (val outcome = repository.vehicle(vehicleId)) {
                is Outcome.Ok -> mutableState.update { it.copy(loading = false, vehicle = outcome.value) }
                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
        load(mutableState.value.tab, more = false)
    }

    fun selectTab(tab: HistoryTab) {
        mutableState.update { it.copy(tab = tab) }
        if (!list(tab).loaded) load(tab, more = false)
    }

    fun loadMore() = load(mutableState.value.tab, more = true)

    fun retryTab() = load(mutableState.value.tab, more = false)

    private fun list(tab: HistoryTab): HistoryList<*> = with(mutableState.value) {
        when (tab) {
            HistoryTab.Maintenances -> maintenances
            HistoryTab.Fuelings -> fuelings
            HistoryTab.OilChanges -> oilChanges
            HistoryTab.Km -> readings
        }
    }

    private fun load(tab: HistoryTab, more: Boolean) {
        val offset = if (more) list(tab).items.size else 0
        mutableState.update {
            when (tab) {
                HistoryTab.Maintenances -> it.copy(maintenances = it.maintenances.copy(loading = true, error = null))
                HistoryTab.Fuelings -> it.copy(fuelings = it.fuelings.copy(loading = true, error = null))
                HistoryTab.OilChanges -> it.copy(oilChanges = it.oilChanges.copy(loading = true, error = null))
                HistoryTab.Km -> it.copy(readings = it.readings.copy(loading = true, error = null))
            }
        }
        viewModelScope.launch {
            when (tab) {
                HistoryTab.Maintenances -> {
                    val page = repository.maintenances(vehicleId, MaintenanceKind.General, offset = offset)
                    mutableState.update { it.copy(maintenances = it.maintenances.next(page, more)) }
                }

                HistoryTab.Fuelings -> {
                    val page = repository.fuelings(vehicleId, offset = offset)
                    mutableState.update { it.copy(fuelings = it.fuelings.next(page, more)) }
                }

                HistoryTab.OilChanges -> {
                    val page = repository.maintenances(vehicleId, MaintenanceKind.OilChange, offset = offset)
                    mutableState.update { it.copy(oilChanges = it.oilChanges.next(page, more)) }
                }

                HistoryTab.Km -> {
                    val page = repository.readings(vehicleId, offset = offset)
                    mutableState.update { it.copy(readings = it.readings.next(page, more)) }
                }
            }
        }
    }

    fun setArchived(archived: Boolean) = act { repository.setArchived(vehicleId, archived) }

    fun delete() = act(onDone = {
        mutableState.update { it.copy(gone = true) }
    }) { repository.deleteVehicle(vehicleId) }

    fun deleteReading(readingId: String) = act { repository.deleteReading(vehicleId, readingId) }

    fun actionErrorShown() {
        mutableState.update { it.copy(actionError = null) }
    }

    private fun act(onDone: () -> Unit = {}, call: suspend () -> Outcome<*>) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            when (val outcome = call()) {
                is Outcome.Ok -> {
                    mutableState.update { it.copy(busy = false) }
                    onDone()
                }

                is Outcome.Failed -> mutableState.update { it.copy(busy = false, actionError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String): VehicleDetailViewModel
    }
}

/** The list after a page: appended for "Carregar mais", replaced otherwise; an error keeps what was shown. */
internal fun <T> HistoryList<T>.next(outcome: Outcome<Page<T>>, more: Boolean): HistoryList<T> = when (outcome) {
    is Outcome.Ok -> HistoryList(
        items = if (more) items + outcome.value.items else outcome.value.items,
        total = outcome.value.total,
        loaded = true,
    )

    is Outcome.Failed -> copy(loading = false, loaded = true, error = outcome.error)
}
