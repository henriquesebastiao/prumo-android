package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.UnreachableCause
import java.time.Instant

/** The server's answer to a `GET`, kept on the device: [body] as it came, derived values included. */
data class CachedResponse(val body: String, val fetchedAt: Instant, val stale: Boolean)

/**
 * The answers of the session's server kept on the device (Room, in `:core:data`, D21), for the
 * signed-in account only. Paths are relative to the server's address, with the query:
 * `api/v1/garage/vehicles?archived=false&limit=200`.
 */
interface ResponseCache {
    fun read(path: String): CachedResponse?

    /** Keeps [body] for [path]; [stale] when a write happened while the request was out. */
    fun write(path: String, body: String, fetchedAt: Instant, stale: Boolean)

    /** The server no longer has [path] (`404`): its answer is forgotten. */
    fun remove(path: String)

    /** A write happened (or a pull to refresh): every answer kept asks the server again first. */
    fun expire()

    /** A check in the background brought a different answer for [path]: whoever shows it loads again. */
    fun changed(path: String)
}

/** Whether the session's server is answering, as the app's requests find it (the Home's cloud icon). */
interface ServerReachability {
    fun answered()

    fun unanswered(cause: UnreachableCause)
}
