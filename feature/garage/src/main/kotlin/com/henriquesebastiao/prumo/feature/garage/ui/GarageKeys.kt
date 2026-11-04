package com.henriquesebastiao.prumo.feature.garage.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// The destinations of Veículos. Sheets (km, fueling) open over the current screen, not as destinations.

/** The list of vehicles: where Veículos opens. */
@Serializable
data object GarageHome : NavKey

/** The archived vehicles. */
@Serializable
data object ArchivedVehicles : NavKey

@Serializable
data class VehicleDetail(val vehicleId: String) : NavKey

/** A new vehicle ([vehicleId] null) or the edit of one. */
@Serializable
data class VehicleEditor(val vehicleId: String? = null) : NavKey

@Serializable
data class MaintenanceDetail(val vehicleId: String, val maintenanceId: String) : NavKey

/**
 * A new maintenance or oil change ([oilChange]), for [vehicleId] or a vehicle to choose, or the
 * edit of [maintenanceId].
 */
@Serializable
data class MaintenanceEditor(
    val vehicleId: String? = null,
    val maintenanceId: String? = null,
    val oilChange: Boolean = false,
) : NavKey

@Serializable
data object Workshops : NavKey

/** A list of every vehicle's records, newest first. */
@Serializable
data class GarageRecords(val list: RecordList) : NavKey

@Serializable
enum class RecordList { Fuelings, Maintenances, OilChanges }
