package com.henriquesebastiao.prumo.feature.studies

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
import com.henriquesebastiao.prumo.feature.studies.ui.SubjectDetailKey
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** How the recorded sessions of Estudos (fixtures/dashboard.json, the e2e server) read on the Home. */
@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class StudiesEventsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dashboard: Dashboard =
        NetworkJson.decodeFromString(DashboardDto.serializer(), Fixtures.read("dashboard.json")).toModel()

    private val context = HomeContext("BRL", LocalDate.of(2026, 11, 17), ZoneId.of("America/Sao_Paulo"))

    @Test
    fun aSessionOpensItsSubject() {
        val session = dashboard.activity.first { it.kind == "studies.session" }
        val long = session.copy(data = session.data + ("duration_minutes" to JsonPrimitive(90)))
        val withoutTopic = session.copy(data = session.data - "topic")
        var events: List<HomeEvent?> = emptyList()
        compose.setContent {
            events = listOf(session, long, withoutTopic, session.copy(kind = "studies.exam"))
                .map { studiesActivity(it, context) }
        }
        compose.waitForIdle()

        val first = events[0]
        val second = events[1]
        val third = events[2]
        val unknown = events[3]
        assertEquals("Estudo · Inglês", first!!.title)
        // 10:05 UTC is 07:05 in São Paulo.
        assertEquals("Hoje, 07:05 · Listening", first.subtitle)
        assertEquals("30\u00A0min", first.value)
        assertEquals(SubjectDetailKey(session.entityId!!), first.target)
        assertEquals("1h\u00A030", second!!.value)
        assertEquals("Hoje, 07:05", third!!.subtitle)
        assertNull(unknown)
    }
}
