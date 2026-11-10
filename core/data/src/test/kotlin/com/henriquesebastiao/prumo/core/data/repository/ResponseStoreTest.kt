package com.henriquesebastiao.prumo.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.data.session.Session
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.database.PrumoDatabase
import com.henriquesebastiao.prumo.core.model.Outcome
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class ResponseStoreTest {
    private val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PrumoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val sessions = Sessions(henrique)
    private val changes = DataChanges()
    private val fetched = Instant.parse("2026-11-17T17:30:00Z")
    private val clock = java.time.Clock.fixed(fetched.plusSeconds(60), java.time.ZoneOffset.UTC)

    @After
    fun tearDown() = database.close()

    private fun kotlinx.coroutines.test.TestScope.store(): ResponseStore {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return ResponseStore(database.responses(), sessions, changes, clock, backgroundScope, dispatcher)
    }

    @Test
    fun anAnswerIsKeptForTheAccountThatAskedOnly() = runTest {
        val store = store()
        store.write(DASHBOARD, "{}", fetched, stale = false)
        assertEquals("{}", store.read(DASHBOARD)?.body)
        assertEquals(fetched, store.read(DASHBOARD)?.fetchedAt)

        sessions.state.value = SessionState.SignedIn(henrique.copy(username = "ana"))
        assertNull(store.read(DASHBOARD))
    }

    @Test
    fun anotherAccountSigningInForgetsThePreviousOnes() = runTest {
        val store = store()
        store.write(DASHBOARD, "{}", fetched, stale = false)
        sessions.state.value = SessionState.SignedOut(SignOutReason.Expired, henrique.baseUrl, henrique.username)
        sessions.state.value = SessionState.SignedIn(henrique.copy(username = "ana"))
        sessions.state.value = SessionState.SignedIn(henrique)
        assertNull(store.read(DASHBOARD))
    }

    @Test
    fun anExpiredSessionKeepsTheAnswersForTheSameUser() = runTest {
        val store = store()
        store.write(DASHBOARD, "{}", fetched, stale = false)
        sessions.state.value = SessionState.SignedOut(SignOutReason.Expired, henrique.baseUrl, henrique.username)
        // A restart while signed out reads the session as "None" with the last server.
        sessions.state.value = SessionState.SignedOut(SignOutReason.None, henrique.baseUrl, henrique.username)
        sessions.state.value = SessionState.SignedIn(henrique)
        assertNotNull(store.read(DASHBOARD))
    }

    @Test
    fun signingOutForgetsEverything() = runTest {
        val store = store()
        store.write(DASHBOARD, "{}", fetched, stale = false)
        sessions.state.value = SessionState.SignedOut(SignOutReason.SignedOut, henrique.baseUrl, henrique.username)
        sessions.state.value = SessionState.SignedIn(henrique)
        assertNull(store.read(DASHBOARD))
    }

    @Test
    fun expiringMarksEveryAnswerAndRemovingForgetsOne() = runTest {
        val store = store()
        store.write(DASHBOARD, "{}", fetched, stale = false)
        store.write(VEHICLES, "[]", fetched, stale = false)
        store.expire()
        assertTrue(store.read(DASHBOARD)!!.stale && store.read(VEHICLES)!!.stale)
        store.remove(VEHICLES)
        assertNull(store.read(VEHICLES))
    }

    @Test
    fun anAnswerThatArrivesAfterThePullIsFresh() = runTest {
        val store = store()
        store.expire()
        store.write(DASHBOARD, "{}", clock.instant().plusSeconds(1), stale = false)
        assertFalse(store.read(DASHBOARD)!!.stale)
    }

    @Test
    fun aDifferentAnswerTellsItsModule() = runTest {
        val store = store()
        val heard = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { changes.changes.collect { heard += it } }
        store.changed(VEHICLES)
        store.changed(DASHBOARD)
        assertEquals(listOf("garage", "dashboard"), heard)
    }

    private class Sessions(session: Session) : SessionRepository {
        override val state = MutableStateFlow<SessionState>(SessionState.SignedIn(session))

        override suspend fun signIn(baseUrl: String, username: String, password: String): Outcome<Session> =
            error("not used")

        override suspend fun signOut() = Unit

        override suspend fun forgetServer() = Unit

        override suspend fun updateDisplayName(name: String) = Unit
    }

    private companion object {
        const val DASHBOARD = "api/v1/dashboard"
        const val VEHICLES = "api/v1/garage/vehicles?archived=false&limit=200"
        val henrique = Session("http://10.0.2.2:8001", "henrique", "Henrique", "secret")
    }
}
