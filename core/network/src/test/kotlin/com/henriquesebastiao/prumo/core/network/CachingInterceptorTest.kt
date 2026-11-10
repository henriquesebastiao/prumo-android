package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.network.dto.UserUpdateDto
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CachingInterceptorTest {
    private val server = MockWebServer()
    private val cache = MemoryCache()
    private val reachability = Reachability()
    private val clock = MovableClock(Instant.parse("2026-11-17T17:30:00Z"))
    private lateinit var client: OkHttpClient
    private lateinit var api: PrumoApi

    @Before
    fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(baseUrl, "secret")

            override fun onUnauthorized(token: String) = Unit
        }
        val interceptor = CachingInterceptor({ cache }, { reachability }, { gateway }, { client }, clock)
        client = OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(AuthInterceptor { gateway }).build()
        api = ApiFactory(client).system(baseUrl)
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun theFirstReadAsksTheServerAndKeepsItsAnswer() = runTest {
        server.enqueue(json(200, Fixtures.read("dashboard.json")))
        assertTrue(apiCall { api.dashboard() } is Outcome.Ok)
        assertEquals(Fixtures.read("dashboard.json"), cache.responses.getValue("api/v1/dashboard").body)
        assertEquals(clock.instant(), cache.responses.getValue("api/v1/dashboard").fetchedAt)
        assertTrue(reachability.answered)
    }

    @Test
    fun aRecentAnswerIsShownWithoutAskingTheServer() = runTest {
        cache.write("api/v1/me", Fixtures.read("me.json"), clock.instant(), stale = false)
        clock.advance(Duration.ofSeconds(10))
        val outcome = apiCall { api.me() }
        assertEquals("henrique", (outcome as Outcome.Ok).value.username)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun anOlderAnswerIsShownAndCheckedInTheBackground() = runTest {
        val old = Fixtures.read("me.json")
        cache.write("api/v1/me", old, clock.instant(), stale = false)
        clock.advance(Duration.ofMinutes(5))
        val renamed = old.replace("\"display_name\":", "\"display_name\": \"Outro\", \"ignored\":")
        server.enqueue(json(200, renamed))

        val outcome = apiCall { api.me() }
        assertTrue(outcome is Outcome.Ok)
        // The check goes out after the screen got the kept answer, with the bearer.
        assertEquals("Bearer secret", server.takeRequest(5, TimeUnit.SECONDS)?.headers?.get("Authorization"))
        waitFor { cache.changed.isNotEmpty() }
        assertEquals(listOf("api/v1/me"), cache.changed)
        assertEquals(renamed, cache.responses.getValue("api/v1/me").body)
    }

    @Test
    fun aWriteMakesTheNextReadAskTheServerFirst() = runTest {
        cache.write("api/v1/me", Fixtures.read("me.json"), clock.instant(), stale = false)
        server.enqueue(json(200, Fixtures.read("me.json")))
        apiCall { api.updateMe(UserUpdateDto("Henrique")) }
        assertEquals(1, cache.expired)

        server.enqueue(json(200, Fixtures.read("me.json")))
        apiCall { api.me() }
        assertEquals(2, server.requestCount)
        assertFalse(cache.responses.getValue("api/v1/me").stale)
    }

    @Test
    fun whenTheServerDoesNotAnswerTheKeptAnswerIsShown() = runTest {
        cache.write("api/v1/dashboard", Fixtures.read("dashboard.json"), clock.instant(), stale = true)
        server.close()
        assertTrue(apiCall { api.dashboard() } is Outcome.Ok)
        assertEquals(UnreachableCause.Refused, reachability.cause)
    }

    @Test
    fun aProxyAnsweringForAnApiThatIsDownCountsAsNoAnswer() = runTest {
        cache.write("api/v1/dashboard", Fixtures.read("dashboard.json"), clock.instant(), stale = true)
        server.enqueue(MockResponse.Builder().code(502).body("<html>Bad Gateway</html>").build())
        assertTrue(apiCall { api.dashboard() } is Outcome.Ok)
        assertEquals(UnreachableCause.Other, reachability.cause)
    }

    @Test
    fun withoutAKeptAnswerTheFailureReachesTheScreen() = runTest {
        server.close()
        val outcome = apiCall { api.dashboard() }
        assertTrue(outcome is Outcome.Failed)
    }

    @Test
    fun aRecordTheServerNoLongerHasIsForgotten() = runTest {
        cache.write("api/v1/me", Fixtures.read("me.json"), clock.instant(), stale = true)
        server.enqueue(json(404, """{"error": {"code": "not_found", "message": "Not found."}}"""))
        assertTrue(apiCall { api.me() } is Outcome.Failed)
        assertNull(cache.responses["api/v1/me"])
    }

    @Test
    fun sessionsTheExportAndTheSyncAreNeverKept() {
        assertTrue(CachingInterceptor.cacheable("api/v1/dashboard"))
        assertTrue(CachingInterceptor.cacheable("api/v1/garage/vehicles?archived=false&limit=200"))
        assertFalse(CachingInterceptor.cacheable("api/v1/auth/sessions"))
        assertFalse(CachingInterceptor.cacheable("api/v1/me/export"))
        assertFalse(CachingInterceptor.cacheable("api/v1/sync/deletions?since=x"))
        assertFalse(CachingInterceptor.cacheable("api/health"))
    }

    @Test
    fun pathsAreRelativeToTheServersAddress() {
        val base = "https://exemplo.com/prumo"
        assertEquals(
            "api/v1/garage/vehicles?archived=true",
            CachingInterceptor.relativePath(
                "https://exemplo.com/prumo/api/v1/garage/vehicles?archived=true".toHttpUrl(),
                base,
            ),
        )
        assertNull(CachingInterceptor.relativePath("https://outro.com/prumo/api/v1/me".toHttpUrl(), base))
    }

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    private fun waitFor(condition: () -> Boolean) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < until) Thread.sleep(10)
    }

    private class MemoryCache : ResponseCache {
        val responses = java.util.concurrent.ConcurrentHashMap<String, CachedResponse>()
        val changed = java.util.concurrent.CopyOnWriteArrayList<String>()

        @Volatile
        var expired = 0

        override fun read(path: String) = responses[path]

        override fun write(path: String, body: String, fetchedAt: Instant, stale: Boolean) {
            responses[path] = CachedResponse(body, fetchedAt, stale)
        }

        override fun remove(path: String) {
            responses.remove(path)
        }

        override fun expire() {
            expired++
            responses.replaceAll { _, response -> response.copy(stale = true) }
        }

        override fun changed(path: String) {
            changed += path
        }
    }

    private class Reachability : ServerReachability {
        @Volatile
        var answered = false

        @Volatile
        var cause: UnreachableCause? = null

        override fun answered() {
            answered = true
            cause = null
        }

        override fun unanswered(cause: UnreachableCause) {
            this.cause = cause
        }
    }

    private class MovableClock(@Volatile private var now: Instant) : Clock() {
        fun advance(by: Duration) {
            now += by
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }
}
