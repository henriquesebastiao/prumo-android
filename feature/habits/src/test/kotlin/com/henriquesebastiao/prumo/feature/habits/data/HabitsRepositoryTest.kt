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
import com.henriquesebastiao.prumo.feature.habits.model.Cadence
import com.henriquesebastiao.prumo.feature.habits.model.DayState
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Period
import com.henriquesebastiao.prumo.feature.habits.model.Promotion
import com.henriquesebastiao.prumo.feature.habits.model.StreakUnit
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    /** How many times a write asked the widget to show the new day. */
    private var widgetRefreshes = 0
    private lateinit var repository: DefaultHabitsRepository

    @Before
    fun setUp() {
        server.start()
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        repository = DefaultHabitsRepository(
            ServerCalls(ApiFactory(OkHttpClient()), gateway, NoCache),
            changes,
        ) { widgetRefreshes++ }
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
    fun theDayComesGroupedAndOrderedByTheServer() = runTest {
        server.enqueue(json(200, Fixtures.read("habits/day.json")))
        server.enqueue(json(200, Fixtures.read("habits/day_past.json")))

        val day = (repository.day() as Outcome.Ok).value

        val request = server.takeRequest().url
        assertEquals("/api/v1/habits/day", request.encodedPath)
        assertNull(request.queryParameter("on"))
        assertEquals(LocalDate.of(2026, 11, 17), day.on)
        assertTrue(day.isToday)
        assertEquals(LocalTime.of(14, 30), day.now)
        assertEquals(1 to 6, day.done to day.scheduled)
        assertEquals(listOf("Alongar", "Regar as plantas"), day.timeline.map { it.habit.name })
        assertTrue(day.timeline.first().late)
        assertEquals(LocalTime.of(7, 0), day.timeline.first().habit.timeOfDay)
        assertEquals(listOf("Tarefa do mês", "Anki", "Beber água"), day.untimed.map { it.habit.name })
        assertEquals(Promotion.PreferredDay, day.untimed.first().promoted)
        val water = day.untimed.first { it.habit.name == "Beber água" }
        assertEquals(Measure.Quantity, water.habit.measure)
        assertEquals(1250, water.value)
        assertEquals(2000, water.habit.targetValue)
        val gym = day.period.single()
        assertEquals(Period.Week, gym.period?.period)
        assertEquals(1 to 3, gym.period?.done to gym.period?.target)
        assertEquals(6, gym.period?.daysLeft)
        assertEquals(listOf("Ler a Bíblia"), day.doneItems.map { it.habit.name })
        assertEquals(8, day.quit.single().cleanDays)

        val yesterday = (repository.day(LocalDate.of(2026, 11, 16)) as Outcome.Ok).value
        assertEquals("2026-11-16", server.takeRequest().url.queryParameter("on"))
        assertFalse(yesterday.isToday)
        assertNull(yesterday.now)
    }

    @Test
    fun reorderingSendsTheIdsInTheirNewOrder() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        changes.changes.test {
            assertTrue(repository.reorder(listOf("b", "a")) is Outcome.Ok)
            assertEquals("habits", awaitItem())
        }
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/habits/order", request.url.encodedPath)
        assertEquals("""{"ids":["b","a"]}""", request.body!!.utf8())
    }

    @Test
    fun theListComesWholeWithTheDerivedValues() = runTest {
        server.enqueue(json(200, Fixtures.read("habits/habits.json")))

        val habits = (repository.habits(ArchivedFilter.All) as Outcome.Ok).value

        val url = server.takeRequest().url
        assertEquals("all", url.queryParameter("archived"))
        assertEquals("200", url.queryParameter("limit"))
        // In the server's order (H10): with a time first, by time.
        assertEquals(listOf("Ler a Bíblia", "Alongar", "Regar as plantas"), habits.take(3).map { it.name })
        val bible = habits.first { it.name == "Ler a Bíblia" }
        assertEquals(41, bible.currentStreak)
        assertEquals(100, bible.ratePercent)
        assertEquals(LocalTime.of(5, 15), bible.timeOfDay)
        assertEquals(FrequencyKind.Daily, bible.frequency?.rule?.kind)
        assertEquals(Cadence.Daily, bible.frequency?.cadence)
        assertEquals(LocalDate.of(2026, 11, 18), bible.nextDue)
        val anki = habits.first { it.name == "Anki" }
        assertEquals(DayOfWeek.entries.take(5).toSet(), anki.frequency?.rule?.weekdays)
        val gym = habits.first { it.name == "Academia" }
        assertEquals(StreakUnit.Week, gym.streakUnit)
        assertEquals(FrequencyKind.PerPeriod, gym.frequency?.rule?.kind)
        assertEquals(3, gym.frequency?.rule?.times)
        assertEquals(Period.Week, gym.period?.period)
        val sheets = habits.first { it.name == "Trocar roupa de cama" }
        assertEquals(2, sheets.frequency?.rule?.everyNWeeks)
        assertEquals(StreakUnit.Occurrence, sheets.streakUnit)
        assertEquals(LocalDate.of(2026, 11, 21), sheets.nextDue)
        val tank = habits.first { it.name == "Limpar caixa d'água" }
        assertEquals(setOf(5), tank.frequency?.rule?.monthDays)
        assertEquals(3, tank.frequency?.rule?.everyNMonths)
        assertEquals(LocalDate.of(2027, 2, 5), tank.nextDue)
        val review = habits.first { it.name == "Revisão mensal" }
        assertEquals(DayOfWeek.SUNDAY, review.frequency?.rule?.weekday)
        assertEquals(1, review.frequency?.rule?.weekOfMonth)
        val chore = habits.first { it.name == "Tarefa do mês" }
        assertEquals(15, chore.frequency?.rule?.preferredDay)
        assertEquals(Period.Month, chore.frequency?.rule?.period)
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
        // The demo server has today's reading done already.
        assertEquals(DayState.Done, month.days.first { it.on == month.today }.state)
        assertEquals(DayState.Future, month.days.first { it.on == month.today.plusDays(1) }.state)
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
        assertEquals(1, widgetRefreshes)
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
                    weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
                    everyNWeeks = 2,
                ),
                timeOfDay = LocalTime.of(7, 5),
            ),
        )
        val build = sentBody()
        assertEquals("quantity", build["measure"]!!.jsonPrimitive.content)
        assertEquals("2000", build["target_value"]!!.jsonPrimitive.content)
        assertEquals("07:05", build["time_of_day"]!!.jsonPrimitive.content)
        val frequency = build["frequency"]!!.jsonObject
        assertEquals("weekdays", frequency["kind"]!!.jsonPrimitive.content)
        assertEquals("[0,4]", frequency["weekdays"].toString())
        assertEquals("2", frequency["every_n_weeks"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, frequency["times"])
        assertFalse("times_per_week" in frequency)

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
        assertFalse("time_of_day" in quit)
    }

    @Test
    fun aPeriodVersionNeverSendsWeeklyAndAnEditClearsTheTime() = runTest {
        server.enqueue(
            json(201, Json.parseToJsonElement(Fixtures.read("habits/habit.json")).jsonObject["frequency"].toString()),
        )
        server.enqueue(json(200, habitJson()))

        val monthly =
            FrequencyInput(kind = FrequencyKind.PerPeriod, times = 40, period = Period.Month, preferredDay = 15)
        repository.createFrequency("h1", LocalDate.of(2026, 11, 23), monthly)
        val version = sentBody()
        assertEquals("2026-11-23", version["valid_from"]!!.jsonPrimitive.content)
        assertEquals("per_period", version["kind"]!!.jsonPrimitive.content)
        assertEquals("month", version["period"]!!.jsonPrimitive.content)
        // Never more than the period's days (the server would refuse it).
        assertEquals("28", version["times"]!!.jsonPrimitive.content)
        assertEquals("15", version["preferred_day"]!!.jsonPrimitive.content)

        repository.updateHabit(
            "h1",
            HabitInput("Ler", HabitKind.Build, HabitColor.Amber, "book", LocalDate.of(2026, 9, 1)),
            quantity = false,
        )
        assertEquals(JsonNull, sentBody()["time_of_day"])
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
