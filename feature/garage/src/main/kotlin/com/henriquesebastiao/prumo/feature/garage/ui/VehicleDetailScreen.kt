package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.ErrorCard
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PillTabs
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.errorTitle
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.OilChange
import com.henriquesebastiao.prumo.feature.garage.model.OilCriterion
import com.henriquesebastiao.prumo.feature.garage.model.OilStatus
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle

/** What the vehicle's screen does, besides the toolbar's records. */
internal class VehicleDetailEvents(
    val onEdit: () -> Unit,
    val onArchive: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onTab: (HistoryTab) -> Unit,
    val onLoadMore: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenMaintenance: (Maintenance) -> Unit,
    val onOpenFueling: (Fueling) -> Unit,
    val onDeleteReading: (String) -> Unit,
)

@Composable
internal fun VehicleDetailRoute(
    vehicleId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: VehicleDetailViewModel = hiltViewModel<VehicleDetailViewModel, VehicleDetailViewModel.Factory>(
        creationCallback = { it.create(vehicleId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    LaunchedEffect(state.gone) { if (state.gone) navigator.back() }
    VehicleDetailScreen(
        state = state,
        actions = GarageActions(
            onOdometer = { sheet = SheetRequest(SheetKind.Odometer, vehicleId) },
            onMaintenance = { navigator.navigate(MaintenanceEditor(vehicleId)) },
            onOilChange = { navigator.navigate(MaintenanceEditor(vehicleId, oilChange = true)) },
            onFueling = { sheet = SheetRequest(SheetKind.Fueling, vehicleId) },
        ),
        onBack = navigator::back,
        events = VehicleDetailEvents(
            onEdit = { navigator.navigate(VehicleEditor(vehicleId)) },
            onArchive = viewModel::setArchived,
            onDelete = viewModel::delete,
            onTab = viewModel::selectTab,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::refresh,
            onOpenMaintenance = { navigator.navigate(MaintenanceDetail(it.vehicle.id, it.id)) },
            onOpenFueling = { sheet = SheetRequest(SheetKind.Fueling, it.vehicle.id, it.id) },
            onDeleteReading = viewModel::deleteReading,
        ),
        onActionErrorShown = viewModel::actionErrorShown,
        modifier = modifier,
    )
    GarageSheet(sheet, onDismiss = { sheet = null })
}

/** A vehicle (board 05): its km, the next oil change, consumption and spending, and its history. */
@Composable
internal fun VehicleDetailScreen(
    state: VehicleDetailUiState,
    onBack: () -> Unit,
    actions: GarageActions,
    events: VehicleDetailEvents,
    onActionErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vehicle = state.vehicle
    val snackbar = remember { SnackbarHostState() }
    val actionError = state.actionError?.let { garageErrorText(it) }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbar.showSnackbar(actionError)
            onActionErrorShown()
        }
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleteReading by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                title = vehicle?.name.orEmpty(),
                onBack = onBack,
                subtitle = vehicle?.let { join(brandModel(it), it.year.toString()) },
                actions = {
                    if (vehicle !=
                        null
                    ) {
                        DetailActions(vehicle, state.busy, events, onDelete = { confirmDelete = true })
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                vehicle == null && state.loading -> LoadingState(stringResource(R.string.garage_loading))

                vehicle == null -> ErrorState(state.error ?: AppError.Unexpected(), onRetry = events.onRetry)

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.sm,
                        bottom = TOOLBAR_SPACE,
                    ),
                ) {
                    overview(vehicle, state, actions, events)
                    history(state, events, onDeleteReading = { deleteReading = it })
                }
            }
            if (vehicle != null && !vehicle.archived) {
                GarageToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
            }
        }
    }
    if (confirmDelete && vehicle != null) {
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.garage_vehicle_delete_title, vehicle.name),
            body = stringResource(R.string.garage_vehicle_delete_body),
            confirmLabel = stringResource(R.string.garage_delete),
            onConfirm = {
                confirmDelete = false
                events.onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
    deleteReading?.let { id ->
        ConfirmDialog(
            icon = PrumoIcons.Delete,
            title = stringResource(R.string.garage_reading_delete_title),
            body = stringResource(R.string.garage_reading_delete_body),
            confirmLabel = stringResource(R.string.garage_delete),
            onConfirm = {
                deleteReading = null
                events.onDeleteReading(id)
            },
            onDismiss = { deleteReading = null },
            destructive = true,
        )
    }
}

@Composable
private fun DetailActions(vehicle: Vehicle, busy: Boolean, events: VehicleDetailEvents, onDelete: () -> Unit) {
    IconButton(onClick = events.onEdit) {
        Icon(painterResource(PrumoIcons.Edit), contentDescription = stringResource(R.string.garage_edit))
    }
    val archive = if (vehicle.archived) {
        MenuOption(stringResource(R.string.garage_unarchive), PrumoIcons.Unarchive) {
            if (!busy) events.onArchive(false)
        }
    } else {
        MenuOption(stringResource(R.string.garage_archive), PrumoIcons.Archive) { if (!busy) events.onArchive(true) }
    }
    OverflowMenu(
        listOf(
            archive,
            MenuOption(stringResource(R.string.garage_delete), PrumoIcons.Delete) {
                if (!busy) onDelete()
            },
        ),
    )
}

private fun LazyListScope.overview(
    vehicle: Vehicle,
    state: VehicleDetailUiState,
    actions: GarageActions,
    events: VehicleDetailEvents,
) {
    if (vehicle.archived) {
        item {
            StatusChip(
                stringResource(R.string.garage_archived),
                StatusTone.Neutral,
                Modifier.padding(bottom = Spacing.md),
            )
        }
    }
    item { KmCard(vehicle, state.context, actions.onOdometer, Modifier.padding(bottom = Spacing.md)) }
    item { OilCard(vehicle, state.context, actions.onOilChange, Modifier.padding(bottom = Spacing.md)) }
    item { StatsCard(vehicle, state.context, Modifier.padding(bottom = Spacing.lg)) }
    item {
        PillTabs(
            options = HistoryTab.entries,
            selected = state.tab,
            label = { it.label() },
            onSelect = events.onTab,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
    }
}

@Composable
private fun KmCard(vehicle: Vehicle, context: GarageContext, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier, tone = SectionTone.Container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.garage_current_km_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(Formats.km(vehicle.currentKm), style = PrumoType.dataLarge)
                Text(
                    stringResource(R.string.garage_updated_at, whenText(vehicle.currentKmAt, context)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!vehicle.archived) {
                FilledTonalButton(onClick = onUpdate) { Text(stringResource(R.string.garage_update)) }
            }
        }
    }
}

/** The next oil change (G6): its leading criterion, the share of the interval used and both criteria. */
@Composable
private fun OilCard(vehicle: Vehicle, context: GarageContext, onRecord: () -> Unit, modifier: Modifier = Modifier) {
    val oil = vehicle.oilChange
    when {
        oil == null -> SectionCard(
            modifier,
            icon = PrumoIcons.OilBarrel,
            overline = stringResource(R.string.garage_oil_change),
        ) {
            Text(stringResource(R.string.garage_oil_no_interval), style = MaterialTheme.typography.bodyMedium)
        }

        oil.status == OilStatus.Unknown -> SectionCard(
            modifier,
            tone = SectionTone.Primary,
            hero = true,
            icon = PrumoIcons.OilBarrel,
            overline = stringResource(R.string.garage_oil_next),
            title = stringResource(R.string.garage_oil_first),
        ) {
            StatusChip(oil.status.label(), oil.status.tone())
            Text(stringResource(R.string.garage_oil_no_change), style = MaterialTheme.typography.bodyMedium)
            if (!vehicle.archived) {
                Button(onClick = onRecord) { Text(stringResource(R.string.garage_action_oil_change)) }
            }
        }

        else -> SectionCard(
            modifier,
            tone = SectionTone.Primary,
            hero = true,
            icon = PrumoIcons.OilBarrel,
            overline = stringResource(R.string.garage_oil_next),
            title = oilHeadline(oil),
        ) {
            OilDetails(oil, vehicle, context)
        }
    }
}

@Composable
private fun OilDetails(oil: OilChange, vehicle: Vehicle, context: GarageContext) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(oil.status.label(), oil.status.tone())
            oil.primary?.let {
                Text(
                    stringResource(
                        if (it ==
                            OilCriterion.Date
                        ) {
                            R.string.garage_oil_first_by_date
                        } else {
                            R.string.garage_oil_first_by_km
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        val used = (if (oil.primary == OilCriterion.Date) oil.usedDaysPercent else oil.usedKmPercent) ?: 0
        LinearWavyProgressIndicator(
            progress = { used.coerceIn(0, PERCENT) / PERCENT.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (oil.nextKm != null) {
                KpiTile(Formats.km(oil.nextKm), kmTileLabel(oil), Modifier.weight(1f))
            }
            if (oil.nextOn != null) {
                KpiTile(Formats.date(oil.nextOn), dateTileLabel(oil), Modifier.weight(1f))
            }
        }
        val pace = vehicle.stats.paceKmPerDay
        if (oil.primary == OilCriterion.Km && oil.estimatedKmOn != null && pace != null) {
            Text(
                stringResource(R.string.garage_oil_pace, Formats.decimal(pace, 1), Formats.date(oil.estimatedKmOn)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        oil.last?.let { last ->
            var text = stringResource(
                R.string.garage_oil_last,
                whenText(last.performedAt, context),
                Formats.km(last.km),
            )
            last.oilType?.let { text = join(text, it) }
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "Faltam 220 km", "Passou 50 km", "Faltam 48 dias", "Venceu há 3 dias". */
@Composable
private fun oilHeadline(oil: OilChange): String {
    val left = oil.remainingDays
    val km = oil.remainingKm
    return when {
        oil.primary == OilCriterion.Date && left != null && left >= 0 -> stringResource(
            R.string.garage_oil_left,
            days(left),
        )

        oil.primary == OilCriterion.Date && left != null -> stringResource(
            R.string.garage_oil_overdue_days,
            days(-left),
        )

        km != null && km >= 0 -> stringResource(R.string.garage_oil_left, Formats.km(km))

        km != null -> stringResource(R.string.garage_oil_over_km, Formats.km(-km))

        else -> oil.status.label()
    }
}

@Composable
private fun kmTileLabel(oil: OilChange): String {
    val km = oil.remainingKm ?: return stringResource(R.string.garage_oil_by_km)
    return if (km >= 0) {
        stringResource(R.string.garage_oil_by_km_left, Formats.km(km))
    } else {
        stringResource(R.string.garage_oil_by_km_over, Formats.km(-km))
    }
}

@Composable
private fun dateTileLabel(oil: OilChange): String {
    val left = oil.remainingDays ?: return stringResource(R.string.garage_oil_by_date)
    return if (left >= 0) {
        stringResource(R.string.garage_oil_by_date_left, days(left))
    } else {
        stringResource(R.string.garage_oil_by_date_over, days(-left))
    }
}

/** Consumption and spending (G8, G9), as the server computes them. */
@Composable
private fun StatsCard(vehicle: Vehicle, context: GarageContext, modifier: Modifier = Modifier) {
    val stats = vehicle.stats
    val none = stringResource(R.string.garage_none)
    SectionCard(modifier, tone = SectionTone.Container, overline = stringResource(R.string.garage_stats_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiTile(
                stats.avgConsumptionKmL?.let { kmPerLitre(it) } ?: none,
                stringResource(R.string.garage_avg_consumption),
                Modifier.weight(1f),
            )
            KpiTile(
                stats.lastConsumptionKmL?.let { kmPerLitre(it) } ?: none,
                stringResource(R.string.garage_last_tank),
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val last = stats.lastFueling
            KpiTile(
                last?.let { join(Formats.money(it.totalCents, context.currency), Formats.litres(it.volumeMl)) } ?: none,
                last?.let {
                    stringResource(
                        R.string.garage_fueled_on,
                        Formats.dayAndMonth(it.fueledAt.atZone(context.zone).toLocalDate()),
                    )
                } ?: stringResource(R.string.garage_last_fueling),
                Modifier.weight(1f),
            )
            KpiTile(
                Formats.money(stats.monthSpentCents, context.currency),
                stringResource(R.string.garage_month_spent, Formats.month(context.today)),
                Modifier.weight(1f),
            )
        }
        if (stats.avgConsumptionKmL == null) {
            Text(
                stringResource(R.string.garage_consumption_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryTab.label(): String = stringResource(
    when (this) {
        HistoryTab.Maintenances -> R.string.garage_maintenances
        HistoryTab.Fuelings -> R.string.garage_fuelings
        HistoryTab.OilChanges -> R.string.garage_tab_oil
        HistoryTab.Km -> R.string.garage_tab_km
    },
)

private fun LazyListScope.history(
    state: VehicleDetailUiState,
    events: VehicleDetailEvents,
    onDeleteReading: (String) -> Unit,
) {
    val context = state.context
    when (state.tab) {
        HistoryTab.Maintenances -> records(state.maintenances, events, { it.id }) {
            maintenanceRow(it, context, showVehicle = false) { events.onOpenMaintenance(it) }
        }

        HistoryTab.Fuelings -> records(state.fuelings, events, { it.id }) {
            fuelingRow(it, context, showVehicle = false) { events.onOpenFueling(it) }
        }

        HistoryTab.OilChanges -> records(state.oilChanges, events, { it.id }) {
            maintenanceRow(it, context, showVehicle = false) { events.onOpenMaintenance(it) }
        }

        HistoryTab.Km -> records(state.readings, events, { it.id }) {
            readingRow(it, context) { onDeleteReading(it.id) }
        }
    }
}

/** A tab's rows as a segmented group, then what is missing: loading, error, nothing yet or more pages. */
internal fun <T> LazyListScope.records(
    list: HistoryList<T>,
    events: VehicleDetailEvents,
    key: (T) -> String,
    row: @Composable (T) -> GroupedRow,
) = recordRows(list, key, onLoadMore = events.onLoadMore, onRetry = events.onRetry, row = row)

internal fun <T> LazyListScope.recordRows(
    list: HistoryList<T>,
    key: (T) -> String,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    row: @Composable (T) -> GroupedRow,
) {
    itemsIndexed(list.items, key = { _, item -> key(item) }) { index, item ->
        GroupedListItem(
            row(item),
            index = index,
            count = list.items.size,
            modifier = Modifier.padding(bottom = ListStyle.Segmented.gap),
        )
    }
    val error = list.error
    when {
        list.loading && list.items.isEmpty() -> item {
            Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.size(48.dp))
            }
        }

        error != null -> item {
            ErrorCard(
                title = errorTitle(error),
                body = garageErrorText(error),
                retryLabel = stringResource(R.string.garage_retry),
                onRetry = onRetry,
            )
        }

        list.loaded && list.items.isEmpty() -> item {
            Text(
                stringResource(R.string.garage_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.lg),
            )
        }

        list.hasMore -> item { LoadMore(list.loading, onLoadMore) }
    }
}

private const val PERCENT = 100

@PhonePreviews
@Composable
private fun VehicleDetailPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            VehicleDetailScreen(
                state = VehicleDetailUiState(
                    loading = false,
                    vehicle = GaragePreview.cg,
                    maintenances = HistoryList(listOf(GaragePreview.maintenance), total = 1, loaded = true),
                ),
                onBack = {},
                actions = GarageActions({}, {}, {}, {}),
                events = VehicleDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onActionErrorShown = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun VehicleDetailNoOilChangePreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(GarageFeature.SEED) {
            VehicleDetailScreen(
                state = VehicleDetailUiState(
                    loading = false,
                    vehicle = GaragePreview.strada.copy(
                        oilChange = GaragePreview.strada.oilChange!!.copy(status = OilStatus.Unknown, last = null),
                    ),
                    tab = HistoryTab.Km,
                    readings = HistoryList(GaragePreview.readings, total = 3, loaded = true),
                ),
                onBack = {},
                actions = GarageActions({}, {}, {}, {}),
                events = VehicleDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onActionErrorShown = {},
            )
        }
    }
}
