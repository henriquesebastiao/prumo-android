package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
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

data class RecordsUiState(
    val list: RecordList = RecordList.Fuelings,
    val fuelings: HistoryList<Fueling> = HistoryList(loading = true),
    val maintenances: HistoryList<Maintenance> = HistoryList(loading = true),
    val context: GarageContext = GarageContext(),
) {
    val loading: Boolean get() = if (list == RecordList.Fuelings) fuelings.loading else maintenances.loading
}

@HiltViewModel(assistedFactory = RecordsViewModel.Factory::class)
class RecordsViewModel @AssistedInject constructor(
    @Assisted private val list: RecordList,
    private val repository: GarageRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        RecordsUiState(list = list, context = account.preferences.value.garageContext(clock)),
    )
    val state: StateFlow<RecordsUiState> = mutableState.asStateFlow()

    init {
        load(more = false)
        viewModelScope.launch { repository.changes.collect { load(more = false) } }
    }

    fun refresh() = load(more = false)

    fun loadMore() = load(more = true)

    private fun load(more: Boolean) {
        viewModelScope.launch {
            if (list == RecordList.Fuelings) {
                val current = mutableState.value.fuelings
                mutableState.update { it.copy(fuelings = current.copy(loading = true, error = null)) }
                val page = repository.fuelings(offset = if (more) current.items.size else 0)
                mutableState.update { it.copy(fuelings = current.next(page, more)) }
            } else {
                val kind = if (list == RecordList.OilChanges) MaintenanceKind.OilChange else MaintenanceKind.General
                val current = mutableState.value.maintenances
                mutableState.update { it.copy(maintenances = current.copy(loading = true, error = null)) }
                val page = repository.maintenances(kind = kind, offset = if (more) current.items.size else 0)
                mutableState.update { it.copy(maintenances = current.next(page, more)) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(list: RecordList): RecordsViewModel
    }
}

@Composable
internal fun RecordsRoute(
    list: RecordList,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = hiltViewModel<RecordsViewModel, RecordsViewModel.Factory>(
        creationCallback = { it.create(list) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    RecordsScreen(
        state = state,
        onBack = navigator::back,
        onOpenFueling = { sheet = SheetRequest(SheetKind.Fueling, it.vehicle.id, it.id) },
        onOpenMaintenance = { navigator.navigate(MaintenanceDetail(it.vehicle.id, it.id)) },
        onLoadMore = viewModel::loadMore,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
    GarageSheet(sheet, onDismiss = { sheet = null })
}

/** Every vehicle's fuelings, maintenances or oil changes, newest first (the web's tabs). */
@Composable
internal fun RecordsScreen(
    state: RecordsUiState,
    onBack: () -> Unit,
    onOpenFueling: (Fueling) -> Unit,
    onOpenMaintenance: (Maintenance) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (state.list) {
        RecordList.Fuelings -> R.string.garage_fuelings
        RecordList.Maintenances -> R.string.garage_maintenances
        RecordList.OilChanges -> R.string.garage_oil_changes
    }
    val fuelings = state.list == RecordList.Fuelings
    val error = if (fuelings) state.fuelings.error else state.maintenances.error
    val empty = if (fuelings) state.fuelings.items.isEmpty() else state.maintenances.items.isEmpty()
    Scaffold(modifier, topBar = { PrumoTopBar(stringResource(title), onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && empty -> LoadingState(stringResource(R.string.garage_loading))

                error != null && empty -> ErrorState(error, onRetry = onRefresh)

                else -> PullToRefreshBox(isRefreshing = state.loading && !empty, onRefresh = onRefresh) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.screen)) {
                        val context = state.context
                        if (fuelings) {
                            recordRows(state.fuelings, { it.id }, onLoadMore, onRefresh) {
                                fuelingRow(it, context, showVehicle = true) { onOpenFueling(it) }
                            }
                        } else {
                            recordRows(state.maintenances, { it.id }, onLoadMore, onRefresh) {
                                maintenanceRow(it, context, showVehicle = true) { onOpenMaintenance(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun RecordsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            RecordsScreen(
                state = RecordsUiState(
                    fuelings = HistoryList(GaragePreview.fuelings, total = 2, loaded = true),
                ),
                onBack = {},
                onOpenFueling = {},
                onOpenMaintenance = {},
                onLoadMore = {},
                onRefresh = {},
            )
        }
    }
}
