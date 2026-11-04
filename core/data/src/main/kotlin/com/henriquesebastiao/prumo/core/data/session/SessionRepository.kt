package com.henriquesebastiao.prumo.core.data.session

import com.henriquesebastiao.prumo.core.data.di.ApplicationScope
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.ActiveSession
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.SessionGateway
import com.henriquesebastiao.prumo.core.network.apiCall
import com.henriquesebastiao.prumo.core.network.dto.LoginRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Signing in and out of a Prumo server, and the session the rest of the app runs on. */
interface SessionRepository {
    val state: StateFlow<SessionState>

    /** `POST /auth/login` with `client: "app"` and this device's name; keeps the token encrypted. */
    suspend fun signIn(baseUrl: String, username: String, password: String): Outcome<Session>

    /** `POST /auth/logout` (a failure doesn't stop it) and the token is forgotten. */
    suspend fun signOut()

    /** Signs out and forgets the server too: the first run starts again (Trocar servidor). */
    suspend fun forgetServer()

    /** The user changed their name: the session shows the new one. */
    suspend fun updateDisplayName(name: String)
}

@Singleton
class DefaultSessionRepository @Inject constructor(
    private val store: SessionStore,
    private val apis: ApiFactory,
    private val device: DeviceInfo,
    @ApplicationScope private val scope: CoroutineScope,
) : SessionRepository,
    SessionGateway {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Loading)
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        scope.launch { mutableState.value = store.read() }
    }

    private val signedIn: Session? get() = (state.value as? SessionState.SignedIn)?.session

    override val activeSession: ActiveSession?
        get() = signedIn?.let { ActiveSession(it.baseUrl, it.token) }

    override fun onUnauthorized(token: String) {
        var ended: Session? = null
        mutableState.update { current ->
            val session = (current as? SessionState.SignedIn)?.session
            if (session?.token == token) {
                ended = session
                SessionState.SignedOut(SignOutReason.Expired, session.baseUrl, session.username)
            } else {
                current
            }
        }
        if (ended != null) scope.launch { store.clearToken() }
    }

    override suspend fun signIn(baseUrl: String, username: String, password: String): Outcome<Session> {
        val request = LoginRequestDto(username = username.trim(), password = password, deviceName = device.name())
        return when (val outcome = apiCall { apis.system(baseUrl).login(request) }) {
            is Outcome.Failed -> outcome

            is Outcome.Ok -> {
                val token = outcome.value.token ?: return Outcome.Failed(AppError.Unexpected())
                val user = outcome.value.user
                val session = Session(baseUrl, user.username, user.displayName, token)
                store.save(session)
                mutableState.value = SessionState.SignedIn(session)
                Outcome.Ok(session)
            }
        }
    }

    override suspend fun signOut() {
        val session = signedIn ?: return
        apiCall { apis.system(session.baseUrl).logout() }
        store.clearToken()
        mutableState.value = SessionState.SignedOut(SignOutReason.SignedOut, session.baseUrl, session.username)
    }

    override suspend fun forgetServer() {
        signedIn?.let { session -> apiCall { apis.system(session.baseUrl).logout() } }
        store.clear()
        mutableState.value = SessionState.SignedOut(SignOutReason.None, null, null)
    }

    override suspend fun updateDisplayName(name: String) {
        val session = signedIn ?: return
        val updated = session.copy(displayName = name)
        store.save(updated)
        mutableState.value = SessionState.SignedIn(updated)
    }
}
