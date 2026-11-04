package com.henriquesebastiao.prumo.feature.garage.data

import com.henriquesebastiao.prumo.core.network.parseInstant
import com.henriquesebastiao.prumo.feature.garage.VehicleRefDto
import com.henriquesebastiao.prumo.feature.garage.model.ConsumptionPoint
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.FuelingInput
import com.henriquesebastiao.prumo.feature.garage.model.LastFueling
import com.henriquesebastiao.prumo.feature.garage.model.LastOilChange
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceItem
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OdometerReading
import com.henriquesebastiao.prumo.feature.garage.model.OilChange
import com.henriquesebastiao.prumo.feature.garage.model.OilChangeSuggestion
import com.henriquesebastiao.prumo.feature.garage.model.OilCriterion
import com.henriquesebastiao.prumo.feature.garage.model.OilStatus
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.VehicleInput
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import com.henriquesebastiao.prumo.feature.garage.model.VehicleRef
import com.henriquesebastiao.prumo.feature.garage.model.VehicleStats
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import com.henriquesebastiao.prumo.feature.garage.model.WorkshopRef
import com.henriquesebastiao.prumo.feature.garage.model.wireOf
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// DTOs stay in the data package: the screens get the models of `model/`.

/**
 * The JSON of request bodies. Unlike the app's lenient [com.henriquesebastiao.prumo.core.network.NetworkJson],
 * it writes `null`s: on a `PATCH` a `null` clears the field (`docs/api.md`, "Formatos"), and on an
 * oil change a `null` next km or date means "no forecast".
 */
internal val GarageWriteJson = Json {
    explicitNulls = true
    encodeDefaults = true
}

internal fun <T> body(serializer: KSerializer<T>, value: T): JsonObject =
    GarageWriteJson.encodeToJsonElement(serializer, value).jsonObject

internal fun VehicleRefDto.toModel() = VehicleRef(id = id, name = name, kind = wireOf(kind, VehicleKind.Car))

internal fun VehicleDto.toModel() = Vehicle(
    id = id,
    kind = wireOf(kind, VehicleKind.Car),
    name = name,
    brand = brand,
    model = model,
    year = year,
    fuelType = fuelType?.let { wireOf(it, FuelType.Gasoline) },
    currentKm = currentKm,
    currentKmAt = parseInstant(currentKmAt),
    oilIntervalKm = oilIntervalKm,
    oilIntervalMonths = oilIntervalMonths,
    notes = notes,
    archivedAt = archivedAt?.let(::parseInstant),
    oilChange = oilChange?.toModel(),
    stats = stats.toModel(),
)

internal fun OilChangeDto.toModel() = OilChange(
    status = wireOf(status, OilStatus.Unknown),
    primary = primary?.let { wireOf(it, OilCriterion.Km) },
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    last = last?.let {
        LastOilChange(it.maintenanceId, parseInstant(it.performedAt), it.km, it.oilType)
    },
    nextKm = nextKm,
    remainingKm = remainingKm,
    usedKmPercent = usedKmPercent,
    nextOn = nextOn?.let(LocalDate::parse),
    remainingDays = remainingDays,
    usedDaysPercent = usedDaysPercent,
    estimatedKmOn = estimatedKmOn?.let(LocalDate::parse),
)

internal fun VehicleStatsDto.toModel() = VehicleStats(
    lastFueling = lastFueling?.let {
        LastFueling(it.id, parseInstant(it.fueledAt), it.volumeMl, it.totalCents, it.pricePerLiter, it.fullTank)
    },
    lastConsumptionKmL = lastConsumptionKmL,
    avgConsumptionKmL = avgConsumptionKmL,
    recentConsumption = recentConsumption.map { ConsumptionPoint(it.fuelingId, parseInstant(it.fueledAt), it.kmL) },
    paceKmPerDay = paceKmPerDay,
    monthKm = monthKm,
    monthSpentCents = monthSpentCents,
    monthFuelingsCount = monthFuelingsCount,
    monthMaintenancesCount = monthMaintenancesCount,
)

internal fun OdometerReadingDto.toModel() = OdometerReading(
    id = id,
    km = km,
    recordedAt = parseInstant(recordedAt),
    source = wireOf(source, ReadingSource.Manual),
    sourceId = sourceId,
)

internal fun FuelingDto.toModel() = Fueling(
    id = id,
    vehicle = vehicle.toModel(),
    fueledAt = parseInstant(fueledAt),
    km = km,
    fuelType = wireOf(fuelType, FuelType.Gasoline),
    volumeMl = volumeMl,
    totalCents = totalCents,
    pricePerLiter = pricePerLiter,
    fullTank = fullTank,
    consumptionKmL = consumptionKmL,
    notes = notes,
)

internal fun MaintenanceDto.toModel() = Maintenance(
    id = id,
    vehicle = vehicle.toModel(),
    kind = wireOf(kind, MaintenanceKind.General),
    performedAt = parseInstant(performedAt),
    km = km,
    workshop = workshop?.let { WorkshopRef(it.id, it.name) },
    laborCostCents = laborCostCents,
    partsCents = partsCents,
    totalCents = totalCents,
    notes = notes,
    oilType = oilType,
    nextOilChangeKm = nextOilChangeKm,
    nextOilChangeOn = nextOilChangeOn?.let(LocalDate::parse),
    items = items.map { MaintenanceItem(it.id, it.description, it.quantity, it.unitPriceCents, it.totalCents) },
)

internal fun OilChangeSuggestionDto.toModel() = OilChangeSuggestion(
    nextKm = nextOilChangeKm,
    nextOn = nextOilChangeOn?.let(LocalDate::parse),
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    paceKmPerDay = paceKmPerDay,
    estimatedKmOn = estimatedKmOn?.let(LocalDate::parse),
    previousOilTypes = previousOilTypes,
)

internal fun WorkshopDto.toModel() = Workshop(
    id = id,
    name = name,
    notes = notes,
    maintenancesCount = maintenancesCount,
    lastMaintenanceAt = lastMaintenanceAt?.let(::parseInstant),
)

internal fun VehicleInput.toDto() = VehicleWriteDto(
    kind = kind.wire,
    name = name,
    brand = brand,
    model = model,
    year = year,
    fuelType = fuelType?.wire,
    currentKm = currentKm,
    oilIntervalKm = oilIntervalKm,
    oilIntervalMonths = oilIntervalMonths,
    notes = notes,
)

internal fun FuelingInput.toDto() = FuelingWriteDto(
    fueledAt = fueledAt.toString(),
    km = km,
    fuelType = fuelType.wire,
    volumeMl = volumeMl,
    totalCents = totalCents,
    fullTank = fullTank,
    notes = notes,
)

private fun MaintenanceInput.itemsDto() = items.map {
    MaintenanceItemInDto(it.description, it.quantity, it.unitPriceCents)
}

private val MaintenanceInput.isOilChange get() = kind == MaintenanceKind.OilChange

internal fun MaintenanceInput.toCreateDto() = MaintenanceCreateDto(
    kind = kind.wire,
    performedAt = performedAt.toString(),
    km = km,
    workshopId = workshopId,
    workshopName = workshopName.takeIf { workshopId == null },
    laborCostCents = laborCostCents,
    notes = notes,
    oilType = oilType.takeIf { isOilChange },
    nextOilChangeKm = nextOilChangeKm.takeIf { isOilChange },
    nextOilChangeOn = nextOilChangeOn?.toString().takeIf { isOilChange },
    items = itemsDto(),
)

internal fun MaintenanceInput.toUpdateDto() = MaintenanceUpdateDto(
    performedAt = performedAt.toString(),
    km = km,
    workshopId = workshopId,
    workshopName = workshopName.takeIf { workshopId == null },
    laborCostCents = laborCostCents,
    notes = notes,
    oilType = oilType.takeIf { isOilChange },
    nextOilChangeKm = nextOilChangeKm.takeIf { isOilChange },
    nextOilChangeOn = nextOilChangeOn?.toString().takeIf { isOilChange },
    items = itemsDto(),
)
