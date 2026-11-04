package com.henriquesebastiao.prumo.core.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import java.io.File
import java.security.GeneralSecurityException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultSessionRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var baseUrl: String
    private lateinit var file: File

    /** Reversible, and visibly not the token: what the Keystore's AES-GCM does, for the test. */
    private class FakeCipher(private val broken: Boolean = false) : TokenCipher {
        override fun encrypt(plain: String) = "enc:" + plain.reversed()

        override fun decrypt(cipher: String): String {
            if (broken) throw GeneralSecurityException("key gone")
            return cipher.removePrefix("enc:").reversed()
        }
    }

    @Before
    fun setUp() {
        server.start()
        baseUrl = server.url("/").toString().trimEnd('/')
        file = File(folder.root, "session.preferences_pb")
    }

    @After
    fun tearDown() = server.close()

    private fun repository(scope: CoroutineScope, cipher: TokenCipher = FakeCipher()) = DefaultSessionRepository(
        SessionStore(PreferenceDataStoreFactory.create(scope = scope) { file }, cipher),
        ApiFactory(OkHttpClient()),
        { "Pixel 9" },
        scope,
    )

    private suspend fun DefaultSessionRepository.loaded() = state.first { it != SessionState.Loading }

    /** A scope of its own: DataStore allows one instance per file, so a "new start" closes the first. */
    private fun TestScope.scope() =
        CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext.job))

    @Test
    fun signingInKeepsTheSessionWithTheTokenEncrypted() = runTest {
        server.enqueue(json(200, Fixtures.read("login.json")))
        val firstStart = scope()
        val repository = repository(firstStart)
        repository.loaded()

        val outcome = repository.signIn(baseUrl, " henrique ", "prumo-demo-01")

        assertTrue(outcome is Outcome.Ok)
        val sent = server.takeRequest().body!!.utf8()
        assertTrue(sent, sent.contains("\"client\":\"app\"") && sent.contains("\"device_name\":\"Pixel 9\""))
        assertTrue(sent, sent.contains("\"username\":\"henrique\""))
        assertEquals("test-token", repository.activeSession?.token)
        assertFalse("the token is on disk in the clear", file.readText(Charsets.ISO_8859_1).contains("test-token"))

        // A new start reads it back.
        firstStart.coroutineContext.job.cancelAndJoin()
        val again = repository(backgroundScope)
        val state = again.loaded() as SessionState.SignedIn
        assertEquals("test-token", state.session.token)
        assertEquals("Henrique Sebastião", state.session.displayName)
    }

    @Test
    fun aRefusedTokenEndsTheSessionKeepingTheServer() = runTest {
        server.enqueue(json(200, Fixtures.read("login.json")))
        val repository = repository(backgroundScope)
        repository.loaded()
        repository.signIn(baseUrl, "henrique", "prumo-demo-01")

        repository.onUnauthorized("an older token")
        assertTrue(repository.state.value is SessionState.SignedIn)

        repository.onUnauthorized("test-token")
        assertEquals(SessionState.SignedOut(SignOutReason.Expired, baseUrl, "henrique"), repository.state.value)
        assertEquals(null, repository.activeSession)
    }

    @Test
    fun aWrongPasswordIsTheServersError() = runTest {
        server.enqueue(json(401, Fixtures.read("error_invalid_credentials.json")))
        val repository = repository(backgroundScope)
        repository.loaded()

        val outcome = repository.signIn(baseUrl, "henrique", "wrong")

        assertEquals("auth.invalid_credentials", ((outcome as Outcome.Failed).error as AppError.Api).code)
        assertTrue(repository.state.value is SessionState.SignedOut)
    }

    @Test
    fun signingOutTellsTheServerAndKeepsTheUsername() = runTest {
        server.enqueue(json(200, Fixtures.read("login.json")))
        server.enqueue(MockResponse.Builder().code(204).build())
        val repository = repository(backgroundScope)
        repository.loaded()
        repository.signIn(baseUrl, "henrique", "prumo-demo-01")
        server.takeRequest()

        repository.signOut()

        val logout = server.takeRequest()
        assertEquals("/api/v1/auth/logout", logout.url.encodedPath)
        assertEquals(SessionState.SignedOut(SignOutReason.SignedOut, baseUrl, "henrique"), repository.state.value)
    }

    @Test
    fun aTokenThatNoLongerOpensMeansSigningInAgain() = runTest {
        server.enqueue(json(200, Fixtures.read("login.json")))
        val firstStart = scope()
        val first = repository(firstStart)
        first.loaded()
        first.signIn(baseUrl, "henrique", "prumo-demo-01")
        firstStart.coroutineContext.job.cancelAndJoin()

        val restored = repository(backgroundScope, FakeCipher(broken = true))
        assertEquals(SessionState.SignedOut(SignOutReason.None, baseUrl, "henrique"), restored.loaded())
    }

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()
}
