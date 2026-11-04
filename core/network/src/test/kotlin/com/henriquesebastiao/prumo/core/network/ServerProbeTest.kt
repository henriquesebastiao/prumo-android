package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import java.net.ServerSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerProbeTest {
    private val server = MockWebServer()
    private val probe = ServerProbe(OkHttpClient(), Dispatchers.IO)
    private lateinit var baseUrl: String

    @Before
    fun setUp() {
        server.start()
        baseUrl = server.url("/").toString().trimEnd('/')
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun aPrumoServerIsFoundWithItsVersion() = runTest {
        server.enqueue(response(200, Fixtures.read("health.json")))
        val result = probe.check(baseUrl)
        assertTrue(result is ServerCheck.Found)
        assertEquals("0.0.0", (result as ServerCheck.Found).version)
        assertEquals("/api/health", server.takeRequest().url.encodedPath)
    }

    @Test
    fun aServerOlderThanTheServiceFieldIsStillFound() = runTest {
        server.enqueue(response(200, """{"status":"ok","database":"ok","version":"0.0.0"}"""))
        assertTrue(probe.check(baseUrl) is ServerCheck.Found)
    }

    @Test
    fun aServerWithoutItsDatabaseIsTold() = runTest {
        server.enqueue(
            response(503, """{"service":"prumo","status":"degraded","database":"unavailable","version":"0.1.0"}"""),
        )
        assertEquals(ServerCheck.DatabaseDown(baseUrl, "0.1.0"), probe.check(baseUrl))
    }

    @Test
    fun anythingElseIsNotPrumo() = runTest {
        server.enqueue(response(200, "<html><body>Router</body></html>"))
        assertEquals(ServerCheck.NotPrumo(baseUrl), probe.check(baseUrl))
        server.enqueue(response(200, """{"status":"ok","database":"ok","version":"1.0","service":"downtify"}"""))
        assertEquals(ServerCheck.NotPrumo(baseUrl), probe.check(baseUrl))
        server.enqueue(response(404, """{"detail":"Not Found"}"""))
        assertEquals(ServerCheck.NotPrumo(baseUrl), probe.check(baseUrl))
    }

    @Test
    fun aClosedPortIsUnreachable() = runTest {
        val port = ServerSocket(0).use { it.localPort }
        val result = probe.check("http://127.0.0.1:$port")
        assertEquals(ServerCheck.Unreachable("http://127.0.0.1:$port", UnreachableCause.Refused), result)
    }

    @Test
    fun theFirstCandidateThatAnswersWins() = runTest {
        // Without a scheme, a home-network address tries http first: the mock server.
        server.enqueue(response(200, Fixtures.read("health.json")))
        val typed = baseUrl.removePrefix("http://")
        val result = probe.probe(typed)
        assertEquals(baseUrl, (result as ServerCheck.Found).baseUrl)
        assertEquals(ServerCheck.InvalidAddress, probe.probe("não é endereço"))
    }

    private fun response(code: Int, body: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()
}
