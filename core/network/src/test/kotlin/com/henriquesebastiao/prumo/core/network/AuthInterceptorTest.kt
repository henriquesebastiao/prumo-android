package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.dto.LoginRequestDto
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private val server = MockWebServer()
    private val gateway = FakeGateway()
    private lateinit var api: PrumoApi
    private lateinit var baseUrl: String

    @Before
    fun setUp() {
        server.start()
        baseUrl = server.url("/").toString().trimEnd('/')
        gateway.activeSession = ActiveSession(baseUrl, "secret")
        api = ApiFactory(OkHttpClient.Builder().addInterceptor(AuthInterceptor { gateway }).build()).system(baseUrl)
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun everyRequestToTheServerCarriesTheBearer() = runTest {
        server.enqueue(json(200, Fixtures.read("me.json")))
        val outcome = apiCall { api.me() }
        assertTrue(outcome is Outcome.Ok)
        assertEquals("Bearer secret", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun aRefused401EndsTheSession() = runTest {
        server.enqueue(json(401, Fixtures.read("error_unauthorized.json")))
        val outcome = apiCall { api.dashboard() }
        assertEquals(Outcome.Failed(AppError.SessionExpired), outcome)
        assertEquals(listOf("secret"), gateway.refused)
    }

    @Test
    fun aWrongPasswordDoesNotEndTheSession() = runTest {
        server.enqueue(json(401, Fixtures.read("error_invalid_credentials.json")))
        apiCall { api.login(LoginRequestDto("henrique", "wrong")) }
        assertTrue(gateway.refused.isEmpty())
    }

    @Test
    fun theTokenNeverGoesToAnotherHost() {
        val other = MockWebServer().apply { start() }
        other.enqueue(MockResponse.Builder().code(200).build())
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor { gateway }).build()
        // The same host and another port is another server.
        client.newCall(Request.Builder().url(other.url("/cover.webp")).build()).execute().close()
        assertNull(other.takeRequest().headers["Authorization"])
        other.close()
    }

    @Test
    fun signedOutRequestsGoWithoutAToken() = runTest {
        gateway.activeSession = null
        server.enqueue(json(200, Fixtures.read("login.json")))
        apiCall { api.login(LoginRequestDto("henrique", "prumo-demo-01", deviceName = "Pixel 9")) }
        val request = server.takeRequest()
        assertNull(request.headers["Authorization"])
        val sent = request.body!!.utf8()
        assertTrue(sent, sent.contains("\"client\":\"app\"") && sent.contains("\"device_name\":\"Pixel 9\""))
    }

    @Test
    fun theServerIsMatchedByItsBasePath() {
        assertTrue(
            AuthInterceptor.belongsTo("https://exemplo.com/prumo/api/v1/me".toHttpUrl(), "https://exemplo.com/prumo"),
        )
        assertFalse(AuthInterceptor.belongsTo("https://exemplo.com/outro/api".toHttpUrl(), "https://exemplo.com/prumo"))
        assertFalse(AuthInterceptor.belongsTo("http://exemplo.com/api".toHttpUrl(), "https://exemplo.com"))
        assertTrue(AuthInterceptor.belongsTo("https://exemplo.com/api/v1/me".toHttpUrl(), "https://exemplo.com"))
    }

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    private class FakeGateway : SessionGateway {
        override var activeSession: ActiveSession? = null
        val refused = mutableListOf<String>()

        override fun onUnauthorized(token: String) {
            refused += token
        }
    }
}
