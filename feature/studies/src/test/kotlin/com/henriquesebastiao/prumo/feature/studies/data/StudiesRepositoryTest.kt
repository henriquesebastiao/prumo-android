package com.henriquesebastiao.prumo.feature.studies.data

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
import com.henriquesebastiao.prumo.feature.studies.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.studies.model.AreaInput
import com.henriquesebastiao.prumo.feature.studies.model.BlockInput
import com.henriquesebastiao.prumo.feature.studies.model.DayStatus
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.SessionInput
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import com.henriquesebastiao.prumo.feature.studies.model.SubjectInput
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Estudos against recorded answers of the server (fixtures/studies/, from the e2e server). */
class StudiesRepositoryTest {
    private val server = MockWebServer()
    private val changes = DataChanges()
    private lateinit var repository: DefaultStudiesRepository

    @Before
    fun setUp() {
        server.start()
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        repository = DefaultStudiesRepository(ServerCalls(ApiFactory(OkHttpClient()), gateway, NoCache), changes)
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
    fun todayComesWithGoalsMinutesAndStatusesFromTheServer() = runTest {
        server.enqueue(json(200, Fixtures.read("studies/day.json")))

        val day = (repository.day() as Outcome.Ok).value

        assertEquals("/api/v1/studies/day", server.takeRequest().url.encodedPath)
        assertEquals(LocalDate.of(2026, 11, 17), day.on)
        assertTrue(day.isToday)
        assertEquals(30, day.doneMinutes)
        assertEquals(75, day.plannedMinutes)
        assertEquals(false, day.met)
        assertEquals(listOf("Inglês", "FastAPI"), day.rows.map { it.subject.name })
        assertEquals(listOf(DayStatus.Met, DayStatus.InProgress), day.rows.map { it.status })
        assertEquals(StudyColor.Orange, day.rows[0].subject.color)
        assertEquals(LocalTime.of(20, 0), day.rows[1].blocks.single().startsAt)
    }

    @Test
    fun aWeekIsAskedByItsStartAndKnowsItsNeighbours() = runTest {
        server.enqueue(json(200, Fixtures.read("studies/week_previous.json")))

        val week = (repository.week(LocalDate.of(2026, 11, 9)) as Outcome.Ok).value

        assertEquals("2026-11-09", server.takeRequest().url.queryParameter("start"))
        assertEquals(7, week.days.size)
        assertEquals(3, week.daysMet)
        assertEquals(6, week.daysPlanned)
        assertEquals(LocalDate.of(2026, 11, 16), week.nextStart)
        assertEquals(LocalDate.of(2026, 11, 2), week.previousStart)
        assertNull(week.days.last().met)
        assertTrue(week.bySubject.isNotEmpty())
    }

    @Test
    fun theCurrentWeekSendsNoStart() = runTest {
        server.enqueue(json(200, Fixtures.read("studies/week.json")))

        val week = (repository.week() as Outcome.Ok).value

        assertNull(server.takeRequest().url.queryParameter("start"))
        assertEquals(LocalDate.of(2026, 11, 16), week.start)
        assertEquals(LocalDate.of(2026, 11, 23), week.nextStart)
    }

    @Test
    fun areasAndSubjectsComeWholeWithTheArchivedFilter() = runTest {
        server.enqueue(json(200, Fixtures.read("studies/areas.json")))
        server.enqueue(json(200, Fixtures.read("studies/subjects.json")))

        val areas = (repository.areas(ArchivedFilter.All) as Outcome.Ok).value
        val subjects = (repository.subjects() as Outcome.Ok).value

        val areasUrl = server.takeRequest().url
        assertEquals("all", areasUrl.queryParameter("archived"))
        assertEquals("200", areasUrl.queryParameter("limit"))
        assertEquals("false", server.takeRequest().url.queryParameter("archived"))
        assertEquals(listOf("Desenvolvimento", "Faculdade", "Inglês"), areas.map { it.name })
        assertFalse(areas.any { it.archived })
        assertEquals(5, subjects.size)
        val calculus = subjects.first { it.name == "Cálculo II" }
        assertEquals("Faculdade", calculus.areaName)
        assertEquals(StudyColor.Blue, calculus.color)
        assertEquals(60, calculus.minutesThisWeek)
        assertNull(calculus.obsidianUri)
    }

    @Test
    fun aSubjectComesWithTopicsWeeksAndSessions() = runTest {
        server.enqueue(json(200, Fixtures.read("studies/subject.json")))

        val detail = (repository.subject("s1") as Outcome.Ok).value

        assertEquals("/api/v1/studies/subjects/s1", server.takeRequest().url.encodedPath)
        assertEquals("Estruturas de Dados", detail.subject.name)
        assertTrue(detail.subject.obsidianUri!!.startsWith("obsidian://"))
        assertEquals(listOf("Grafos", "Árvores"), detail.topics.map { it.name })
        assertEquals(8, detail.weeklyMinutes.size)
        assertEquals(105, detail.weeklyMinutes.first { it.weekStart == LocalDate.of(2026, 11, 9) }.minutes)
        assertEquals(Instant.parse("2026-11-15T19:00:00Z"), detail.recentSessions.first().startedAt)
        assertEquals("Grafos", detail.recentSessions.first().topic?.name)
    }

    @Test
    fun thePlansComeNewestFirstWithTheirState() = runTest {
        server.enqueue(json(200, Fixtures.read("studies/plans.json")))

        val plans = (repository.plans() as Outcome.Ok).value

        assertEquals(LocalDate.of(2026, 11, 17), plans.today)
        assertEquals(listOf(PlanState.Future, PlanState.Current, PlanState.Past), plans.items.map { it.state })
        val current = plans.items[1]
        assertEquals(LocalDate.of(2026, 11, 22), current.validUntil)
        val english = current.blocks.first { it.subject.name == "Inglês" }
        assertEquals(DayOfWeek.entries.take(5), english.weekdays)
        assertEquals(LocalTime.of(7, 0), english.startsAt)
    }

    @Test
    fun aNewVersionSendsItsDayAndBlocksAndTellsTheScreens() = runTest {
        val plan = Json.parseToJsonElement(Fixtures.read("studies/plans.json")).jsonObject["items"]!!.jsonArray[0]
        server.enqueue(json(201, plan.toString()))

        changes.changes.test {
            val blocks = listOf(
                BlockInput("s1", listOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY), LocalTime.of(19, 0), 60),
                BlockInput("s2", listOf(DayOfWeek.SATURDAY), null, 90),
            )
            val saved = repository.createPlan(LocalDate.of(2026, 11, 23), blocks)

            assertTrue(saved is Outcome.Ok)
            assertEquals("studies", awaitItem())
        }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/studies/plans", request.url.encodedPath)
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("2026-11-23", body["valid_from"]!!.jsonPrimitive.content)
        val first = body["blocks"]!!.jsonArray[0].jsonObject
        assertEquals("[0,2]", first["weekdays"].toString())
        assertEquals("19:00", first["starts_at"]!!.jsonPrimitive.content)
        assertEquals(60, first["duration_minutes"]!!.jsonPrimitive.content.toInt())
        assertEquals(JsonNull, body["blocks"]!!.jsonArray[1].jsonObject["starts_at"])
    }

    @Test
    fun aVersionInForceIsRefusedWithItsCode() = runTest {
        server.enqueue(json(422, Fixtures.read("studies/error_plan_locked.json")))

        val refused = repository.updatePlan("p1", emptyList())

        val error = (refused as Outcome.Failed).error as AppError.Api
        assertEquals("studies.plan_locked", error.code)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/studies/plans/p1", request.url.encodedPath)
    }

    @Test
    fun aSessionSendsATypedTopicByName() = runTest {
        val session = Json.parseToJsonElement(Fixtures.read("studies/subject.json"))
            .jsonObject["recent_sessions"]!!.jsonArray[0]
        server.enqueue(json(201, session.toString()))

        val input = SessionInput("s1", null, "Grafos", Instant.parse("2026-11-17T16:45:00Z"), 45)
        val saved = (repository.createSession(input) as Outcome.Ok).value

        assertEquals("Grafos", saved.topic?.name)
        val body = sentBody()
        assertEquals("s1", body["subject_id"]!!.jsonPrimitive.content)
        assertEquals("Grafos", body["topic_name"]!!.jsonPrimitive.content)
        assertFalse("topic_id" in body)
        assertEquals("2026-11-17T16:45:00Z", body["started_at"]!!.jsonPrimitive.content)
        assertEquals(45, body["duration_minutes"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun aChosenTopicGoesByIdAndAnEditWithoutOneClearsIt() = runTest {
        val session = Json.parseToJsonElement(Fixtures.read("studies/subject.json"))
            .jsonObject["recent_sessions"]!!.jsonArray[0]
        server.enqueue(json(201, session.toString()))
        server.enqueue(json(200, session.toString()))

        repository.createSession(SessionInput("s1", "t1", "Grafos", Instant.parse("2026-11-17T16:45:00Z"), 45))
        val created = sentBody()
        assertEquals("t1", created["topic_id"]!!.jsonPrimitive.content)
        assertFalse("topic_name" in created)

        repository.updateSession("x", SessionInput("s1", null, null, Instant.parse("2026-11-17T16:45:00Z"), 30))
        val updated = sentBody()
        assertEquals(JsonNull, updated["topic_id"])
        assertFalse("topic_name" in updated)
    }

    @Test
    fun aSubjectWithoutLinkSendsNullAndABadLinkIsRefused() = runTest {
        server.enqueue(json(422, Fixtures.read("studies/error_invalid_obsidian_uri.json")))
        server.enqueue(
            json(
                200,
                Json.parseToJsonElement(
                    Fixtures.read("studies/subjects.json"),
                ).jsonObject["items"]!!.jsonArray[0].toString(),
            ),
        )

        val refused = repository.createSubject(SubjectInput("a1", "Física", "https://x.dev"))
        assertEquals("studies.invalid_obsidian_uri", ((refused as Outcome.Failed).error as AppError.Api).code)
        sentBody()

        repository.updateSubject("s1", SubjectInput("a1", "Cálculo II", null))
        val body = sentBody()
        assertEquals(JsonNull, body["obsidian_uri"])
        assertEquals("a1", body["area_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun archivingUsesTheArchiveRoutes() = runTest {
        val area = Json.parseToJsonElement(Fixtures.read("studies/areas.json")).jsonObject["items"]!!.jsonArray[0]
        server.enqueue(json(200, area.toString()))
        server.enqueue(json(200, area.toString()))
        server.enqueue(json(201, area.toString()))

        repository.setAreaArchived("a1", true)
        assertEquals("/api/v1/studies/areas/a1/archive", server.takeRequest().url.encodedPath)
        repository.setAreaArchived("a1", false)
        assertEquals("/api/v1/studies/areas/a1/unarchive", server.takeRequest().url.encodedPath)
        repository.createArea(AreaInput("Música", StudyColor.Pink))
        val body = sentBody()
        assertEquals("Música", body["name"]!!.jsonPrimitive.content)
        assertEquals("pink", body["color"]!!.jsonPrimitive.content)
    }
}
