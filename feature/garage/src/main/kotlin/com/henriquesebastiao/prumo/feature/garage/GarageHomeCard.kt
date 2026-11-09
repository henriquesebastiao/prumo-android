package com.henriquesebastiao.prumo.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.Formats

/**
 * Veículos on the Home: the oil change that comes first, in its leading criterion ("180 km" or
 * "65 dias" until the change), how much of the interval has gone and its status. Every number
 * comes from the API (G6 of the server).
 */
@Composable
internal fun GarageHomeCard(summary: GarageSummaryDto?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val next = summary?.nextDue
    ModuleTheme(GarageFeature.SEED) {
        SectionCard(
            modifier = modifier.heightIn(min = TILE_MIN_HEIGHT),
            tone = SectionTone.Primary,
            icon = if (next?.vehicle?.kind == "car") PrumoIcons.DirectionsCar else PrumoIcons.TwoWheeler,
            overline = stringResource(R.string.garage_name),
            onClick = onOpen,
        ) {
            val vehicles = summary?.vehiclesCount ?: 0
            when {
                next != null -> NextOilChange(next)

                vehicles == 0 -> Empty()

                else -> Text(
                    pluralStringResource(R.plurals.garage_card_vehicles, vehicles, vehicles),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun NextOilChange(next: NextDueDto) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        val value = when {
            next.status == "overdue" -> stringResource(R.string.garage_status_overdue)

            next.primary == "date" && next.remainingDays != null ->
                pluralStringResource(R.plurals.garage_card_days, next.remainingDays, next.remainingDays)

            next.remainingKm != null -> Formats.km(next.remainingKm)

            else -> null
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (value != null) Text(value, style = PrumoType.dataHeadline, maxLines = 1)
            Text(
                when (next.status) {
                    "overdue" -> stringResource(R.string.garage_card_oil_overdue)

                    "unknown" -> stringResource(R.string.garage_card_oil_unknown)

                    // The article follows the kind: "da CG" (a moto), "do Strada" (o carro).
                    else -> stringResource(
                        if (next.vehicle.kind == "car") {
                            R.string.garage_card_until_oil_change_car
                        } else {
                            R.string.garage_card_until_oil_change_motorcycle
                        },
                        next.vehicle.name,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (next.status == "overdue" || next.status == "unknown") {
                Text(
                    next.vehicle.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        next.usedPercent?.let { used ->
            LinearWavyProgressIndicator(progress = { used / PERCENT }, modifier = Modifier.fillMaxWidth())
        }
        when (next.status) {
            "ok" -> StatusChip(stringResource(R.string.garage_status_ok), StatusTone.Success, small = true)
            "due_soon" -> StatusChip(stringResource(R.string.garage_status_due_soon), StatusTone.Warning, small = true)
            "overdue" -> StatusChip(stringResource(R.string.garage_status_overdue), StatusTone.Error, small = true)
            else -> StatusChip(stringResource(R.string.garage_status_unknown), StatusTone.Neutral, small = true)
        }
    }
}

@Composable
private fun Empty() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.garage_card_no_vehicles), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.garage_card_add_vehicle), style = MaterialTheme.typography.bodyMedium)
    }
}

private const val PERCENT = 100f
private val TILE_MIN_HEIGHT = 224.dp

@PhonePreviews
@Composable
private fun GarageHomeCardPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            GarageHomeCard(
                GarageSummaryDto(
                    vehiclesCount = 2,
                    nextDue = NextDueDto(
                        vehicle = VehicleRefDto("1", "Honda CG 160 Fan", "motorcycle"),
                        status = "due_soon",
                        primary = "km",
                        remainingKm = 180,
                        remainingDays = 65,
                        usedPercent = 82,
                    ),
                ),
                onOpen = {},
            )
        }
    }
}
