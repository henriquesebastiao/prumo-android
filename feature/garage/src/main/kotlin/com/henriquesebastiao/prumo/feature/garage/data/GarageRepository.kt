package com.henriquesebastiao.prumo.feature.garage.data

import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.FuelingInput
import com.henriquesebastiao.prumo.feature.garage.model.GARAGE_MODULE_ID
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OdometerReading
import com.henriquesebastiao.prumo.feature.garage.model.OilChangeSuggestion
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.VehicleInput
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** A page of a list (`{items, total, limit, offset}`); [hasMore] while the server has more. */
data class Page<T>(val items: List<T>, val total: Int) {
    val hasMore: Boolean get() = items.size < total

    operator fun plus(next: Page<T>) = Page(items + next.items, next.total)
}

/**
 * Veículos on the server. Every write tells [changes] (and the Home, through [DataChanges]), so open
 * screens load again; nothing is computed here (D02).
 */
@Suppress("TooManyFunctions")
interface GarageRepository {
    /** Emits after each successful write of Veículos. */
    val changes: Flow<Unit>

    /** The vehicle last used in a form during this run of the app (C9, D15), or null. */
    var lastVehicleId: String?

    suspend fun vehicles(archived: Boolean = false): Outcome<List<Vehicle>>

    suspend fun vehicle(id: String): Outcome<Vehicle>

    suspend fun createVehicle(input: VehicleInput): Outcome<Vehicle>

    suspend fun updateVehicle(id: String, input: VehicleInput): Outcome<Vehicle>

    suspend fun setArchived(id: String, archived: Boolean): Outcome<Vehicle>

    suspend fun deleteVehicle(id: String): Outcome<Unit>

    suspend fun readings(vehicleId: String, offset: Int = 0): Outcome<Page<OdometerReading>>

    suspend fun addReading(vehicleId: String, km: Long): Outcome<OdometerReading>

    suspend fun deleteReading(vehicleId: String, readingId: String): Outcome<Unit>

    /** Maintenances, newest first; [vehicleId] null lists every vehicle's. */
    suspend fun maintenances(
        vehicleId: String? = null,
        kind: MaintenanceKind? = null,
        workshopId: String? = null,
        offset: Int = 0,
    ): Outcome<Page<Maintenance>>

    suspend fun maintenance(vehicleId: String, id: String): Outcome<Maintenance>

    suspend fun createMaintenance(vehicleId: String, input: MaintenanceInput): Outcome<Maintenance>

    suspend fun updateMaintenance(vehicleId: String, id: String, input: MaintenanceInput): Outcome<Maintenance>

    suspend fun deleteMaintenance(vehicleId: String, id: String): Outcome<Unit>

    /** The next oil change for an oil change done at [km] on [on] (G5), and the oils used before. */
    suspend fun oilChangeSuggestion(vehicleId: String, km: Long?, on: LocalDate?): Outcome<OilChangeSuggestion>

    /** Fuelings, newest first; [vehicleId] null lists every vehicle's. */
    suspend fun fuelings(vehicleId: String? = null, offset: Int = 0): Outcome<Page<Fueling>>

    suspend fun fueling(vehicleId: String, id: String): Outcome<Fueling>

    suspend fun createFueling(vehicleId: String, input: FuelingInput): Outcome<Fueling>

    suspend fun updateFueling(vehicleId: String, id: String, input: FuelingInput): Outcome<Fueling>

    suspend fun deleteFueling(vehicleId: String, id: String): Outcome<Unit>

    suspend fun workshops(query: String? = null): Outcome<List<Workshop>>

    suspend fun createWorkshop(name: String, notes: String?): Outcome<Workshop>

    suspend fun updateWorkshop(id: String, name: String, notes: String?): Outcome<Workshop>

    suspend fun deleteWorkshop(id: String): Outcome<Unit>

    companion object {
        /** A page of history (the server's default). */
        const val PAGE_SIZE = 50

        /** Lists the app shows whole (vehicles, workshops): the server's maximum page. */
        const val ALL = 200
    }
}

@Suppress("TooManyFunctions")
@Singleton
class DefaultGarageRepository @Inject constructor(
    private val calls: ServerCalls,
    private val dataChanges: DataChanges,
) : GarageRepository {
    override val changes: Flow<Unit> = dataChanges.changes.filter { it == GARAGE_MODULE_ID }.map { }

    @Volatile
    override var lastVehicleId: String? = null

    private suspend fun <T> api(block: suspend GarageApi.() -> T): Outcome<T> =
        calls.module(GarageApi::class.java, block)

    /** A write: tells the screens and the Home when it worked. */
    private suspend fun <T> write(block: suspend GarageApi.() -> T): Outcome<T> =
        api(block).onOk { dataChanges.changed(GARAGE_MODULE_ID) }

    override suspend fun vehicles(archived: Boolean) =
        api { vehicles(archived, GarageRepository.ALL) }.map { page -> page.items.map { it.toModel() } }

    override suspend fun vehicle(id: String) = api { vehicle(id) }.map { it.toModel() }

    override suspend fun createVehicle(input: VehicleInput) =
        write { createVehicle(body(VehicleWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun updateVehicle(id: String, input: VehicleInput) =
        write { updateVehicle(id, body(VehicleWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun setArchived(id: String, archived: Boolean) =
        write { if (archived) archiveVehicle(id) else unarchiveVehicle(id) }.map { it.toModel() }

    override suspend fun deleteVehicle(id: String) = write { deleteVehicle(id) }

    override suspend fun readings(vehicleId: String, offset: Int) =
        api { odometer(vehicleId, GarageRepository.PAGE_SIZE, offset) }.map { it.toPage { dto -> dto.toModel() } }

    override suspend fun addReading(vehicleId: String, km: Long) =
        write { addReading(vehicleId, body(OdometerReadingCreateDto.serializer(), OdometerReadingCreateDto(km))) }
            .map { it.toModel() }

    override suspend fun deleteReading(vehicleId: String, readingId: String) =
        write { deleteReading(vehicleId, readingId) }

    override suspend fun maintenances(vehicleId: String?, kind: MaintenanceKind?, workshopId: String?, offset: Int) =
        api { maintenances(vehicleId, kind?.wire, workshopId, GarageRepository.PAGE_SIZE, offset) }
            .map { it.toPage { dto -> dto.toModel() } }

    override suspend fun maintenance(vehicleId: String, id: String) =
        api { maintenance(vehicleId, id) }.map { it.toModel() }

    override suspend fun createMaintenance(vehicleId: String, input: MaintenanceInput) =
        write { createMaintenance(vehicleId, body(MaintenanceCreateDto.serializer(), input.toCreateDto())) }
            .map { it.toModel() }
            .onOk { lastVehicleId = vehicleId }

    override suspend fun updateMaintenance(vehicleId: String, id: String, input: MaintenanceInput) =
        write { updateMaintenance(vehicleId, id, body(MaintenanceUpdateDto.serializer(), input.toUpdateDto())) }
            .map { it.toModel() }

    override suspend fun deleteMaintenance(vehicleId: String, id: String) = write { deleteMaintenance(vehicleId, id) }

    override suspend fun oilChangeSuggestion(vehicleId: String, km: Long?, on: LocalDate?) =
        api { oilChangeSuggestion(vehicleId, km, on?.toString()) }.map { it.toModel() }

    override suspend fun fuelings(vehicleId: String?, offset: Int) =
        api { fuelings(vehicleId, GarageRepository.PAGE_SIZE, offset) }.map { it.toPage { dto -> dto.toModel() } }

    override suspend fun fueling(vehicleId: String, id: String) = api { fueling(vehicleId, id) }.map { it.toModel() }

    override suspend fun createFueling(vehicleId: String, input: FuelingInput) =
        write { createFueling(vehicleId, body(FuelingWriteDto.serializer(), input.toDto())) }
            .map { it.toModel() }
            .onOk { lastVehicleId = vehicleId }

    override suspend fun updateFueling(vehicleId: String, id: String, input: FuelingInput) =
        write { updateFueling(vehicleId, id, body(FuelingWriteDto.serializer(), input.toDto())) }.map { it.toModel() }

    override suspend fun deleteFueling(vehicleId: String, id: String) = write { deleteFueling(vehicleId, id) }

    override suspend fun workshops(query: String?) =
        api { workshops(query?.takeIf { it.isNotBlank() }, GarageRepository.ALL) }
            .map { page -> page.items.map { it.toModel() } }

    override suspend fun createWorkshop(name: String, notes: String?) =
        write { createWorkshop(body(WorkshopWriteDto.serializer(), WorkshopWriteDto(name, notes))) }
            .map { it.toModel() }

    override suspend fun updateWorkshop(id: String, name: String, notes: String?) =
        write { updateWorkshop(id, body(WorkshopWriteDto.serializer(), WorkshopWriteDto(name, notes))) }
            .map { it.toModel() }

    override suspend fun deleteWorkshop(id: String) = write { deleteWorkshop(id) }
}

private fun <D, T> PageDto<D>.toPage(transform: (D) -> T) = Page(items.map(transform), total)
