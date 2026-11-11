package com.henriquesebastiao.prumo.feature.habits.data

import app.cash.turbine.test
import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.ActiveSession
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.SessionGateway
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.core.network.testing.NoCache
import com.henriquesebastiao.prumo.feature.habits.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Hábitos against recorded answers of the server (fixtures/habits/, from the e2e server). */
class HabitsRepositoryTest {
    private val server = MockWebServer()
    private val changes = DataChanges()
    private lateinit var repository: DefaultHabitsRepository

    @Before
    fun setUp() {
        server.start()
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        repository = DefaultHabitsRepository(ServerCalls(ApiFactory(OkHttpClient()), gateway, NoCache), changes)
    }

    @After
    fun tearDown() = server.close()

    private fun json(status: Int, body: String) = MockResponse.Builder()
        .code(status)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun sentBody(): JsonObject = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    private fun habitJson(): String {
        val habit = Json.parseToJsonElement(Fixtures.read("habits/habits.json")).jsonObject["items"]!!
        return (habit as kotlinx.serialization.json.JsonArray)[0].toString()
    }

    @Test
    fun todayComesWithWhatEachHabitAsks() = runTest {
        server.enqueue(json(200, Fixtures.read("habits/today.json")))

        val today = (repository.today() as Outcome.Ok).value

        assertEquals("/api/v1/habits/today", server.takeRequest().url.encodedPath)
        assertEquals(LocalDate.of(2026, 11, 17), today.today)
        assertEquals(4, today.scheduled)
        val water = today.items.first { it.habit.name == "Beber água" }
        assertEquals(Measure.Quantity, water.habit.measure)
        assertEquals(1250, water.value)
        assertEquals(2000, water.habit.targetValue)
        assertEquals("ml", water.habit.unit)
        val gym = today.items.first { it.habit.name == "Academia" }
        assertEquals(1, gym.week?.done)
        assertEquals(3, gym.week?.target)
    }

    @Test
    fun theListComesWholeWithTheDerivedValues() = runTest {
        server.enqueue(json(200, Fixtures.read("habits/habits.json")))

        val habits = (repository.habits(ArchivedFilter.All) as Outcome.Ok).value

        val url = server.takeRequest().url
        assertEquals("all", url.queryParameter("archived"))
        assertEquals("200", url.queryParameter("limit"))
        val bible = habits.first { it.name == "Ler a Bíblia" }
        assertEquals(40, bible.currentStreak)
        assertEquals(100, bible.ratePercent)
        assertEquals(FrequencyKind.Daily, bible.frequency?.kind)
        val anki = habits.first { it.name == "Anki" }
        assertEquals(DayOfWeek.entries.take(5), anki.frequency?.weekdays)
        val gym = habits.first { it.name == "Academia" }
        assertTrue(gym.streakInWeeks)
        assertTrue(habits.first { it.name == "Meditar" }.archived)
        val quit = habits.first { it.kind == HabitKind.Quit }
        assertEquals(8, quit.cleanDays)
        assertEquals(42, quit.bestCleanDays)
        assertNull(quit.ratePercent)
    }

    @Test
    fun aHabitComesWithItsVersionsPausesAndRelapses() = runTest {
        server.enqueue(json(200, Fixtures.read("habits/habit.json")))
        server.enqueue(json(200, Fixtures.read("habits/habit_quit.json")))

        val bible = (repository.habit("h1") as Outcome.Ok).value
        val quit = (repository.habit("h2") as Outcome.Ok).value

        assertEquals("/api/v1/habits/h1", server.takeRequest().url.encodedPath)
        assertEquals(LocalDate.of(2026, 10, 20), bible.pauses.single().startsOn)
        assertEquals(1, bible.frequencies.size)
        assertEquals(3, quit.relapses.size)
        assertEquals(Instant.parse("2026-11-09T02:30:00Z"), quit.relapses.first().occurredAt)
        assertEquals("Domingo entediado.", quit.relapses.first().note)
    }

    @Test
    fun theCalendarAsksTheMonth() = runTest {
        server.enqueue(json(200, Fixtures.read("habits/calendar.json")))

        val month = (repository.calendar("h1", YearMonth.of(2026, 11)) as Outcome.Ok).value

        assertEquals("2026-11", server.takeRequest().url.queryParameter("month"))
        assertNull(month.next)
        assertEquals(YearMonth.of(2026, 10), month.previous)
        assertEquals(DayState.Pending, month.days.first { it.on == month.today }.state)
        assertEquals(DayState.Done, month.days.first { it.on == LocalDate.of(2026, 11, 16) }.state)
    }

    @Test
    fun markingPutsTheDayAndAFutureDayIsRefused() = runTest {
        server.enqueue(json(200, habitJson()))
        server.enqueue(json(422, Fixtures.read("habits/error_future_date.json")))

        changes.changes.test {
            assertTrue(repository.mark("h1", LocalDate.of(2026, 11, 17), 1250) is Outcome.Ok)
            assertEquals("habits", awaitItem())
        }
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/habits/h1/entries/2026-11-17", request.url.encodedPath)
        assertEquals("1250", Json.parseToJsonElement(request.body!!.utf8()).jsonObject["value"]!!.jsonPrimitive.content)

        val refused = repository.mark("h1", LocalDate.of(2026, 11, 18), 1)
        assertEquals("habits.future_date", ((refused as Outcome.Failed).error as AppError.Api).code)
    }

    @Test
    fun unmarkingDeletesTheDay() = runTest {
        server.enqueue(json(200, habitJson()))

        repository.unmark("h1", LocalDate.of(2026, 11, 17))

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/habits/h1/entries/2026-11-17", request.url.encodedPath)
    }

    @Test
    fun aNewHabitSendsOnlyWhatItsKindTakes() = runTest {
        server.enqueue(json(201, habitJson()))
        server.enqueue(json(201, habitJson()))

        repository.createHabit(
            HabitInput(
                name = "Beber água",
                kind = HabitKind.Build,
                color = HabitColor.Cyan,
                icon = "droplet",
                startedOn = LocalDate.of(2026, 11, 17),
                measure = Measure.Quantity,
                targetValue = 2000,
                unit = "ml",
                frequency = FrequencyInput(
                    FrequencyKind.Weekdays,
                    weekdays = listOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
                ),
            ),
        )
        val build = sentBody()
        assertEquals("quantity", build["measure"]!!.jsonPrimitive.content)
        assertEquals("2000", build["target_value"]!!.jsonPrimitive.content)
        val frequency = build["frequency"]!!.jsonObject
        assertEquals("weekdays", frequency["kind"]!!.jsonPrimitive.content)
        assertEquals("[0,4]", frequency["weekdays"].toString())
        assertEquals(JsonNull, frequency["times_per_week"])

        repository.createHabit(
            HabitInput(
                "Doce",
                HabitKind.Quit,
                HabitColor.Red,
                "cake",
                LocalDate.of(2026, 11, 1),
                measure = Measure.Check,
            ),
        )
        val quit = sentBody()
        assertEquals("quit", quit["kind"]!!.jsonPrimitive.content)
        assertFalse("measure" in quit)
        assertFalse("frequency" in quit)
    }

    @Test
    fun aRelapseAndAPauseGoWithTheirDays() = runTest {
        server.enqueue(json(201, Fixtures.read("habits/habit_quit.json")))
        server.enqueue(
            json(
                201,
                """{"id":"p","starts_on":"2026-12-01","ends_on":null,""" +
                    """"created_at":"2026-11-17T17:30:00Z","updated_at":"2026-11-17T17:30:00Z"}""",
            ),
        )

        repository.logRelapse("h2", Instant.parse("2026-11-17T02:00:00Z"), null)
        val relapse = sentBody()
        assertEquals("2026-11-17T02:00:00Z", relapse["occurred_at"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, relapse["note"])

        repository.createPause("h1", LocalDate.of(2026, 12, 1), null)
        val pause = sentBody()
        assertEquals("2026-12-01", pause["starts_on"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, pause["ends_on"])
    }
}
