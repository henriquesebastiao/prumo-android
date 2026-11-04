package com.henriquesebastiao.prumo.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.ErrorCodes
import com.henriquesebastiao.prumo.core.model.UnreachableCause

/**
 * The text the app shows for [error]: each known `code` has its own; an unknown one falls back to
 * the generic text of its status. [server] names the server in the "no answer" texts.
 */
@Composable
@ReadOnlyComposable
fun errorText(error: AppError, server: String? = null): String = when (error) {
    is AppError.Api -> apiErrorText(error)
    AppError.SessionExpired -> stringResource(R.string.ui_error_session_expired)
    is AppError.Unreachable -> unreachableText(error.cause, server ?: stringResource(R.string.ui_this_server))
    is AppError.Unexpected -> unexpectedText(error.status)
}

/** A short title for [error] ("Não foi possível conectar", "Algo deu errado"). */
@Composable
@ReadOnlyComposable
fun errorTitle(error: AppError): String = when (error) {
    is AppError.Unreachable -> stringResource(R.string.ui_unreachable_title)
    AppError.SessionExpired -> stringResource(R.string.ui_session_expired_title)
    else -> stringResource(R.string.ui_error_title)
}

/** What to check when [host] didn't answer, by [cause]. */
@Composable
@ReadOnlyComposable
fun unreachableText(cause: UnreachableCause, host: String): String = when (cause) {
    UnreachableCause.UnknownHost -> stringResource(R.string.ui_unreachable_unknown_host, host)
    UnreachableCause.Timeout -> stringResource(R.string.ui_unreachable_timeout, host)
    UnreachableCause.Refused -> stringResource(R.string.ui_unreachable_refused, host)
    UnreachableCause.Certificate -> stringResource(R.string.ui_unreachable_certificate, host)
    UnreachableCause.Other -> stringResource(R.string.ui_unreachable_other, host)
}

/** The cause in a few words, for a field's error ("Endereço não encontrado"). */
@Composable
@ReadOnlyComposable
fun unreachableShort(cause: UnreachableCause): String = when (cause) {
    UnreachableCause.UnknownHost -> stringResource(R.string.ui_unreachable_short_unknown_host)
    UnreachableCause.Timeout -> stringResource(R.string.ui_unreachable_short_timeout)
    UnreachableCause.Refused -> stringResource(R.string.ui_unreachable_short_refused)
    UnreachableCause.Certificate -> stringResource(R.string.ui_unreachable_short_certificate)
    UnreachableCause.Other -> stringResource(R.string.ui_unreachable_short_other)
}

@Composable
@ReadOnlyComposable
private fun unexpectedText(status: Int?): String = if (status != null && status >= SERVER_ERROR) {
    stringResource(R.string.ui_error_generic, status)
} else {
    stringResource(R.string.ui_error_unexpected)
}

@Composable
@ReadOnlyComposable
private fun apiErrorText(error: AppError.Api): String = when (error.code) {
    ErrorCodes.AUTH_INVALID_CREDENTIALS -> stringResource(R.string.ui_error_invalid_credentials)

    ErrorCodes.AUTH_RATE_LIMITED -> {
        val minutes = ((error.retryAfterSeconds ?: DEFAULT_WAIT_SECONDS) + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
        pluralStringResource(R.plurals.ui_error_login_blocked, minutes.toInt(), minutes.toInt())
    }

    ErrorCodes.AUTH_WRONG_PASSWORD -> stringResource(R.string.ui_error_wrong_password)

    ErrorCodes.USERS_PASSWORD_TOO_SHORT ->
        stringResource(R.string.ui_error_password_too_short, error.details["min_length"] ?: MIN_PASSWORD)

    ErrorCodes.USERS_PASSWORD_TOO_LONG ->
        stringResource(R.string.ui_error_password_too_long, error.details["max_length"] ?: MAX_PASSWORD)

    ErrorCodes.USERS_INVALID_NAME -> stringResource(R.string.ui_error_invalid_name)

    ErrorCodes.VALIDATION -> stringResource(R.string.ui_error_validation)

    ErrorCodes.NOT_FOUND -> stringResource(R.string.ui_error_not_found)

    ErrorCodes.CONFLICT, ErrorCodes.ID_CONFLICT -> stringResource(R.string.ui_error_conflict)

    ErrorCodes.FORBIDDEN -> stringResource(R.string.ui_error_forbidden)

    ErrorCodes.RATE_LIMITED -> stringResource(R.string.ui_error_rate_limited)

    ErrorCodes.AUTH_REQUIRED, ErrorCodes.AUTH_SESSION_EXPIRED -> stringResource(R.string.ui_error_session_expired)

    else -> stringResource(R.string.ui_error_generic, error.status)
}

private const val SERVER_ERROR = 500
private const val DEFAULT_WAIT_SECONDS = 900L
private const val SECONDS_PER_MINUTE = 60L
private const val MIN_PASSWORD = "8"
private const val MAX_PASSWORD = "128"
