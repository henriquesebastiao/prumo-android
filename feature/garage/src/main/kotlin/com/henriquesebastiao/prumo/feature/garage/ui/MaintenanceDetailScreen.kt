package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedList
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
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

data class MaintenanceDetailUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val maintenance: Maintenance? = null,
    val deleting: Boolean = false,
    val deleteError: AppError? = null,
    /** Deleted: the screen closes. */
    val gone: Boolean = false,
    val context: GarageContext = GarageContext(),
)

@HiltViewModel(assistedFactory = MaintenanceDetailViewModel.Factory::class)
class MaintenanceDetailViewModel @AssistedInject constructor(
    @Assisted("vehicle") private val vehicleId: String,
    @Assisted("maintenance") private val maintenanceId: String,
    private val repository: GarageRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        MaintenanceDetailUiState(context = account.preferences.value.garageContext(clock)),
    )
    val state: StateFlow<MaintenanceDetailUiState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.changes.collect { if (!mutableState.value.gone) load() } }
    }

    fun load() {
        mutableState.update { it.copy(error = null) }
        viewModelScope.launch {
            when (val outcome = repository.maintenance(vehicleId, maintenanceId)) {
                is Outcome.Ok -> mutableState.update { it.copy(loading = false, maintenance = outcome.value) }
                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun delete() {
        if (mutableState.value.deleting) return
        mutableState.update { it.copy(deleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val outcome = repository.deleteMaintenance(vehicleId, maintenanceId)) {
                is Outcome.Ok -> mutableState.update { it.copy(deleting = false, gone = true) }
                is Outcome.Failed -> mutableState.update { it.copy(deleting = false, deleteError = outcome.error) }
            }
        }
    }

    fun deleteErrorShown() {
        mutableState.update { it.copy(deleteError = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("vehicle") vehicleId: String,
            @Assisted("maintenance") maintenanceId: String,
        ): MaintenanceDetailViewModel
    }
}

@Composable
internal fun MaintenanceDetailRoute(
    key: MaintenanceDetail,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: MaintenanceDetailViewModel =
        hiltViewModel<MaintenanceDetailViewModel, MaintenanceDetailViewModel.Factory>(
            creationCallback = { it.create(key.vehicleId, key.maintenanceId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    MaintenanceDetailScreen(
        state = state,
        onBack = navigator::back,
        onEdit = { navigator.navigate(MaintenanceEditor(key.vehicleId, key.maintenanceId)) },
        onDelete = viewModel::delete,
        onRetry = viewModel::load,
        onDeleteErrorShown = viewModel::deleteErrorShown,
        modifier = modifier,
    )
}

/** A maintenance or oil change (board 07): when, at what km, where, its values and notes. */
@Composable
internal fun MaintenanceDetailScreen(
    state: MaintenanceDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maintenance = state.maintenance
    val snackbar = remember { SnackbarHostState() }
    val deleteError = state.deleteError?.let { garageErrorText(it) }
    LaunchedEffect(deleteError) {
        if (deleteError != null) {
            snackbar.showSnackbar(deleteError)
            onDeleteErrorShown()
        }
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                title = maintenance?.let { title(it) }.orEmpty(),
                onBack = onBack,
                subtitle = maintenance?.let { join(it.vehicle.name, it.kind.label()) },
                actions = {
                    if (maintenance != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                painterResource(PrumoIcons.Edit),
                                contentDescription = stringResource(R.string.garage_edit),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }, enabled = !state.deleting) {
                            Icon(
                                painterResource(PrumoIcons.Delete),
                                contentDescription = stringResource(R.string.garage_delete),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                maintenance == null && state.loading -> LoadingState(stringResource(R.string.garage_loading))
                maintenance == null -> ErrorState(state.error ?: AppError.Unexpected(), onRetry = onRetry)
                else -> MaintenanceContent(maintenance, state.context)
            }
        }
    }
    if (confirmDelete && maintenance != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(
                if (maintenance.kind == MaintenanceKind.OilChange) {
                    R.string.garage_oil_change_delete_title
                } else {
                    R.string.garage_maintenance_delete_title
                },
            ),
            body = stringResource(
                R.string.garage_maintenance_delete_body,
                Formats.date(maintenance.performedAt.atZone(state.context.zone).toLocalDate()),
                Formats.money(maintenance.totalCents, state.context.currency),
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

/** The first part, or the oil, or the kind: "Pastilha de freio dianteira". */
@Composable
private fun title(maintenance: Maintenance): String = when {
    maintenance.kind == MaintenanceKind.OilChange -> maintenance.kind.label()
    else -> maintenance.items.firstOrNull()?.description ?: maintenance.kind.label()
}

@Composable
private fun MaintenanceContent(maintenance: Maintenance, context: GarageContext) {
    val currency = context.currency
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val at = maintenance.performedAt.atZone(context.zone)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                join(Formats.date(at.toLocalDate()), Formats.time(at.toLocalTime())),
                stringResource(R.string.garage_date_time),
                Modifier.weight(1f),
            )
            KpiTile(Formats.km(maintenance.km), stringResource(R.string.garage_tab_km), Modifier.weight(1f))
        }
        KpiTile(
            maintenance.workshop?.name ?: stringResource(R.string.garage_no_workshop),
            stringResource(R.string.garage_workshop),
            valueStyle = MaterialTheme.typography.titleMedium,
        )
        if (maintenance.kind == MaintenanceKind.OilChange) OilSummary(maintenance)
        val parts = maintenance.items.size
        SectionHeader(
            stringResource(R.string.garage_values),
            trailing = if (parts > 0) pluralStringResource(R.plurals.garage_parts_count, parts, parts) else null,
        )
        GroupedList(values(maintenance, currency))
        maintenance.notes?.let { notes ->
            SectionCard(
                tone = SectionTone.Container,
                icon = PrumoIcons.Notes,
                overline = stringResource(R.string.garage_notes),
            ) {
                Text(notes, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun OilSummary(maintenance: Maintenance) {
    SectionCard(
        tone = SectionTone.Primary,
        icon = PrumoIcons.OilBarrel,
        overline = stringResource(R.string.garage_oil_type),
    ) {
        Text(maintenance.oilType ?: stringResource(R.string.garage_none), style = MaterialTheme.typography.titleMedium)
        val km = maintenance.nextOilChangeKm?.let(Formats::km)
        val on = maintenance.nextOilChangeOn?.let(Formats::date)
        val next = when {
            km != null && on != null -> stringResource(R.string.garage_next_change_both, km, on)
            else -> km ?: on
        }
        next?.let {
            Text(stringResource(R.string.garage_next_change_value, it), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Each part (× quantity), the labour and the total, as the server computed them (G4). */
@Composable
private fun values(maintenance: Maintenance, currency: String): List<GroupedRow> {
    val money: @Composable (Long) -> Unit = { Text(Formats.money(it, currency), style = PrumoType.dataSmall) }
    val items = maintenance.items.map { item ->
        GroupedRow(
            title = if (item.quantity > 1) {
                stringResource(R.string.garage_part_times, item.description, item.quantity)
            } else {
                item.description
            },
            summary = if (item.quantity > 1) Formats.money(item.unitPriceCents, currency) else null,
            action = RowAction.Static { money(item.totalCents) },
        )
    }
    return items + listOf(
        GroupedRow(stringResource(R.string.garage_labor), RowAction.Static { money(maintenance.laborCostCents) }),
        GroupedRow(
            stringResource(R.string.garage_total),
            RowAction.Static { Text(Formats.money(maintenance.totalCents, currency), style = PrumoType.dataMedium) },
            emphasized = true,
        ),
    )
}

@PhonePreviews
@Composable
private fun MaintenanceDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            MaintenanceDetailScreen(
                state = MaintenanceDetailUiState(loading = false, maintenance = GaragePreview.maintenance),
                onBack = {},
                onEdit = {},
                onDelete = {},
                onRetry = {},
                onDeleteErrorShown = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun OilChangeDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            MaintenanceDetailScreen(
                state = MaintenanceDetailUiState(loading = false, maintenance = GaragePreview.oilChange),
                onBack = {},
                onEdit = {},
                onDelete = {},
                onRetry = {},
                onDeleteErrorShown = {},
            )
        }
    }
}
