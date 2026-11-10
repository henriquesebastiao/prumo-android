package com.henriquesebastiao.prumo.feature.garage.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.FuelingInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.VehicleInput
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GarageViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    // 17/11/2026 at 14:30 in São Paulo, as the design boards.
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)
    private val account = FakeAccountRepository()
    private val repository = FakeGarageRepository()
    private val cg = GaragePreview.cg
    private val strada = GaragePreview.strada

    @Test
    fun pullingAsksTheServerAndLoadsAgain() = runTest {
        val vm = VehicleListViewModel(archived = false, repository, account, clock)
        repository.vehicles = listOf(cg)
        vm.pull()
        assertEquals(1, repository.expired)
        assertEquals(listOf(cg.id), vm.state.value.vehicles.map { it.id })
    }

    @Test
    fun theListLoadsAndReloadsAfterAWrite() = runTest {
        val vm = VehicleListViewModel(archived = false, repository, account, clock)
        assertEquals(listOf(cg.id, strada.id), vm.state.value.vehicles.map { it.id })

        repository.vehicles = listOf(cg)
        repository.addReading(cg.id, 18_500)

        assertEquals(listOf(cg.id), vm.state.value.vehicles.map { it.id })
    }

    @Test
    fun aVehiclesHistoryLoadsEachTabWhenOpened() = runTest {
        val vm = VehicleDetailViewModel(cg.id, repository, account, clock)
        val state = vm.state.value
        assertEquals(cg, state.vehicle)
        assertEquals(listOf(MaintenanceKind.General), state.maintenances.items.map { it.kind })
        assertFalse(state.readings.loaded)

        vm.selectTab(HistoryTab.Km)

        assertEquals(3, vm.state.value.readings.items.size)
    }

    @Test
    fun aRefusedDeleteOfAReadingIsShownAndDeletingTheVehicleClosesIt() = runTest {
        val vm = VehicleDetailViewModel(cg.id, repository, account, clock)
        repository.failure = AppError.Api(422, "garage.last_reading")

        vm.deleteReading("r1")
        assertEquals("garage.last_reading", (vm.state.value.actionError as AppError.Api).code)

        repository.failure = null
        vm.delete()
        assertTrue(vm.state.value.gone)
    }

    @Test
    fun theKmSheetStartsWithTheLastVehicleUsedAndSavesTheReading() = runTest {
        repository.lastVehicleId = strada.id
        val vm = OdometerViewModel(repository, account, clock)
        vm.start(request = 1, vehicleId = null)
        assertEquals(strada.id, vm.state.value.vehicleId)

        vm.onKmChange("61400")
        vm.save()

        assertTrue(vm.state.value.saved)
        assertEquals(strada.id to 61_400L, repository.sent.single())
    }

    @Test
    fun theKmSheetKeepsWhatWasTypedOnTheSameOpening() = runTest {
        val vm = OdometerViewModel(repository, account, clock)
        vm.start(request = 1, vehicleId = cg.id)
        vm.onKmChange("18500")

        vm.start(request = 1, vehicleId = cg.id)
        assertEquals("18500", vm.km)

        vm.start(request = 2, vehicleId = cg.id)
        assertEquals("", vm.km)
    }

    @Test
    fun aNewFuelingStartsWithTheVehiclesFuelKmAndNowInTheUsersZone() = runTest {
        val vm = FuelingViewModel(repository, account, clock)
        vm.start(request = 1, vehicleId = strada.id, fuelingId = null)

        val state = vm.state.value
        assertEquals(FuelType.Ethanol, state.fuelType)
        assertEquals("61250", vm.km)
        assertEquals(LocalDateTime.of(2026, 11, 17, 14, 30), state.fueledAt)
        assertFalse(vm.canSave)

        vm.onTotalChange("4636")
        vm.onLitresChange("7,3")
        vm.save()

        val sent = repository.sent.single() as FuelingInput
        assertEquals(4_636L, sent.totalCents)
        assertEquals(7_300L, sent.volumeMl)
        assertEquals(61_250L, sent.km)
        assertEquals(Instant.parse("2026-11-17T17:30:00Z"), sent.fueledAt)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun aFuelingEditStartsWithItsValues() = runTest {
        val vm = FuelingViewModel(repository, account, clock)
        vm.start(request = 1, vehicleId = cg.id, fuelingId = "f2")

        assertEquals("4850", vm.total)
        assertEquals("7,85", vm.litres)
        assertEquals(FuelType.GasolineAdditive, vm.state.value.fuelType)
        assertFalse(vm.state.value.fullTank)
    }

    @Test
    fun anOilChangeTakesTheServersSuggestionUnlessTypedByHand() = runTest {
        val vm = MaintenanceEditorViewModel(MaintenanceEditor(cg.id, oilChange = true), repository, account, clock)
        assertEquals("19420", vm.fields.nextKm)
        assertEquals(LocalDate.of(2027, 2, 17), vm.state.value.nextOn)
        assertEquals(18_420L to LocalDate.of(2026, 11, 17), repository.suggestionsAsked.last())

        vm.onFieldsChange(vm.fields.copy(nextKm = "19000"))
        repository.suggestion = repository.suggestion.copy(nextKm = 19_600)
        vm.onFieldsChange(vm.fields.copy(km = "18600"))
        advanceTimeBy(1_000)

        assertEquals("19000", vm.fields.nextKm)
        assertEquals(18_600L, repository.suggestionsAsked.last().first)
    }

    @Test
    fun aPartWithAValueNeedsAName() = runTest {
        val vm = MaintenanceEditorViewModel(MaintenanceEditor(cg.id), repository, account, clock)
        val part = vm.fields.parts.single()

        vm.onPartChange(part.copy(price = "4500"))
        assertFalse(vm.canSave)

        vm.onPartChange(part.copy(description = "Pastilha", quantity = "2", price = "4500"))
        vm.onFieldsChange(vm.fields.copy(labor = "2000", workshop = "Moto Peças Rocha"))
        assertTrue(vm.canSave)
        assertEquals(9_000L to 2_000L, vm.fields.previewTotals())
        vm.save()

        val sent = repository.sent.single() as MaintenanceInput
        assertEquals(MaintenanceKind.General, sent.kind)
        assertEquals(2, sent.items.single().quantity)
        assertNull(sent.workshopId)
        assertEquals("Moto Peças Rocha", sent.workshopName)
    }

    @Test
    fun aPickedWorkshopGoesByItsId() = runTest {
        val vm = MaintenanceEditorViewModel(MaintenanceEditor(cg.id), repository, account, clock)

        vm.pickWorkshop(GaragePreview.workshops.single())
        vm.save()

        assertEquals("w1", (repository.sent.single() as MaintenanceInput).workshopId)
    }

    @Test
    fun theVehicleFormSendsWhatWasTyped() = runTest {
        val vm = VehicleEditorViewModel(null, repository, account, clock)
        assertFalse(vm.canSave)

        vm.onKindChange(VehicleKind.Motorcycle)
        vm.onFuelChange(FuelType.Gasoline)
        vm.onFieldsChange(VehicleFields(" CG ", "Honda", "CG 160 Fan", "2023", "18420", "1000", "", ""))
        vm.save()

        val sent = repository.sent.single() as VehicleInput
        assertEquals("CG", sent.name)
        assertEquals(1_000L, sent.oilIntervalKm)
        assertNull(sent.oilIntervalMonths)
        assertNull(sent.notes)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun editingAVehicleStartsWithItsValues() = runTest {
        val vm = VehicleEditorViewModel(cg.id, repository, account, clock)

        assertEquals("Honda CG 160 Fan", vm.fields.name)
        assertEquals("18420", vm.fields.km)
        assertEquals("3", vm.fields.intervalMonths)
        assertEquals(VehicleKind.Motorcycle, vm.state.value.kind)
    }
}
