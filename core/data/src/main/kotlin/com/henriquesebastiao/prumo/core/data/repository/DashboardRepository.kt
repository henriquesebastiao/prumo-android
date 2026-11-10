package com.henriquesebastiao.prumo.core.data.repository

import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.ResponseCache
import com.henriquesebastiao.prumo.core.network.di.IoDispatcher
import com.henriquesebastiao.prumo.core.network.toModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** The Home's data (`GET /dashboard`), with the time the server sent it ("Dados de hoje, 13:05"). */
data class LoadedDashboard(val dashboard: Dashboard, val loadedAt: Instant)

interface DashboardRepository {
    /** The last Home that loaded (from the device's copy when the server doesn't answer, D21). */
    val last: StateFlow<LoadedDashboard?>

    suspend fun refresh(): Outcome<LoadedDashboard>

    fun clear()
}

@Singleton
class DefaultDashboardRepository @Inject constructor(
    private val calls: ServerCalls,
    private val cache: ResponseCache,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DashboardRepository {
    private val mutableLast = MutableStateFlow<LoadedDashboard?>(null)
    override val last: StateFlow<LoadedDashboard?> = mutableLast.asStateFlow()

    override suspend fun refresh(): Outcome<LoadedDashboard> = when (val outcome = calls.system { dashboard() }) {
        is Outcome.Failed -> outcome

        is Outcome.Ok -> {
            // When the server sent it: now, or the time of the device's copy when it didn't answer.
            val sentAt = withContext(io) { cache.read(PATH)?.fetchedAt } ?: clock.instant()
            Outcome.Ok(LoadedDashboard(outcome.value.toModel(), sentAt)).onOk { mutableLast.value = it }
        }
    }

    override fun clear() {
        mutableLast.value = null
    }

    private companion object {
        const val PATH = "api/v1/dashboard"
    }
}
