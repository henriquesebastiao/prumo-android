package com.henriquesebastiao.prumo.feature.garage.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.garage.data.GarageRepository
import com.henriquesebastiao.prumo.feature.garage.data.Page
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.FuelingInput
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OdometerReading
import com.henriquesebastiao.prumo.feature.garage.model.OilChangeSuggestion
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.VehicleInput
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Veículos in memory, with the design boards' data. [failure] makes the next writes fail; what the
 * view models sent is kept in [sent].
 */
@Suppress("TooManyFunctions")
class FakeGarageRepository(
    var vehicles: List<Vehicle> = GaragePreview.vehicles,
    var maintenances: List<Maintenance> = listOf(GaragePreview.oilChange, GaragePreview.maintenance),
    var fuelings: List<Fueling> = GaragePreview.fuelings,
    var readings: List<OdometerReading> = GaragePreview.readings,
    var workshops: List<Workshop> = GaragePreview.workshops,
    var suggestion: OilChangeSuggestion = OilChangeSuggestion(
        nextKm = 19_420,
        nextOn = LocalDate.of(2027, 2, 17),
        intervalKm = 1_000,
        intervalMonths = 3,
        paceKmPerDay = 29.7,
        estimatedKmOn = null,
        previousOilTypes = listOf("4T 20W-50 semissintético"),
    ),
) : GarageRepository {
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val changes = mutableChanges

    /** Pulls to refresh. */
    var expired = 0

    override fun expireCache() {
        expired++
    }

    override var lastVehicleId: String? = null

    /** The error of the next writes; null makes them work. */
    var failure: AppError? = null

    /** The error of the reads; null makes them work. */
    var readFailure: AppError? = null

    val sent = mutableListOf<Any>()
    val suggestionsAsked = mutableListOf<Pair<Long?, LocalDate?>>()

    private fun <T> read(value: () -> T): Outcome<T> = readFailure?.let { Outcome.Failed(it) } ?: Outcome.Ok(value())

    private fun <T> write(body: Any, value: () -> T): Outcome<T> {
        failure?.let { return Outcome.Failed(it) }
        sent += body
        mutableChanges.tryEmit(Unit)
        return Outcome.Ok(value())
    }

    private fun <T> List<T>.page(offset: Int) = Page(drop(offset).take(GarageRepository.PAGE_SIZE), size)

    override suspend fun vehicles(archived: Boolean) = read { vehicles.filter { it.archived == archived } }

    override suspend fun vehicle(id: String) = read { vehicles.first { it.id == id } }

    override suspend fun createVehicle(input: VehicleInput) = write(input) { vehicles.first() }

    override suspend fun updateVehicle(id: String, input: VehicleInput) = write(input) {
        vehicles.first { it.id == id }
    }

    override suspend fun setArchived(id: String, archived: Boolean) = write(id to archived) {
        vehicles.first { it.id == id }.copy(archivedAt = if (archived) Instant.EPOCH else null)
    }

    override suspend fun deleteVehicle(id: String) = write("delete $id") { }

    override suspend fun readings(vehicleId: String, offset: Int) = read { readings.page(offset) }

    override suspend fun addReading(vehicleId: String, km: Long) = write(vehicleId to km) {
        OdometerReading("new", km, Instant.EPOCH, ReadingSource.Manual, null)
    }

    override suspend fun deleteReading(vehicleId: String, readingId: String) = write("delete $readingId") { }

    override suspend fun maintenances(vehicleId: String?, kind: MaintenanceKind?, workshopId: String?, offset: Int) =
        read { maintenances.filter { kind == null || it.kind == kind }.page(offset) }

    override suspend fun maintenance(vehicleId: String, id: String) = read { maintenances.first { it.id == id } }

    override suspend fun createMaintenance(vehicleId: String, input: MaintenanceInput) =
        write(input) { maintenances.first() }

    override suspend fun updateMaintenance(vehicleId: String, id: String, input: MaintenanceInput) =
        write(input) { maintenances.first { it.id == id } }

    override suspend fun deleteMaintenance(vehicleId: String, id: String) = write("delete $id") { }

    override suspend fun oilChangeSuggestion(
        vehicleId: String,
        km: Long?,
        on: LocalDate?,
    ): Outcome<OilChangeSuggestion> {
        suggestionsAsked += km to on
        return read { suggestion }
    }

    override suspend fun fuelings(vehicleId: String?, offset: Int) = read { fuelings.page(offset) }

    override suspend fun fueling(vehicleId: String, id: String) = read { fuelings.first { it.id == id } }

    override suspend fun createFueling(vehicleId: String, input: FuelingInput) = write(input) { fuelings.first() }

    override suspend fun updateFueling(vehicleId: String, id: String, input: FuelingInput) =
        write(input) { fuelings.first { it.id == id } }

    override suspend fun deleteFueling(vehicleId: String, id: String) = write("delete $id") { }

    override suspend fun workshops(query: String?) = read { workshops }

    override suspend fun createWorkshop(name: String, notes: String?) =
        write(name) { Workshop("new", name, notes, 0, null) }

    override suspend fun updateWorkshop(id: String, name: String, notes: String?) =
        write(name) { workshops.first { it.id == id }.copy(name = name) }

    override suspend fun deleteWorkshop(id: String) = write("delete $id") { }
}
