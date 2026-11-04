package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.network.parseInstant
import com.henriquesebastiao.prumo.core.ui.errorText
import com.henriquesebastiao.prumo.feature.garage.R
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OilStatus
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the screens of Veículos need from the account: the currency and the user's zone and today (S3). */
data class GarageContext(
    val currency: String = Preferences.Default.currency,
    val zone: ZoneId = Preferences.Default.zone,
    val today: LocalDate = LocalDate.of(2026, 11, 17),
)

internal fun Preferences?.garageContext(clock: Clock): GarageContext {
    val prefs = this ?: Preferences.Default
    return GarageContext(prefs.currency, prefs.zone, prefs.today(clock.instant()))
}

@DrawableRes
internal fun VehicleKind.icon(): Int = when (this) {
    VehicleKind.Car -> PrumoIcons.DirectionsCar
    VehicleKind.Motorcycle -> PrumoIcons.TwoWheeler
}

@Composable
@ReadOnlyComposable
internal fun VehicleKind.label(): String = when (this) {
    VehicleKind.Car -> stringResource(R.string.garage_kind_car)
    VehicleKind.Motorcycle -> stringResource(R.string.garage_kind_motorcycle)
}

@Composable
@ReadOnlyComposable
internal fun FuelType.label(): String = stringResource(
    when (this) {
        FuelType.Gasoline -> R.string.garage_fuel_gasoline
        FuelType.GasolineAdditive -> R.string.garage_fuel_gasoline_additive
        FuelType.GasolinePremium -> R.string.garage_fuel_gasoline_premium
        FuelType.Ethanol -> R.string.garage_fuel_ethanol
        FuelType.Diesel -> R.string.garage_fuel_diesel
        FuelType.Cng -> R.string.garage_fuel_cng
    },
)

/** The fuel in a chip: "Aditivada". */
@Composable
@ReadOnlyComposable
internal fun FuelType.shortLabel(): String = stringResource(
    when (this) {
        FuelType.Gasoline -> R.string.garage_fuel_short_gasoline
        FuelType.GasolineAdditive -> R.string.garage_fuel_short_gasoline_additive
        FuelType.GasolinePremium -> R.string.garage_fuel_short_gasoline_premium
        FuelType.Ethanol -> R.string.garage_fuel_ethanol
        FuelType.Diesel -> R.string.garage_fuel_diesel
        FuelType.Cng -> R.string.garage_fuel_cng
    },
)

@Composable
@ReadOnlyComposable
internal fun OilStatus.label(): String = stringResource(
    when (this) {
        OilStatus.Ok -> R.string.garage_status_ok
        OilStatus.DueSoon -> R.string.garage_status_due_soon
        OilStatus.Overdue -> R.string.garage_status_overdue
        OilStatus.Unknown -> R.string.garage_status_unknown
    },
)

internal fun OilStatus.tone(): StatusTone = when (this) {
    OilStatus.Ok -> StatusTone.Success
    OilStatus.DueSoon -> StatusTone.Warning
    OilStatus.Overdue -> StatusTone.Error
    OilStatus.Unknown -> StatusTone.Neutral
}

@Composable
@ReadOnlyComposable
internal fun MaintenanceKind.label(): String = when (this) {
    MaintenanceKind.General -> stringResource(R.string.garage_maintenance)
    MaintenanceKind.OilChange -> stringResource(R.string.garage_oil_change)
}

@Composable
@ReadOnlyComposable
internal fun ReadingSource.label(): String = when (this) {
    ReadingSource.Manual -> stringResource(R.string.garage_source_manual)
    ReadingSource.Maintenance -> stringResource(R.string.garage_source_maintenance)
    ReadingSource.Fueling -> stringResource(R.string.garage_source_fueling)
}

/** "Honda CG 160 Fan · 2023". */
@Composable
@ReadOnlyComposable
internal fun join(first: String, second: String): String = stringResource(R.string.garage_join, first, second)

/** "Honda CG 160 Fan". */
@Composable
@ReadOnlyComposable
internal fun brandModel(vehicle: Vehicle): String =
    stringResource(R.string.garage_brand_model, vehicle.brand, vehicle.model)

/** When something happened, short: "hoje, 08:12", "ontem, 21:40", "12/11/2026". */
@Composable
@ReadOnlyComposable
internal fun whenText(instant: Instant, context: GarageContext): String {
    val at = instant.atZone(context.zone)
    val time = Formats.time(at.toLocalTime())
    return when (at.toLocalDate()) {
        context.today -> stringResource(R.string.garage_today_at, time)
        context.today.minusDays(1) -> stringResource(R.string.garage_yesterday_at, time)
        else -> Formats.date(at.toLocalDate())
    }
}

@Composable
@ReadOnlyComposable
internal fun days(count: Int): String = pluralStringResource(R.plurals.garage_days, count, count)

/** "40,3 km/l". */
@Composable
@ReadOnlyComposable
internal fun kmPerLitre(value: Double): String = stringResource(R.string.garage_km_l, Formats.decimal(value))

/**
 * The text of an error of Veículos: the module's codes (`docs/api.md` of the server, "Veículos"),
 * then the app's.
 */
@Composable
@ReadOnlyComposable
internal fun garageErrorText(error: AppError): String = when ((error as? AppError.Api)?.code) {
    ODOMETER_INCONSISTENT -> stringResource(R.string.garage_error_odometer_inconsistent)
    LAST_READING -> stringResource(R.string.garage_error_last_reading)
    READING_NOT_MANUAL -> stringResource(R.string.garage_error_reading_not_manual)
    WORKSHOP_IN_USE -> stringResource(R.string.garage_error_workshop_in_use)
    WORKSHOP_EXISTS -> stringResource(R.string.garage_error_workshop_exists)
    OIL_FIELDS_REQUIRE_OIL_CHANGE -> stringResource(R.string.garage_error_oil_fields)
    FUEL_TYPE_REQUIRED -> stringResource(R.string.garage_error_fuel_type_required)
    else -> errorText(error)
}

/**
 * The error of a km field: a reading out of order (G3) names the reading it conflicts with, as the
 * web does; another error with that field's path says to check it.
 */
@Composable
@ReadOnlyComposable
internal fun kmError(error: AppError?, context: GarageContext, path: String = "km"): String? {
    val api = error as? AppError.Api ?: return null
    if (api.code == ODOMETER_INCONSISTENT) {
        val km = api.details["km"]?.toLongOrNull()?.let(Formats::km).orEmpty()
        val date = api.details["recorded_at"]?.let { text ->
            runCatching { Formats.date(parseInstant(text).atZone(context.zone).toLocalDate()) }.getOrNull()
        }.orEmpty()
        return if (api.details["conflict"] == "next") {
            stringResource(R.string.garage_km_higher, date, km)
        } else {
            stringResource(R.string.garage_km_lower, date, km)
        }
    }
    return fieldError(error, path)
}

/** "Confira este valor." when the server refused the field at [path] (`validation_error`). */
@Composable
@ReadOnlyComposable
internal fun fieldError(error: AppError?, path: String): String? {
    val api = error as? AppError.Api ?: return null
    val refused = api.fields.any { it.path == path || it.path.startsWith("$path.") }
    return if (refused) stringResource(R.string.garage_field_invalid) else null
}

/** Whether [error] belongs to a field (shown there) rather than to the whole form. */
internal fun AppError.isFieldError(): Boolean =
    this is AppError.Api && (code == ODOMETER_INCONSISTENT || fields.isNotEmpty())

private const val ODOMETER_INCONSISTENT = "garage.odometer_inconsistent"
private const val LAST_READING = "garage.last_reading"
private const val READING_NOT_MANUAL = "garage.reading_not_manual"
private const val WORKSHOP_IN_USE = "garage.workshop_in_use"
private const val WORKSHOP_EXISTS = "garage.workshop_exists"
private const val OIL_FIELDS_REQUIRE_OIL_CHANGE = "garage.oil_fields_require_oil_change"
private const val FUEL_TYPE_REQUIRED = "garage.fuel_type_required"
