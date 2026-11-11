package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.SessionClient
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Real responses of the server (the design boards' data) through Retrofit to the models. */
class DashboardTest {
    private val server = MockWebServer()
    private lateinit var api: PrumoApi

    @Before
    fun setUp() {
        server.start()
        api = ApiFactory(OkHttpClient()).system(server.url("/").toString())
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun theHomeKeepsEachSummaryForItsModuleInTheUsersOrder() = runTest {
        enqueue("dashboard.json")
        val dashboard = (apiCall { api.dashboard() } as Outcome.Ok).value.toModel()
        assertEquals(listOf("finance", "garage", "reading", "studies"), dashboard.summaries.map { it.module })
        assertEquals(JsonPrimitive(715_427), dashboard.summaries.first().json["total_balance_cents"])

        val oil = dashboard.upcoming.first { it.kind == "garage.oil_change" }
        assertEquals(LocalDate.of(2026, 11, 25), oil.on)
        assertEquals("Honda CG 160 Fan", oil.subject)
        assertEquals(JsonPrimitive(220), oil.data["remaining_km"])

        // Activity times come with an offset ("2026-11-17T08:10:00-03:00").
        val bakery = dashboard.activity.first { it.subject == "Padaria" }
        assertEquals(Instant.parse("2026-11-17T11:10:00Z"), bakery.occurredAt)
        assertTrue(bakery.data.containsKey("destination_account")) // null values stay as JsonNull
    }

    @Test
    fun aSummaryWithoutModuleIsSkipped() = runTest {
        server.enqueue(json("""{"modules":[{"module":"finance"},{"something":1}],"upcoming":[],"activity":[]}"""))
        val dashboard = (apiCall { api.dashboard() } as Outcome.Ok).value.toModel()
        assertEquals(listOf("finance"), dashboard.summaries.map { it.module })
    }

    @Test
    fun theSystemResponsesMapToModels() = runTest {
        enqueue("preferences.json")
        val prefs = (apiCall { api.preferences() } as Outcome.Ok).value.toModel()
        assertEquals(ThemeMode.System, prefs.theme)
        assertEquals(listOf("finance", "garage", "reading", "studies"), prefs.enabledModules)

        enqueue("sessions.json")
        val session = (apiCall { api.sessions() } as Outcome.Ok).value.items.single().toModel()
        assertEquals(SessionClient.App, session.client)
        assertEquals("Pixel 9", session.deviceName)
        assertTrue(session.current)

        enqueue("modules.json")
        val modules = (apiCall { api.modules() } as Outcome.Ok).value.items.map { it.toModel() }
        assertEquals(listOf(0, 1, 2, 3), modules.map { it.position })

        enqueue("me.json")
        assertEquals("henrique", (apiCall { api.me() } as Outcome.Ok).value.toModel().username)
    }

    @Test
    fun noContentAnswersAreUnit() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertEquals(Outcome.Ok(Unit), apiCall { api.logout() })
    }

    private fun enqueue(fixture: String) = server.enqueue(json(Fixtures.read(fixture)))

    private fun json(body: String) =
        MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()
}
