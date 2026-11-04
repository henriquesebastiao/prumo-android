package com.henriquesebastiao.prumo.feature.garage.data

import com.henriquesebastiao.prumo.core.network.NetworkJson
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OilCriterion
import com.henriquesebastiao.prumo.feature.garage.model.OilStatus
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real responses of the server (scripts/record-fixtures.py, from e2e_server.py) read as the app's models. */
class GarageFixturesTest {
    private fun <T> read(name: String, serializer: KSerializer<T>): T =
        NetworkJson.decodeFromString(serializer, Fixtures.read("garage/$name"))

    @Test
    fun theVehicleHasItsOilChangeAndStats() {
        val vehicle = read("vehicle.json", VehicleDto.serializer()).toModel()

        assertEquals(VehicleKind.Motorcycle, vehicle.kind)
        assertEquals("Honda", vehicle.brand)
        assertEquals(18_420L, vehicle.currentKm)
        assertEquals(Instant.parse("2026-11-17T10:42:00Z"), vehicle.currentKmAt)
        assertEquals(FuelType.Gasoline, vehicle.fuelType)
        val oil = vehicle.oilChange!!
        assertEquals(OilStatus.Ok, oil.status)
        assertEquals(OilCriterion.Km, oil.primary)
        assertEquals(220L, oil.remainingKm)
        assertEquals(78, oil.usedKmPercent)
        assertEquals(LocalDate.parse("2027-01-21"), oil.nextOn)
        assertEquals(65, oil.remainingDays)
        assertEquals(LocalDate.parse("2026-11-25"), oil.estimatedKmOn)
        assertEquals("4T 20W-50 semissintético", oil.last!!.oilType)
        assertEquals(40.3, vehicle.stats.avgConsumptionKmL!!, 0.001)
        assertEquals(4_636L, vehicle.stats.lastFueling!!.totalCents)
        assertEquals(9_486L, vehicle.stats.monthSpentCents)
        assertEquals(2, vehicle.stats.monthFuelingsCount)
        assertNull(vehicle.archivedAt)
    }

    @Test
    fun theListsDecode() {
        val vehicles = read("vehicles.json", PageDto.serializer(VehicleDto.serializer()))
        assertEquals(listOf(VehicleKind.Motorcycle, VehicleKind.Car), vehicles.items.map { it.toModel().kind })

        val readings = read("odometer.json", PageDto.serializer(OdometerReadingDto.serializer()))
        assertEquals(14, readings.total)
        assertTrue(readings.items.map { it.toModel().source }.containsAll(ReadingSource.entries))

        val fuelings = read("fuelings.json", PageDto.serializer(FuelingDto.serializer()))
        assertEquals(6, fuelings.total)
        assertTrue(fuelings.items.all { it.toModel().vehicle.name == "Honda CG 160 Fan" })

        val workshops = read("workshops.json", PageDto.serializer(WorkshopDto.serializer())).items.map { it.toModel() }
        assertEquals("Moto Peças Rocha", workshops.single().name)
        assertEquals(3, workshops.single().maintenancesCount)
    }

    @Test
    fun theMaintenanceHasItsItemsAndTotals() {
        val maintenances = read("maintenances.json", PageDto.serializer(MaintenanceDto.serializer()))
        assertEquals(6, maintenances.total)

        val maintenance = read("maintenance.json", MaintenanceDto.serializer()).toModel()
        assertEquals(MaintenanceKind.OilChange, maintenance.kind)
        assertEquals(17_640L, maintenance.km)
        assertEquals(1_500L, maintenance.laborCostCents)
        assertEquals(3_890L, maintenance.partsCents)
        assertEquals(5_390L, maintenance.totalCents)
        assertEquals(maintenance.partsCents, maintenance.items.sumOf { it.totalCents })
    }

    @Test
    fun theSuggestionListsThePreviousOils() {
        val suggestion = read("oil_suggestion.json", OilChangeSuggestionDto.serializer()).toModel()

        assertEquals(19_420L, suggestion.nextKm)
        assertEquals(LocalDate.parse("2027-02-17"), suggestion.nextOn)
        assertEquals(listOf("4T 20W-50 semissintético", "4T 10W-30 sintético"), suggestion.previousOilTypes)
    }
}
