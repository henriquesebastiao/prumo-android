package com.henriquesebastiao.prumo.feature.garage.data

import com.henriquesebastiao.prumo.feature.garage.VehicleRefDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The garage part of the contract (openapi.json), written by hand with the fields the app uses
// (docs/decisoes.md, D04). GarageContractTest checks them against the server's schemas. Enums
// travel as strings and unknown values map to a fallback (a newer server).

/** The standard page of a list (`docs/api.md`, "Paginação"). */
@Serializable
data class PageDto<T>(val items: List<T>, val total: Int, val limit: Int, val offset: Int)

/** Schema `VehicleRead`. */
@Serializable
data class VehicleDto(
    val id: String,
    val kind: String,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    @SerialName("fuel_type") val fuelType: String?,
    @SerialName("current_km") val currentKm: Long,
    @SerialName("current_km_at") val currentKmAt: String,
    @SerialName("oil_interval_km") val oilIntervalKm: Long?,
    @SerialName("oil_interval_months") val oilIntervalMonths: Int?,
    val notes: String?,
    @SerialName("archived_at") val archivedAt: String?,
    @SerialName("oil_change") val oilChange: OilChangeDto?,
    val stats: VehicleStatsDto,
)

/** Schema `OilChangeRead`: every field is required and nullable. */
@Serializable
data class OilChangeDto(
    val status: String,
    val primary: String?,
    @SerialName("interval_km") val intervalKm: Long?,
    @SerialName("interval_months") val intervalMonths: Int?,
    val last: LastOilChangeDto?,
    @SerialName("next_km") val nextKm: Long?,
    @SerialName("remaining_km") val remainingKm: Long?,
    @SerialName("used_km_percent") val usedKmPercent: Int?,
    @SerialName("next_on") val nextOn: String?,
    @SerialName("remaining_days") val remainingDays: Int?,
    @SerialName("used_days_percent") val usedDaysPercent: Int?,
    @SerialName("estimated_km_on") val estimatedKmOn: String?,
)

/** Schema `LastOilChangeRead`. */
@Serializable
data class LastOilChangeDto(
    @SerialName("maintenance_id") val maintenanceId: String,
    @SerialName("performed_at") val performedAt: String,
    val km: Long,
    @SerialName("oil_type") val oilType: String?,
)

/** Schema `VehicleStats`. */
@Serializable
data class VehicleStatsDto(
    @SerialName("last_fueling") val lastFueling: LastFuelingDto?,
    @SerialName("last_consumption_km_l") val lastConsumptionKmL: Double?,
    @SerialName("avg_consumption_km_l") val avgConsumptionKmL: Double?,
    @SerialName("recent_consumption") val recentConsumption: List<ConsumptionPointDto>,
    @SerialName("pace_km_per_day") val paceKmPerDay: Double?,
    @SerialName("month_km") val monthKm: Long,
    @SerialName("month_spent_cents") val monthSpentCents: Long,
    @SerialName("month_fuelings_count") val monthFuelingsCount: Int,
    @SerialName("month_maintenances_count") val monthMaintenancesCount: Int,
)

/** Schema `LastFuelingRead`. */
@Serializable
data class LastFuelingDto(
    val id: String,
    @SerialName("fueled_at") val fueledAt: String,
    @SerialName("volume_ml") val volumeMl: Long,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("price_per_liter") val pricePerLiter: Double,
    @SerialName("full_tank") val fullTank: Boolean,
)

/** Schema `ConsumptionPoint`. */
@Serializable
data class ConsumptionPointDto(
    @SerialName("fueling_id") val fuelingId: String,
    @SerialName("fueled_at") val fueledAt: String,
    @SerialName("km_l") val kmL: Double,
)

/** `POST /garage/vehicles` (schema `VehicleCreate`) and, with the same fields, `VehicleUpdate`. */
@Serializable
data class VehicleWriteDto(
    val kind: String,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    @SerialName("fuel_type") val fuelType: String?,
    @SerialName("current_km") val currentKm: Long,
    @SerialName("oil_interval_km") val oilIntervalKm: Long?,
    @SerialName("oil_interval_months") val oilIntervalMonths: Int?,
    val notes: String?,
)

/** Schema `OdometerReadingRead`. */
@Serializable
data class OdometerReadingDto(
    val id: String,
    val km: Long,
    @SerialName("recorded_at") val recordedAt: String,
    val source: String,
    @SerialName("source_id") val sourceId: String?,
)

/** `POST /garage/vehicles/{id}/odometer` (schema `OdometerReadingCreate`); the server stamps now. */
@Serializable
data class OdometerReadingCreateDto(val km: Long)

/** Schema `FuelingRead`. */
@Serializable
data class FuelingDto(
    val id: String,
    val vehicle: VehicleRefDto,
    @SerialName("fueled_at") val fueledAt: String,
    val km: Long,
    @SerialName("fuel_type") val fuelType: String,
    @SerialName("volume_ml") val volumeMl: Long,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("price_per_liter") val pricePerLiter: Double,
    @SerialName("full_tank") val fullTank: Boolean,
    @SerialName("consumption_km_l") val consumptionKmL: Double?,
    val notes: String?,
)

/** Schema `FuelingCreate` and, with the same fields, `FuelingUpdate`. */
@Serializable
data class FuelingWriteDto(
    @SerialName("fueled_at") val fueledAt: String,
    val km: Long,
    @SerialName("fuel_type") val fuelType: String,
    @SerialName("volume_ml") val volumeMl: Long,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("full_tank") val fullTank: Boolean,
    val notes: String?,
)

/** Schema `WorkshopRef`. */
@Serializable
data class WorkshopRefDto(val id: String, val name: String)

/** Schema `MaintenanceItemRead`. */
@Serializable
data class MaintenanceItemDto(
    val id: String,
    val description: String,
    val quantity: Int,
    @SerialName("unit_price_cents") val unitPriceCents: Long,
    @SerialName("total_cents") val totalCents: Long,
)

/** Schema `MaintenanceRead`. */
@Serializable
data class MaintenanceDto(
    val id: String,
    val vehicle: VehicleRefDto,
    val kind: String,
    @SerialName("performed_at") val performedAt: String,
    val km: Long,
    val workshop: WorkshopRefDto?,
    @SerialName("labor_cost_cents") val laborCostCents: Long,
    @SerialName("parts_cents") val partsCents: Long,
    @SerialName("total_cents") val totalCents: Long,
    val notes: String?,
    @SerialName("oil_type") val oilType: String?,
    @SerialName("next_oil_change_km") val nextOilChangeKm: Long?,
    @SerialName("next_oil_change_on") val nextOilChangeOn: String?,
    val items: List<MaintenanceItemDto>,
)

/** Schema `MaintenanceItemIn`. */
@Serializable
data class MaintenanceItemInDto(
    val description: String,
    val quantity: Int,
    @SerialName("unit_price_cents") val unitPriceCents: Long,
)

/**
 * Schema `MaintenanceCreate`. The oil fields go only on an oil change; there, a `null` next km or
 * date means "no forecast" (an absent one would be suggested by the server).
 */
@Serializable
data class MaintenanceCreateDto(
    val kind: String,
    @SerialName("performed_at") val performedAt: String,
    val km: Long,
    @SerialName("workshop_id") val workshopId: String?,
    @SerialName("workshop_name") val workshopName: String?,
    @SerialName("labor_cost_cents") val laborCostCents: Long,
    val notes: String?,
    @SerialName("oil_type") val oilType: String?,
    @SerialName("next_oil_change_km") val nextOilChangeKm: Long?,
    @SerialName("next_oil_change_on") val nextOilChangeOn: String?,
    val items: List<MaintenanceItemInDto>,
)

/** Schema `MaintenanceUpdate`: the same as the creation without `kind`; `items` replace the current ones. */
@Serializable
data class MaintenanceUpdateDto(
    @SerialName("performed_at") val performedAt: String,
    val km: Long,
    @SerialName("workshop_id") val workshopId: String?,
    @SerialName("workshop_name") val workshopName: String?,
    @SerialName("labor_cost_cents") val laborCostCents: Long,
    val notes: String?,
    @SerialName("oil_type") val oilType: String?,
    @SerialName("next_oil_change_km") val nextOilChangeKm: Long?,
    @SerialName("next_oil_change_on") val nextOilChangeOn: String?,
    val items: List<MaintenanceItemInDto>,
)

/** Schema `OilChangeSuggestion`. */
@Serializable
data class OilChangeSuggestionDto(
    @SerialName("next_oil_change_km") val nextOilChangeKm: Long?,
    @SerialName("next_oil_change_on") val nextOilChangeOn: String?,
    @SerialName("interval_km") val intervalKm: Long?,
    @SerialName("interval_months") val intervalMonths: Int?,
    @SerialName("pace_km_per_day") val paceKmPerDay: Double?,
    @SerialName("estimated_km_on") val estimatedKmOn: String?,
    @SerialName("previous_oil_types") val previousOilTypes: List<String> = emptyList(),
)

/** Schema `WorkshopRead`. */
@Serializable
data class WorkshopDto(
    val id: String,
    val name: String,
    val notes: String?,
    @SerialName("maintenances_count") val maintenancesCount: Int,
    @SerialName("last_maintenance_at") val lastMaintenanceAt: String?,
)

/** Schema `WorkshopCreate` and, with the same fields, `WorkshopUpdate`. */
@Serializable
data class WorkshopWriteDto(val name: String, val notes: String?)
