package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.ServerAddress
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.network.di.IoDispatcher
import com.henriquesebastiao.prumo.core.network.di.ProbeClient
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Tests an address before the app signs in to it, with `GET /api/health` and no token (docs/decisoes.md,
 * D10). Each candidate base URL for what the user typed is tried in order; the first Prumo server
 * wins. The time the health took to answer is the "Resposta" tile of the server screen.
 */
class ServerProbe @Inject constructor(
    @ProbeClient private val client: OkHttpClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Tries each candidate for [input]. */
    suspend fun probe(input: String): ServerCheck {
        val candidates = ServerAddress.candidates(input)
        if (candidates.isEmpty()) return ServerCheck.InvalidAddress
        var best: ServerCheck? = null
        for (baseUrl in candidates) {
            when (val result = check(baseUrl)) {
                is ServerCheck.Found, is ServerCheck.DatabaseDown -> return result
                is ServerCheck.NotPrumo -> if (best !is ServerCheck.NotPrumo) best = result
                else -> if (best == null) best = result
            }
        }
        return best ?: ServerCheck.InvalidAddress
    }

    /** Checks exactly [baseUrl]. */
    suspend fun check(baseUrl: String): ServerCheck = withContext(io) {
        val request = Request.Builder().url("${baseUrl.trimEnd('/')}/$HEALTH_PATH").get().build()
        val started = System.nanoTime()
        try {
            client.newCall(request).execute().use { response ->
                val elapsed = (System.nanoTime() - started) / NANOS_PER_MILLI
                val health = response.body.string().let(::readHealth)
                when {
                    health == null -> ServerCheck.NotPrumo(baseUrl)
                    response.code == HTTP_OK -> ServerCheck.Found(baseUrl, health, elapsed)
                    response.code == HTTP_UNAVAILABLE -> ServerCheck.DatabaseDown(baseUrl, health)
                    else -> ServerCheck.NotPrumo(baseUrl)
                }
            }
        } catch (e: IOException) {
            ServerCheck.Unreachable(baseUrl, unreachableCause(e))
        } catch (_: IllegalArgumentException) {
            ServerCheck.InvalidAddress
        }
    }

    /**
     * The server's version when [body] is Prumo's health: `service: "prumo"`, or (servers older than
     * the field) no `service` and the other three fields with their values. Null for anything else.
     */
    private fun readHealth(body: String): String? {
        val json = try {
            NetworkJson.parseToJsonElement(body).jsonObject
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        val service = json.text("service")
        val version = json.text("version") ?: return null
        val shaped = json.text("status") in STATUSES && json.text("database") in DATABASES
        return version.takeIf { service == PRUMO || (service == null && shaped) }
    }

    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    private companion object {
        const val HEALTH_PATH = "api/health"
        const val PRUMO = "prumo"
        const val HTTP_OK = 200
        const val HTTP_UNAVAILABLE = 503
        const val NANOS_PER_MILLI = 1_000_000
        val STATUSES = setOf("ok", "degraded")
        val DATABASES = setOf("ok", "unavailable")
    }
}
