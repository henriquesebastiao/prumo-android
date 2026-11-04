package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.IconAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
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

data class VehicleListUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val refreshing: Boolean = false,
    val context: GarageContext = GarageContext(),
)

@HiltViewModel(assistedFactory = VehicleListViewModel.Factory::class)
class VehicleListViewModel @AssistedInject constructor(
    @Assisted private val archived: Boolean,
    private val repository: GarageRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(VehicleListUiState(context = account.preferences.value.garageContext(clock)))
    val state: StateFlow<VehicleListUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load() } }
    }

    /** Loads the vehicles; what is on screen stays while it loads. */
    fun load() {
        mutableState.update { it.copy(refreshing = !it.loading, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.vehicles(archived)) {
                is Outcome.Ok -> mutableState.update {
                    it.copy(loading = false, refreshing = false, vehicles = outcome.value)
                }

                is Outcome.Failed -> mutableState.update {
                    it.copy(loading = false, refreshing = false, error = outcome.error)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(archived: Boolean): VehicleListViewModel
    }
}

/** Veículos ([GarageHome]) or its archived vehicles ([ArchivedVehicles]). */
@Composable
internal fun VehicleListRoute(
    archived: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: VehicleListViewModel = hiltViewModel<VehicleListViewModel, VehicleListViewModel.Factory>(
        key = "vehicles-$archived",
        creationCallback = { it.create(archived) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    VehicleListScreen(
        state = state,
        archived = archived,
        onBack = navigator::back,
        onAdd = { navigator.navigate(VehicleEditor()) },
        onOpen = { navigator.navigate(VehicleDetail(it)) },
        onNavigate = navigator::navigate,
        actions = GarageActions(
            onOdometer = { sheet = SheetRequest(SheetKind.Odometer) },
            onMaintenance = { navigator.navigate(MaintenanceEditor()) },
            onOilChange = { navigator.navigate(MaintenanceEditor(oilChange = true)) },
            onFueling = { sheet = SheetRequest(SheetKind.Fueling) },
        ),
        onRefresh = viewModel::load,
        modifier = modifier,
    )
    GarageSheet(sheet, onDismiss = { sheet = null })
}

/** The vehicles (board 01), the empty state (board 03) or the archived ones. */
@Composable
internal fun VehicleListScreen(
    state: VehicleListUiState,
    archived: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onNavigate: (NavKey) -> Unit,
    actions: GarageActions,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            PrumoTopBar(
                title = stringResource(if (archived) R.string.garage_archived_title else R.string.garage_name),
                onBack = onBack,
                large = true,
                scrollBehavior = scroll,
                actions = { if (!archived) ListActions(onAdd, onNavigate) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.garage_loading))

                state.error != null && state.vehicles.isEmpty() -> ErrorState(state.error, onRetry = onRefresh)

                state.vehicles.isEmpty() && archived -> EmptyState(
                    icon = PrumoIcons.Inventory2,
                    title = stringResource(R.string.garage_archived_empty),
                )

                state.vehicles.isEmpty() -> EmptyState(
                    icon = PrumoIcons.TwoWheeler,
                    title = stringResource(R.string.garage_empty_title),
                    body = stringResource(R.string.garage_empty_body),
                    hero = true,
                    action = {
                        Button(onClick = onAdd) {
                            Icon(painterResource(PrumoIcons.Add), contentDescription = null)
                            Text(stringResource(R.string.garage_add_vehicle), Modifier.padding(start = Spacing.sm))
                        }
                    },
                )

                else -> PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh) {
                    VehicleList(state, archived, onOpen)
                }
            }
            if (!archived && state.vehicles.isNotEmpty()) {
                GarageToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
            }
        }
    }
}

@Composable
private fun ListActions(onAdd: () -> Unit, onNavigate: (NavKey) -> Unit) {
    FilledTonalIconButton(onClick = onAdd) {
        Icon(painterResource(PrumoIcons.Add), contentDescription = stringResource(R.string.garage_add_vehicle))
    }
    OverflowMenu(
        listOf(
            MenuOption(stringResource(R.string.garage_fuelings), PrumoIcons.LocalGasStation) {
                onNavigate(GarageRecords(RecordList.Fuelings))
            },
            MenuOption(stringResource(R.string.garage_maintenances), PrumoIcons.Build) {
                onNavigate(GarageRecords(RecordList.Maintenances))
            },
            MenuOption(stringResource(R.string.garage_oil_changes), PrumoIcons.OilBarrel) {
                onNavigate(GarageRecords(RecordList.OilChanges))
            },
            MenuOption(stringResource(R.string.garage_workshops), PrumoIcons.Storefront) { onNavigate(Workshops) },
            MenuOption(stringResource(R.string.garage_archived_title), PrumoIcons.Inventory2) {
                onNavigate(ArchivedVehicles)
            },
        ),
    )
}

@Composable
private fun VehicleList(state: VehicleListUiState, archived: Boolean, onOpen: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = TOOLBAR_SPACE),
    ) {
        itemsIndexed(state.vehicles, key = { _, vehicle -> vehicle.id }) { index, vehicle ->
            GroupedListItem(
                vehicleRow(vehicle) { onOpen(vehicle.id) },
                index = index,
                count = state.vehicles.size,
                style = ListStyle.Separated,
                modifier = Modifier.padding(bottom = ListStyle.Separated.gap),
            )
        }
        if (!archived) item { GroupNote(stringResource(R.string.garage_list_note)) }
    }
}

/** A vehicle: its kind, "Honda CG 160 Fan · 2023 · 18.420 km" and the status of its next oil change. */
@Composable
private fun vehicleRow(vehicle: Vehicle, onClick: () -> Unit): GroupedRow {
    val status = vehicle.oilChange?.status
    return GroupedRow(
        title = vehicle.name,
        summary = join(join(brandModel(vehicle), vehicle.year.toString()), Formats.km(vehicle.currentKm)),
        action = RowAction.Click(
            onClick = onClick,
            chevron = false,
            trailing = status?.let { { StatusChip(it.label(), it.tone(), small = true) } },
        ),
        leading = {
            IconAvatar(
                vehicle.kind.icon(),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    )
}

/** Room under a list for the floating toolbar. */
internal val TOOLBAR_SPACE = 112.dp

@PhonePreviews
@Composable
private fun VehicleListPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            VehicleListScreen(
                state = VehicleListUiState(loading = false, vehicles = GaragePreview.vehicles),
                archived = false,
                onBack = {},
                onAdd = {},
                onOpen = {},
                onNavigate = {},
                actions = GarageActions({}, {}, {}, {}),
                onRefresh = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun VehicleListEmptyPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            VehicleListScreen(
                state = VehicleListUiState(loading = false),
                archived = false,
                onBack = {},
                onAdd = {},
                onOpen = {},
                onNavigate = {},
                actions = GarageActions({}, {}, {}, {}),
                onRefresh = {},
            )
        }
    }
}
