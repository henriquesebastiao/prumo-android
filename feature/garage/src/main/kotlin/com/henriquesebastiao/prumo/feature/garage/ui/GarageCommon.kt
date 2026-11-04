package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import java.io.Serializable

/** Which sheet of Veículos is open over a screen, and for what. Kept across a rotation. */
data class SheetRequest(val kind: SheetKind, val vehicleId: String? = null, val fuelingId: String? = null) :
    Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

enum class SheetKind { Odometer, Fueling }

/** The sheet of [request], in the module's colors (also over the Home). */
@Composable
internal fun GarageSheet(request: SheetRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    ModuleTheme(GarageFeature.SEED) {
        when (request.kind) {
            SheetKind.Odometer -> OdometerSheet(request.vehicleId, onDismiss)
            SheetKind.Fueling -> FuelingSheet(request.vehicleId, request.fuelingId, onDismiss)
        }
    }
}

/** What the toolbar of the list and of a vehicle records. */
internal class GarageActions(
    val onOdometer: () -> Unit,
    val onMaintenance: () -> Unit,
    val onOilChange: () -> Unit,
    val onFueling: () -> Unit,
)

/**
 * The floating toolbar of Veículos (board 01): km, maintenance and oil change; the FAB records a
 * fueling. On the list, each asks for the vehicle when there is more than one.
 */
@Composable
internal fun GarageToolbar(actions: GarageActions, modifier: Modifier = Modifier) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = actions.onFueling) {
                Icon(
                    painterResource(PrumoIcons.LocalGasStation),
                    contentDescription = stringResource(R.string.garage_action_fueling),
                )
            }
        },
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier,
    ) {
        IconButton(onClick = actions.onOdometer) {
            Icon(
                painterResource(PrumoIcons.Speed),
                contentDescription = stringResource(R.string.garage_action_odometer),
            )
        }
        IconButton(onClick = actions.onMaintenance) {
            Icon(
                painterResource(PrumoIcons.Build),
                contentDescription = stringResource(R.string.garage_action_maintenance),
            )
        }
        IconButton(onClick = actions.onOilChange) {
            Icon(
                painterResource(PrumoIcons.OilBarrel),
                contentDescription = stringResource(R.string.garage_action_oil_change),
            )
        }
    }
}

/** The vehicle a record goes to, as a connected button group; nothing to choose with one vehicle. */
@Composable
internal fun VehicleChoice(
    vehicles: List<Vehicle>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (vehicles.size < 2) return
    val current = vehicles.firstOrNull { it.id == selected } ?: vehicles.first()
    ChoiceGroup(
        options = vehicles,
        selected = current,
        label = { it.name },
        onSelect = { onSelect(it.id) },
        icon = { it.kind.icon() },
        modifier = modifier,
    )
}

/** The vehicle a form starts with (C9, D15): the one asked, else the last used in this run, else the first. */
internal fun chooseVehicle(vehicles: List<Vehicle>, wanted: String?, last: String?): Vehicle? =
    vehicles.firstOrNull { it.id == wanted } ?: vehicles.firstOrNull { it.id == last } ?: vehicles.firstOrNull()

/** "Carregar mais" under a list that has more pages. */
@Composable
internal fun LoadMore(loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = Spacing.sm), contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = onClick, enabled = !loading) {
            Text(stringResource(R.string.garage_load_more))
        }
    }
}

/** One option of a ⋮ menu. */
internal data class MenuOption(val label: String, val icon: Int, val onClick: () -> Unit)

/** The ⋮ button of a top bar and its menu. */
@Composable
internal fun OverflowMenu(options: List<MenuOption>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(PrumoIcons.MoreVert), contentDescription = stringResource(R.string.garage_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        open = false
                        option.onClick()
                    },
                    leadingIcon = { Icon(painterResource(option.icon), contentDescription = null) },
                )
            }
        }
    }
}
