package com.henriquesebastiao.prumo.feature.garage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.ComingSoonScreen
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Where Veículos opens. */
@Serializable
data object GarageHome : NavKey

/** Veículos (`garage`). In the foundation: its Home card, its actions and the "em breve" screen. */
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
        ),
        QuickAction(
            "garage.odometer",
            R.string.garage_action_odometer,
            R.string.garage_action_odometer,
            PrumoIcons.Speed,
            GarageHome,
        ),
        QuickAction(
            "garage.maintenance",
            R.string.garage_action_maintenance,
            R.string.garage_action_maintenance_short,
            PrumoIcons.Build,
            GarageHome,
        ),
        QuickAction(
            "garage.oil_change",
            R.string.garage_action_oil_change,
            R.string.garage_action_oil_change_short,
            PrumoIcons.OilBarrel,
            GarageHome,
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<GarageHome> {
            ComingSoonScreen(stringResource(nameRes), icon, seed, onBack = navigator::back)
        }
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

    companion object {
        const val ID = "garage"
        val SEED = Seeds.GARAGE
    }
}
