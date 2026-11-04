package com.henriquesebastiao.prumo.feature.garage.data

import com.henriquesebastiao.prumo.core.model.Wire
import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OilStatus
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hand-written DTOs of Veículos against the server's openapi.json (D04). */
class GarageContractTest {
    @Test
    fun theReadDtosFollowTheContract() {
        OpenApiContract.assertReads(VehicleDto.serializer().descriptor, "VehicleRead")
        OpenApiContract.assertReads(OilChangeDto.serializer().descriptor, "OilChangeRead")
        OpenApiContract.assertReads(LastOilChangeDto.serializer().descriptor, "LastOilChangeRead")
        OpenApiContract.assertReads(VehicleStatsDto.serializer().descriptor, "VehicleStats")
        OpenApiContract.assertReads(LastFuelingDto.serializer().descriptor, "LastFuelingRead")
        OpenApiContract.assertReads(ConsumptionPointDto.serializer().descriptor, "ConsumptionPoint")
        OpenApiContract.assertReads(OdometerReadingDto.serializer().descriptor, "OdometerReadingRead")
        OpenApiContract.assertReads(FuelingDto.serializer().descriptor, "FuelingRead")
        OpenApiContract.assertReads(WorkshopRefDto.serializer().descriptor, "WorkshopRef")
        OpenApiContract.assertReads(MaintenanceItemDto.serializer().descriptor, "MaintenanceItemRead")
        OpenApiContract.assertReads(MaintenanceDto.serializer().descriptor, "MaintenanceRead")
        OpenApiContract.assertReads(OilChangeSuggestionDto.serializer().descriptor, "OilChangeSuggestion")
        OpenApiContract.assertReads(WorkshopDto.serializer().descriptor, "WorkshopRead")
    }

    @Test
    fun theWriteDtosFollowTheContract() {
        OpenApiContract.assertWrites(VehicleWriteDto.serializer().descriptor, "VehicleCreate")
        OpenApiContract.assertWrites(VehicleWriteDto.serializer().descriptor, "VehicleUpdate")
        OpenApiContract.assertWrites(OdometerReadingCreateDto.serializer().descriptor, "OdometerReadingCreate")
        OpenApiContract.assertWrites(FuelingWriteDto.serializer().descriptor, "FuelingCreate")
        OpenApiContract.assertWrites(FuelingWriteDto.serializer().descriptor, "FuelingUpdate")
        OpenApiContract.assertWrites(MaintenanceItemInDto.serializer().descriptor, "MaintenanceItemIn")
        OpenApiContract.assertWrites(MaintenanceCreateDto.serializer().descriptor, "MaintenanceCreate")
        OpenApiContract.assertWrites(MaintenanceUpdateDto.serializer().descriptor, "MaintenanceUpdate")
        OpenApiContract.assertWrites(WorkshopWriteDto.serializer().descriptor, "WorkshopCreate")
        OpenApiContract.assertWrites(WorkshopWriteDto.serializer().descriptor, "WorkshopUpdate")
    }

    @Test
    fun theEnumsAreTheServers() {
        assertEquals(OpenApiContract.enumValues("VehicleKind"), wires<VehicleKind>())
        assertEquals(OpenApiContract.enumValues("FuelType"), wires<FuelType>())
        assertEquals(OpenApiContract.enumValues("MaintenanceKind"), wires<MaintenanceKind>())
        assertEquals(OpenApiContract.enumValues("ReadingSource"), wires<ReadingSource>())
        assertEquals(OpenApiContract.enumValues("OilChangeRead", "status"), wires<OilStatus>())
    }

    private inline fun <reified T> wires(): Set<String> where T : Enum<T>, T : Wire =
        enumValues<T>().map { it.wire }.toSet()
}
