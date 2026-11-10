package com.henriquesebastiao.prumo.feature.garage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.UpcomingItem
import com.henriquesebastiao.prumo.core.model.double
import com.henriquesebastiao.prumo.core.model.long
import com.henriquesebastiao.prumo.core.model.text
import com.henriquesebastiao.prumo.core.ui.relativeDateTime
import com.henriquesebastiao.prumo.feature.garage.ui.MaintenanceDetail
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleDetail

// How the items of Veículos read on the Home (docs/api.md of the server, "Itens do Início"), as
// the web's garage/events.ts writes them. Every value comes from the server.

/** "Vem aí": the next oil change of a vehicle (`garage.oil_change`), by pace or by date. */
@Composable
@ReadOnlyComposable
internal fun garageUpcoming(item: UpcomingItem): HomeEvent? {
    if (item.kind != "garage.oil_change") return null
    val data = item.data
    val pace = data.double("pace_km_per_day")
    val months = data.long("interval_months")?.toInt()
    val subtitle = when {
        data.text("status") == "overdue" -> stringResource(R.string.garage_event_overdue)

        data.text("basis") == "km_pace" && pace != null ->
            stringResource(R.string.garage_event_by_pace, Formats.decimal(pace, maxDecimals = 0))

        months != null -> stringResource(
            R.string.garage_event_by_date,
            pluralStringResource(R.plurals.garage_months, months, months),
        )

        else -> stringResource(R.string.garage_event_by_date_plain)
    }
    return HomeEvent(
        title = stringResource(R.string.garage_event_oil_change, item.subject),
        subtitle = subtitle,
        target = item.entityId?.let(::VehicleDetail),
    )
}

/** "Atividade recente": a km reading, a fueling or a maintenance (an oil change among them). */
@Composable
@ReadOnlyComposable
internal fun garageActivity(item: ActivityItem, context: HomeContext): HomeEvent? {
    val data = item.data
    val whenText = relativeDateTime(item.occurredAt, context.zone, context.today)
    val total = data.long("total_cents")?.let { Formats.money(it, context.currency) }
    val vehicleId = data.text("vehicle_id")
    return when (item.kind) {
        "garage.odometer" -> HomeEvent(
            title = stringResource(R.string.garage_event_odometer, item.subject),
            subtitle = whenText,
            value = data.long("km")?.let(Formats::km),
            icon = PrumoIcons.Speed,
            target = item.entityId?.let(::VehicleDetail),
        )

        "garage.fueling" -> HomeEvent(
            title = stringResource(R.string.garage_event_fueling, item.subject),
            subtitle =
                data.long("volume_ml")?.let { stringResource(R.string.garage_join, whenText, Formats.litres(it)) }
                    ?: whenText,
            value = total,
            icon = PrumoIcons.LocalGasStation,
            target = vehicleId?.let(::VehicleDetail),
        )

        "garage.maintenance" -> {
            val oil = data.text("maintenance_kind") == "oil_change"
            val id = item.entityId
            HomeEvent(
                title = stringResource(
                    if (oil) R.string.garage_event_oil_change else R.string.garage_event_maintenance,
                    item.subject,
                ),
                subtitle = data.text("workshop")?.let {
                    stringResource(R.string.garage_join, whenText, it)
                } ?: whenText,
                value = total,
                icon = if (oil) PrumoIcons.OilBarrel else PrumoIcons.Build,
                target = if (vehicleId != null && id != null) MaintenanceDetail(vehicleId, id) else null,
            )
        }

        else -> null
    }
}
