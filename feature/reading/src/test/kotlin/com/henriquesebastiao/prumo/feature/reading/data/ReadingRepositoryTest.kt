package com.henriquesebastiao.prumo.feature.reading.data

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
import com.henriquesebastiao.prumo.feature.reading.model.BookInput
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import com.henriquesebastiao.prumo.feature.reading.model.CoverImage
import com.henriquesebastiao.prumo.feature.reading.model.NoteInput
import com.henriquesebastiao.prumo.feature.reading.model.ShelfFilter
import com.henriquesebastiao.prumo.feature.reading.model.ShelfOrder
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Livros against recorded answers of the server (fixtures/reading/, from the e2e server). */
class ReadingRepositoryTest {
    private val server = MockWebServer()
    private val changes = DataChanges()
    private lateinit var repository: DefaultReadingRepository

    private val cleanCode = Fixtures.idOf("reading/books.json", "title", "Código Limpo")

    @Before
    fun setUp() {
        server.start()
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        repository = DefaultReadingRepository(ServerCalls(ApiFactory(OkHttpClient()), gateway, NoCache), changes)
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
    fun theShelfIsAskedWholeWithItsFiltersAndOrder() = runTest {
        server.enqueue(json(200, Fixtures.read("reading/books.json")))

        val filter = ShelfFilter(BookStatus.Read, owned = true, query = " machado ", order = ShelfOrder.Title)
        val page = (repository.books(filter) as Outcome.Ok).value

        assertEquals(3, page.items.size)
        assertFalse(page.hasMore)
        val url = server.takeRequest().url
        assertEquals("/api/v1/reading/books", url.encodedPath)
        assertEquals("read", url.queryParameter("status"))
        assertEquals("true", url.queryParameter("owned"))
        assertEquals("machado", url.queryParameter("q"))
        assertEquals("title", url.queryParameter("sort"))
        assertEquals("200", url.queryParameter("limit"))
    }

    @Test
    fun withoutFiltersOnlyTheNewestFirstOrderIsSent() = runTest {
        server.enqueue(json(200, Fixtures.read("reading/books.json")))

        repository.books()

        val url = server.takeRequest().url
        assertNull(url.queryParameter("status"))
        assertNull(url.queryParameter("owned"))
        assertNull(url.queryParameter("q"))
        assertEquals("-created_at", url.queryParameter("sort"))
    }

    @Test
    fun aBookComesWithTheServersProgressPaceAndWeeks() = runTest {
        server.enqueue(json(200, Fixtures.read("reading/book.json")))

        val detail = (repository.book(cleanCode) as Outcome.Ok).value

        val book = detail.book
        assertEquals("Código Limpo", book.title)
        assertEquals(BookStatus.Reading, book.status)
        assertEquals(50, book.progressPercent)
        assertEquals(213, book.remainingPages)
        assertEquals(10.4, book.pacePagesPerDay!!, 0.0)
        assertEquals(LocalDate.of(2026, 12, 8), book.forecastFinishOn)
        assertEquals(listOf(62, 71, 79), detail.weeklyPages.map { it.pages })
        assertNull(repository.coverUrl(book))
    }

    @Test
    fun theNotesComeWithTheirBook() = runTest {
        server.enqueue(json(200, Fixtures.read("reading/notes.json")))

        val page = (repository.notes(" erros ") as Outcome.Ok).value

        val first = page.items.first()
        assertEquals("Código Limpo", first.bookTitle)
        assertEquals(cleanCode, first.bookId)
        val url = server.takeRequest().url
        assertEquals("/api/v1/reading/notes", url.encodedPath)
        assertEquals("erros", url.queryParameter("q"))
        assertEquals("50", url.queryParameter("limit"))
    }

    @Test
    fun aNewBookSendsItsPageAndNullDates() = runTest {
        server.enqueue(json(201, Fixtures.read("reading/book.json")))
        val input = BookInput("Sapiens", "Yuval Noah Harari", 464, BookStatus.WantToRead, false, null, null, null)

        changes.changes.test {
            repository.createBook(input)
            assertEquals("reading", awaitItem())
        }

        val sent = sentBody()
        assertEquals("want_to_read", sent["status"]!!.jsonPrimitive.content)
        assertEquals("0", sent["current_page"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["started_on"])
        assertEquals(JsonNull, sent["finished_on"])
    }

    @Test
    fun anEditLeavesThePageToTheProgress() = runTest {
        server.enqueue(json(200, Fixtures.read("reading/book.json")))
        val input = BookInput(
            "Código Limpo",
            "Robert C. Martin",
            425,
            BookStatus.Reading,
            true,
            212,
            LocalDate.of(2026, 10, 26),
            null,
        )

        repository.updateBook(cleanCode, input)

        val sent = sentBody()
        assertFalse("current_page" in sent)
        assertEquals("2026-10-26", sent["started_on"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["finished_on"])
    }

    @Test
    fun aPagePastTheEndIsRefusedByItsCode() = runTest {
        server.enqueue(json(422, Fixtures.read("reading/error_page_out_of_range.json")))

        val outcome = repository.addProgress(cleanCode, 426)

        assertEquals("reading.page_out_of_range", ((outcome as Outcome.Failed).error as AppError.Api).code)
        val request = server.takeRequest()
        assertEquals("/api/v1/reading/books/$cleanCode/progress", request.url.encodedPath)
    }

    @Test
    fun theCoverGoesAsAFileAndItsAddressChangesWithIt() = runTest {
        val withCover = Fixtures.read("reading/book.json")
            .replace("\"has_cover\": false", "\"has_cover\": true")
            .replace("\"cover_updated_at\": null", "\"cover_updated_at\": \"2026-11-17T18:31:44.650697Z\"")
        server.enqueue(json(200, withCover))

        val book = (repository.setCover(cleanCode, CoverImage(byteArrayOf(1, 2, 3), "image/jpeg")) as Outcome.Ok).value

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"file\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
        val url = repository.coverUrl(book)!!
        assertTrue(url.endsWith("/api/v1/reading/books/$cleanCode/cover?v=1794940304650"))
    }

    @Test
    fun aNoteEditSendsItsDateOnlyWhenThereIsOne() = runTest {
        // The first note of the list, as the server answers an edit.
        val note = Json.parseToJsonElement(Fixtures.read("reading/notes.json")).jsonObject["items"]!!.jsonArray.first()
        server.enqueue(json(200, note.toString()))

        repository.updateNote(cleanCode, "n1", NoteInput("Reler", null))

        val sent = sentBody()
        assertEquals("Reler", sent["content"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["page"])
        assertFalse("noted_on" in sent)

        server.enqueue(json(200, note.toString()))
        repository.updateNote(cleanCode, "n1", NoteInput("Reler", 10, LocalDate.of(2026, 10, 29)))
        assertEquals("2026-10-29", sentBody()["noted_on"]!!.jsonPrimitive.content)
    }
}
