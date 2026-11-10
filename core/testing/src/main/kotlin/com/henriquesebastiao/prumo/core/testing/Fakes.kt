package com.henriquesebastiao.prumo.core.testing

import android.net.Uri
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DashboardRepository
import com.henriquesebastiao.prumo.core.data.repository.DataExportRepository
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.data.repository.ExportResult
import com.henriquesebastiao.prumo.core.data.repository.LoadedDashboard
import com.henriquesebastiao.prumo.core.data.repository.ServerRepository
import com.henriquesebastiao.prumo.core.data.session.Session
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.model.DevicePreferences
import com.henriquesebastiao.prumo.core.model.DeviceSession
import com.henriquesebastiao.prumo.core.model.InstalledModule
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.model.SessionClient
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.core.model.User
import com.henriquesebastiao.prumo.core.network.CachedResponse
import com.henriquesebastiao.prumo.core.network.ResponseCache
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// In-memory stand-ins for :core:data's repositories, for ViewModel and screen tests.

val TestSession = Session("http://10.0.2.2:8000", "marina", "Marina Costa", "token")

class FakeSessionRepository(initial: SessionState = SessionState.SignedIn(TestSession)) : SessionRepository {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    /** What the next sign-in answers; null signs in. */
    var signInError: AppError? = null
    val signIns = mutableListOf<Triple<String, String, String>>()

    override suspend fun signIn(baseUrl: String, username: String, password: String): Outcome<Session> {
        signIns += Triple(baseUrl, username, password)
        signInError?.let { return Outcome.Failed(it) }
        val session = Session(baseUrl, username, username, "token")
        mutableState.value = SessionState.SignedIn(session)
        return Outcome.Ok(session)
    }

    override suspend fun signOut() {
        val session = (state.value as? SessionState.SignedIn)?.session
        mutableState.value = SessionState.SignedOut(SignOutReason.SignedOut, session?.baseUrl, session?.username)
    }

    override suspend fun forgetServer() {
        mutableState.value = SessionState.SignedOut(SignOutReason.None, null, null)
    }

    override suspend fun updateDisplayName(name: String) = Unit

    /** The server refused the token. */
    fun expire() {
        val session = (state.value as? SessionState.SignedIn)?.session
        mutableState.value = SessionState.SignedOut(SignOutReason.Expired, session?.baseUrl, session?.username)
    }
}

class FakeAccountRepository(
    preferences: Preferences =
        Preferences(ThemeMode.System, "America/Sao_Paulo", "BRL", listOf("finance", "garage", "reading")),
    user: User = User("1", "marina", "Marina Costa", isAdmin = false),
) : AccountRepository {
    private val serverPreferences = MutableStateFlow(preferences)
    private val mutableUser = MutableStateFlow<User?>(user)
    private val mutablePreferences = MutableStateFlow<Preferences?>(null)
    override val user: StateFlow<User?> = mutableUser.asStateFlow()
    override val preferences: StateFlow<Preferences?> = mutablePreferences.asStateFlow()

    /** Makes every call fail with this error, as a server that went away. */
    var failure: AppError? = null
    var modules = listOf(
        InstalledModule("finance", "0.1.0", true, 0),
        InstalledModule("garage", "0.1.0", true, 1),
        InstalledModule("reading", "0.1.0", true, 2),
    )
    var sessions = listOf(
        DeviceSession("1", SessionClient.App, "Pixel 9", true, Instant.EPOCH, Instant.MAX),
    )
    val revoked = mutableListOf<String>()

    private fun <T> answer(value: () -> T): Outcome<T> = failure?.let { Outcome.Failed(it) } ?: Outcome.Ok(value())

    override suspend fun refreshUser(): Outcome<User> = answer { mutableUser.value!! }

    override suspend fun refreshPreferences(): Outcome<Preferences> =
        answer { serverPreferences.value }.also { if (it is Outcome.Ok) mutablePreferences.value = it.value }

    override suspend fun updateName(name: String): Outcome<User> =
        answer { mutableUser.value!!.copy(displayName = name) }.also {
            if (it is Outcome.Ok) {
                mutableUser.value =
                    it.value
            }
        }

    override suspend fun changePassword(current: String, new: String): Outcome<Unit> = answer { }

    override suspend fun setTheme(theme: ThemeMode): Outcome<Preferences> = update { it.copy(theme = theme) }

    override suspend fun setEnabledModules(ids: List<String>): Outcome<Preferences> = update {
        it.copy(enabledModules = ids)
    }

    private fun update(change: (Preferences) -> Preferences): Outcome<Preferences> = answer {
        change(serverPreferences.value).also {
            serverPreferences.value = it
            mutablePreferences.value = it
        }
    }

    override suspend fun modules(): Outcome<List<InstalledModule>> = answer { modules }

    override suspend fun sessions(): Outcome<List<DeviceSession>> = answer { sessions }

    override suspend fun revokeSession(id: String): Outcome<Unit> = answer { revoked += id }

    override fun clear() {
        mutablePreferences.value = null
    }
}

class FakeDashboardRepository(var dashboard: Dashboard = Dashboard(emptyList(), emptyList(), emptyList())) :
    DashboardRepository {
    private val mutableLast = MutableStateFlow<LoadedDashboard?>(null)
    override val last: StateFlow<LoadedDashboard?> = mutableLast.asStateFlow()
    var failure: AppError? = null

    override suspend fun refresh(): Outcome<LoadedDashboard> {
        failure?.let { return Outcome.Failed(it) }
        val loaded = LoadedDashboard(dashboard, Instant.parse("2026-11-17T16:05:00Z"))
        mutableLast.value = loaded
        return Outcome.Ok(loaded)
    }

    override fun clear() {
        mutableLast.value = null
    }
}

class FakeDataExportRepository(var result: ExportResult = ExportResult.Saved) : DataExportRepository {
    val exported = mutableListOf<Uri>()

    override suspend fun export(target: Uri): ExportResult {
        exported += target
        return result
    }
}

class FakeServerRepository(
    var answer: (String) -> ServerCheck = {
        ServerCheck.Found("http://$it", "0.1.0", 42)
    },
) : ServerRepository {
    val checked = mutableListOf<String>()

    override suspend fun check(address: String): ServerCheck {
        checked += address
        return answer(address)
    }
}

class FakeDevicePreferences(initial: DevicePreferences = DevicePreferences()) : DevicePreferencesRepository {
    private val mutable = MutableStateFlow(initial)
    override val preferences: StateFlow<DevicePreferences> = mutable.asStateFlow()

    override suspend fun setPureBlack(enabled: Boolean) {
        mutable.value = mutable.value.copy(pureBlack = enabled)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        mutable.value = mutable.value.copy(dynamicColor = enabled)
    }

    override suspend fun setPinnedActions(ids: List<String>) {
        mutable.value = mutable.value.copy(pinnedActions = ids)
    }
}

/** The device's copy of the server's answers, in memory; counts each [expire] (a pull to refresh). */
class FakeResponseCache : ResponseCache {
    val responses = mutableMapOf<String, CachedResponse>()
    var expired = 0
    val changedPaths = mutableListOf<String>()

    override fun read(path: String): CachedResponse? = responses[path]

    override fun write(path: String, body: String, fetchedAt: Instant, stale: Boolean) {
        responses[path] = CachedResponse(body, fetchedAt, stale)
    }

    override fun remove(path: String) {
        responses -= path
    }

    override fun expire() {
        expired++
        responses.replaceAll { _, response -> response.copy(stale = true) }
    }

    override fun changed(path: String) {
        changedPaths += path
    }
}
