package com.henriquesebastiao.prumo.core.data.repository

import com.henriquesebastiao.prumo.core.data.di.ApplicationScope
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.database.ResponseDao
import com.henriquesebastiao.prumo.core.database.ResponseEntity
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.network.CachedResponse
import com.henriquesebastiao.prumo.core.network.ResponseCache
import com.henriquesebastiao.prumo.core.network.ServerReachability
import com.henriquesebastiao.prumo.core.network.di.IoDispatcher
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The server's answers kept in Room (D21), for the signed-in account only. Signing in as another
 * user (or on another server) forgets the previous account's answers; "Sair" and "Trocar servidor"
 * forget them all. A session that expired keeps them: the same user signs in again.
 */
@Singleton
class ResponseStore @Inject constructor(
    private val dao: ResponseDao,
    private val sessions: SessionRepository,
    private val changes: DataChanges,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ResponseCache {
    /**
     * When everything was last made stale, in epoch milliseconds: [expire] is called on the main
     * thread (a pull to refresh), so the answers kept before it count as stale at once, while the
     * table catches up in the background.
     */
    @Volatile
    private var expiredAt = 0L

    init {
        scope.launch(io) {
            sessions.state.collect { state ->
                when {
                    state is SessionState.SignedIn -> dao.keepOnly(ownerOf(state))
                    state is SessionState.SignedOut && state.forgetsData() -> dao.clear()
                }
            }
        }
    }

    private val owner: String?
        get() = (sessions.state.value as? SessionState.SignedIn)?.let(::ownerOf)

    /** On a background thread (the interceptor's, or the caller's I/O dispatcher). */
    override fun read(path: String): CachedResponse? = owner?.let { dao.get(it, path) }?.let {
        CachedResponse(it.body, Instant.ofEpochMilli(it.fetchedAt), it.stale || it.fetchedAt < expiredAt)
    }

    override fun write(path: String, body: String, fetchedAt: Instant, stale: Boolean) {
        val owner = owner ?: return
        dao.put(ResponseEntity(owner, path, body, fetchedAt.toEpochMilli(), stale))
    }

    override fun remove(path: String) {
        owner?.let { dao.remove(it, path) }
    }

    override fun expire() {
        val owner = owner ?: return
        expiredAt = clock.millis()
        scope.launch(io) { dao.expire(owner) }
    }

    /** `api/v1/garage/vehicles/…` changed: Veículos' open screens and the Home load again. */
    override fun changed(path: String) {
        changes.changed(path.removePrefix(API).substringBefore('/').substringBefore('?'))
    }

    private companion object {
        const val API = "api/v1/"

        fun ownerOf(state: SessionState.SignedIn) = "${state.session.username}@${state.session.baseUrl}"

        /** "Sair" and "Trocar servidor"; not an expired session, nor a start without one. */
        fun SessionState.SignedOut.forgetsData() =
            reason == SignOutReason.SignedOut || (reason == SignOutReason.None && lastServer == null)
    }
}

/** Whether the session's server answers, as the app's last request found it. */
sealed interface ServerStatus {
    data object Answering : ServerStatus

    data class NotAnswering(val cause: UnreachableCause) : ServerStatus
}

/** The Home's cloud icon: [status] follows every request to the session's server. */
@Singleton
class ServerStatusRepository @Inject constructor() : ServerReachability {
    private val mutableStatus = MutableStateFlow<ServerStatus>(ServerStatus.Answering)
    val status: StateFlow<ServerStatus> = mutableStatus.asStateFlow()

    override fun answered() {
        mutableStatus.value = ServerStatus.Answering
    }

    override fun unanswered(cause: UnreachableCause) {
        mutableStatus.value = ServerStatus.NotAnswering(cause)
    }
}
