package com.henriquesebastiao.prumo.core.data.repository

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.PrumoApi
import com.henriquesebastiao.prumo.core.network.ResponseCache
import com.henriquesebastiao.prumo.core.network.SessionGateway
import com.henriquesebastiao.prumo.core.network.apiCall
import javax.inject.Inject

/**
 * Calls to the signed-in server. Without a session the call fails as [AppError.SessionExpired]
 * without going out. Modules call their own Retrofit interfaces with [module]. Reads may be answered
 * from the device's copy of the server's answers (D21); [expireCache] makes the next ones ask first.
 */
class ServerCalls @Inject constructor(
    private val apis: ApiFactory,
    private val gateway: SessionGateway,
    private val cache: ResponseCache,
) {
    /** The base URL of the session's server, or null when signed out. */
    val baseUrl: String? get() = gateway.activeSession?.baseUrl

    /** Pull to refresh: every kept answer is stale, so the next reads ask the server first. */
    fun expireCache() = cache.expire()

    suspend fun <T> system(block: suspend PrumoApi.() -> T): Outcome<T> = module(PrumoApi::class.java, block)

    suspend fun <S, T> module(service: Class<S>, block: suspend S.() -> T): Outcome<T> {
        val session = gateway.activeSession ?: return Outcome.Failed(AppError.SessionExpired)
        return apiCall { apis.create(session.baseUrl, service).block() }
    }
}
