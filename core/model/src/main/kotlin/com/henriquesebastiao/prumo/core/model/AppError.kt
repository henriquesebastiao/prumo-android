package com.henriquesebastiao.prumo.core.model

/**
 * Why a call to the server failed, as the screens need to tell it apart. The text shown for each
 * is the app's (`errorText` in `:core:ui`); the server's `message` is only for debugging.
 */
sealed interface AppError {
    /**
     * The server answered with its error format: `{"error": {"code", "message", "details", "fields"}}`.
     * [code] is stable (`auth.invalid_credentials`, `garage.odometer_inconsistent`); [details] holds
     * its values as text; [retryAfterSeconds] comes from `Retry-After` (`429`).
     */
    data class Api(
        val status: Int,
        val code: String,
        val message: String = "",
        val details: Map<String, String> = emptyMap(),
        val fields: List<FieldError> = emptyList(),
        val retryAfterSeconds: Long? = null,
    ) : AppError

    /** `401` to a signed-in request: the session ended (revoked, expired). It is cleared already. */
    data object SessionExpired : AppError

    /** No answer: no connection, the name doesn't resolve, refused, timed out or a TLS failure. */
    data class Unreachable(val cause: UnreachableCause = UnreachableCause.Other) : AppError

    /** An answer that isn't the Prumo API's (an HTML page, a `5xx` without the error format). */
    data class Unexpected(val status: Int? = null) : AppError
}

/** Why the server didn't answer, when it can be told: each one says what to check. */
enum class UnreachableCause { UnknownHost, Timeout, Refused, Certificate, Other }

/** One invalid field of a `validation_error`: [path] like `items.0.unit_price_cents`. */
data class FieldError(val path: String, val code: String, val message: String = "")
