package com.henriquesebastiao.prumo.feature.reading

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
import com.henriquesebastiao.prumo.feature.reading.ui.BookDetailKey
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** How the recorded items of Livros (fixtures/dashboard.json, the e2e server) read on the Home. */
@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class ReadingEventsTest {
    @get:Rule
    val compose = createComposeRule()

    private val dashboard: Dashboard =
        NetworkJson.decodeFromString(DashboardDto.serializer(), Fixtures.read("dashboard.json")).toModel()

    private val context = HomeContext("BRL", LocalDate.of(2026, 11, 17), ZoneId.of("America/Sao_Paulo"))

    @Test
    fun progressAndNotesOpenTheBook() {
        val progress = dashboard.activity.first { it.kind == "reading.progress" }
        val finished = progress.copy(
            data = mapOf(
                "page" to JsonPrimitive(425),
                "pages" to JsonPrimitive(0),
                "total_pages" to JsonPrimitive(425),
                "status" to JsonPrimitive("read"),
            ),
        )
        var events: List<HomeEvent?> = emptyList()
        compose.setContent {
            events =
                (dashboard.activity.filter { it.module == "reading" } + finished + progress.copy(kind = "reading.x"))
                    .map { readingActivity(it, context) }
        }
        compose.waitForIdle()

        val note = events[0]
        val read = events[1]
        val done = events[2]
        val unknown = events[3]
        assertEquals("Anotação · Código Limpo", note!!.title)
        // 01:10 UTC on the 16th is still the 15th in São Paulo.
        assertEquals(true, note.subtitle!!.startsWith("15/11, 22:10 · Capítulo sobre tratamento de erros"))
        assertEquals(BookDetailKey("01a17a8c-a4c0-7193-ae39-2e9431a570a6"), note.target)
        assertEquals("Leitura · Código Limpo", read!!.title)
        assertEquals("15/11, 22:05 · até a página 212", read.subtitle)
        assertEquals("+14 p.", read.value)
        assertEquals("Leitura concluída · Código Limpo", done!!.title)
        assertNull(done.value)
        assertNull(unknown)
    }
}
