package com.henriquesebastiao.prumo.core.network.testing

import com.henriquesebastiao.prumo.core.network.CachedResponse
import com.henriquesebastiao.prumo.core.network.ResponseCache
import java.time.Instant

/** A device that keeps nothing: every read goes to the server (tests of the calls themselves). */
object NoCache : ResponseCache {
    override fun read(path: String): CachedResponse? = null

    override fun write(path: String, body: String, fetchedAt: Instant, stale: Boolean) = Unit

    override fun remove(path: String) = Unit

    override fun expire() = Unit

    override fun changed(path: String) = Unit
}
