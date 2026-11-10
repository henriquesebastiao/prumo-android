package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.network.di.ApiClient
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * The screens read the server through the device (D21): the answer to each `GET` of the session's
 * server is kept in [ResponseCache], and the next request for it is answered from there at once. An
 * answer older than [FRESH] is checked in the background, and a different one tells the screens to
 * load again. After a write (or a pull to refresh) every answer is stale: the next request asks the
 * server first and falls back to the kept answer only when the server doesn't answer. Nothing is
 * computed or merged: what the screen gets is always a whole answer of the server.
 *
 * It also tells [ServerReachability] whether the server answered, for the Home's cloud icon.
 */
@Singleton
class CachingInterceptor @Inject constructor(
    private val cacheProvider: Provider<ResponseCache>,
    private val reachabilityProvider: Provider<ServerReachability>,
    private val gatewayProvider: Provider<SessionGateway>,
    @param:ApiClient private val clientProvider: Provider<OkHttpClient>,
    private val clock: Clock,
) : Interceptor {
    private val cache: ResponseCache get() = cacheProvider.get()
    private val reachability: ServerReachability get() = reachabilityProvider.get()

    /** Goes up with each write, so an answer that left before it isn't kept as fresh. */
    private val writes = AtomicLong()

    /** Paths being checked in the background, so a screen loading twice checks once. */
    private val checking = ConcurrentHashMap.newKeySet<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val baseUrl = gatewayProvider.get().activeSession?.baseUrl
        val path = baseUrl?.let { relativePath(request.url, it) } ?: return chain.proceed(request)
        return when {
            request.method != GET -> write(chain, request)
            !cacheable(path) -> reach(chain, request)
            else -> read(chain, request, path)
        }
    }

    private fun write(chain: Interceptor.Chain, request: Request): Response {
        val response = reach(chain, request)
        if (response.isSuccessful) {
            writes.incrementAndGet()
            cache.expire()
        }
        return response
    }

    /** Asks the server, telling [reachability] whether it answered. */
    private fun reach(chain: Interceptor.Chain, request: Request): Response {
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            reachability.unanswered(unreachableCause(e))
            throw e
        }
        if (response.code in
            GATEWAY_ERRORS
        ) {
            reachability.unanswered(UnreachableCause.Other)
        } else {
            reachability.answered()
        }
        return response
    }

    private fun read(chain: Interceptor.Chain, request: Request, path: String): Response {
        val inBackground = request.tag(Check::class.java) != null
        val kept = cache.read(path)
        val sent = clock.instant()
        if (kept != null && !kept.stale && !inBackground) {
            if (Duration.between(kept.fetchedAt, sent) > FRESH) check(request, path)
            return kept.toResponse(request)
        }
        // What the screen gets when the server doesn't answer; a check in the background has no screen.
        val fallback = kept.takeUnless { inBackground }
        val before = writes.get()
        val response = try {
            reach(chain, request)
        } catch (e: IOException) {
            return fallback?.toResponse(request) ?: throw e
        }
        return when {
            response.code in GATEWAY_ERRORS && fallback != null -> {
                response.close()
                fallback.toResponse(request)
            }

            response.code == HTTP_NOT_FOUND -> forget(response, path, kept, inBackground)

            response.code == HTTP_OK && response.isJson() -> keep(response, path, kept, sent, before, inBackground)

            else -> response
        }
    }

    /** The server no longer has [path]: its kept answer goes, and an open screen learns it. */
    private fun forget(response: Response, path: String, kept: CachedResponse?, inBackground: Boolean): Response {
        if (kept != null) {
            cache.remove(path)
            if (inBackground) cache.changed(path)
        }
        return response
    }

    private fun keep(
        response: Response,
        path: String,
        kept: CachedResponse?,
        sent: Instant,
        writesBefore: Long,
        inBackground: Boolean,
    ): Response {
        val type = response.body.contentType()
        val body = response.body.string()
        cache.write(path, body, sent, stale = writes.get() != writesBefore)
        if (inBackground && kept?.body != body) cache.changed(path)
        return response.newBuilder().body(body.toResponseBody(type)).build()
    }

    /** The same request again, out of the screen's way; its answer lands in the cache. */
    private fun check(request: Request, path: String) {
        if (!checking.add(path)) return
        val call = clientProvider.get().newCall(request.newBuilder().tag(Check::class.java, Check).build())
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    checking.remove(path)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                    checking.remove(path)
                }
            },
        )
    }

    private fun CachedResponse.toResponse(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(HTTP_OK)
        .message("OK")
        .header(CACHED_AT, fetchedAt.toString())
        .body(body.toResponseBody(JSON))
        .build()

    private fun Response.isJson(): Boolean =
        body.contentType()?.let { it.type == "application" && it.subtype == "json" } == true

    /** Marks the requests that check a kept answer in the background. */
    private object Check

    companion object {
        /** How long a kept answer is shown without asking the server again. */
        val FRESH: Duration = Duration.ofSeconds(30)

        /** Set on an answer that came from the device, with the time the server sent it. */
        const val CACHED_AT = "X-Prumo-Cached-At"

        private const val GET = "GET"
        private const val HTTP_OK = 200
        private const val HTTP_NOT_FOUND = 404

        /** A proxy (nginx) answering for an API that is down. */
        private val GATEWAY_ERRORS = 502..504
        private val JSON: MediaType = "application/json".toMediaType()

        /** Routes whose answers are never kept: sessions, the export and the sync cursors are live. */
        private val NOT_KEPT = listOf("api/v1/auth/", "api/v1/me/export", "api/v1/sync/")

        fun cacheable(path: String): Boolean = path.startsWith("api/v1/") && NOT_KEPT.none { path.startsWith(it) }

        /** [url] under the server at [baseUrl], as `api/v1/dashboard?x=1`; null for another server. */
        fun relativePath(url: HttpUrl, baseUrl: String): String? {
            if (!AuthInterceptor.belongsTo(url, baseUrl)) return null
            val basePath = baseUrl.toHttpUrlOrNull()?.encodedPath?.trimEnd('/').orEmpty()
            val path = url.encodedPath.removePrefix(basePath).trimStart('/')
            return url.encodedQuery?.let { "$path?$it" } ?: path
        }
    }
}
