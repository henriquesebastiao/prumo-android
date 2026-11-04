package com.henriquesebastiao.prumo.feature.garage.model

import java.time.Instant
import java.time.LocalDate

/** The module's id on the server (`/api/v1/modules`, the Home's summaries). */
const val GARAGE_MODULE_ID = "garage"

// Veículos as the API gives it (docs/dominio.md of the server, "Veículos"). Every derived number
// (oil status, consumption, pace, the month's spending, totals) comes ready from the server (D02).

/** An enum value as sent on the wire; an unknown one (a newer server) maps to a fallback. */
interface Wire {
    val wire: String
}

inline fun <reified T> wireOf(value: String?, fallback: T): T where T : Enum<T>, T : Wire =
    enumValues<T>().firstOrNull { it.wire == value } ?: fallback

enum class VehicleKind(override val wire: String) : Wire {
    Car("car"),
    Motorcycle("motorcycle"),
}

enum class FuelType(override val wire: String) : Wire {
    Gasoline("gasoline"),
    GasolineAdditive("gasoline_additive"),
    GasolinePremium("gasoline_premium"),
    Ethanol("ethanol"),
    Diesel("diesel"),
    Cng("cng"),
}

/** The oil change's status (G6): `unknown` is an interval with no change recorded yet. */
enum class OilStatus(override val wire: String) : Wire {
    Ok("ok"),
    DueSoon("due_soon"),
    Overdue("overdue"),
    Unknown("unknown"),
}

/** Which criterion of the oil change comes first. */
enum class OilCriterion(override val wire: String) : Wire {
    Km("km"),
    Date("date"),
}

enum class MaintenanceKind(override val wire: String) : Wire {
    General("general"),
    OilChange("oil_change"),
}

/** Where an odometer reading came from (G2); only manual ones are deleted directly. */
enum class ReadingSource(override val wire: String) : Wire {
    Manual("manual"),
    Maintenance("maintenance"),
    Fueling("fueling"),
}

data class VehicleRef(val id: String, val name: String, val kind: VehicleKind)

data class Vehicle(
    val id: String,
    val kind: VehicleKind,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    val fuelType: FuelType?,
    val currentKm: Long,
    val currentKmAt: Instant,
    val oilIntervalKm: Long?,
    val oilIntervalMonths: Int?,
    val notes: String?,
    val archivedAt: Instant?,
    /** Null without an oil interval (G1). */
    val oilChange: OilChange?,
    val stats: VehicleStats,
) {
    val ref: VehicleRef get() = VehicleRef(id, name, kind)
    val archived: Boolean get() = archivedAt != null
}

/** The next oil change (G6), for the vehicle's detail. */
data class OilChange(
    val status: OilStatus,
    val primary: OilCriterion?,
    val intervalKm: Long?,
    val intervalMonths: Int?,
    val last: LastOilChange?,
    val nextKm: Long?,
    val remainingKm: Long?,
    val usedKmPercent: Int?,
    val nextOn: LocalDate?,
    val remainingDays: Int?,
    val usedDaysPercent: Int?,
    /** When the km criterion should come, at the current pace (G12). */
    val estimatedKmOn: LocalDate?,
)

data class LastOilChange(val maintenanceId: String, val performedAt: Instant, val km: Long, val oilType: String?)

data class VehicleStats(
    val lastFueling: LastFueling?,
    val lastConsumptionKmL: Double?,
    val avgConsumptionKmL: Double?,
    val recentConsumption: List<ConsumptionPoint>,
    val paceKmPerDay: Double?,
    val monthKm: Long,
    val monthSpentCents: Long,
    val monthFuelingsCount: Int,
    val monthMaintenancesCount: Int,
)

data class LastFueling(
    val id: String,
    val fueledAt: Instant,
    val volumeMl: Long,
    val totalCents: Long,
    val pricePerLiter: Double,
    val fullTank: Boolean,
)

data class ConsumptionPoint(val fuelingId: String, val fueledAt: Instant, val kmL: Double)

data class OdometerReading(
    val id: String,
    val km: Long,
    val recordedAt: Instant,
    val source: ReadingSource,
    val sourceId: String?,
)

data class Fueling(
    val id: String,
    val vehicle: VehicleRef,
    val fueledAt: Instant,
    val km: Long,
    val fuelType: FuelType,
    val volumeMl: Long,
    val totalCents: Long,
    val pricePerLiter: Double,
    val fullTank: Boolean,
    /** The consumption of the cycle this fueling closes (G8), or null. */
    val consumptionKmL: Double?,
    val notes: String?,
)

data class WorkshopRef(val id: String, val name: String)

data class MaintenanceItem(
    val id: String,
    val description: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val totalCents: Long,
)

data class Maintenance(
    val id: String,
    val vehicle: VehicleRef,
    val kind: MaintenanceKind,
    val performedAt: Instant,
    val km: Long,
    val workshop: WorkshopRef?,
    val laborCostCents: Long,
    val partsCents: Long,
    val totalCents: Long,
    val notes: String?,
    val oilType: String?,
    val nextOilChangeKm: Long?,
    val nextOilChangeOn: LocalDate?,
    val items: List<MaintenanceItem>,
)

data class Workshop(
    val id: String,
    val name: String,
    val notes: String?,
    val maintenancesCount: Int,
    val lastMaintenanceAt: Instant?,
)

/** The next oil change the server suggests for a km and a date (G5), and the oils used before (G14). */
data class OilChangeSuggestion(
    val nextKm: Long?,
    val nextOn: LocalDate?,
    val intervalKm: Long?,
    val intervalMonths: Int?,
    val paceKmPerDay: Double?,
    val estimatedKmOn: LocalDate?,
    val previousOilTypes: List<String>,
)

// What the forms send. Nullable fields are "not set"; on an edit they clear the stored value.

data class VehicleInput(
    val kind: VehicleKind,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    val fuelType: FuelType?,
    val currentKm: Long,
    val oilIntervalKm: Long?,
    val oilIntervalMonths: Int?,
    val notes: String?,
)

data class FuelingInput(
    val fueledAt: Instant,
    val km: Long,
    val fuelType: FuelType,
    val volumeMl: Long,
    val totalCents: Long,
    val fullTank: Boolean,
    val notes: String?,
)

data class ItemInput(val description: String, val quantity: Int, val unitPriceCents: Long)

/**
 * A maintenance or an oil change. The workshop is an existing one ([workshopId]) or a name that the
 * server creates when new (G10). [nextOilChangeKm] and [nextOilChangeOn] only for oil changes:
 * null means "no forecast".
 */
data class MaintenanceInput(
    val kind: MaintenanceKind,
    val performedAt: Instant,
    val km: Long,
    val workshopId: String?,
    val workshopName: String?,
    val laborCostCents: Long,
    val notes: String?,
    val items: List<ItemInput>,
    val oilType: String? = null,
    val nextOilChangeKm: Long? = null,
    val nextOilChangeOn: LocalDate? = null,
)
