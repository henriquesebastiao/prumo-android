package com.henriquesebastiao.prumo.feature.garage.data

import app.cash.turbine.test
import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.ActiveSession
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.SessionGateway
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.feature.garage.model.FuelType
import com.henriquesebastiao.prumo.feature.garage.model.FuelingInput
import com.henriquesebastiao.prumo.feature.garage.model.ItemInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceInput
import com.henriquesebastiao.prumo.feature.garage.model.MaintenanceKind
import com.henriquesebastiao.prumo.feature.garage.model.VehicleInput
import com.henriquesebastiao.prumo.feature.garage.model.VehicleKind
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GarageRepositoryTest {
    private val server = MockWebServer()
    private val changes = DataChanges()
    private lateinit var repository: DefaultGarageRepository

    private val cg = "019a9bfc-628e-753e-9ac2-b5793e4d7003"

    @Before
    fun setUp() {
        server.start()
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        repository = DefaultGarageRepository(ServerCalls(ApiFactory(OkHttpClient()), gateway), changes)
    }

    @After
    fun tearDown() = server.close()

    private fun json(status: Int, body: String) = MockResponse.Builder()
        .code(status)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun sentBody(): JsonObject {
        val request = server.takeRequest()
        return Json.parseToJsonElement(request.body!!.utf8()).jsonObject
    }

    @Test
    fun theVehiclesAreAskedWholeAndWithoutTheArchived() = runTest {
        server.enqueue(json(200, Fixtures.read("garage/vehicles.json")))

        val vehicles = (repository.vehicles() as Outcome.Ok).value

        assertEquals(2, vehicles.size)
        val url = server.takeRequest().url
        assertEquals("/api/v1/garage/vehicles", url.encodedPath)
        assertEquals("false", url.queryParameter("archived"))
        assertEquals("200", url.queryParameter("limit"))
    }

    @Test
    fun anEditSendsNullsToClearFields() = runTest {
        server.enqueue(json(200, Fixtures.read("garage/vehicle.json")))
        val input = VehicleInput(
            kind = VehicleKind.Motorcycle,
            name = "CG",
            brand = "Honda",
            model = "CG 160 Fan",
            year = 2023,
            fuelType = null,
            currentKm = 18_420,
            oilIntervalKm = 1_000,
            oilIntervalMonths = null,
            notes = null,
        )

        repository.updateVehicle(cg, input)

        val sent = sentBody()
        assertEquals(JsonNull, sent["fuel_type"])
        assertEquals(JsonNull, sent["oil_interval_months"])
        assertEquals(JsonNull, sent["notes"])
        assertEquals("motorcycle", sent["kind"]!!.jsonPrimitive.content)
        assertEquals("1000", sent["oil_interval_km"]!!.jsonPrimitive.content)
    }

    @Test
    fun aMaintenanceLeavesTheOilFieldsOutOfAGeneralOne() = runTest {
        server.enqueue(json(201, Fixtures.read("garage/maintenance.json")))
        val input = MaintenanceInput(
            kind = MaintenanceKind.General,
            performedAt = Instant.parse("2026-11-17T17:30:00Z"),
            km = 18_420,
            workshopId = null,
            workshopName = "Moto Peças Rocha",
            laborCostCents = 2_000,
            notes = null,
            items = listOf(ItemInput("Pastilha de freio", 2, 4_500)),
            oilType = "4T 20W-50",
            nextOilChangeKm = 19_420,
            nextOilChangeOn = LocalDate.parse("2027-02-17"),
        )

        repository.createMaintenance(cg, input)

        val sent = sentBody()
        assertEquals("general", sent["kind"]!!.jsonPrimitive.content)
        assertEquals("2026-11-17T17:30:00Z", sent["performed_at"]!!.jsonPrimitive.content)
        assertEquals("Moto Peças Rocha", sent["workshop_name"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["oil_type"])
        assertEquals(JsonNull, sent["next_oil_change_km"])
        val item = sent["items"]!!.jsonArray.single().jsonObject
        assertEquals("2", item["quantity"]!!.jsonPrimitive.content)
        assertEquals("4500", item["unit_price_cents"]!!.jsonPrimitive.content)
        assertEquals(cg, repository.lastVehicleId)
    }

    @Test
    fun anOilChangeWithoutForecastSendsNulls() = runTest {
        server.enqueue(json(201, Fixtures.read("garage/maintenance.json")))
        val input = MaintenanceInput(
            kind = MaintenanceKind.OilChange,
            performedAt = Instant.parse("2026-11-17T17:30:00Z"),
            km = 18_420,
            workshopId = "019a9bfc-0000-7000-8000-000000000001",
            workshopName = "Moto Peças Rocha",
            laborCostCents = 0,
            notes = null,
            items = emptyList(),
            oilType = "4T 20W-50 semissintético",
            nextOilChangeKm = null,
            nextOilChangeOn = LocalDate.parse("2027-02-17"),
        )

        repository.createMaintenance(cg, input)

        val sent = sentBody()
        assertEquals("oil_change", sent["kind"]!!.jsonPrimitive.content)
        assertTrue("next_oil_change_km" in sent)
        assertEquals(JsonNull, sent["next_oil_change_km"])
        assertEquals("2027-02-17", sent["next_oil_change_on"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["workshop_name"])
    }

    @Test
    fun aRefusedReadingKeepsTheServersDetails() = runTest {
        server.enqueue(json(422, Fixtures.read("garage/error_odometer_inconsistent.json")))

        val error = (repository.addReading(cg, 1) as Outcome.Failed).error as AppError.Api

        assertEquals("garage.odometer_inconsistent", error.code)
        assertEquals("18420", error.details["km"])
        assertEquals("""{"km":1}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun aWriteThatWorkedTellsTheScreens() = runTest {
        repository.changes.test {
            server.enqueue(json(201, Fixtures.read("garage/fuelings.json").let(::firstItem)))
            server.enqueue(json(422, Fixtures.read("garage/error_odometer_inconsistent.json")))
            val input = FuelingInput(
                fueledAt = Instant.parse("2026-11-17T17:30:00Z"),
                km = 18_512,
                fuelType = FuelType.Gasoline,
                volumeMl = 7_300,
                totalCents = 4_636,
                fullTank = true,
                notes = null,
            )

            assertTrue(repository.createFueling(cg, input) is Outcome.Ok)
            awaitItem()
            assertFalse(repository.addReading(cg, 1) is Outcome.Ok)
            expectNoEvents()
        }
    }

    @Test
    fun theHistoryComesInPages() = runTest {
        server.enqueue(json(200, Fixtures.read("garage/odometer.json")))

        val page = (repository.readings(cg) as Outcome.Ok).value

        assertEquals(14, page.total)
        assertFalse(page.hasMore)
        val url = server.takeRequest().url
        assertEquals("/api/v1/garage/vehicles/$cg/odometer", url.encodedPath)
        assertEquals("0", url.queryParameter("offset"))
    }

    private fun firstItem(page: String): String =
        Json.parseToJsonElement(page).jsonObject["items"]!!.jsonArray.first().toString()
}
