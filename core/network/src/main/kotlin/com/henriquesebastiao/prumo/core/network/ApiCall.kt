package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.ErrorCodes
import com.henriquesebastiao.prumo.core.model.FieldError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.network.dto.ErrorResponseDto
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

/**
 * Runs a call to the server and turns what can go wrong into an [AppError], so no network exception
 * reaches a screen. Cancellation still propagates.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
suspend fun <T> apiCall(block: suspend () -> T): Outcome<T> = try {
    Outcome.Ok(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Outcome.Failed(e.toAppError())
} catch (e: IOException) {
    Outcome.Failed(AppError.Unreachable(unreachableCause(e)))
} catch (e: SerializationException) {
    Outcome.Failed(AppError.Unexpected())
} catch (e: IllegalArgumentException) {
    // Retrofit's converters wrap a body they can't read in IllegalArgumentException.
    Outcome.Failed(AppError.Unexpected())
}

/** The error of a failed response: the server's error format, or [AppError.Unexpected] without it. */
fun HttpException.toAppError(): AppError {
    val status = code()
    val raw = response()?.errorBody()?.string()
    val retryAfter = response()?.headers()?.get("Retry-After")?.trim()?.toLongOrNull()
    return parseError(status, raw, retryAfter)
}

/** The [AppError] for an error response with [status] and [body] (the server's error format). */
fun parseError(status: Int, body: String?, retryAfterSeconds: Long? = null): AppError {
    val error = body?.let {
        try {
            NetworkJson.decodeFromString(ErrorResponseDto.serializer(), it).error
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    } ?: return AppError.Unexpected(status)
    if (status == HTTP_UNAUTHORIZED && error.code in ErrorCodes.SESSION_ENDED) return AppError.SessionExpired
    return AppError.Api(
        status = status,
        code = error.code,
        message = error.message,
        details = error.details.orEmpty().mapValues { (_, value) -> value.asText() },
        fields = error.fields.orEmpty().map { FieldError(it.path, it.code, it.message) },
        retryAfterSeconds = retryAfterSeconds,
    )
}

/** What kind of "no answer" [e] is, so the screen can say what to check. */
fun unreachableCause(e: IOException): UnreachableCause {
    var cause: Throwable? = e
    while (cause != null) {
        when (cause) {
            is UnknownHostException -> return UnreachableCause.UnknownHost
            is ConnectException, is NoRouteToHostException -> return UnreachableCause.Refused
            is SSLException, is CertificateException -> return UnreachableCause.Certificate
            is InterruptedIOException -> return UnreachableCause.Timeout
        }
        cause = cause.cause
    }
    return UnreachableCause.Other
}

private fun JsonElement.asText(): String = if (this is JsonPrimitive) content else toString()

private const val HTTP_UNAUTHORIZED = 401
