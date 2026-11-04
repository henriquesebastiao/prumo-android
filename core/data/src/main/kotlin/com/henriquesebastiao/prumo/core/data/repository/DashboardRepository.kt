package com.henriquesebastiao.prumo.core.data.repository

import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.toModel
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The Home's data (`GET /dashboard`), with the time it was loaded ("Dados de hoje, 13:05"). */
data class LoadedDashboard(val dashboard: Dashboard, val loadedAt: Instant)

interface DashboardRepository {
    /** The last Home that loaded, kept while the app runs (the Fase 4 cache keeps it on disk). */
    val last: StateFlow<LoadedDashboard?>

    suspend fun refresh(): Outcome<LoadedDashboard>

    fun clear()
}

@Singleton
class DefaultDashboardRepository @Inject constructor(private val calls: ServerCalls) : DashboardRepository {
    private val mutableLast = MutableStateFlow<LoadedDashboard?>(null)
    override val last: StateFlow<LoadedDashboard?> = mutableLast.asStateFlow()

    override suspend fun refresh(): Outcome<LoadedDashboard> = calls.system { dashboard() }
        .map { LoadedDashboard(it.toModel(), Instant.now()) }
        .onOk { mutableLast.value = it }

    override fun clear() {
        mutableLast.value = null
    }
}
