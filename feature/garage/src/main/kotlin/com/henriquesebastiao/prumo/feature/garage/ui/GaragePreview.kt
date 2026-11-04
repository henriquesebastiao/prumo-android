package com.henriquesebastiao.prumo.feature.garage.ui

import com.henriquesebastiao.prumo.feature.garage.model.ConsumptionPoint
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.Fueling
import com.henriquesebastiao.prumo.feature.garage.model.LastFueling
import com.henriquesebastiao.prumo.feature.garage.model.LastOilChange
import com.henriquesebastiao.prumo.feature.garage.model.Maintenance
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceItem
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.OdometerReading
import com.henriquesebastiao.prumo.feature.garage.model.OilChange
import com.henriquesebastiao.prumo.feature.garage.model.OilCriterion
import com.henriquesebastiao.prumo.feature.garage.model.OilStatus
import com.henriquesebastiao.prumo.feature.garage.model.ReadingSource
import com.henriquesebastiao.prumo.feature.garage.model.Vehicle
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import com.henriquesebastiao.prumo.feature.garage.model.VehicleStats
import com.henriquesebastiao.prumo.feature.garage.model.Workshop
import com.henriquesebastiao.prumo.feature.garage.model.WorkshopRef
import java.time.Instant
import java.time.LocalDate

/** The data of the design boards (e2e_server.py of the server, 17/11/2026 14:30), for previews and tests. */
internal object GaragePreview {
    val context = GarageContext()

    private val stats = VehicleStats(
        lastFueling = LastFueling("f1", Instant.parse("2026-11-01T12:10:00Z"), 7_300, 4_636, 6.351, fullTank = true),
        lastConsumptionKmL = 39.73,
        avgConsumptionKmL = 40.3,
        recentConsumption = listOf(ConsumptionPoint("f1", Instant.parse("2026-11-01T12:10:00Z"), 39.73)),
        paceKmPerDay = 29.7,
        monthKm = 620,
        monthSpentCents = 9_486,
        monthFuelingsCount = 2,
        monthMaintenancesCount = 0,
    )

    val cg = Vehicle(
        id = "019a9bfc-628e-753e-9ac2-b5793e4d7003",
        kind = VehicleKind.Motorcycle,
        name = "Honda CG 160 Fan",
        brand = "Honda",
        model = "CG 160 Fan",
        year = 2023,
        fuelType = FuelType.Gasoline,
        currentKm = 18_420,
        currentKmAt = Instant.parse("2026-11-17T10:42:00Z"),
        oilIntervalKm = 1_000,
        oilIntervalMonths = 3,
        notes = null,
        archivedAt = null,
        oilChange = OilChange(
            status = OilStatus.Ok,
            primary = OilCriterion.Km,
            intervalKm = 1_000,
            intervalMonths = 3,
            last = LastOilChange("m1", Instant.parse("2026-10-21T13:00:00Z"), 17_640, "4T 20W-50 semissintético"),
            nextKm = 18_640,
            remainingKm = 220,
            usedKmPercent = 78,
            nextOn = LocalDate.of(2027, 1, 21),
            remainingDays = 65,
            usedDaysPercent = 29,
            estimatedKmOn = LocalDate.of(2026, 11, 25),
        ),
        stats = stats,
    )

    val strada = cg.copy(
        id = "019a9bfc-628e-753e-9ac2-b5793e4d7004",
        kind = VehicleKind.Car,
        name = "Fiat Strada Freedom",
        brand = "Fiat",
        model = "Strada Freedom",
        year = 2021,
        fuelType = FuelType.Ethanol,
        currentKm = 61_250,
        oilIntervalKm = 10_000,
        oilIntervalMonths = 12,
        oilChange = cg.oilChange!!.copy(
            status = OilStatus.DueSoon,
            primary = OilCriterion.Date,
            remainingKm = 3_100,
            usedKmPercent = 69,
            remainingDays = 18,
            usedDaysPercent = 95,
        ),
    )

    val vehicles = listOf(cg, strada)

    private val workshop = WorkshopRef("w1", "Moto Peças Rocha")

    val oilChange = Maintenance(
        id = "m1",
        vehicle = cg.ref,
        kind = MaintenanceKind.OilChange,
        performedAt = Instant.parse("2026-10-21T13:00:00Z"),
        km = 17_640,
        workshop = workshop,
        laborCostCents = 1_500,
        partsCents = 3_890,
        totalCents = 5_390,
        notes = "Trocar o filtro na próxima.",
        oilType = "4T 20W-50 semissintético",
        nextOilChangeKm = 18_640,
        nextOilChangeOn = LocalDate.of(2027, 1, 21),
        items = listOf(MaintenanceItem("i1", "Óleo 4T 20W-50 semissintético", 1, 3_890, 3_890)),
    )

    val maintenance = Maintenance(
        id = "m2",
        vehicle = cg.ref,
        kind = MaintenanceKind.General,
        performedAt = Instant.parse("2026-09-12T15:20:00Z"),
        km = 16_980,
        workshop = workshop,
        laborCostCents = 4_000,
        partsCents = 9_000,
        totalCents = 13_000,
        notes = null,
        oilType = null,
        nextOilChangeKm = null,
        nextOilChangeOn = null,
        items = listOf(
            MaintenanceItem("i2", "Pastilha de freio dianteira", 2, 3_000, 6_000),
            MaintenanceItem("i3", "Cabo de embreagem", 1, 3_000, 3_000),
        ),
    )

    val fuelings = listOf(
        Fueling(
            "f1",
            cg.ref,
            Instant.parse("2026-11-01T12:10:00Z"),
            18_180,
            FuelType.Gasoline,
            7_300,
            4_636,
            6.351,
            fullTank = true,
            consumptionKmL = 39.73,
            notes = null,
        ),
        Fueling(
            "f2",
            cg.ref,
            Instant.parse("2026-11-12T22:40:00Z"),
            18_390,
            FuelType.GasolineAdditive,
            7_850,
            4_850,
            6.178,
            fullTank = false,
            consumptionKmL = null,
            notes = null,
        ),
    )

    val readings = listOf(
        OdometerReading("r1", 18_420, Instant.parse("2026-11-17T10:42:00Z"), ReadingSource.Manual, null),
        OdometerReading("r2", 18_390, Instant.parse("2026-11-12T22:40:00Z"), ReadingSource.Fueling, "f2"),
        OdometerReading("r3", 17_640, Instant.parse("2026-10-21T13:00:00Z"), ReadingSource.Maintenance, "m1"),
    )

    val workshops = listOf(Workshop("w1", "Moto Peças Rocha", null, 3, Instant.parse("2026-10-21T13:00:00Z")))
}
