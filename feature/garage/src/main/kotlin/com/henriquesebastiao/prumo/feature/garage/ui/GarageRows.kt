package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OdometerReading
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource

// The rows of the history of a vehicle and of the lists of every vehicle's records.

/** A maintenance or an oil change: what was done, when, at what km and where; the total at the end. */
@Composable
internal fun maintenanceRow(
    maintenance: Maintenance,
    context: GarageContext,
    showVehicle: Boolean,
    onClick: () -> Unit,
): GroupedRow {
    val first = maintenance.items.firstOrNull()?.description
    val title = when {
        maintenance.kind == MaintenanceKind.OilChange -> maintenance.oilType ?: maintenance.kind.label()
        first == null -> maintenance.kind.label()
        maintenance.items.size > 1 -> stringResource(R.string.garage_item_and_more, first, maintenance.items.size - 1)
        else -> first
    }
    var summary = join(whenText(maintenance.performedAt, context), Formats.km(maintenance.km))
    maintenance.workshop?.let { summary = join(summary, it.name) }
    if (showVehicle) summary = join(maintenance.vehicle.name, summary)
    return GroupedRow(
        title = title,
        summary = summary,
        icon = if (maintenance.kind == MaintenanceKind.OilChange) PrumoIcons.OilBarrel else PrumoIcons.Build,
        action = RowAction.Click(
            onClick = onClick,
            chevron = false,
            trailing = { Text(Formats.money(maintenance.totalCents, context.currency), style = PrumoType.dataSmall) },
        ),
    )
}

/** A fueling: how much and how many litres; when, the km, the fuel and the consumption it closed. */
@Composable
internal fun fuelingRow(
    fueling: Fueling,
    context: GarageContext,
    showVehicle: Boolean,
    onClick: () -> Unit,
): GroupedRow {
    var summary = join(whenText(fueling.fueledAt, context), Formats.km(fueling.km))
    summary = join(summary, fueling.fuelType.shortLabel())
    fueling.consumptionKmL?.let { summary = join(summary, kmPerLitre(it)) }
    if (showVehicle) summary = join(fueling.vehicle.name, summary)
    return GroupedRow(
        title = join(Formats.money(fueling.totalCents, context.currency), Formats.litres(fueling.volumeMl)),
        summary = summary,
        icon = PrumoIcons.LocalGasStation,
        action = RowAction.Click(onClick = onClick, chevron = false),
    )
}

/** A reading of the odometer and where it came from; a manual one can be deleted (G2). */
@Composable
internal fun readingRow(reading: OdometerReading, context: GarageContext, onDelete: () -> Unit): GroupedRow =
    GroupedRow(
        title = Formats.km(reading.km),
        summary = join(whenText(reading.recordedAt, context), reading.source.label()),
        icon = PrumoIcons.Speed,
        action = RowAction.Static(
            trailing = if (reading.source == ReadingSource.Manual) {
                {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painterResource(PrumoIcons.Delete),
                            contentDescription = stringResource(R.string.garage_reading_delete),
                        )
                    }
                }
            } else {
                null
            },
        ),
    )
