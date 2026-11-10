package com.henriquesebastiao.prumo.feature.garage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.UpcomingItem
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import com.henriquesebastiao.prumo.feature.garage.model.GARAGE_MODULE_ID
import com.henriquesebastiao.prumo.feature.garage.ui.ArchivedVehicles
import com.henriquesebastiao.prumo.feature.garage.ui.GarageHome
import com.henriquesebastiao.prumo.feature.garage.ui.GarageRecords
import com.henriquesebastiao.prumo.feature.garage.ui.GarageSheet
import com.henriquesebastiao.prumo.feature.garage.ui.MaintenanceDetail
import com.henriquesebastiao.prumo.feature.garage.ui.MaintenanceDetailRoute
import com.henriquesebastiao.prumo.feature.garage.ui.MaintenanceEditor
import com.henriquesebastiao.prumo.feature.garage.ui.MaintenanceEditorRoute
import com.henriquesebastiao.prumo.feature.garage.ui.RecordsRoute
import com.henriquesebastiao.prumo.feature.garage.ui.SheetKind
import com.henriquesebastiao.prumo.feature.garage.ui.SheetRequest
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleDetail
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleDetailRoute
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleEditor
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleEditorRoute
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleListRoute
import com.henriquesebastiao.prumo.feature.garage.ui.Workshops
import com.henriquesebastiao.prumo.feature.garage.ui.WorkshopsRoute
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Veículos (`garage`): vehicles, km, fuelings, maintenances and oil changes, on the server's
 * `/api/v1/garage` (docs/api.md of the server, "Veículos").
 */
class GarageFeature @Inject constructor() : PrumoFeature {
    override val id = ID
    override val nameRes = R.string.garage_name
    override val descriptionRes = R.string.garage_description
    override val icon = PrumoIcons.TwoWheeler
    override val seed = SEED
    override val entryKey: NavKey = GarageHome

    override val quickActions = listOf(
        QuickAction(
            "garage.fueling",
            R.string.garage_action_fueling,
            R.string.garage_action_fueling_short,
            PrumoIcons.LocalGasStation,
            GarageHome,
            sheet = { onDismiss -> GarageSheet(SheetRequest(SheetKind.Fueling), onDismiss) },
        ),
        QuickAction(
            "garage.odometer",
            R.string.garage_action_odometer,
            R.string.garage_action_odometer,
            PrumoIcons.Speed,
            GarageHome,
            sheet = { onDismiss -> GarageSheet(SheetRequest(SheetKind.Odometer), onDismiss) },
        ),
        QuickAction(
            "garage.maintenance",
            R.string.garage_action_maintenance,
            R.string.garage_action_maintenance_short,
            PrumoIcons.Build,
            MaintenanceEditor(),
        ),
        QuickAction(
            "garage.oil_change",
            R.string.garage_action_oil_change,
            R.string.garage_action_oil_change_short,
            PrumoIcons.OilBarrel,
            MaintenanceEditor(oilChange = true),
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<GarageHome> { Module { VehicleListRoute(archived = false, navigator = navigator) } }
        entry<ArchivedVehicles> { Module { VehicleListRoute(archived = true, navigator = navigator) } }
        entry<VehicleDetail> { key -> Module { VehicleDetailRoute(key.vehicleId, navigator) } }
        entry<VehicleEditor> { key -> Module { VehicleEditorRoute(key.vehicleId, navigator) } }
        entry<MaintenanceDetail> { key -> Module { MaintenanceDetailRoute(key, navigator) } }
        entry<MaintenanceEditor> { key -> Module { MaintenanceEditorRoute(key, navigator) } }
        entry<Workshops> { Module { WorkshopsRoute(onBack = navigator::back) } }
        entry<GarageRecords> { key -> Module { RecordsRoute(key.list, navigator) } }
    }

    @Composable
    override fun HomeCard(
        summary: JsonObject?,
        hero: Boolean,
        context: HomeContext,
        onOpen: () -> Unit,
        modifier: Modifier,
    ) {
        GarageHomeCard(decodeOrNull(GarageSummaryDto.serializer(), summary), onOpen, modifier)
    }

    @Composable
    override fun upcoming(item: UpcomingItem, context: HomeContext): HomeEvent? = garageUpcoming(item)

    @Composable
    override fun activity(item: ActivityItem, context: HomeContext): HomeEvent? = garageActivity(item, context)

    /** A destination of the module, in its colors. */
    @Composable
    private fun Module(content: @Composable () -> Unit) = ModuleTheme(SEED, content)

    companion object {
        const val ID = GARAGE_MODULE_ID
        val SEED = Seeds.GARAGE
    }
}
