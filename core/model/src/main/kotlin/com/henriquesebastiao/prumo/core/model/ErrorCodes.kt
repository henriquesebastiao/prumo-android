package com.henriquesebastiao.prumo.core.model

/** The error codes of the server (`docs/api.md`, "Erros") that the system screens handle. */
object ErrorCodes {
    const val VALIDATION = "validation_error"
    const val NOT_FOUND = "not_found"
    const val CONFLICT = "conflict"
    const val ID_CONFLICT = "id_conflict"
    const val FORBIDDEN = "forbidden"
    const val RATE_LIMITED = "rate_limited"

    const val AUTH_REQUIRED = "auth.required"
    const val AUTH_SESSION_EXPIRED = "auth.session_expired"
    const val AUTH_INVALID_CREDENTIALS = "auth.invalid_credentials"
    const val AUTH_RATE_LIMITED = "auth.rate_limited"
    const val AUTH_WRONG_PASSWORD = "auth.wrong_password"
    const val AUTH_CLIENT_HEADER_REQUIRED = "auth.client_header_required"

    const val USERS_INVALID_NAME = "users.invalid_name"
    const val USERS_INVALID_USERNAME = "users.invalid_username"
    const val USERS_PASSWORD_TOO_SHORT = "users.password_too_short"
    const val USERS_PASSWORD_TOO_LONG = "users.password_too_long"
    const val USERS_USERNAME_TAKEN = "users.username_taken"

    const val SYNC_SINCE_TOO_OLD = "sync.since_too_old"

    /** The `401` codes that mean the session is over (not a wrong password). */
    val SESSION_ENDED = setOf(AUTH_REQUIRED, AUTH_SESSION_EXPIRED)
}
