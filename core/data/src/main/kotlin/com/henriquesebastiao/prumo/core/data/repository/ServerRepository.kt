package com.henriquesebastiao.prumo.core.data.repository

import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.network.ServerProbe
import javax.inject.Inject

/** Testing a server's address (first run, Settings › Servidor). */
interface ServerRepository {
    /** Tries what the user typed (`GET /api/health` on each candidate address). */
    suspend fun check(address: String): ServerCheck
}

class DefaultServerRepository @Inject constructor(private val probe: ServerProbe) : ServerRepository {
    override suspend fun check(address: String): ServerCheck = probe.probe(address)
}
