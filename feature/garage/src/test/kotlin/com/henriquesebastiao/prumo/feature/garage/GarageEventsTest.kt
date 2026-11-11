package com.henriquesebastiao.prumo.feature.garage

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.network.NetworkJson
import com.henriquesebastiao.prumo.core.network.dto.DashboardDto
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.core.network.toModel
import com.henriquesebastiao.prumo.feature.garage.ui.MaintenanceDetail
import com.henriquesebastiao.prumo.feature.garage.ui.VehicleDetail
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** How the recorded items of Veículos (fixtures/dashboard.json, the e2e server) read on the Home. */
@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class GarageEventsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun honda() = Fixtures.idOf("garage/vehicles.json", "name", "Honda CG 160 Fan")

    private val dashboard: Dashboard =
        NetworkJson.decodeFromString(DashboardDto.serializer(), Fixtures.read("dashboard.json")).toModel()

    // 17/11/2026 in São Paulo, the e2e server's frozen clock.
    private val context = HomeContext("BRL", LocalDate.of(2026, 11, 17), ZoneId.of("America/Sao_Paulo"))

    private fun HomeEvent.plain() = copy(value = value?.replace(' ', ' '))

    @Test
    fun theOilChangesReadByPaceOrByDate() {
        var events: List<HomeEvent?> = emptyList()
        compose.setContent { events = dashboard.upcoming.filter { it.module == "garage" }.map { garageUpcoming(it) } }
        compose.waitForIdle()

        val honda = events[0]!!
        assertEquals("Troca de óleo · Honda CG 160 Fan", honda.title)
        assertEquals("Estimativa pelo ritmo de 30 km/dia", honda.subtitle)
        assertEquals(VehicleDetail(honda()), honda.target)
        assertEquals("Vence por data (12 meses)", events[1]!!.subtitle)
    }

    @Test
    fun theRecordsReadWithWhenTheyHappenedInTheUsersZone() {
        var events: List<HomeEvent?> = emptyList()
        compose.setContent {
            events = dashboard.activity.filter { it.module == "garage" }.map { garageActivity(it, context) }
        }
        compose.waitForIdle()

        // The latest ten items of every module: Estudos's sessions leave two of Veículos.
        val (odometer, fueling) = events.map { it!!.plain() }
        assertEquals("Km atualizado · Honda CG 160 Fan", odometer.title)
        // 10:42 UTC is 07:42 in São Paulo.
        assertEquals("Hoje, 07:42", odometer.subtitle)
        assertEquals("18.420 km", odometer.value)
        assertEquals("Abastecimento · Honda CG 160 Fan", fueling.title)
        assertEquals("Ontem, 18:10 · 7,3 L", fueling.subtitle)
        assertEquals("R$ 46,36", fueling.value)
        assertEquals(VehicleDetail(honda()), fueling.target)
    }

    @Test
    fun anOilChangeDoneOpensItsMaintenanceAndUnknownKindsAreSkipped() {
        val maintenance = dashboard.activity.first { it.kind == "garage.fueling" }.copy(
            kind = "garage.maintenance",
            entityId = "m1",
            data = mapOf(
                "vehicle_id" to kotlinx.serialization.json.JsonPrimitive("v1"),
                "maintenance_kind" to kotlinx.serialization.json.JsonPrimitive("oil_change"),
                "total_cents" to kotlinx.serialization.json.JsonPrimitive(8990),
                "workshop" to kotlinx.serialization.json.JsonPrimitive("Moto Peças Silva"),
            ),
        )
        var event: HomeEvent? = null
        var unknown: HomeEvent? = HomeEvent("")
        compose.setContent {
            event = garageActivity(maintenance, context)
            unknown = garageActivity(maintenance.copy(kind = "garage.tire_change"), context)
        }
        compose.waitForIdle()

        assertEquals("Troca de óleo · Honda CG 160 Fan", event!!.title)
        assertEquals("Ontem, 18:10 · Moto Peças Silva", event!!.subtitle)
        assertEquals(MaintenanceDetail("v1", "m1"), event!!.target)
        assertNull(unknown)
    }
}
